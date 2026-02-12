package io.pocketbase

import io.pocketbase.apple.ApplePocketBaseClient
import io.pocketbase.apple.AppleRecordBridge
import io.pocketbase.apple.AppleSubscription
import io.pocketbase.dtos.File
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
