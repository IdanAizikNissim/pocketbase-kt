package io.pocketbase.http

import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.sse
import io.ktor.sse.ServerSentEvent
import io.pocketbase.ClientConfig
import io.pocketbase.auth.AuthStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.milliseconds

internal class SSEClient(
    private val config: ClientConfig,
    private val authStore: AuthStore,
) {
    private var client: HttpClient? = null
    private var retryAttempts = 0

    var clientId: String? = null
        private set

    val isConnected
        get() = client != null

    fun connect(url: String) =
        flow {
            disconnect()

            client =
                HttpClientBuilder(HttpClientFactory()).build(
                    baseUrl = config.baseUrl,
                    protocol = config.protocol,
                    port = config.port,
                    lang = config.lang,
                    logLevel = config.logLevel,
                    authStore = authStore,
                )

            client?.sse(
                path = url,
                reconnectionTime = config.reconnectionTime.milliseconds,
                showRetryEvents = true,
            ) {
                incoming.collect { msg ->
                    if (msg.retry != null) {
                        reconnect(url, msg.retry ?: 0)
                    } else {
                        when (msg.event) {
                            "PB_CONNECT" -> {
                                clientId = msg.id
                                Incoming.PBConnect(clientId)
                            }
                            else -> Incoming.Message(msg)
                        }.let {
                            emit(it)
                        }
                    }
                }
            }
        }

    fun disconnect() {
        client?.close()
        client = null
        clientId = null
    }

    private suspend fun reconnect(
        url: String,
        retry: Long,
    ) {
        if (retryAttempts > config.maxReconnectionRetries) {
            disconnect()
            return
        }

        delay(retry)
        retryAttempts++

        connect(url)
    }
}

internal sealed interface Incoming {
    data class PBConnect(val clientId: String?) : Incoming

    data class Message(val message: ServerSentEvent) : Incoming
}
