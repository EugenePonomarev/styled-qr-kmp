import XCTest
import StyledQrKmp

final class ScanabilityChecksTests: XCTestCase {
    func testReportAndInvalidStyle() throws {
        let code = QrCodeGenerator.shared.encodeText(
            text: "x", errorCorrection: .h, minVersion: 3, maxVersion: 3
        )
        let analyzer = StyledQrScanabilityAnalyzer()
        let style = QrStyleConfiguration()
        style.foregroundAlpha = 0
        let result = analyzer.analyze(code: code, options: style, output: nil, logoIsRendered: false)
        XCTAssertTrue(result.isSuccess)
        let report = try XCTUnwrap(result.report)
        XCTAssertTrue(report.isRenderable)
        XCTAssertNil(report.measurements.minContrastRatio)
        XCTAssertNil(report.measurements.pixelsPerModule)
        XCTAssertEqual(report.warnings.count, 1)
        XCTAssertEqual(report.warnings.first?.code.name, "CONTRAST_INDETERMINATE_TRANSPARENCY")

        style.moduleScale = 0.1
        let invalid = analyzer.analyze(code: code, options: style, output: nil, logoIsRendered: false)
        XCTAssertFalse(invalid.isSuccess)
        XCTAssertNil(invalid.report)
        XCTAssertEqual(invalid.error?.code, .invalidstyle)
    }

    func testRasterMeasurementAndError() throws {
        let code = QrCodeGenerator.shared.encodeText(
            text: "x", errorCorrection: .h, minVersion: 3, maxVersion: 3
        )
        let analyzer = StyledQrScanabilityAnalyzer()
        let options = QrStyleConfiguration()
        let valid = analyzer.analyze(
            code: code, options: options,
            output: QrRasterRenderOptions(sizePx: 300, sizeMode: .integermodules), logoIsRendered: false
        )
        XCTAssertEqual(try XCTUnwrap(valid.report?.measurements.pixelsPerModule).doubleValue, 8.0)
        let invalid = analyzer.analyze(
            code: code, options: options,
            output: QrRasterRenderOptions(sizePx: 0, sizeMode: .integermodules), logoIsRendered: false
        )
        let report = try XCTUnwrap(invalid.report)
        XCTAssertFalse(report.isRenderable)
        XCTAssertNil(report.measurements.pixelsPerModule)
        XCTAssertEqual(report.errors.first?.code.name, "INVALID_RENDER_PARAMETERS")
    }
}
