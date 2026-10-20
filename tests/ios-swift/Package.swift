// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "StyledQrSwiftChecks",
    platforms: [.iOS(.v13)],
    products: [
        .library(
            name: "StyledQrSwiftChecks",
            targets: ["StyledQrSwiftChecks"]
        )
    ],
    targets: [
        .binaryTarget(
            name: "StyledQrKmp",
            path: "Frameworks/StyledQrKmp.xcframework"
        ),
        .target(
            name: "StyledQrSwiftChecks",
            dependencies: ["StyledQrKmp"],
            linkerSettings: [
                .linkedLibrary("c++"),
                .unsafeFlags(["-ObjC"])
            ]
        ),
        .testTarget(
            name: "StyledQrSwiftChecksTests",
            dependencies: ["StyledQrSwiftChecks", "StyledQrKmp"]
        )
    ]
)
