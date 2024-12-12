// swift-tools-version:5.3
import PackageDescription

// BEGIN KMMBRIDGE VARIABLES BLOCK (do not edit)
let remoteKotlinUrl = "https://maven.pkg.github.com/IdanAizikNissim/pocketbase-kt/io/pocketbase/shared-kmmbridge/0.1.11/shared-kmmbridge-0.1.11.zip"
let remoteKotlinChecksum = "31aceef7edcb972789344c8f643ae57a684b1401e3120843916f392b10124356"
let packageName = "PocketBase"
// END KMMBRIDGE BLOCK

let package = Package(
    name: packageName,
    platforms: [
        .iOS(.v13)
    ],
    products: [
        .library(
            name: packageName,
            targets: [packageName]
        ),
    ],
    targets: [
        .binaryTarget(
            name: packageName,
            url: remoteKotlinUrl,
            checksum: remoteKotlinChecksum
        )
        ,
    ]
)