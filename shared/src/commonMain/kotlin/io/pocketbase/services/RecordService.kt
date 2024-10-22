package io.pocketbase.services

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.pocketbase.PocketBase
import io.pocketbase.auth.normalizeBase64
import io.pocketbase.dtos.AuthMethodsList
import io.pocketbase.dtos.RecordAuth
import io.pocketbase.dtos.RecordModel
import io.pocketbase.dtos.RecordSubscriptionEvent
import io.pocketbase.http.json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.reflect.KClass

class RecordService<T : RecordModel> internal constructor(
    client: PocketBase,
    cls: KClass<T>,
    private val collectionIdOrName: String,
    private val baseCollectionPath: String = "/api/collections/$collectionIdOrName",
) : BaseCRUDService<T>(
        client = client,
        cls = cls,
        baseCrudPath = "$baseCollectionPath/records",
    ) {
    private val recordAuthSerializer by lazy {
        RecordAuth.serializer(serializer)
    }

    private val recordSubscriptionEventSerializer by lazy {
        RecordSubscriptionEvent.serializer(serializer)
    }

    suspend fun subscribe(
        topic: String,
        callback: (event: RecordSubscriptionEvent<T>) -> Unit,
        expand: String? = null,
        filter: String? = null,
        fields: String? = null,
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): UnsubscribeFunc =
        client.realtime.subscribe(
            topic = "$collectionIdOrName/$topic",
            listener = { e ->
                e.data?.let {
                    json.decodeFromString(recordSubscriptionEventSerializer, it)
                }?.let {
                    callback(it)
                }
            },
            expand = expand,
            filter = filter,
            fields = fields,
            query = query,
            headers = headers,
        )

    suspend fun unsubscribe(topic: String = "") {
        if (topic.isBlank()) {
            client.realtime.unsubscribeByPrefix(collectionIdOrName)
        } else {
            client.realtime.unsubscribe("$collectionIdOrName/$topic")
        }
    }

    override suspend fun update(
        id: String,
        body: T?,
        expand: String?,
        fields: String?,
        query: Map<String, Any?>,
        headers: Map<String, String>,
    ): T =
        super.update(id, body, expand, fields, query, headers).also {
            onAuthModelChanged(id) {
                client.authStore.save(client.authStore.token, it)
            }
        }

    override suspend fun delete(
        id: String,
        body: T?,
        query: Map<String, Any?>,
        headers: Map<String, String>,
    ) = super.delete(id, body, query, headers).also {
        onAuthModelChanged(id) {
            client.authStore.clear()
        }
    }

    suspend fun listAuthMethods(
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) = client.send(
        path = "$baseCollectionPath/auth-methods",
        query = query,
        headers = headers,
    ).body<AuthMethodsList>()

    suspend fun authWithPassword(
        usernameOrEmail: String,
        password: String,
        expand: String? = null,
        fields: String? = null,
        body: Map<String, Any?> = emptyMap(),
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): RecordAuth<T> {
        val enrichedBody =
            body +
                mapOf(
                    "identity" to usernameOrEmail,
                    "password" to password,
                )

        val enrichedQuery =
            query +
                mapOfNotNull(
                    "expand" to expand,
                    "fields" to fields,
                )

        return client.send(
            path = "$baseCollectionPath/auth-with-password",
            method = HttpMethod.Post,
            body = enrichedBody,
            query = enrichedQuery,
            headers = headers,
        ).authResponse()
    }

    suspend fun authRefresh(
        expand: String? = null,
        fields: String? = null,
        body: Map<String, Any?> = emptyMap(),
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): RecordAuth<T> {
        val enrichedQuery =
            query +
                mapOfNotNull(
                    "expand" to expand,
                    "fields" to fields,
                )

        return client.send(
            path = "$baseCollectionPath/auth-refresh",
            method = HttpMethod.Post,
            body = body,
            query = enrichedQuery,
            headers = headers,
        ).authResponse()
    }

    suspend fun requestPasswordReset(
        email: String,
        body: Map<String, Any?> = emptyMap(),
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) {
        val enrichedBody =
            body +
                mapOf(
                    "email" to email,
                )

        client.send(
            path = "$baseCollectionPath/request-password-reset",
            method = HttpMethod.Post,
            body = enrichedBody,
            query = query,
            headers = headers,
        )
    }

    suspend fun confirmPasswordReset(
        passwordResetToken: String,
        password: String,
        passwordConfirm: String,
        body: Map<String, Any?> = emptyMap(),
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) {
        val enrichedBody =
            body +
                mapOf(
                    "token" to passwordResetToken,
                    "password" to password,
                    "passwordConfirm" to passwordConfirm,
                )

        client.send(
            path = "$baseCollectionPath/confirm-password-reset",
            method = HttpMethod.Post,
            body = enrichedBody,
            query = query,
            headers = headers,
        )
    }

    suspend fun requestVerification(
        email: String,
        body: Map<String, Any?> = emptyMap(),
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) {
        val enrichedBody =
            body +
                mapOf(
                    "email" to email,
                )

        client.send(
            path = "$baseCollectionPath/request-verification",
            method = HttpMethod.Post,
            body = enrichedBody,
            query = query,
            headers = headers,
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun confirmVerification(
        verificationToken: String,
        body: Map<String, Any?> = emptyMap(),
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) {
        val enrichedBody =
            body +
                mapOf(
                    "token" to verificationToken,
                )

        val response =
            client.send(
                path = "$baseCollectionPath/confirm-verification",
                method = HttpMethod.Post,
                body = enrichedBody,
                query = query,
                headers = headers,
            )

        if (response.status == HttpStatusCode.NoContent) {
            val parts = verificationToken.split(".")
            if (parts.size != 3) {
                return
            }

            val payloadPart = normalizeBase64(parts[1])
            val payload = json.decodeFromString<JsonObject>(Base64.Default.decode(payloadPart).decodeToString())

            (client.authStore.model as? RecordModel)?.let { model ->
                if (model.id == payload["id"]?.jsonPrimitive?.content &&
                    model.collectionId == payload["collectionId"]?.jsonPrimitive?.content &&
                    !model.data.verified
                ) {
                    client.authStore.save(client.authStore.token, model.apply { data = model.data.copy(verified = true) })
                }
            }
        }
    }

    private fun onAuthModelChanged(
        id: String,
        onChange: (RecordModel) -> Unit,
    ) {
        (client.authStore.model as? RecordModel)?.let {
            if (it.id == id && listOf(it.collectionId, it.collectionName).contains(collectionIdOrName)) {
                onChange(it)
            }
        }
    }

    private suspend fun HttpResponse.authResponse(): RecordAuth<T> {
        val authResponse = json.decodeFromString(recordAuthSerializer, bodyAsText())
        client.authStore.save(authResponse.token, authResponse.record)

        return authResponse
    }
}
