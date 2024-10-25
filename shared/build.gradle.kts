import java.lang.System.getenv

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kmmbridge)
    alias(libs.plugins.skie)
    `maven-publish`
}

kotlin {
    androidTarget()

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "PocketBase"
            isStatic = true
        }
    }

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.test)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        val desktopMain by getting
        desktopMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
        val desktopTest by getting
        desktopTest.dependencies {
        }
    }

    targets.all {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs =
                    listOf(
                        "-Xexpect-actual-classes",
                    )
            }
        }
    }

    androidTarget {
        publishLibraryVariants("release", "debug")
    }
}

android {
    namespace = "io.pocketbase"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

group = project.property("GROUP") as String
version =
    when {
        getenv("TAG_NAME")?.isNotBlank() == true -> getenv("TAG_NAME") as String
        project.hasProperty("tagName") -> project.property("tagName") as String
        else -> project.property("LIBRARY_VERSION") as String
    }

if (project.hasProperty("gprBuild")) {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = project.property("GROUP") as String
                artifactId = "pocketbase"
                version = version
                from(components["kotlin"])
            }
        }

        repositories {
            maven {
                name = "github"
                url = uri("https://maven.pkg.github.com/${getenv("GITHUB_REPOSITORY")}")
                credentials(PasswordCredentials::class)
            }
        }
    }
} else {
    val autoVersion =
        project.property(
            if (project.hasProperty("AUTO_VERSION")) {
                "AUTO_VERSION"
            } else {
                "LIBRARY_VERSION"
            },
        ) as String

    version = autoVersion

    addGithubPackagesRepository()

    kmmbridge {
        frameworkName.set("PocketBase")
        mavenPublishArtifacts()
        spm()
    }
}
