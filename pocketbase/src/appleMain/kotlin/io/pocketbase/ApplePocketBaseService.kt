package io.pocketbase

import io.ktor.http.HttpMethod
import io.pocketbase.apple.ApplePocketBaseClient
import io.pocketbase.apple.AppleRecordBridge
import io.pocketbase.apple.AppleSubscription
import io.pocketbase.dtos.File
import io.pocketbase.http.json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import platform.Foundation.NSUserDefaults

class ApplePocketBaseService(
    url: String,
    private val authCollection: String = "users",
    private val sessionKey: String = "pocketbase.session",
) {
    private val client = ApplePocketBaseClient(url)
    private val subscriptions = mutableMapOf<String, AppleSubscription>()

    private var authToken: String? = null
    private var authRecordJson: String? = null

    init {
        loadSession()
    }

    fun isLoggedIn(): Boolean = !authToken.isNullOrBlank()

    fun authHeaders(): Map<String, String> {
        val token = authToken ?: return emptyMap()
        return mapOf("Authorization" to "Bearer $token")
    }

    fun authToken(): String? = authToken

    fun authRecordJson(): String? = authRecordJson

    fun updateAuthSession(
        token: String,
        authRecordJson: String?,
    ) {
        authToken = token
        this.authRecordJson = authRecordJson
        persistSession(token, authRecordJson)
    }

    suspend fun clearAuth() {
        subscriptions.values.forEach { subscription ->
            runCatching { subscription.cancel() }
        }
        subscriptions.clear()

        client.clearAuth()
        authToken = null
        authRecordJson = null

        val defaults = NSUserDefaults.standardUserDefaults
        defaults.removeObjectForKey(tokenStorageKey())
        defaults.removeObjectForKey(recordStorageKey())
    }

    suspend fun authWithPasswordJson(
        usernameOrEmail: String,
        password: String,
        expand: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): String =
        bridge(authCollection).authWithPasswordJson(
            usernameOrEmail = usernameOrEmail,
            password = password,
            expand = expand,
            fields = fields,
            query = query,
            headers = headers,
        )

    suspend fun authWithPasswordJson(
        usernameOrEmail: String,
        password: String,
    ): String =
        authWithPasswordJson(
            usernameOrEmail = usernameOrEmail,
            password = password,
            expand = null,
            fields = null,
            query = emptyMap(),
            headers = emptyMap(),
        )

    suspend fun requestPasswordReset(
        email: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) {
        bridge(authCollection).requestPasswordReset(
            email = email,
            query = query,
            headers = headers,
        )
    }

    suspend fun requestPasswordResetEmail(email: String) {
        requestPasswordReset(
            email = email,
            query = emptyMap(),
            headers = emptyMap(),
        )
    }

    suspend fun listAuthMethodsJson(
        collection: String = authCollection,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String = bridge(collection).listAuthMethodsJson(query = query, headers = headers)

    suspend fun authWithOAuth2CodeJson(
        provider: String,
        code: String,
        codeVerifier: String,
        redirectUrl: String,
        createDataJson: String? = null,
        collection: String = authCollection,
        expand: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String =
        bridge(collection).authWithOAuth2CodeJson(
            provider = provider,
            code = code,
            codeVerifier = codeVerifier,
            redirectUrl = redirectUrl,
            createDataJson = createDataJson,
            expand = expand,
            fields = fields,
            query = query,
            headers = headers,
        )

    suspend fun authRefreshJson(
        collection: String = authCollection,
        expand: String? = null,
        fields: String? = null,
        bodyJson: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String = bridge(collection).authRefreshJson(expand, fields, bodyJson, query, headers)

    suspend fun confirmPasswordReset(
        passwordResetToken: String,
        password: String,
        passwordConfirm: String,
        collection: String = authCollection,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) {
        bridge(collection).confirmPasswordReset(
            passwordResetToken = passwordResetToken,
            password = password,
            passwordConfirm = passwordConfirm,
            query = query,
            headers = headers,
        )
    }

    suspend fun requestEmailChange(
        newEmail: String,
        collection: String = authCollection,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ) {
        bridge(collection).requestEmailChange(
            newEmail = newEmail,
            query = query,
            headers = headers,
        )
    }

    suspend fun confirmEmailChange(
        emailChangeToken: String,
        password: String,
        collection: String = authCollection,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) {
        bridge(collection).confirmEmailChange(
            emailChangeToken = emailChangeToken,
            password = password,
            query = query,
            headers = headers,
        )
    }

    suspend fun requestVerification(
        email: String,
        collection: String = authCollection,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) {
        bridge(collection).requestVerification(
            email = email,
            query = query,
            headers = headers,
        )
    }

    suspend fun confirmVerification(
        verificationToken: String,
        collection: String = authCollection,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) {
        bridge(collection).confirmVerification(
            verificationToken = verificationToken,
            query = query,
            headers = headers,
        )
    }

    suspend fun requestOTPJson(
        email: String,
        collection: String = authCollection,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): String = bridge(collection).requestOTPJson(email = email, query = query, headers = headers)

    suspend fun authWithOTPJson(
        otpId: String,
        password: String,
        collection: String = authCollection,
        expand: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): String =
        bridge(collection).authWithOTPJson(
            otpId = otpId,
            password = password,
            expand = expand,
            fields = fields,
            query = query,
            headers = headers,
        )

    suspend fun impersonateJson(
        id: String,
        duration: Int? = null,
        collection: String = authCollection,
        expand: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String =
        bridge(collection).impersonateJson(
            id = id,
            duration = duration,
            expand = expand,
            fields = fields,
            query = query,
            headers = headers,
        )

    suspend fun listExternalAuthsJson(
        recordId: String,
        collection: String = authCollection,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String = bridge(collection).listExternalAuthsJson(recordId = recordId, query = query, headers = headers)

    suspend fun unlinkExternalAuth(
        recordId: String,
        provider: String,
        collection: String = authCollection,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): Boolean = bridge(collection).unlinkExternalAuth(recordId = recordId, provider = provider, query = query, headers = headers)

    suspend fun createJson(
        collection: String,
        bodyJson: String?,
        expand: String?,
        fields: String?,
        query: Map<String, String>,
        headers: Map<String, String>,
        files: List<io.pocketbase.dtos.File>,
    ): String =
        bridge(collection).createJson(
            bodyJson = bodyJson,
            expand = expand,
            fields = fields,
            query = query,
            headers = headers,
            files = files,
        )

    suspend fun createJson(
        collection: String,
        bodyJson: String,
    ): String =
        createJson(
            collection = collection,
            bodyJson = bodyJson,
            expand = null,
            fields = null,
            query = emptyMap(),
            headers = authHeaders(),
            files = emptyList(),
        )

    suspend fun createJsonWithFile(
        collection: String,
        bodyJson: String,
        fileField: String,
        fileData: ByteArray,
        fileName: String,
    ): String =
        createJson(
            collection = collection,
            bodyJson = bodyJson,
            expand = null,
            fields = null,
            query = emptyMap(),
            headers = authHeaders(),
            files = listOf(File(field = fileField, data = fileData, fileName = fileName)),
        )

    suspend fun createJson(
        collection: String,
        bodyJson: String?,
        files: List<File> = emptyList(),
    ): String =
        createJson(
            collection = collection,
            bodyJson = bodyJson,
            expand = null,
            fields = null,
            query = emptyMap(),
            headers = authHeaders(),
            files = files,
        )

    suspend fun updateJson(
        collection: String,
        id: String,
        bodyJson: String?,
        expand: String?,
        fields: String?,
        query: Map<String, String>,
        headers: Map<String, String>,
        files: List<io.pocketbase.dtos.File>,
    ): String =
        bridge(collection).updateJson(
            id = id,
            bodyJson = bodyJson,
            expand = expand,
            fields = fields,
            query = query,
            headers = headers,
            files = files,
        )

    suspend fun updateJson(
        collection: String,
        id: String,
        bodyJson: String,
    ): String =
        updateJson(
            collection = collection,
            id = id,
            bodyJson = bodyJson,
            expand = null,
            fields = null,
            query = emptyMap(),
            headers = authHeaders(),
            files = emptyList(),
        )

    suspend fun updateJsonWithFile(
        collection: String,
        id: String,
        bodyJson: String,
        fileField: String,
        fileData: ByteArray,
        fileName: String,
    ): String =
        updateJson(
            collection = collection,
            id = id,
            bodyJson = bodyJson,
            expand = null,
            fields = null,
            query = emptyMap(),
            headers = authHeaders(),
            files = listOf(File(field = fileField, data = fileData, fileName = fileName)),
        )

    suspend fun updateJson(
        collection: String,
        id: String,
        bodyJson: String?,
        files: List<File> = emptyList(),
    ): String =
        updateJson(
            collection = collection,
            id = id,
            bodyJson = bodyJson,
            expand = null,
            fields = null,
            query = emptyMap(),
            headers = authHeaders(),
            files = files,
        )

    suspend fun getListJson(
        collection: String,
        page: Int,
        perPage: Int,
        skipTotal: Boolean,
        expand: String?,
        filter: String?,
        sort: String?,
        fields: String?,
        query: Map<String, String>,
        headers: Map<String, String>,
    ): String =
        bridge(collection).getListJson(
            page = page,
            perPage = perPage,
            skipTotal = skipTotal,
            expand = expand,
            filter = filter,
            sort = sort,
            fields = fields,
            query = query,
            headers = headers,
        )

    suspend fun getFullListJson(
        collection: String,
        batch: Int = 1000,
        expand: String? = null,
        filter: String? = null,
        sort: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String =
        bridge(collection).getFullListJson(
            batch = batch,
            expand = expand,
            filter = filter,
            sort = sort,
            fields = fields,
            query = query,
            headers = headers,
        )

    suspend fun getFirstListItemJson(
        collection: String,
        filter: String,
        expand: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String =
        bridge(collection).getFirstListItemJson(
            filter = filter,
            expand = expand,
            fields = fields,
            query = query,
            headers = headers,
        )

    suspend fun getListJson(collection: String): String =
        getListJson(
            collection = collection,
            page = 1,
            perPage = 50,
            skipTotal = false,
            expand = null,
            filter = null,
            sort = "-created",
            fields = null,
            query = emptyMap(),
            headers = authHeaders(),
        )

    suspend fun getListJson(
        collection: String,
        page: Int = 1,
        perPage: Int = 30,
        skipTotal: Boolean = false,
        filter: String? = null,
        sort: String? = null,
    ): String =
        getListJson(
            collection = collection,
            page = page,
            perPage = perPage,
            skipTotal = skipTotal,
            expand = null,
            filter = filter,
            sort = sort,
            fields = null,
            query = emptyMap(),
            headers = authHeaders(),
        )

    suspend fun getOneJson(
        collection: String,
        id: String,
        expand: String?,
        fields: String?,
        query: Map<String, String>,
        headers: Map<String, String>,
    ): String =
        bridge(collection).getOneJson(
            id = id,
            expand = expand,
            fields = fields,
            query = query,
            headers = headers,
        )

    suspend fun getOneJson(
        collection: String,
        id: String,
    ): String =
        getOneJson(
            collection = collection,
            id = id,
            expand = null,
            fields = null,
            query = emptyMap(),
            headers = authHeaders(),
        )

    suspend fun deleteRecord(
        collection: String,
        id: String,
        query: Map<String, String>,
        headers: Map<String, String>,
    ) {
        bridge(collection).deleteRecord(
            id = id,
            query = query,
            headers = headers,
        )
    }

    suspend fun deleteRecord(
        collection: String,
        id: String,
    ) {
        deleteRecord(
            collection = collection,
            id = id,
            query = emptyMap(),
            headers = authHeaders(),
        )
    }

    suspend fun subscribeJson(
        key: String,
        collection: String,
        topic: String,
        callback: (String) -> Unit,
        expand: String?,
        filter: String?,
        fields: String?,
        query: Map<String, String>,
        headers: Map<String, String>,
    ) {
        subscriptions.remove(key)?.let { previous ->
            runCatching { previous.cancel() }
        }

        val subscription =
            bridge(collection).subscribeJson(
                topic = topic,
                callback = callback,
                expand = expand,
                filter = filter,
                fields = fields,
                query = query,
                headers = headers,
            )

        subscriptions[key] = subscription
    }

    suspend fun subscribeJson(
        key: String,
        collection: String,
        topic: String,
        callback: (String) -> Unit,
    ) {
        subscribeJson(
            key = key,
            collection = collection,
            topic = topic,
            callback = callback,
            expand = null,
            filter = null,
            fields = null,
            query = emptyMap(),
            headers = authHeaders(),
        )
    }

    suspend fun unsubscribe(key: String) {
        subscriptions.remove(key)?.let { subscription ->
            runCatching { subscription.cancel() }
        }
    }

    suspend fun healthCheckJson(
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): String {
        return client.sendJson(
            path = "/api/health",
            method = HttpMethod.Get,
            query = query,
            headers = headers,
        )
    }

    fun fileGetUrl(
        collectionIdOrName: String,
        recordId: String,
        fileName: String,
        thumb: String? = null,
        token: String? = null,
        download: Boolean? = null,
        query: Map<String, String> = emptyMap(),
    ): String {
        val params =
            query +
                mapOf(
                    "thumb" to thumb,
                    "token" to token,
                    "download" to if (download == true) "" else null,
                ).filterValues { it != null }.mapValues { it.value!! }

        return client.buildUrl(
            path = "/api/files/$collectionIdOrName/$recordId/$fileName",
            query = params,
        )
    }

    @Deprecated("Please use fileGetURL()")
    fun fileGetUrlLegacy(
        collectionIdOrName: String,
        recordId: String,
        fileName: String,
        thumb: String? = null,
        token: String? = null,
        download: Boolean? = null,
        query: Map<String, String> = emptyMap(),
    ): String = fileGetURL(collectionIdOrName, recordId, fileName, thumb, token, download, query)

    fun fileGetURL(
        collectionIdOrName: String,
        recordId: String,
        fileName: String,
        thumb: String? = null,
        token: String? = null,
        download: Boolean? = null,
        query: Map<String, String> = emptyMap(),
    ): String = fileGetUrl(collectionIdOrName, recordId, fileName, thumb, token, download, query)

    suspend fun fileGetTokenJson(
        bodyJson: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String {
        return client.sendJson(
            path = "/api/files/token",
            method = HttpMethod.Post,
            body = bodyJson,
            query = query,
            headers = headers,
        )
    }

    suspend fun settingsGetAllJson(): String =
        client.sendJson(
            path = "/api/settings",
            method = HttpMethod.Get,
            headers = authHeaders(),
        )

    suspend fun settingsUpdateJson(
        bodyJson: String,
        headers: Map<String, String> = authHeaders(),
    ): String =
        client.sendJson(
            path = "/api/settings",
            method = HttpMethod.Patch,
            body = bodyJson,
            headers = headers,
        )

    suspend fun settingsTestS3(
        filesystem: String = "storage",
        headers: Map<String, String> = authHeaders(),
    ) {
        client.sendJson(
            path = "/api/settings/test/s3",
            method = HttpMethod.Post,
            body = """{"filesystem":"$filesystem"}""",
            headers = headers,
        )
    }

    suspend fun settingsTestEmail(
        email: String,
        template: String,
        collectionIdOrName: String? = null,
        headers: Map<String, String> = authHeaders(),
    ) {
        val body =
            buildJsonObject {
                put("email", email)
                put("template", template)
                collectionIdOrName?.let { put("collection", it) }
            }.toString()

        client.sendJson(
            path = "/api/settings/test/email",
            method = HttpMethod.Post,
            body = body,
            headers = headers,
        )
    }

    suspend fun settingsGenerateAppleClientSecretJson(
        bodyJson: String,
        headers: Map<String, String> = authHeaders(),
    ): String =
        client.sendJson(
            path = "/api/settings/apple/generate-client-secret",
            method = HttpMethod.Post,
            body = bodyJson,
            headers = headers,
        )

    suspend fun collectionsGetListJson(
        page: Int = 1,
        perPage: Int = 30,
        skipTotal: Boolean = false,
        expand: String? = null,
        filter: String? = null,
        sort: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String {
        val enrichedQuery =
            query +
                mapOf(
                    "page" to page.toString(),
                    "perPage" to perPage.toString(),
                    "skipTotal" to skipTotal.toString(),
                ) +
                mapOf(
                    "expand" to expand,
                    "filter" to filter,
                    "sort" to sort,
                    "fields" to fields,
                ).filterValues { it != null }.mapValues { it.value!! }

        return client.sendJson(
            path = "/api/collections",
            method = HttpMethod.Get,
            query = enrichedQuery,
            headers = headers,
        )
    }

    suspend fun collectionsGetOneJson(
        id: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String =
        client.sendJson(
            path = "/api/collections/$id",
            method = HttpMethod.Get,
            query = query,
            headers = headers,
        )

    suspend fun collectionsGetFullListJson(
        batch: Int = 1000,
        expand: String? = null,
        filter: String? = null,
        sort: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String {
        val items = mutableListOf<kotlinx.serialization.json.JsonElement>()
        var page = 1

        while (true) {
            val pageResponse =
                collectionsGetListJson(
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
            val parsed = json.parseToJsonElement(pageResponse).jsonObject
            val pageItems = parsed["items"]?.jsonArray ?: JsonArray(emptyList())
            items.addAll(pageItems)
            if (pageItems.size < batch) break
            page += 1
        }

        return buildJsonArray { items.forEach { add(it) } }.toString()
    }

    suspend fun collectionsGetFirstListItemJson(
        filter: String,
        expand: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String {
        val list =
            collectionsGetListJson(
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

        val first = json.parseToJsonElement(list).jsonObject["items"]?.jsonArray?.firstOrNull()
        if (first == null) {
            throw io.pocketbase.http.ClientException(
                statusCode = 404,
                url = "/api/collections",
                data = null,
            )
        }
        return first.toString()
    }

    suspend fun collectionsCreateJson(
        bodyJson: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String =
        client.sendJson(
            path = "/api/collections",
            method = HttpMethod.Post,
            body = bodyJson,
            query = query,
            headers = headers,
        )

    suspend fun collectionsUpdateJson(
        id: String,
        bodyJson: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String =
        client.sendJson(
            path = "/api/collections/$id",
            method = HttpMethod.Patch,
            body = bodyJson,
            query = query,
            headers = headers,
        )

    suspend fun collectionsDelete(
        id: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ) {
        client.sendJson(
            path = "/api/collections/$id",
            method = HttpMethod.Delete,
            query = query,
            headers = headers,
        )
    }

    suspend fun collectionsImportJson(
        bodyJson: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ) {
        client.sendJson(
            path = "/api/collections/import",
            method = HttpMethod.Put,
            body = bodyJson,
            query = query,
            headers = headers,
        )
    }

    suspend fun collectionsGetScaffoldsJson(
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String =
        client.sendJson(
            path = "/api/collections/meta/scaffolds",
            method = HttpMethod.Get,
            query = query,
            headers = headers,
        )

    suspend fun collectionsTruncate(
        collectionIdOrName: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ) {
        client.sendJson(
            path = "/api/collections/$collectionIdOrName/truncate",
            method = HttpMethod.Delete,
            query = query,
            headers = headers,
        )
    }

    suspend fun logsGetListJson(
        page: Int = 1,
        perPage: Int = 30,
        skipTotal: Boolean = false,
        expand: String? = null,
        filter: String? = null,
        sort: String? = null,
        fields: String? = null,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String {
        val enrichedQuery =
            query +
                mapOf(
                    "page" to page.toString(),
                    "perPage" to perPage.toString(),
                    "skipTotal" to skipTotal.toString(),
                ) +
                mapOf(
                    "expand" to expand,
                    "filter" to filter,
                    "sort" to sort,
                    "fields" to fields,
                ).filterValues { it != null }.mapValues { it.value!! }

        return client.sendJson(
            path = "/api/logs",
            method = HttpMethod.Get,
            query = enrichedQuery,
            headers = headers,
        )
    }

    suspend fun logsGetOneJson(
        id: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String =
        client.sendJson(
            path = "/api/logs/$id",
            method = HttpMethod.Get,
            query = query,
            headers = headers,
        )

    suspend fun logsGetStatsJson(
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String =
        client.sendJson(
            path = "/api/logs/stats",
            method = HttpMethod.Get,
            query = query,
            headers = headers,
        )

    suspend fun backupsGetFullListJson(
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String =
        client.sendJson(
            path = "/api/backups",
            method = HttpMethod.Get,
            query = query,
            headers = headers,
        )

    suspend fun backupsCreate(
        basename: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ) {
        client.sendJson(
            path = "/api/backups",
            method = HttpMethod.Post,
            body = """{"name":"$basename"}""",
            query = query,
            headers = headers,
        )
    }

    suspend fun backupsUploadJson(
        bodyJson: String? = null,
        files: List<File> = emptyList(),
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ) {
        val bodyPayload = bodyJson.takeIf { files.isEmpty() }
        val enrichedFiles =
            if (!bodyJson.isNullOrBlank() && files.isNotEmpty()) {
                files + File(field = "@jsonPayload", data = bodyJson.encodeToByteArray())
            } else {
                files
            }

        client.sendJson(
            path = "/api/backups/upload",
            method = HttpMethod.Post,
            body = bodyPayload,
            files = enrichedFiles,
            query = query,
            headers = headers,
        )
    }

    suspend fun backupsDelete(
        key: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ) {
        client.sendJson(
            path = "/api/backups/$key",
            method = HttpMethod.Delete,
            query = query,
            headers = headers,
        )
    }

    suspend fun backupsRestore(
        key: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ) {
        client.sendJson(
            path = "/api/backups/$key/restore",
            method = HttpMethod.Post,
            query = query,
            headers = headers,
        )
    }

    fun backupsGetDownloadURL(
        token: String,
        key: String,
    ): String = client.buildUrl(path = "/api/backups/$key", query = mapOf("token" to token))

    @Deprecated("Please use backupsGetDownloadURL()")
    fun backupsGetDownloadUrl(
        token: String,
        key: String,
    ): String = backupsGetDownloadURL(token, key)

    suspend fun cronsGetFullListJson(
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String =
        client.sendJson(
            path = "/api/crons",
            method = HttpMethod.Get,
            query = query,
            headers = headers,
        )

    suspend fun cronsRun(
        jobId: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ) {
        client.sendJson(
            path = "/api/crons/$jobId",
            method = HttpMethod.Post,
            query = query,
            headers = headers,
        )
    }

    suspend fun batchSendJson(
        requestsJson: String,
        bodyJson: String? = null,
        files: List<File> = emptyList(),
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = authHeaders(),
    ): String {
        val payload =
            buildJsonObject {
                put("requests", json.parseToJsonElement(requestsJson))
                if (!bodyJson.isNullOrBlank()) {
                    val extra = json.parseToJsonElement(bodyJson)
                    if (extra is JsonObject) {
                        extra.forEach { (k, v) -> put(k, v) }
                    }
                }
            }.toString()

        val bodyPayload = payload.takeIf { files.isEmpty() }
        val enrichedFiles =
            if (files.isNotEmpty()) {
                files + File(field = "@jsonPayload", data = payload.encodeToByteArray())
            } else {
                files
            }

        return client.sendJson(
            path = "/api/batch",
            method = HttpMethod.Post,
            body = bodyPayload,
            files = enrichedFiles,
            query = query,
            headers = headers,
        )
    }

    private fun bridge(collection: String): AppleRecordBridge = client.collection(collection)

    private fun loadSession() {
        val defaults = NSUserDefaults.standardUserDefaults
        authToken = defaults.stringForKey(tokenStorageKey())
        authRecordJson = defaults.stringForKey(recordStorageKey())
    }

    private fun persistSession(
        token: String,
        recordJson: String?,
    ) {
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.setObject(token, forKey = tokenStorageKey())

        if (recordJson.isNullOrBlank()) {
            defaults.removeObjectForKey(recordStorageKey())
        } else {
            defaults.setObject(recordJson, forKey = recordStorageKey())
        }
    }

    private fun tokenStorageKey(): String = "$sessionKey.token"

    private fun recordStorageKey(): String = "$sessionKey.record"
}
