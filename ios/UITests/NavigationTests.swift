import XCTest

final class NavigationTests: XCTestCase {
    func testLaunchTabsAndRotation() {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.tabBars.buttons["Home"].waitForExistence(timeout: 15))
        app.tabBars.buttons["Downloads"].tap()
        XCTAssertTrue(app.staticTexts["Your downloaded videos will appear here."].waitForExistence(timeout: 5))
        app.tabBars.buttons["My Cubox"].tap()
        XCTAssertTrue(app.staticTexts["Developer: Sayem"].waitForExistence(timeout: 5))
        XCUIDevice.shared.orientation = .landscapeLeft
        XCTAssertTrue(app.staticTexts["Developer: Sayem"].exists)
        XCUIDevice.shared.orientation = .portrait
        app.tabBars.buttons["Home"].tap()
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "iPhone home"
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }
}
