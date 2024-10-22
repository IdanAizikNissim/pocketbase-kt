// swift-tools-version:5.3
import PackageDescription

// BEGIN KMMBRIDGE VARIABLES BLOCK (do not edit)
let remoteKotlinUrl = "https://maven.pkg.github.com/IdanAizikNissim/pocketbase-kt/io/pocketbase/shared-kmmbridge/0.1.0/shared-kmmbridge-0.1.0.zip"
let remoteKotlinChecksum = "643da7f309efe66669b62479b2aa8f3061e289d20ee805844ea4b3c739ee9409"
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