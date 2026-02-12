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
        url: "https://github.com/IdanAizikNissim/pocketbase-kt/releases/download/0.2.3/PocketBase.xcframework.zip",
        checksum: "8d6aa9d5977146371a07c4de3109e34abe2c4c370e90293d1912754b12618db5"
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
