package io.pocketbase.http

import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.sse
import io.ktor.sse.ServerSentEvent
import io.pocketbase.ClientConfig
import io.pocketbase.auth.AuthStore
import kotlinx.coroutines.flow.flow

internal class SSEClient(
    private val config: ClientConfig,
    private val authStore: AuthStore,
) {
    private var client: HttpClient? = null
    var clientId: String? = null
        private set

    val isConnected
        get() = client != null

    suspend fun connect(url: String) =
        flow {
            disconnect()

            client =
                HttpClientBuilder(HttpClientFactory()).build(
                    baseUrl = config.baseUrl,
                    protocol = config.protocol,
                    port = config.port,
                    lang = config.lang,
                    authStore = authStore,
                )

            client?.sse(
                path = url,
            ) {
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
