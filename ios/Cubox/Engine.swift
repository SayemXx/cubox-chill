import Foundation

struct CuboxError: LocalizedError {
    let message: String
    var errorDescription: String? { message }
}

struct MediaID: Codable, Hashable { let provider: String; let value: String }
struct Title: Codable, Identifiable {
    let id: MediaID
    let title: String
    let media_type: String
    let year: String?
    let poster_url: String?
}
struct Episode: Codable { let number: Int; let title: String? }
struct Season: Codable { let number: Int; let episodes: [Episode] }
struct Dub: Codable { let subject_id: String; let label: String }
struct Details: Codable {
    let id: MediaID
    let title: String
    let description: String?
    let poster_url: String?
    let seasons: [Season]
    let dubs: [Dub]
}
struct Mirror: Codable { let label: String; let resolver_url: String; let headers: [[String]]; let direct_file: Bool }
struct Release: Codable {
    let provider: String
    let filename: String
    let quality: String?
    let codec: String?
    let language: String?
    let size_bytes: UInt64?
    let season: Int?
    let episode: Int?
    let mirrors: [Mirror]
    let resource_id: String?
}
struct Source: Codable {
    let url: String
    let headers: [[String]]
    var httpHeaders: [String: String] {
        headers.reduce(into: [:]) { result, pair in
            if pair.count == 2 { result[pair[0]] = pair[1] }
        }
    }
}

// Serialized off the main thread: the synchronous Rust runtime must never block UI rendering.
actor Engine {
    static let shared = Engine()
    private let root: String
    init() {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("Engine", isDirectory: true)
        try? FileManager.default.createDirectory(at: base, withIntermediateDirectories: true)
        root = base.path
    }
    func request<T: Decodable>(_ payload: [String: Any], as: T.Type = T.self) throws -> T {
        let input = try JSONSerialization.data(withJSONObject: payload)
        guard let text = String(data: input, encoding: .utf8) else { throw CuboxError(message: "Invalid request") }
        let pointer = text.withCString { request in root.withCString { cubox_request(request, $0) } }
        guard let pointer else { throw CuboxError(message: "Engine returned no response") }
        defer { cubox_free(pointer) }
        let response = Data(String(cString: pointer).utf8)
        let result = try JSONDecoder().decode(Envelope<T>.self, from: response)
        guard result.ok, let value = result.data else { throw CuboxError(message: result.error ?? "Provider unavailable") }
        return value
    }
    func resolve(_ release: Release, mirror: Int, download: Bool) throws -> Source {
        let object = try JSONSerialization.jsonObject(with: JSONEncoder().encode(release))
        return try request(["op": "resolve", "provider": release.provider, "release": object,
                            "mirror": mirror, "download": download])
    }
}
private struct Envelope<T: Decodable>: Decodable {
    let ok: Bool
    let data: T?
    let error: String?
}
