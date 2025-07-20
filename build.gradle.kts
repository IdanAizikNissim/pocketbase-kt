plugins {
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.serialization).apply(false)
    alias(libs.plugins.skie).apply(false)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.mavenPublish)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

subprojects {
    val GROUP: String by project
    val LIBRARY_VERSION: String by project

    group = GROUP
    version = LIBRARY_VERSION

    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    ktlint {
        filter {
            include("**/*.kts")
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

mavenPublishing {
    val GROUP: String by project
    val LIBRARY_VERSION: String by project

    coordinates(
        groupId = GROUP,
        artifactId = "pocketbase",
        version = LIBRARY_VERSION
    )

    pom {
        name.set("PocketBase KMP Library")
        inceptionYear.set("2025")
        url.set("https://github.com/IdanAizikNissim/pocketbase-kt")

        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                id.set("IdanAizikNissim")
            }
        }

        scm {
            url.set("https://github.com/IdanAizikNissim/pocketbase-kt")
        }
    }
}
