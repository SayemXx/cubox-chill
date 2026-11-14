import XCTest
@testable import CuboxChill

final class ModelTests: XCTestCase {
    func testProviderHeadersKeepLastValueAndIgnoreMalformedPairs() {
        let source = Source(url: "https://example.com/video.mp4", headers: [["Referer", "first"], ["broken"], ["Referer", "second"]])
        XCTAssertEqual(source.httpHeaders, ["Referer": "second"])
    }
    func testCatalogDecodesUpstreamShape() throws {
        let json = #"{"id":{"provider":"moviebox","value":"123"},"title":"Example","media_type":"series","year":null,"poster_url":null}"#
        let title = try JSONDecoder().decode(Title.self, from: Data(json.utf8))
        XCTAssertEqual(title.id.value, "123")
        XCTAssertEqual(title.media_type, "series")
    }
    func testCacheRejectsExpiredAndOversizeData() async {
        let cache = DiskCache(name: "Tests-\(UUID())", limit: 4)
        await cache.write(Data([1, 2]), key: "one")
        let fresh = await cache.read("one", maxAge: 60)
        XCTAssertEqual(fresh, Data([1, 2]))
        let expired = await cache.read("one", maxAge: -1)
        XCTAssertNil(expired)
        await cache.write(Data(repeating: 0, count: 8), key: "large")
        let large = await cache.read("large", maxAge: 60)
        XCTAssertNil(large)
    }
}
