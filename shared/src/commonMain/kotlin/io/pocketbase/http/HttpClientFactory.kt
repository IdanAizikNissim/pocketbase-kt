package io.pocketbase.http

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO

internal class HttpClientFactory {
    fun create(config: (HttpClientConfig<*>) -> Unit): HttpClient =
        HttpClient(engine ?: CIO.create()) {
            config(this)
        }

    companion object {
        internal var engine: HttpClientEngine? = null
    }
}
