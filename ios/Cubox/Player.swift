import SwiftUI
import AVKit
import UniformTypeIdentifiers

struct Playback: Identifiable {
    let id = UUID()
    let title: String
    let source: Source
}

// Public AVAssetResourceLoader API forwards required provider headers for progressive files.
// Manifest streams with custom headers require a separate HLS integration.
final class HeaderResourceLoader: NSObject, AVAssetResourceLoaderDelegate {
    let source: Source
    private var tasks: [ObjectIdentifier: Task<Void, Never>] = [:]
    init(source: Source) { self.source = source }
    func resourceLoader(_ loader: AVAssetResourceLoader, shouldWaitForLoadingOfRequestedResource request: AVAssetResourceLoadingRequest) -> Bool {
        let key = ObjectIdentifier(request)
        tasks[key] = Task { @MainActor in
            defer { tasks[key] = nil }
            do {
                guard let url = URL(string: source.url) else { throw CuboxError(message: "Invalid video URL") }
                var network = URLRequest(url: url)
                source.httpHeaders.forEach { network.setValue($0.value, forHTTPHeaderField: $0.key) }
                let offset = request.dataRequest.map { max($0.requestedOffset, $0.currentOffset) } ?? 0
                let length = request.dataRequest?.requestedLength ?? 2
                let all = request.dataRequest?.requestsAllDataToEndOfResource ?? false
                network.setValue(all ? "bytes=\(offset)-" : "bytes=\(offset)-\(offset + Int64(max(1, length)) - 1)", forHTTPHeaderField: "Range")
                let (bytes, response) = try await URLSession.shared.bytes(for: network)
                guard let http = response as? HTTPURLResponse, [200, 206].contains(http.statusCode) else {
                    throw CuboxError(message: "This server rejected playback. Try another server.")
                }
                let total = http.value(forHTTPHeaderField: "Content-Range")?.split(separator: "/").last.flatMap { Int64($0) }
                if let info = request.contentInformationRequest {
                    info.contentLength = total ?? response.expectedContentLength
                    info.isByteRangeAccessSupported = http.statusCode == 206
                    info.contentType = UTType(mimeType: response.mimeType ?? "video/mp4")?.identifier ?? UTType.mpeg4Movie.identifier
                }
                // Refuse huge offset skipping on servers that ignore byte ranges.
                guard offset == 0 || http.statusCode == 206 else { throw CuboxError(message: "This server does not support seeking. Try another server.") }
                var buffer = Data()
                var received: Int64 = 0
                if request.dataRequest != nil {
                    for try await byte in bytes {
                        try Task.checkCancellation()
                        buffer.append(byte)
                        received += 1
                        if buffer.count >= 64 * 1024 {
                            request.dataRequest?.respond(with: buffer)
                            buffer.removeAll(keepingCapacity: true)
                        }
                        if !all && received >= Int64(length) { break }
                    }
                    if !buffer.isEmpty { request.dataRequest?.respond(with: buffer) }
                }
                if !request.isCancelled { request.finishLoading() }
            } catch {
                if !request.isCancelled { request.finishLoading(with: error) }
            }
        }
        return true
    }
    func resourceLoader(_ loader: AVAssetResourceLoader, didCancel request: AVAssetResourceLoadingRequest) {
        tasks.removeValue(forKey: ObjectIdentifier(request))?.cancel()
    }
    func cancel() { tasks.values.forEach { $0.cancel() }; tasks.removeAll() }
}

@MainActor final class PlayerModel: ObservableObject {
    let player = AVPlayer()
    @Published var error: String?
    private var loader: HeaderResourceLoader?
    private var observation: NSKeyValueObservation?
    init(_ playback: Playback) {
        do {
            guard let url = URL(string: playback.source.url) else { throw CuboxError(message: "Invalid video URL") }
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .moviePlayback)
            try AVAudioSession.sharedInstance().setActive(true)
            let asset: AVURLAsset
            if playback.source.headers.isEmpty || url.isFileURL {
                asset = AVURLAsset(url: url)
            } else {
                guard !url.pathExtension.lowercased().contains("m3u") else {
                    throw CuboxError(message: "This HLS server requires headers that this first iPhone build does not support. Choose a direct-file server.")
                }
                var components = URLComponents(url: url, resolvingAgainstBaseURL: false)!
                components.scheme = "cubox-media"
                guard let custom = components.url else { throw CuboxError(message: "Invalid video URL") }
                asset = AVURLAsset(url: custom)
                let delegate = HeaderResourceLoader(source: playback.source)
                loader = delegate
                asset.resourceLoader.setDelegate(delegate, queue: .main)
            }
            let item = AVPlayerItem(asset: asset)
            observation = item.observe(\.status, options: [.new]) { [weak self] item, _ in
                if item.status == .failed {
                    Task { @MainActor in self?.error = item.error?.localizedDescription ?? "Video format unsupported. Try another server." }
                }
            }
            player.replaceCurrentItem(with: item)
            player.play()
        } catch { self.error = error.localizedDescription }
    }
    func stop() {
        player.pause()
        player.replaceCurrentItem(with: nil)
        loader?.cancel()
        observation = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
}

struct NativePlayer: UIViewControllerRepresentable {
    let player: AVPlayer
    func makeUIViewController(context: Context) -> AVPlayerViewController {
        let controller = AVPlayerViewController()
        controller.player = player
        controller.view.backgroundColor = .black
        controller.videoGravity = .resizeAspect
        controller.allowsPictureInPicturePlayback = true
        return controller
    }
    func updateUIViewController(_ controller: AVPlayerViewController, context: Context) {}
}

struct PlayerScreen: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var model: PlayerModel
    let playback: Playback
    init(playback: Playback) {
        self.playback = playback
        _model = StateObject(wrappedValue: PlayerModel(playback))
    }
    var body: some View {
        ZStack(alignment: .topLeading) {
            Color.black.ignoresSafeArea()
            NativePlayer(player: model.player).ignoresSafeArea()
            Button { dismiss() } label: { Label("Close", systemImage: "xmark.circle.fill").padding(12).background(.black.opacity(0.75), in: Capsule()) }.padding()
            if let error = model.error {
                VStack(spacing: 16) { Text("Unable to play").font(.headline); Text(error); Button("Go back") { dismiss() } }
                    .padding(24).background(.black).frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }.onDisappear { model.stop() }
    }
}
