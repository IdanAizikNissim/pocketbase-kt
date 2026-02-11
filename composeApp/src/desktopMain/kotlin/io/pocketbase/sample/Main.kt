package io.pocketbase.sample

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.pocketbase.sample.ui.App

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "PocketBase Sample") {
        App()
    }
}
