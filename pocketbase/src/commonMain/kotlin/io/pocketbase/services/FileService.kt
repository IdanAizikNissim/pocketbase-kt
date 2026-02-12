package io.pocketbase.services

import io.ktor.client.call.body
import io.ktor.http.HttpMethod
import io.pocketbase.PocketBase
import io.pocketbase.dtos.RecordModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class FileService internal constructor(
    client: PocketBase,
) : BaseService(
        client = client,
    ) {
    @Deprecated("Please use getURL()")
    fun getUrl(
        record: RecordModel,
        fileName: String,
        thumb: String? = null,
        token: String? = null,
        download: Boolean? = null,
        query: Map<String, Any?> = emptyMap(),
    ): String {
        return getURL(record, fileName, thumb, token, download, query)
    }

    fun getURL(
        record: RecordModel,
        fileName: String,
        thumb: String? = null,
        token: String? = null,
        download: Boolean? = null,
        query: Map<String, Any?> = emptyMap(),
    ): String {
        if (fileName.isBlank() || record.id.isNullOrBlank()) {
            return ""
        }

        val params =
            query +
                mapOfNotNull(
                    "thumb" to thumb,
                    "token" to token,
                    "download" to "".takeIf { download == true },
                )

        return client.buildUrl(
            path = "/api/files/${record.collectionId}/${record.id}/$fileName",
            queryParameters = params,
        )
    }

    suspend fun getToken(
        body: @Serializable Any? = null,
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): String? {
        return client.send(
            path = "/api/files/token",
            method = HttpMethod.Post,
            body = body,
            headers = headers,
            query = query,
        ).body<JsonObject>()["token"]?.jsonPrimitive?.content
    }
}
