package io.pocketbase

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpMethod
import io.pocketbase.auth.AuthStore
import io.pocketbase.dtos.File
import io.pocketbase.dtos.RecordModel
import io.pocketbase.http.ClientException
import io.pocketbase.http.HttpClient
import io.pocketbase.http.SSEClient
import io.pocketbase.http.json
import io.pocketbase.services.BatchService
import io.pocketbase.services.FileService
import io.pocketbase.services.HealthService
import io.pocketbase.services.RealtimeService
import io.pocketbase.services.RecordService
import io.pocketbase.services.SettingsService
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime

class PocketBase(
    config: ClientConfig,
    val authStore: AuthStore = AuthStore(),
) {
    private val recordServices = mutableMapOf<String, RecordService<*>>()
    private val client = HttpClient(authStore, config)

    val healthCheck = HealthService(this)
    val files = FileService(this)
    val settings = SettingsService(this)
    internal val realtime = RealtimeService(this, SSEClient(config, authStore))

    constructor(url: String, authStore: AuthStore = AuthStore()) : this(
        config = ClientConfig.from(url),
        authStore = authStore,
    )

    inline fun <reified T : RecordModel> collection(idOrName: String): RecordService<T> = collection(idOrName, T::class)

    @Suppress("UNCHECKED_CAST")
    fun <T : RecordModel> collection(
        idOrName: String,
        cls: KClass<T>,
    ): RecordService<T> {
        val service =
            (recordServices[idOrName] as? RecordService<T>) ?: RecordService(
                client = this,
                cls = cls,
                collectionIdOrName = idOrName,
            )

        recordServices[idOrName] = service

        return service
    }

    fun createBatch(): BatchService = BatchService(this)

    @OptIn(ExperimentalTime::class)
    fun filter(
        expr: String,
        query: Map<String, Any?> = emptyMap(),
    ): String {
        var result = expr

        if (query.isEmpty()) {
            return result
        }

        query.forEach { (key, value) ->
            val replacement =
                when (value) {
                    null -> "null"
                    is Number, is Boolean -> value.toString()
                    is Instant -> "'${value.toLocalDateTime(kotlinx.datetime.TimeZone.UTC).toString().replace("T", " ")}'"
                    is String -> "'${value.replace("'", "\\'")}'"
                    else -> "'${json.encodeToString(value).replace("'", "\\'")}'"
                }

            result = result.replace("{:$key}", replacement)
        }

        return result
    }

    @Throws(ClientException::class, CancellationException::class)
    internal suspend fun send(
        path: String,
        method: HttpMethod = HttpMethod.Get,
        headers: Map<String, String> = emptyMap(),
        query: Map<String, Any?> = emptyMap(),
        body: @Serializable Any? = null,
        files: List<File> = emptyList(),
    ): HttpResponse {
        val response =
            client.send(
                path = path,
                method = method,
                headers = headers,
                query = query,
                body = body,
                files = files,
            )

        if (response.status.value >= 400) {
            throw ClientException(
                statusCode = response.status.value,
                url = response.request.url.toString(),
                data =
                    try {
                        json.decodeFromString<JsonObject>(response.bodyAsText())
                    } catch (e: SerializationException) {
                        null
                    },
            )
        } else {
            return response
        }
    }

    internal fun buildUrl(
        path: String,
        queryParameters: Map<String, Any?> = emptyMap(),
        includeHost: Boolean = true,
    ): String =
        client.buildUrl(
            path = path,
            queryParameters = queryParameters,
            includeHost = includeHost,
        )
}
