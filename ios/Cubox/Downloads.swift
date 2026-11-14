import SwiftUI

struct SavedVideo: Codable, Identifiable {
    let id: UUID
    let title: String
    let filename: String
}

@MainActor final class Downloads: ObservableObject {
    static let shared = Downloads()
    @Published var videos: [SavedVideo] = []
    @Published var active: [UUID: String] = [:]
    @Published var error: String?
    private var tasks: [UUID: Task<Void, Never>] = [:]
    private let directory: URL
    private var index: URL { directory.appendingPathComponent("index.json") }
    init() {
        directory = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0].appendingPathComponent("Downloads", isDirectory: true)
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        var location = directory
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        try? location.setResourceValues(values)
        if let data = try? Data(contentsOf: index), let saved = try? JSONDecoder().decode([SavedVideo].self, from: data) {
            videos = saved.filter { FileManager.default.fileExists(atPath: directory.appendingPathComponent($0.filename).path) }
        }
    }
    func url(_ video: SavedVideo) -> URL { directory.appendingPathComponent(video.filename) }
    private func persist() throws { try JSONEncoder().encode(videos).write(to: index, options: .atomic) }
    func start(source: Source, title: String) {
        guard let url = URL(string: source.url), ["https", "http"].contains(url.scheme ?? ""),
              !["m3u8", "m3u", "mpd"].contains(url.pathExtension.lowercased()) else {
            error = "Offline downloads currently support direct video files. Choose a direct-file server."; return
        }
        let id = UUID()
        active[id] = title
        tasks[id] = Task {
            defer { active[id] = nil; tasks[id] = nil }
            do {
                var request = URLRequest(url: url)
                source.httpHeaders.forEach { request.setValue($0.value, forHTTPHeaderField: $0.key) }
                let (temporary, response) = try await URLSession.shared.download(for: request)
                defer { try? FileManager.default.removeItem(at: temporary) }
                try Task.checkCancellation()
                guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode),
                      !(response.mimeType ?? "").contains("text"),
                      !(response.mimeType ?? "").contains("mpegurl"),
                      !(response.mimeType ?? "").contains("dash+xml") else {
                    throw CuboxError(message: "The server did not return a video file.")
                }
                let ext = ["mp4", "mov", "m4v", "mkv", "webm"].contains(url.pathExtension.lowercased()) ? url.pathExtension.lowercased() : "mp4"
                let saved = SavedVideo(id: id, title: title, filename: "\(id.uuidString).\(ext)")
                try FileManager.default.moveItem(at: temporary, to: self.url(saved))
                videos.append(saved)
                do { try persist() } catch {
                    videos.removeAll { $0.id == id }
                    try? FileManager.default.removeItem(at: self.url(saved))
                    throw error
                }
            } catch {
                if !Task.isCancelled { self.error = error.localizedDescription }
            }
        }
    }
    func cancel(_ id: UUID) { tasks[id]?.cancel() }
    func remove(_ video: SavedVideo) {
        do {
            try FileManager.default.removeItem(at: url(video))
            videos.removeAll { $0.id == video.id }
            try persist()
        } catch { self.error = error.localizedDescription }
    }
}

struct DownloadsScreen: View {
    @ObservedObject private var store = Downloads.shared
    @State private var playing: Playback?
    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text("Keep cubox open while downloading. Background transfers and resume are not available in this first iPhone build.").font(.footnote).foregroundStyle(.secondary)
                }
                ForEach(store.active.keys.sorted(by: { $0.uuidString < $1.uuidString }), id: \.self) { id in
                    HStack { ProgressView(); Text(store.active[id] ?? "Downloading"); Spacer(); Button("Cancel") { store.cancel(id) } }
                }
                ForEach(store.videos) { video in
                    Button { playing = Playback(title: video.title, source: Source(url: store.url(video).absoluteString, headers: [])) } label: {
                        Label(video.title, systemImage: "play.circle.fill")
                    }.swipeActions { Button("Delete", role: .destructive) { store.remove(video) } }
                }
                if store.videos.isEmpty && store.active.isEmpty { Text("Your downloaded videos will appear here.").foregroundStyle(.secondary) }
            }.navigationTitle("Downloads")
        }.fullScreenCover(item: $playing) { PlayerScreen(playback: $0) }
            .alert("Download", isPresented: Binding(get: { store.error != nil }, set: { if !$0 { store.error = nil } })) { Button("OK") { store.error = nil } } message: { Text(store.error ?? "") }
    }
}
