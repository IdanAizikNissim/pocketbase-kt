package io.pocketbase.http

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import io.pocketbase.auth.AuthStore

internal class HttpClientBuilder(
    private val factory: HttpClientFactory,
) {
    fun build(
        baseUrl: String,
        protocol: Protocol,
        port: Int?,
        lang: String,
        httpTimeout: Long,
        logLevel: LogLevel,
        authStore: AuthStore,
    ): HttpClient =
        factory.create { config ->
            with(config) {
                install(SSE)

                install(HttpTimeout) {
                    requestTimeoutMillis = httpTimeout
                    connectTimeoutMillis = httpTimeout
                    socketTimeoutMillis = httpTimeout
                }

                install(ContentNegotiation) {
                    json(json)
                }

                install(Logging) {
                    level = logLevel.toKtorLogLevel()
                }

                defaultRequest {
                    url(
                        scheme = protocol.name.lowercase(),
                        host = baseUrl.split("://").last(),
                        port = port,
                    )
                    header("Accept-Language", lang)

                    if (!headers.contains("Authorization") && authStore.isValid) {
                        header("Authorization", authStore.token)
                    }
                }
            }
        }
}

internal fun LogLevel.toKtorLogLevel(): io.ktor.client.plugins.logging.LogLevel =
    when (this) {
        LogLevel.ALL -> io.ktor.client.plugins.logging.LogLevel.ALL
        LogLevel.HEADERS -> io.ktor.client.plugins.logging.LogLevel.HEADERS
        LogLevel.BODY -> io.ktor.client.plugins.logging.LogLevel.BODY
        LogLevel.INFO -> io.ktor.client.plugins.logging.LogLevel.INFO
        LogLevel.NONE -> io.ktor.client.plugins.logging.LogLevel.NONE
    }
