package io.pocketbase.http

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.android.Android

internal actual class HttpClientFactory {
    actual fun create(config: (HttpClientConfig<*>) -> Unit): HttpClient =
        HttpClient(Android) {
            config(this)
        }
}
