package io.pocketbase.http

import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.sse
import io.ktor.sse.ServerSentEvent
import io.pocketbase.ClientConfig
import io.pocketbase.auth.AuthStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.milliseconds

internal class SSEClient(
    private val config: ClientConfig,
    private val authStore: AuthStore,
) {
    private var client: HttpClient? = null

    var clientId: String? = null
        private set

    val isConnected
        get() = client != null

    fun connect(url: String) =
        flow {
            disconnect()

            val sseHttpClient =
                HttpClientBuilder(HttpClientFactory()).build(
                    baseUrl = config.baseUrl,
                    protocol = config.protocol,
                    port = config.port,
                    lang = config.lang,
                    httpTimeout = config.httpTimeout,
                    sseReconnectionTime = config.sseReconnectionTime,
                    maxReconnectionAttempts = config.maxReconnectionRetries,
                    logLevel = config.logLevel,
                    authStore = authStore,
                )
            client = sseHttpClient

            try {
                sseHttpClient.sse(
                    path = url,
                    reconnectionTime = config.sseReconnectionTime.milliseconds,
                ) {
                    incoming.collect { msg ->
                        when (msg.event) {
                            "PB_CONNECT" -> {
                                clientId = msg.id
                                emit(Incoming.PBConnect(clientId))
                            }

                            else -> emit(Incoming.Message(msg))
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                disconnect()
            }
        }

    fun disconnect() {
        client?.close()
        client = null
        clientId = null
    }
}

internal sealed interface Incoming {
    data class PBConnect(val clientId: String?) : Incoming

    data class Message(val message: ServerSentEvent) : Incoming
}
