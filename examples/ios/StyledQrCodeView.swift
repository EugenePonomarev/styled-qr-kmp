import SwiftUI
import UIKit
import StyledQrKmp

/// Place this adapter in the iOS app target after importing the StyledQrKmp framework.
public struct StyledQrCodeView: UIViewRepresentable {
    public let content: String
    public let logo: UIImage?

    public init(content: String, logo: UIImage? = nil) {
        self.content = content
        self.logo = logo
    }

    public func makeUIView(context: Context) -> StyledQrView {
        StyledQrView(frame: .zero)
    }

    public func updateUIView(_ view: StyledQrView, context: Context) {
        view.content = content
        view.logo = logo
    }
}
