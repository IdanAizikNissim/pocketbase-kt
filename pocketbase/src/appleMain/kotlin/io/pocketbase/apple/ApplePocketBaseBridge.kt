package io.pocketbase.apple

import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.pocketbase.PocketBase
import io.pocketbase.auth.AuthStore
import io.pocketbase.dtos.File
import io.pocketbase.services.UnsubscribeFunc
import io.pocketbase.services.mapOfNotNull

class ApplePocketBaseClient(
    url: String,
) {
    private val pocketBase = PocketBase(url, AuthStore())

    fun collection(idOrName: String): AppleRecordBridge = AppleRecordBridge(pocketBase, idOrName)

    fun clearAuth() {
        pocketBase.authStore.clear()
    }
}

class AppleSubscription(
    private val unsubscribe: UnsubscribeFunc,
) {
    suspend fun cancel() {
        unsubscribe()
    }
}

class AppleRecordBridge internal constructor(
    private val pocketBase: PocketBase,
    private val collectionIdOrName: String,
) {
    private val baseCollectionPath = "/api/collections/$collectionIdOrName"
    private val baseCrudPath = "$baseCollectionPath/records"

    suspend fun getListJson(
        page: Int = 1,
        perPage: Int = 30,
        skipTotal: Boolean = false,
        expand: String? = null,
        filter: String? = null,
        sort: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): String {
        val enrichedQuery =
            query +
                mapOfNotNull(
                    "page" to page,
                    "perPage" to perPage,
                    "expand" to expand,
                    "filter" to filter,
                    "sort" to sort,
                    "fields" to fields,
                    "skipTotal" to skipTotal,
                )

        return pocketBase.send(
            path = baseCrudPath,
            query = enrichedQuery,
            headers = headers,
        ).bodyAsText()
    }

    suspend fun getOneJson(
        id: String,
        expand: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): String {
        val enrichedQuery =
            query +
                mapOfNotNull(
                    "expand" to expand,
                    "fields" to fields,
                )

        return pocketBase.send(
            path = "$baseCrudPath/$id",
            query = enrichedQuery,
            headers = headers,
        ).bodyAsText()
    }

    suspend fun createJson(
        bodyJson: String? = null,
        expand: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        files: List<File> = emptyList(),
    ): String {
        val enrichedQuery =
            query +
                mapOfNotNull(
                    "expand" to expand,
                    "fields" to fields,
                )

        val bodyPayload = bodyJson.takeIf { files.isEmpty() }
        val enrichedFiles =
            if (bodyJson != null && files.isNotEmpty()) {
                files + File(field = "@jsonPayload", data = bodyJson.encodeToByteArray())
            } else {
                files
            }

        return pocketBase.send(
            path = baseCrudPath,
            method = HttpMethod.Post,
            body = bodyPayload,
            query = enrichedQuery,
            headers = headers,
            files = enrichedFiles,
        ).bodyAsText()
    }

    suspend fun updateJson(
        id: String,
        bodyJson: String? = null,
        expand: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        files: List<File> = emptyList(),
    ): String {
        val enrichedQuery =
            query +
                mapOfNotNull(
                    "expand" to expand,
                    "fields" to fields,
                )

        val bodyPayload = bodyJson.takeIf { files.isEmpty() }
        val enrichedFiles =
            if (bodyJson != null && files.isNotEmpty()) {
                files + File(field = "@jsonPayload", data = bodyJson.encodeToByteArray())
            } else {
                files
            }

        return pocketBase.send(
            path = "$baseCrudPath/$id",
            method = HttpMethod.Patch,
            body = bodyPayload,
            query = enrichedQuery,
            headers = headers,
            files = enrichedFiles,
        ).bodyAsText()
    }

    suspend fun deleteRecord(
        id: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) {
        pocketBase.send(
            path = "$baseCrudPath/$id",
            method = HttpMethod.Delete,
            query = query,
            headers = headers,
        )
    }

    suspend fun authWithPasswordJson(
        usernameOrEmail: String,
        password: String,
        expand: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): String {
        val enrichedQuery =
            query +
                mapOfNotNull(
                    "expand" to expand,
                    "fields" to fields,
                )
        val body =
            mapOf(
                "identity" to usernameOrEmail,
                "password" to password,
            )

        val response =
            pocketBase.send(
                path = "$baseCollectionPath/auth-with-password",
                method = HttpMethod.Post,
                body = body,
                query = enrichedQuery,
                headers = headers,
            ).bodyAsText()

        return response
    }

    suspend fun requestPasswordReset(
        email: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) {
        pocketBase.send(
            path = "$baseCollectionPath/request-password-reset",
            method = HttpMethod.Post,
            body = mapOf("email" to email),
            query = query,
            headers = headers,
        )
    }

    suspend fun subscribeJson(
        topic: String,
        callback: (String) -> Unit,
        expand: String? = null,
        filter: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): AppleSubscription {
        val unsubscribe =
            pocketBase.realtime.subscribe(
                topic = "$collectionIdOrName/$topic",
                listener = { e ->
                    val payload = e.data ?: return@subscribe
                    // Swift callbacks may throw/concurrency-fail; avoid crashing realtime coroutine.
                    runCatching {
                        callback(payload)
                    }.onFailure { error ->
                        error.printStackTrace()
                    }
                },
                expand = expand,
                filter = filter,
                fields = fields,
                query = query,
                headers = headers,
            )

        return AppleSubscription(unsubscribe)
    }

    suspend fun unsubscribe(topic: String = "") {
        if (topic.isBlank()) {
            pocketBase.realtime.unsubscribeByPrefix(collectionIdOrName)
        } else {
            pocketBase.realtime.unsubscribe("$collectionIdOrName/$topic")
        }
    }
}
