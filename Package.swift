// swift-tools-version:5.3
import PackageDescription

// BEGIN KMMBRIDGE VARIABLES BLOCK (do not edit)
let remoteKotlinUrl = "https://maven.pkg.github.com/IdanAizikNissim/pocketbase-kt/io/pocketbase/shared-kmmbridge/0.1.2/shared-kmmbridge-0.1.2.zip"
let remoteKotlinChecksum = "ddc64224a76ed567943782cc7d98351b24e0552eda60fb2a196391f838cd3e75"
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