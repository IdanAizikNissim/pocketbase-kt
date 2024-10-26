// swift-tools-version:5.3
import PackageDescription

// BEGIN KMMBRIDGE VARIABLES BLOCK (do not edit)
let remoteKotlinUrl = "https://maven.pkg.github.com/IdanAizikNissim/pocketbase-kt/io/pocketbase/shared-kmmbridge/0.1.7/shared-kmmbridge-0.1.7.zip"
let remoteKotlinChecksum = "8642c01266c4cd39195788ecb52b799d1403f4bede2777d0b432cddaea1fb324"
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