package io.pocketbase.apple

import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.pocketbase.PocketBase
import io.pocketbase.auth.AuthStore
import io.pocketbase.dtos.File
import io.pocketbase.http.ClientException
import io.pocketbase.http.json
import io.pocketbase.services.UnsubscribeFunc
import io.pocketbase.services.mapOfNotNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

class ApplePocketBaseClient(
    url: String,
) {
    internal val pocketBase = PocketBase(url, AuthStore())

    fun collection(idOrName: String): AppleRecordBridge = AppleRecordBridge(pocketBase, idOrName)

    suspend fun sendJson(
        path: String,
        method: HttpMethod = HttpMethod.Get,
        headers: Map<String, String> = emptyMap(),
        query: Map<String, String> = emptyMap(),
        body: Any? = null,
        files: List<File> = emptyList(),
    ): String {
        return pocketBase.send(
            path = path,
            method = method,
            headers = headers,
            query = query,
            body = body,
            files = files,
        ).bodyAsText()
    }

    fun buildUrl(
        path: String,
        query: Map<String, Any?> = emptyMap(),
    ): String = pocketBase.buildUrl(path, query)

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

    suspend fun getFullListJson(
        batch: Int = 1000,
        expand: String? = null,
        filter: String? = null,
        sort: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): String {
        val items = mutableListOf<kotlinx.serialization.json.JsonElement>()
        var page = 1

        while (true) {
            val pageResponse =
                getListJson(
                    page = page,
                    perPage = batch,
                    skipTotal = true,
                    expand = expand,
                    filter = filter,
                    sort = sort,
                    fields = fields,
                    query = query,
                    headers = headers,
                )
            val listJson = json.parseToJsonElement(pageResponse).jsonObject
            val pageItems = listJson["items"]?.jsonArray ?: JsonArray(emptyList())
            items.addAll(pageItems)

            if (pageItems.size < batch) {
                break
            }

            page += 1
        }

        val result = buildJsonArray { items.forEach { add(it) } }
        return json.encodeToString(JsonArray.serializer(), result)
    }

    suspend fun getFirstListItemJson(
        filter: String,
        expand: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): String {
        val listResponse =
            getListJson(
                page = 1,
                perPage = 1,
                skipTotal = true,
                expand = expand,
                filter = filter,
                sort = null,
                fields = fields,
                query = query,
                headers = headers,
            )

        val listJson = json.parseToJsonElement(listResponse).jsonObject
        val first = listJson["items"]?.jsonArray?.firstOrNull()
        if (first == null) {
            throw ClientException(
                statusCode = 404,
                url = baseCrudPath,
                data = null,
            )
        }

        return first.toString()
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

    suspend fun listAuthMethodsJson(
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): String {
        return pocketBase.send(
            path = "$baseCollectionPath/auth-methods",
            query = query,
            headers = headers,
        ).bodyAsText()
    }

    suspend fun authWithOAuth2CodeJson(
        provider: String,
        code: String,
        codeVerifier: String,
        redirectUrl: String,
        createDataJson: String? = null,
        expand: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): String {
        val enrichedQuery = query + mapOfNotNull("expand" to expand, "fields" to fields)
        val body =
            buildJsonObject {
                put("provider", JsonPrimitive(provider))
                put("code", JsonPrimitive(code))
                put("codeVerifier", JsonPrimitive(codeVerifier))
                put("redirectUrl", JsonPrimitive(redirectUrl))
                if (!createDataJson.isNullOrBlank()) {
                    put("createData", json.parseToJsonElement(createDataJson))
                }
            }.toString()

        return pocketBase.send(
            path = "$baseCollectionPath/auth-with-oauth2",
            method = HttpMethod.Post,
            body = body,
            query = enrichedQuery,
            headers = headers,
        ).bodyAsText()
    }

    suspend fun authRefreshJson(
        expand: String? = null,
        fields: String? = null,
        bodyJson: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): String {
        val enrichedQuery = query + mapOfNotNull("expand" to expand, "fields" to fields)

        return pocketBase.send(
            path = "$baseCollectionPath/auth-refresh",
            method = HttpMethod.Post,
            body = bodyJson,
            query = enrichedQuery,
            headers = headers,
        ).bodyAsText()
    }

    suspend fun confirmPasswordReset(
        passwordResetToken: String,
        password: String,
        passwordConfirm: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) {
        val body =
            mapOf(
                "token" to passwordResetToken,
                "password" to password,
                "passwordConfirm" to passwordConfirm,
            )

        pocketBase.send(
            path = "$baseCollectionPath/confirm-password-reset",
            method = HttpMethod.Post,
            body = body,
            query = query,
            headers = headers,
        )
    }

    suspend fun requestEmailChange(
        newEmail: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) {
        pocketBase.send(
            path = "$baseCollectionPath/request-email-change",
            method = HttpMethod.Post,
            body = mapOf("newEmail" to newEmail),
            query = query,
            headers = headers,
        )
    }

    suspend fun confirmEmailChange(
        emailChangeToken: String,
        password: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) {
        pocketBase.send(
            path = "$baseCollectionPath/confirm-email-change",
            method = HttpMethod.Post,
            body =
                mapOf(
                    "token" to emailChangeToken,
                    "password" to password,
                ),
            query = query,
            headers = headers,
        )
    }

    suspend fun requestVerification(
        email: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) {
        pocketBase.send(
            path = "$baseCollectionPath/request-verification",
            method = HttpMethod.Post,
            body = mapOf("email" to email),
            query = query,
            headers = headers,
        )
    }

    suspend fun confirmVerification(
        verificationToken: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) {
        pocketBase.send(
            path = "$baseCollectionPath/confirm-verification",
            method = HttpMethod.Post,
            body = mapOf("token" to verificationToken),
            query = query,
            headers = headers,
        )
    }

    suspend fun requestOTPJson(
        email: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): String {
        return pocketBase.send(
            path = "$baseCollectionPath/request-otp",
            method = HttpMethod.Post,
            body = mapOf("email" to email),
            query = query,
            headers = headers,
        ).bodyAsText()
    }

    suspend fun authWithOTPJson(
        otpId: String,
        password: String,
        expand: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): String {
        val enrichedQuery = query + mapOfNotNull("expand" to expand, "fields" to fields)

        return pocketBase.send(
            path = "$baseCollectionPath/auth-with-otp",
            method = HttpMethod.Post,
            body = mapOf("otpId" to otpId, "password" to password),
            query = enrichedQuery,
            headers = headers,
        ).bodyAsText()
    }

    suspend fun impersonateJson(
        id: String,
        duration: Int? = null,
        expand: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): String {
        val enrichedQuery = query + mapOfNotNull("expand" to expand, "fields" to fields)
        val body =
            buildMap<String, Any?> {
                duration?.let { put("duration", it) }
            }

        return pocketBase.send(
            path = "$baseCollectionPath/impersonate/$id",
            method = HttpMethod.Post,
            body = body,
            query = enrichedQuery,
            headers = headers,
        ).bodyAsText()
    }

    suspend fun listExternalAuthsJson(
        recordId: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): String {
        val filter = pocketBase.filter("recordRef = {:id}", mapOf("id" to recordId))
        val enrichedQuery = query + mapOf("filter" to filter, "skipTotal" to 1, "page" to 1, "perPage" to 500)

        return pocketBase.send(
            path = "/api/collections/_externalAuths/records",
            method = HttpMethod.Get,
            query = enrichedQuery,
            headers = headers,
        ).bodyAsText()
    }

    suspend fun unlinkExternalAuth(
        recordId: String,
        provider: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): Boolean {
        val filter = pocketBase.filter("recordRef = {:recordId} && provider = {:provider}", mapOf("recordId" to recordId, "provider" to provider))
        val listResponse =
            pocketBase.send(
                path = "/api/collections/_externalAuths/records",
                method = HttpMethod.Get,
                query = query + mapOf("filter" to filter, "skipTotal" to 1, "page" to 1, "perPage" to 1),
                headers = headers,
            ).bodyAsText()

        val first =
            json.parseToJsonElement(listResponse).jsonObject["items"]?.jsonArray?.firstOrNull()?.jsonObject
                ?: return false
        val id = first["id"]?.toString()?.trim('"') ?: return false

        pocketBase.send(
            path = "/api/collections/_externalAuths/records/$id",
            method = HttpMethod.Delete,
            query = query,
            headers = headers,
        )

        return true
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
