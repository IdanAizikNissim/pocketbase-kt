package io.pocketbase.http

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

internal expect class HttpClientFactory() {
    fun create(config: (HttpClientConfig<*>) -> Unit): HttpClient
}
