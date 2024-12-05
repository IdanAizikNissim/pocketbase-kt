// swift-tools-version:5.3
import PackageDescription

// BEGIN KMMBRIDGE VARIABLES BLOCK (do not edit)
let remoteKotlinUrl = "https://maven.pkg.github.com/IdanAizikNissim/pocketbase-kt/io/pocketbase/shared-kmmbridge/0.1.11/shared-kmmbridge-0.1.11.zip"
let remoteKotlinChecksum = "128f722198f46c68d0205e344d84f68bb97808c2776715142bb7612717d5fdf4"
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