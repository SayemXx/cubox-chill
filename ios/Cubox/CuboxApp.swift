import SwiftUI

@main struct CuboxApp: App {
    var body: some Scene {
        WindowGroup {
            TabView {
                BrowseScreen().tabItem { Label("Home", systemImage: "house.fill") }
                DownloadsScreen().tabItem { Label("Downloads", systemImage: "arrow.down.circle") }
                AboutScreen().tabItem { Label("My Cubox", systemImage: "person.crop.circle") }
            }.tint(.red).preferredColorScheme(.dark)
        }
    }
}

@MainActor final class Catalog: ObservableObject {
    @Published var items: [Title] = []
    @Published var loading = false
    @Published var error: String?
    private var generation = 0
    func load(query: String, provider: String, refresh: Bool = false) async {
        generation += 1
        let ticket = generation
        let query = query.trimmingCharacters(in: .whitespacesAndNewlines)
        let key = "\(provider):\(query)"
        error = nil
        loading = true
        defer { if ticket == generation { loading = false } }
        if !refresh {
            // Show older data immediately, refresh only after six hours.
            if let old = await DiskCache.catalog.read(key, maxAge: 30 * 86400),
               let saved = try? JSONDecoder().decode([Title].self, from: old), ticket == generation {
                items = saved
                if await DiskCache.catalog.read(key, maxAge: 6 * 3600) != nil { return }
            } else if ticket == generation { items = [] }
        }
        do {
            let request: [String: Any] = query.isEmpty
                ? ["op": "home", "page": 1]
                : ["op": "search", "provider": provider, "query": query, "page": 1]
            let titles: [Title] = try await Engine.shared.request(request)
            guard ticket == generation, !Task.isCancelled else { return }
            items = titles
            await DiskCache.catalog.write(try JSONEncoder().encode(titles), key: key)
        } catch { if ticket == generation { self.error = error.localizedDescription } }
    }
}

struct BrowseScreen: View {
    @StateObject private var catalog = Catalog()
    @State private var query = ""
    @State private var provider = "moviebox"
    private let providers = [("moviebox", "MovieBox"), ("fourkhdhub", "4KHDHub"), ("dramachi", "Dramachi"), ("bdix_circleftp", "CircleFTP"), ("bdix_dhakaflix", "DhakaFlix")]
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("CUBOX & CHILL").font(.title2.bold()).foregroundStyle(.red)
                    Text("Find your next watch.").font(.largeTitle.bold())
                    Picker("Search provider", selection: $provider) {
                        ForEach(providers, id: \.0) { Text($0.1).tag($0.0) }
                    }.pickerStyle(.menu)
                    if query.isEmpty { Text("Featured on MovieBox. Search to explore your selected provider.").font(.footnote).foregroundStyle(.secondary) }
                    if catalog.loading { ProgressView("Loading titles…") }
                    if let error = catalog.error {
                        Text(error).foregroundStyle(.orange)
                        Button("Retry") { reload(refresh: true) }
                    }
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 130, maximum: 210), spacing: 12)], spacing: 20) {
                        ForEach(catalog.items) { item in
                            NavigationLink { DetailScreen(item: item) } label: {
                                VStack(alignment: .leading, spacing: 6) {
                                    Artwork(address: item.poster_url).frame(height: 215).clipShape(RoundedRectangle(cornerRadius: 10))
                                    Text(item.title).font(.subheadline.bold()).lineLimit(2)
                                    Text([item.year, item.media_type.capitalized].compactMap { $0 }.joined(separator: " · ")).font(.caption).foregroundStyle(.secondary)
                                }
                            }.buttonStyle(.plain)
                        }
                    }
                    if !catalog.loading && catalog.items.isEmpty && catalog.error == nil { Text("No titles found. Try another search or provider.") }
                }.padding()
            }.background(Color.black).navigationBarTitleDisplayMode(.inline)
                .searchable(text: $query, prompt: "Movies and series")
                .onSubmit(of: .search) { reload() }
                .onChange(of: provider) { _ in reload() }
                .onChange(of: query) { if $0.isEmpty { reload() } }
                .refreshable { await catalog.load(query: query, provider: provider, refresh: true) }
                .task { if catalog.items.isEmpty { await catalog.load(query: query, provider: provider) } }
        }
    }
    private func reload(refresh: Bool = false) { Task { await catalog.load(query: query, provider: provider, refresh: refresh) } }
}

private struct EpisodeChoice: Identifiable {
    let id = UUID()
    let media: MediaID
    let season: Int
    let episode: Int
    let title: String
}

struct DetailScreen: View {
    let item: Title
    @State private var details: Details?
    @State private var selectedID = ""
    @State private var error: String?
    @State private var choice: EpisodeChoice?
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                Artwork(address: details?.poster_url ?? item.poster_url).frame(height: 300).clipped()
                Text(item.title).font(.largeTitle.bold())
                if let details {
                    Text(details.description ?? "").foregroundStyle(.secondary)
                    if !details.dubs.isEmpty {
                        Picker("Audio / edition", selection: $selectedID) {
                            Text("Original edition").tag(details.id.value)
                            ForEach(details.dubs, id: \.subject_id) { Text($0.label).tag($0.subject_id) }
                        }
                    }
                    if details.seasons.isEmpty {
                        Button("Choose server & quality") { select(season: 0, episode: 0) }.buttonStyle(.borderedProminent)
                    } else {
                        ForEach(details.seasons, id: \.number) { season in
                            Text("Season \(season.number)").font(.title2.bold())
                            ForEach(season.episodes, id: \.number) { episode in
                                Button { select(season: season.number, episode: episode.number) } label: {
                                    Label("\(episode.number). \(episode.title ?? "Episode \(episode.number)")", systemImage: "play.circle")
                                        .frame(maxWidth: .infinity, alignment: .leading).padding()
                                        .background(Color(white: 0.1), in: RoundedRectangle(cornerRadius: 10))
                                }.buttonStyle(.plain)
                            }
                        }
                    }
                } else if let error { Text(error).foregroundStyle(.orange); Button("Retry") { Task { await load() } } }
                else { ProgressView("Loading details…") }
            }.padding()
        }.background(Color.black).navigationBarTitleDisplayMode(.inline)
            .task { await load() }
            .sheet(item: $choice) { ServerScreen(choice: $0) }
    }
    private func select(season: Int, episode: Int) {
        choice = EpisodeChoice(media: MediaID(provider: item.id.provider, value: selectedID.isEmpty ? item.id.value : selectedID), season: season, episode: episode,
                               title: item.title + (episode > 0 ? " · S\(season) E\(episode)" : ""))
    }
    private func load() async {
        error = nil
        do {
            let result: Details = try await Engine.shared.request(["op": "details", "provider": item.id.provider, "id": item.id.value])
            details = result
            selectedID = result.id.value
        } catch { self.error = error.localizedDescription }
    }
}

private struct ServerScreen: View {
    let choice: EpisodeChoice
    @State private var releases: [Release] = []
    @State private var loading = true
    @State private var error: String?
    @State private var notice: String?
    @State private var playback: Playback?
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        NavigationStack {
            List {
                if loading { ProgressView("Preparing servers…") }
                if let error { Text(error).foregroundStyle(.orange) }
                if let notice { Text(notice).foregroundStyle(.green) }
                if !loading && releases.isEmpty { Text("No servers found for this edition."); Button("Retry") { Task { await load() } } }
                ForEach(releases.indices, id: \.self) { index in
                    let release = releases[index]
                    Section(release.quality ?? release.filename) {
                        ForEach(release.mirrors.indices, id: \.self) { mirror in
                            VStack(alignment: .leading, spacing: 10) {
                                Text(release.mirrors[mirror].label).font(.headline)
                                Text(release.filename).font(.caption).foregroundStyle(.secondary)
                                HStack {
                                    Button { resolve(release, mirror: mirror, download: false) } label: { Label("Play", systemImage: "play.fill") }
                                    Spacer()
                                    Button { resolve(release, mirror: mirror, download: true) } label: { Label("Download", systemImage: "arrow.down") }
                                }.buttonStyle(.bordered).disabled(loading)
                            }.padding(.vertical, 4)
                        }
                    }
                }
            }.navigationTitle("Servers").toolbar { Button("Done") { dismiss() } }
        }.task { await load() }.fullScreenCover(item: $playback) { PlayerScreen(playback: $0) }
    }
    private func load() async {
        loading = true; error = nil
        defer { loading = false }
        do { releases = try await Engine.shared.request(["op": "releases", "provider": choice.media.provider, "id": choice.media.value, "season": choice.season, "episode": choice.episode]) }
        catch { self.error = error.localizedDescription }
    }
    private func resolve(_ release: Release, mirror: Int, download: Bool) {
        loading = true; error = nil; notice = nil
        Task {
            defer { loading = false }
            do {
                let source = try await Engine.shared.resolve(release, mirror: mirror, download: download)
                if download {
                    Downloads.shared.start(source: source, title: choice.title)
                    if let error = Downloads.shared.error { self.error = error; Downloads.shared.error = nil }
                    else { notice = "Download started. See the Downloads tab." }
                } else { playback = Playback(title: choice.title, source: source) }
            } catch { self.error = error.localizedDescription }
        }
    }
}

struct AboutScreen: View {
    var body: some View {
        NavigationStack {
            List {
                Section("cubox & chill") {
                    Text("Developer: Sayem")
                    Text("iPhone preview 0.1.0")
                    Text("MovieBox-TUI engine 0.1.29")
                }
                Section("Updates") {
                    Text("iPhone updates must be signed and installed through your Apple distribution method. Android APK releases cannot update this app.")
                    Link("Project releases", destination: URL(string: "https://github.com/SayemXx/cubox-chill/releases")!)
                }
                Section("Preview limitations") {
                    Text("Playback uses Apple's player and its supported codecs. Custom-header HLS, external subtitles, VLC-style gestures, and resumable background downloads are still pending.")
                }
                Section("Open source") {
                    Text("Includes MovieBox-TUI (MIT / Apache-2.0).")
                    Link("Source and license notices", destination: URL(string: "https://github.com/SayemXx/cubox-chill")!)
                }
            }.navigationTitle("My Cubox")
        }
    }
}
