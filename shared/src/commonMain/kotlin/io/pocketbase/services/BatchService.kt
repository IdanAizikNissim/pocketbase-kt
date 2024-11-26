package io.pocketbase.services

import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.pocketbase.PocketBase
import io.pocketbase.dtos.BatchResult
import io.pocketbase.dtos.File
import io.pocketbase.dtos.RecordModel
import io.pocketbase.http.json
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

class BatchService internal constructor(
    client: PocketBase,
) : BaseService(
        client = client,
    ) {
    private val requests = mutableListOf<BatchRequest>()
    private val subs = mutableMapOf<String, SubBatchService<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : RecordModel> collection(collectionIdOrName: String): SubBatchService<T> {
        val subService =
            (subs[collectionIdOrName] as? SubBatchService<T>) ?: SubBatchService<T>(
                batch = this,
                collectionIdOrName = collectionIdOrName,
            )
        subs[collectionIdOrName] = subService

        return subService
    }

    suspend fun send(
        body: Map<String, Any?> = emptyMap(),
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): List<BatchResult> {
        val jsonBody = mutableListOf<RawRequest>()
        val files = mutableListOf<File>()

        requests.forEach { request ->
            jsonBody +=
                RawRequest(
                    method = request.method.value,
                    url = request.url,
                    headers = request.headers,
                    body = request.body,
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

        return json.decodeFromString(ListSerializer(BatchResult.serializer()), response.bodyAsText())
    }

    class SubBatchService<T : RecordModel> internal constructor(
        private val batch: BatchService,
        private val collectionIdOrName: String,
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
                        ),
                    headers = headers,
                    body = body,
                    files = files,
                )

            batch.requests += request
        }
    }
}

class BatchRequest internal constructor(
    val method: HttpMethod,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: @Serializable Any? = null,
    val files: List<File> = emptyList(),
)

@Serializable
private data class RawRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val body:
        @Contextual @Serializable
        Any?,
)
