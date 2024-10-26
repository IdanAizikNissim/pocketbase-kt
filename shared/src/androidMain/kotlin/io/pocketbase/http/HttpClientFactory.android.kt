package io.pocketbase.http

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO

internal actual class HttpClientFactory {
    actual fun create(config: (HttpClientConfig<*>) -> Unit): HttpClient =
        HttpClient(CIO) {
            config(this)
        }
}
