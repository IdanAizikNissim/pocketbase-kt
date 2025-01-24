package io.pocketbase.http

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import org.jetbrains.annotations.TestOnly

internal actual class HttpClientFactory {
    actual fun create(config: (HttpClientConfig<*>) -> Unit): HttpClient =
        HttpClient(engine ?: CIO.create()) {
            config(this)
        }

    companion object {
        @TestOnly
        internal var engine: HttpClientEngine? = null
    }
}
