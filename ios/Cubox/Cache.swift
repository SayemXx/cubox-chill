import SwiftUI
import CryptoKit

actor DiskCache {
    static let artwork = DiskCache(name: "Artwork", limit: 64 * 1024 * 1024)
    static let catalog = DiskCache(name: "Catalog", limit: 4 * 1024 * 1024)
    private let directory: URL
    private let limit: Int
    init(name: String, limit: Int) {
        directory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
            .appendingPathComponent(name, isDirectory: true)
        self.limit = limit
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
    }
    private func file(_ key: String) -> URL {
        directory.appendingPathComponent(SHA256.hash(data: Data(key.utf8)).map { String(format: "%02x", $0) }.joined())
    }
    func read(_ key: String, maxAge: TimeInterval) -> Data? {
        let path = file(key)
        guard let attributes = try? path.resourceValues(forKeys: [.contentModificationDateKey]),
              let date = attributes.contentModificationDate, Date().timeIntervalSince(date) < maxAge else { return nil }
        return try? Data(contentsOf: path)
    }
    func write(_ data: Data, key: String) {
        guard data.count <= limit else { return }
        try? data.write(to: file(key), options: .atomic)
        let files = (try? FileManager.default.contentsOfDirectory(at: directory,
            includingPropertiesForKeys: [.contentModificationDateKey, .fileSizeKey])) ?? []
        let entries = files.compactMap { url -> (URL, Date, Int)? in
            guard let values = try? url.resourceValues(forKeys: [.contentModificationDateKey, .fileSizeKey]) else { return nil }
            return (url, values.contentModificationDate ?? .distantPast, values.fileSize ?? 0)
        }.sorted { $0.1 < $1.1 }
        var total = entries.reduce(0) { $0 + $1.2 }
        for entry in entries where total > limit {
            try? FileManager.default.removeItem(at: entry.0)
            total -= entry.2
        }
    }
}

actor ArtworkLoader {
    static let shared = ArtworkLoader()
    private var tasks: [String: Task<Data?, Never>] = [:]
    func data(_ address: String) async -> Data? {
        if let cached = await DiskCache.artwork.read(address, maxAge: 30 * 86400) { return cached }
        if let task = tasks[address] { return await task.value }
        let task = Task<Data?, Never> {
            guard let url = URL(string: address), ["https", "http"].contains(url.scheme ?? ""),
                  let (data, response) = try? await URLSession.shared.data(from: url),
                  let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode),
                  data.count <= 8 * 1024 * 1024, UIImage(data: data) != nil else { return nil }
            await DiskCache.artwork.write(data, key: address)
            return data
        }
        tasks[address] = task
        let data = await task.value
        tasks[address] = nil
        return data
    }
}

struct Artwork: View {
    let address: String?
    @State private var image: UIImage?
    var body: some View {
        ZStack {
            Color(white: 0.12)
            if let image { Image(uiImage: image).resizable().scaledToFill() }
            else { Image(systemName: "film").foregroundStyle(.gray) }
        }.clipped().task(id: address) {
            image = nil
            guard let address, let data = await ArtworkLoader.shared.data(address), !Task.isCancelled else { return }
            image = UIImage(data: data)
        }
    }
}
