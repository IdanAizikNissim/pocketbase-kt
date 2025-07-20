plugins {
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.serialization).apply(false)
    alias(libs.plugins.kmmbridge).apply(false)
    alias(libs.plugins.skie).apply(false)
    alias(libs.plugins.ktlint)
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
