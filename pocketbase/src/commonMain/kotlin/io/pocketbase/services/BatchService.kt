package io.pocketbase.services

import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.pocketbase.PocketBase
import io.pocketbase.dtos.BatchResult
import io.pocketbase.dtos.File
import io.pocketbase.dtos.RecordModel
import io.pocketbase.http.json
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

class BatchService internal constructor(
    client: PocketBase,
) : BaseService(
        client = client,
    ) {
    private val requests = mutableListOf<BatchRequest<*>>()
    private val subs = mutableMapOf<String, SubBatchService<*>>()

    inline fun <reified T : RecordModel> collection(idOrName: String): SubBatchService<T> = collection(idOrName, T::class)

    @Suppress("UNCHECKED_CAST")
    fun <T : RecordModel> collection(
        collectionIdOrName: String,
        cls: KClass<T>,
    ): SubBatchService<T> {
        val subService =
            (subs[collectionIdOrName] as? SubBatchService<T>) ?: SubBatchService(
                batch = this,
                collectionIdOrName = collectionIdOrName,
                cls = cls,
            )
        subs[collectionIdOrName] = subService

        return subService
    }

    suspend fun send(
        body: Map<String, Any?> = emptyMap(),
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): List<BatchResult<*>> {
        val jsonBody = ArrayList<RawRequest>(requests.size)
        val files = ArrayList<File>(requests.sumOf { it.files.size })

        requests.forEach { request ->
            jsonBody +=
                RawRequest(
                    method = request.method.value,
                    url = request.url,
                    headers = request.headers,
                    body = request.serializedBody,
                )

            request.files.forEachIndexed { index, file ->
                files +=
                    File(
                        field = "requests.$index.${file.field}",
                        data = file.data,
                        fileName = file.fileName,
                    )
            }
        }

        val enrichedBody =
            body +
                mapOf(
                    "requests" to jsonBody,
                )

        val response =
            client.send(
                path = "/api/batch",
                method = HttpMethod.Post,
                body = enrichedBody,
                query = query,
                headers = headers,
                files = files,
            )

        val jsonElement = json.parseToJsonElement(response.bodyAsText())
        return jsonElement.jsonArray.mapNotNull { json ->
            val jsonObject = json.jsonObject
            if (!jsonObject.keys.contains("body")) {
                null
            } else {
                BatchResult(
                    status = jsonObject["status"]?.jsonPrimitive?.intOrNull,
                    body =
                        if (jsonObject["body"] is JsonNull) {
                            null
                        } else {
                            val jsonObjectBody = jsonObject["body"]?.jsonObject!!
                            val sub =
                                subs[jsonObjectBody["collectionName"]?.jsonPrimitive?.content]
                                    ?: subs[jsonObjectBody["collectionId"]?.jsonPrimitive?.content]

                            sub?.parseResponse(jsonObjectBody)
                        },
                )
            }
        }
    }

    class SubBatchService<T : RecordModel> internal constructor(
        private val batch: BatchService,
        private val collectionIdOrName: String,
        private val cls: KClass<T>,
    ) {
        fun upsert(
            body: @Serializable T? = null,
            query: Map<String, Any?> = emptyMap(),
            files: List<File> = emptyList(),
            headers: Map<String, String> = emptyMap(),
            expand: String? = null,
            fields: String? = null,
        ) = request(
            method = HttpMethod.Put,
            body = body,
            query = query,
            files = files,
            headers = headers,
            expand = expand,
            fields = fields,
        )

        fun create(
            body: @Serializable T? = null,
            query: Map<String, Any?> = emptyMap(),
            files: List<File> = emptyList(),
            headers: Map<String, String> = emptyMap(),
            expand: String? = null,
            fields: String? = null,
        ) = request(
            method = HttpMethod.Post,
            body = body,
            query = query,
            files = files,
            headers = headers,
            expand = expand,
            fields = fields,
        )

        fun update(
            recordId: String,
            body: @Serializable T? = null,
            query: Map<String, Any?> = emptyMap(),
            files: List<File> = emptyList(),
            headers: Map<String, String> = emptyMap(),
            expand: String? = null,
            fields: String? = null,
        ) = request(
            method = HttpMethod.Patch,
            body = body,
            query = query,
            files = files,
            headers = headers,
            expand = expand,
            fields = fields,
            path = { "$it/$recordId" },
        )

        fun delete(
            recordId: String,
            body: @Serializable T? = null,
            query: Map<String, Any?> = emptyMap(),
            headers: Map<String, String> = emptyMap(),
        ) = request(
            method = HttpMethod.Delete,
            body = body,
            query = query,
            headers = headers,
            path = { "$it/$recordId" },
        )

        private fun request(
            method: HttpMethod,
            body: @Serializable T? = null,
            query: Map<String, Any?> = emptyMap(),
            files: List<File> = emptyList(),
            headers: Map<String, String> = emptyMap(),
            expand: String? = null,
            fields: String? = null,
            path: (String) -> String = { it },
        ) {
            val enrichedQuery =
                query +
                    mapOfNotNull(
                        "expand" to expand,
                        "fields" to fields,
                    )

            val request =
                BatchRequest(
                    method = method,
                    url =
                        batch.client.buildUrl(
                            path = path("/api/collections/$collectionIdOrName/records"),
                            queryParameters = enrichedQuery,
                            includeHost = false,
                        ),
                    headers = headers,
                    body = body,
                    files = files,
                    cls = cls,
                )

            batch.requests += request
        }

        @OptIn(InternalSerializationApi::class)
        internal fun parseResponse(jsonElement: JsonElement): T = json.decodeFromJsonElement(cls.serializer(), jsonElement)
    }
}

data class BatchRequest<T : RecordModel> internal constructor(
    val method: HttpMethod,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: @Serializable T? = null,
    val files: List<File> = emptyList(),
    val cls: KClass<T>,
) {
    @OptIn(InternalSerializationApi::class)
    val serializedBody: JsonElement? by lazy {
        if (body == null) {
            null
        } else {
            json.encodeToJsonElement(cls.serializer(), body)
        }
    }
}

@Serializable
data class RawRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val body: JsonElement?,
)
