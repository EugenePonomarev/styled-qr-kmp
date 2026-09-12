import SwiftUI
import UIKit
import StyledQrKmp

/// Copy this adapter to an iOS app target after importing the StyledQrKmp framework.
///
/// UIKit subclasses written in Kotlin cannot be imported directly into Swift. The adapter instead
/// uses StyledQrImageRenderer, which is a Swift-importable KMP type that returns a UIImage.
struct StyledQrCodeView: UIViewRepresentable {
    let content: String
    let logo: UIImage?

    init(content: String, logo: UIImage? = nil) {
        self.content = content
        self.logo = logo
    }

    func makeUIView(context: Context) -> StyledQrImageView {
        StyledQrImageView()
    }

    func updateUIView(_ view: StyledQrImageView, context: Context) {
        view.content = content
        view.logo = logo
    }
}

final class StyledQrImageView: UIImageView {
    private let renderer = StyledQrImageRenderer()

    var content: String = "" {
        didSet { renderer.content = content; setNeedsLayout() }
    }

    var logo: UIImage? {
        didSet { renderer.logo = logo; setNeedsLayout() }
    }

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
        image = side > 0 ? renderer.render(sizePoints: Double(side), scale: 0) : nil
    }
}
