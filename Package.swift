// swift-tools-version:5.9
import PackageDescription
import Foundation

let localXCFrameworkRelativePath = "pocketbase/build/XCFrameworks/release/PocketBase.xcframework"
let packageRootURL = URL(fileURLWithPath: #filePath).deletingLastPathComponent()
let localXCFrameworkAbsolutePath = packageRootURL
    .appendingPathComponent(localXCFrameworkRelativePath)
    .path
let useLocalXCFramework = FileManager.default.fileExists(atPath: localXCFrameworkAbsolutePath)

let pocketBaseTarget: Target = useLocalXCFramework
    ? .binaryTarget(
        name: "PocketBase",
        path: localXCFrameworkRelativePath
    )
    : .binaryTarget(
        name: "PocketBase",
        url: "https://github.com/IdanAizikNissim/pocketbase-kt/releases/download/0.2.1/PocketBase.xcframework.zip",
        checksum: "3177537e6f1285414b28dbafadc37e90014edba911c14e5d74f9ca043490ffed"
    )

let package = Package(
    name: "PocketBase",
    platforms: [
        .iOS(.v14),
        .macOS(.v12),
    ],
    products: [
        .library(
            name: "PocketBase",
            targets: ["PocketBase"]
        ),
    ],
    targets: [
        pocketBaseTarget,
    ]
)
