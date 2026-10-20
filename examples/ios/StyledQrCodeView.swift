import SwiftUI
import UIKit
import StyledQrKmp

struct StyledQrCodeView: UIViewRepresentable {
    let content: String
    let logo: UIImage?
    let configuration: QrStyleConfiguration?

    init(
        content: String,
        logo: UIImage? = nil,
        configuration: QrStyleConfiguration? = nil
    ) {
        self.content = content
        self.logo = logo
        self.configuration = configuration
    }

    func makeUIView(context: Context) -> StyledQrImageView {
        StyledQrImageView(frame: .zero)
    }

    func updateUIView(_ view: StyledQrImageView, context: Context) {
        view.content = content
        view.logo = logo
        view.configuration = configuration
    }
}

final class StyledQrImageView: UIImageView {
    private let renderer = StyledQrImageRenderer()

    var content: String = "" {
        didSet {
            renderer.content = content
            setNeedsLayout()
        }
    }

    var logo: UIImage? {
        didSet {
            renderer.logo = logo
            setNeedsLayout()
        }
    }

    var configuration: QrStyleConfiguration? {
        didSet {
            setNeedsLayout()
        }
    }

    private(set) var lastRenderError: String?
    private(set) var lastRenderErrorCode: QrRenderErrorCode?

    override init(frame: CGRect) {
        super.init(frame: frame)
        contentMode = .scaleAspectFit
        backgroundColor = .white
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()

        let side = min(bounds.width, bounds.height)
        guard side > 0 else {
            image = nil
            lastRenderError = nil
            lastRenderErrorCode = nil
            return
        }

        if let configuration = configuration {
            let result = renderer.renderWithOptions(
                content: content,
                options: configuration,
                sizePoints: Double(side),
                errorCorrection: .h,
                scale: 0,
                logo: logo
            )
            image = result.image
            lastRenderError = result.error?.message
            lastRenderErrorCode = result.error?.code
        } else {
            image = renderer.render(
                sizePoints: Double(side),
                scale: 0
            )
            lastRenderError = renderer.lastRenderError
            lastRenderErrorCode = renderer.lastRenderErrorCode
        }
    }
}