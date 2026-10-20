import CoreImage
import XCTest
import UIKit
import Vision
import StyledQrKmp
import StyledQrSwiftChecks

final class StyledQrSwiftChecksTests: XCTestCase {
    @MainActor
    func testExistingSwiftApiStillProducesAnImage() {
        XCTAssertNotNil(makeSmokeImage())

        let code = QrCodeGenerator.shared.encodeText(
            text: "hi",
            errorCorrection: .h,
            minVersion: 1,
            maxVersion: 40
        )
        let payload = QrCodeDecoder.shared.decode(
            modules: code.doCopyModules()
        )

        XCTAssertEqual(payload.utf8TextOrNull(), "hi")
        XCTAssertEqual(payload.eciAssignment?.intValue, 26)
    }

    @MainActor
    func testSafeConfigurationContainsStyleErrorsAndRecovers() {
        let renderer = StyledQrImageRenderer()
        let options = QrStyleConfiguration()
        options.foregroundHex = "invalid"

        let invalid = render(renderer, options)
        XCTAssertNil(invalid.image)
        XCTAssertFalse(invalid.isSuccess)
        XCTAssertFalse(invalid.isEmpty)
        XCTAssertEqual(invalid.error?.code, .invalidstyle)
        XCTAssertEqual(renderer.lastRenderErrorCode, .invalidstyle)

        options.foregroundHex = "#143A5A"
        let valid = render(renderer, options)

        XCTAssertNotNil(valid.image)
        XCTAssertTrue(valid.isSuccess)
        XCTAssertNil(valid.error)
        XCTAssertNil(renderer.lastRenderError)
        XCTAssertNil(renderer.lastRenderErrorCode)
    }

    @MainActor
    func testNestedGradientValidationNeverCrossesTheSwiftBoundary() {
        let options = QrStyleConfiguration()
        let gradient = QrGradientConfiguration()
        gradient.startX = .nan
        options.foregroundGradient = gradient

        XCTAssertEqual(options.build().error?.code, .invalidstyle)
        XCTAssertEqual(
            render(StyledQrImageRenderer(), options).error?.code,
            .invalidstyle
        )

        gradient.startX = 0
        gradient.startColorHex = "#075985"
        gradient.endColorHex = "#6D28D9"

        XCTAssertTrue(options.build().isSuccess)
        XCTAssertTrue(render(StyledQrImageRenderer(), options).isSuccess)
    }

    @MainActor
    func testRenderParameterErrorsAreReturnedAndDiagnosticsReset() {
        let renderer = StyledQrImageRenderer()
        renderer.content = "hi"

        for points in [
            Double.nan,
            .infinity,
            -.infinity,
            0,
            -1,
            .greatestFiniteMagnitude
        ] {
            XCTAssertNil(renderer.render(sizePoints: points, scale: 1))
            XCTAssertEqual(
                renderer.lastRenderErrorCode,
                .invalidrenderparameters
            )
        }

        for scale in [
            Double.nan,
            .infinity,
            -1,
            .greatestFiniteMagnitude,
            0.00001
        ] {
            XCTAssertNil(renderer.render(sizePoints: 240, scale: scale))
            XCTAssertEqual(
                renderer.lastRenderErrorCode,
                .invalidrenderparameters
            )
        }

        XCTAssertNotNil(renderer.render(sizePoints: 240, scale: 0))
        XCTAssertNil(renderer.lastRenderErrorCode)

        renderer.content = " \n\t"
        XCTAssertNil(renderer.render(sizePoints: 240, scale: 1))
        XCTAssertNil(renderer.lastRenderError)
        XCTAssertNil(renderer.lastRenderErrorCode)
    }

    @MainActor
    func testPayloadAndLogoErrorsHaveStableDistinctCodes() {
        let renderer = StyledQrImageRenderer()
        let options = QrStyleConfiguration()

        let oversized = renderer.renderWithOptions(
            content: String(repeating: "a", count: 5000),
            options: options,
            sizePoints: 240,
            errorCorrection: .h,
            scale: 1,
            logo: nil
        )
        XCTAssertEqual(oversized.error?.code, .payloadtoolong)

        let logo = UIGraphicsImageRenderer(
            size: CGSize(width: 16, height: 16)
        ).image { context in
            UIColor.red.setFill()
            context.fill(CGRect(x: 0, y: 0, width: 16, height: 16))
        }

        let unsafe = renderer.renderWithOptions(
            content: "hi",
            options: options,
            sizePoints: 240,
            errorCorrection: .h,
            scale: 1,
            logo: logo
        )

        XCTAssertEqual(unsafe.error?.code, .logovalidationfailed)
        XCTAssertNil(unsafe.image)
    }

    @MainActor
    func testBlankSafeResultIsExplicitAndDoesNotChangeStoredInputs() {
        let renderer = StyledQrImageRenderer()
        renderer.content = "original"

        let empty = renderer.renderWithOptions(
            content: " \n",
            options: QrStyleConfiguration(),
            sizePoints: 240,
            errorCorrection: .h,
            scale: 1,
            logo: nil
        )

        XCTAssertTrue(empty.isEmpty)
        XCTAssertFalse(empty.isSuccess)
        XCTAssertNil(empty.error)
        XCTAssertEqual(renderer.content, "original")
        XCTAssertNotNil(renderer.render(sizePoints: 240, scale: 1))
    }

    @MainActor
    func testThemeConfigurationsRoundTripFromSwift() {
        for theme in QrThemes.shared.all {
            let original = theme.createStyle(logo: nil)
            let config = QrStyleConfiguration.companion.fromStyle(
                style: original
            )

            XCTAssertTrue(config.build().isSuccess)
            XCTAssertNotNil(render(StyledQrImageRenderer(), config).image)
        }
    }

    @MainActor
    func testSafeUIImageHasExpectedScaleAndIsReadableByAppleVision() throws {
        let payload = "https://example.com"
        let renderer = StyledQrImageRenderer()

        let options = QrStyleConfiguration()
        options.moduleShape = .roundedsquare

        let gradient = QrGradientConfiguration()
        gradient.startColorHex = "#075985"
        gradient.endColorHex = "#6D28D9"
        options.foregroundGradient = gradient

        let styledResult = renderer.renderWithOptions(
            content: payload,
            options: options,
            sizePoints: 240,
            errorCorrection: .h,
            scale: 2,
            logo: nil
        )

        let styledImage = try XCTUnwrap(
            styledResult.image,
            styledResult.error?.message ?? "Styled QR image is missing"
        )

        XCTAssertEqual(styledImage.size.width, 240)
        XCTAssertEqual(styledImage.size.height, 240)
        XCTAssertEqual(styledImage.scale, 2)

        let styledCGImage = try XCTUnwrap(styledImage.cgImage)
        XCTAssertEqual(styledCGImage.width, 480)
        XCTAssertEqual(styledCGImage.height, 480)

        let plainResult = renderer.renderWithOptions(
            content: payload,
            options: QrStyleConfiguration(),
            sizePoints: 240,
            errorCorrection: .h,
            scale: 2,
            logo: nil
        )

        let plainImage = try XCTUnwrap(
            plainResult.image,
            plainResult.error?.message ?? "Plain QR image is missing"
        )

        let referenceImage = try makeAppleReferenceQrImage(payload: payload)

        try assertVisionReads(
            referenceImage,
            expectedPayload: payload,
            label: "apple-reference"
        )

        try assertVisionReads(
            plainImage,
            expectedPayload: payload,
            label: "library-plain"
        )

        try assertVisionReads(
            styledImage,
            expectedPayload: payload,
            label: "library-gradient"
        )
    }

    @MainActor
    private func makeAppleReferenceQrImage(payload: String) throws -> UIImage {
        let filter = try XCTUnwrap(CIFilter(name: "CIQRCodeGenerator"))
        filter.setValue(Data(payload.utf8), forKey: "inputMessage")
        filter.setValue("H", forKey: "inputCorrectionLevel")

        let output = try XCTUnwrap(filter.outputImage)
        let enlarged = output.transformed(
            by: CGAffineTransform(scaleX: 10, y: 10)
        )

        let context = CIContext(options: [.useSoftwareRenderer: true])
        let cgImage = try XCTUnwrap(
            context.createCGImage(enlarged, from: enlarged.extent)
        )

        let padding: CGFloat = 40
        let size = CGSize(
            width: CGFloat(cgImage.width) + padding * 2,
            height: CGFloat(cgImage.height) + padding * 2
        )

        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        format.opaque = true

        return UIGraphicsImageRenderer(
            size: size,
            format: format
        ).image { rendererContext in
            UIColor.white.setFill()
            rendererContext.fill(CGRect(origin: .zero, size: size))
            rendererContext.cgContext.interpolationQuality = .none

            UIImage(cgImage: cgImage).draw(
                in: CGRect(
                    x: padding,
                    y: padding,
                    width: CGFloat(cgImage.width),
                    height: CGFloat(cgImage.height)
                )
            )
        }
    }

    @MainActor
    private func assertVisionReads(
        _ image: UIImage,
        expectedPayload: String,
        label: String,
        file: StaticString = #filePath,
        line: UInt = #line
    ) throws {
        let png = try XCTUnwrap(image.pngData(), file: file, line: line)
        let attachment = XCTAttachment(
            data: png,
            uniformTypeIdentifier: "public.png"
        )
        attachment.name = "\(label).png"
        attachment.lifetime = .keepAlways
        add(attachment)

        let cgImage = try XCTUnwrap(image.cgImage, file: file, line: line)
        let request = VNDetectBarcodesRequest()
        request.symbologies = [.qr]

        #if targetEnvironment(simulator)
        request.usesCPUOnly = true
        #endif

        try VNImageRequestHandler(
            cgImage: cgImage,
            orientation: .up,
            options: [:]
        ).perform([request])

        let observations = request.results ?? []
        let payloads = observations.compactMap { $0.payloadStringValue }

        print(
            "[Vision] \(label): " +
            "\(cgImage.width)x\(cgImage.height), " +
            "observations=\(observations.count), payloads=\(payloads)"
        )

        XCTAssertTrue(
            payloads.contains(expectedPayload),
            "\(label): expected \(expectedPayload), decoded \(payloads), " +
            "observations=\(observations.count)",
            file: file,
            line: line
        )
    }

    @MainActor
    private func render(
        _ renderer: StyledQrImageRenderer,
        _ options: QrStyleConfiguration
    ) -> QrImageRenderResult {
        renderer.renderWithOptions(
            content: "https://example.com",
            options: options,
            sizePoints: 240,
            errorCorrection: .h,
            scale: 1,
            logo: nil
        )
    }
}
