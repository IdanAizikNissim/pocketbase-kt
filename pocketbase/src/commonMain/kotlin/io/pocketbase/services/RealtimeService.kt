package io.pocketbase.services

import io.ktor.http.HttpMethod
import io.ktor.sse.ServerSentEvent
import io.pocketbase.PocketBase
import io.pocketbase.dtos.SubmitSubscriptionsRequest
import io.pocketbase.http.Incoming
import io.pocketbase.http.SSEClient
import io.pocketbase.http.json
import io.pocketbase.utils.encoder.DefaultUrlEncoder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

internal class RealtimeService(
    client: PocketBase,
    private val sseClient: SSEClient,
) : BaseService(
        client = client,
    ) {
    private val subscriptions = mutableMapOf<String, List<SubscriptionFunc>>()
    private val hasNonEmptyTopic
        get() = subscriptions.values.any { it.isNotEmpty() }

    suspend fun subscribe(
        topic: String,
        listener: SubscriptionFunc,
        expand: String? = null,
        filter: String? = null,
        fields: String? = null,
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): UnsubscribeFunc {
        var key = topic

        val enrichedQuery =
            query +
                mapOfNotNull(
                    "expand" to expand,
                    "filter" to filter,
                    "fields" to fields,
                )

        val options = mutableMapOf<String, Any?>()
        if (enrichedQuery.isNotEmpty()) {
            options["query"] = enrichedQuery
        }
        if (headers.isNotEmpty()) {
            options["headers"] = headers
        }
        if (options.isNotEmpty()) {
            val jsonOptions = json.encodeToString(JsonObject.serializer(), options.toJsonObject())
            val encoded = "options=${DefaultUrlEncoder.encode(jsonOptions)}"
            key +=
                if (key.contains("?")) {
                    "&$encoded"
                } else {
                    "?$encoded"
                }
        }

        if (!subscriptions.contains(key)) {
            subscriptions[key] = emptyList()
        }
        subscriptions[key] = subscriptions[key]!! + listener

        val deferred = CompletableDeferred<UnsubscribeFunc>()
        if (sseClient.isConnected) {
            submitSubscriptions()
            deferred.complete {
                suspend { unsubscribeByTopicAndListener(topic, listener) }
            }
        } else {
            connectSSEClient(
                onConnect = {
                    deferred.complete {
                        suspend { unsubscribeByTopicAndListener(topic, listener) }
                    }
                },
                onException = {
                    deferred.completeExceptionally(it)
                },
            )
        }

        return deferred.await()
    }

    suspend fun unsubscribe(topic: String = "") {
        var needToSubmit = false

        if (topic.isBlank()) {
            subscriptions.clear()
        } else {
            val subs = getSubscriptionsByTopic(topic)
            for (key in subs.keys) {
                subscriptions.remove(key)
                needToSubmit = true
            }
        }

        if (!hasNonEmptyTopic) {
            return sseClient.disconnect()
        }

        if (needToSubmit) {
            return submitSubscriptions()
        }
    }

    suspend fun unsubscribeByPrefix(topicPrefix: String) {
        val beforeSize = subscriptions.size
        subscriptions.apply {
            keys.forEach {
                if ("$it?".startsWith(topicPrefix)) {
                    remove(it)
                }
            }
        }

        if (beforeSize == subscriptions.size) {
            return
        }

        if (!hasNonEmptyTopic) {
            return sseClient.disconnect()
        }

        return submitSubscriptions()
    }

    private suspend fun unsubscribeByTopicAndListener(
        topic: String,
        listener: SubscriptionFunc,
    ) {
        var needToSubmit = false
        val subs = getSubscriptionsByTopic(topic)

        for (key in subs.keys) {
            if (subscriptions[key]?.isEmpty() != false) {
                continue
            }

            val beforeSize = subscriptions[key]?.size ?: 0
            subscriptions[key] = subscriptions[key]?.filterNot { it == listener } ?: emptyList()
            val afterSize = subscriptions[key]?.size ?: 0

            if (beforeSize == afterSize) {
                continue
            }

            if (!needToSubmit && afterSize == 0) {
                needToSubmit = true
            }
        }

        if (!hasNonEmptyTopic) {
            return sseClient.disconnect()
        }

        if (needToSubmit) {
            return submitSubscriptions()
        }
    }

    private fun getSubscriptionsByTopic(topic: String): Map<String, List<SubscriptionFunc>> {
        val topicFilter = topic.takeIf { it.contains("?") } ?: "$topic?"
        return subscriptions.filterKeys {
            "$it?".startsWith(topicFilter)
        }
    }

    private suspend fun submitSubscriptions(clientId: String? = sseClient.clientId) {
        clientId?.let {
            client.send(
                path = "/api/realtime",
                method = HttpMethod.Post,
                body =
                    SubmitSubscriptionsRequest(
                        clientId = it,
                        subscriptions = subscriptions.keys.toList(),
                    ),
            )
        }
    }

    private fun connectSSEClient() {
        connectSSEClient(
            onConnect = {},
            onException = {},
        )
    }

    private fun connectSSEClient(
        onConnect: () -> Unit,
        onException: (e: Exception) -> Unit,
    ) = sseClient
        .connect("/api/realtime")
        .onEach {
            when (it) {
                is Incoming.Message -> {
                    subscriptions[it.message.event]?.forEach { sub ->
                        sub(it.message)
                    }
                }
                is Incoming.PBConnect -> {
                    try {
                        submitSubscriptions(it.clientId)
                        onConnect()
                    } catch (e: Exception) {
                        sseClient.disconnect()
                        onException(e)
                    }
                }
            }
        }
        .launchIn(CoroutineScope(Dispatchers.IO))
}

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.toJsonObject(): JsonObject =
    buildJsonObject {
        for ((key, value) in this@toJsonObject) {
            when (value) {
                is String -> put(key, JsonPrimitive(value))
                is Int -> put(key, JsonPrimitive(value))
                is Boolean -> put(key, JsonPrimitive(value))
                is Double -> put(key, JsonPrimitive(value))
                is Map<*, *> -> put(key, (value as Map<String, Any?>).toJsonObject())
                null -> put(key, JsonNull)
                else -> throw IllegalArgumentException("Unsupported type: ${value::class}")
            }
        }
    }

internal typealias SubscriptionFunc = suspend (event: ServerSentEvent) -> Unit
internal typealias UnsubscribeFunc = suspend () -> Unit
