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
        url: "https://github.com/IdanAizikNissim/pocketbase-kt/releases/download/0.0.0/PocketBase.xcframework.zip",
        checksum: "REPLACE_WITH_RELEASE_CHECKSUM"
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
