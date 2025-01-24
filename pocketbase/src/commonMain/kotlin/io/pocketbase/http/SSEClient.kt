package io.pocketbase.http

import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.sse
import io.ktor.sse.ServerSentEvent
import io.pocketbase.ClientConfig
import io.pocketbase.auth.AuthStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.coroutines.CoroutineContext
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
                    httpTimeout = config.httpTimeout,
                    logLevel = config.logLevel,
                    authStore = authStore,
                )

            client?.sse(
                path = url,
                reconnectionTime = config.sseReconnectionTime.milliseconds,
            ) {
                try {
                    while (true) {
                        if (!this.isActive) {
                            disconnect(
                                context = currentCoroutineContext(),
                            )
                            return@sse
                        }

                        incoming.collect { msg ->
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
                } catch (e: Exception) {
                    disconnect(
                        context = currentCoroutineContext(),
                    )
                }
            }
        }

    fun disconnect() {
        client?.close()
        client = null
        clientId = null
    }

    private fun disconnect(context: CoroutineContext) {
        disconnect()

        val shouldReconnect = retryAttempts++ <= config.maxReconnectionRetries
        context.cancel(
            cause =
                SSEClientCancellationException(
                    shouldReconnect = shouldReconnect,
                ),
        )
    }
}

internal sealed interface Incoming {
    data class PBConnect(val clientId: String?) : Incoming

    data class Message(val message: ServerSentEvent) : Incoming
}

internal data class SSEClientCancellationException(
    val shouldReconnect: Boolean,
) : CancellationException("SSE Client Cancellation")
