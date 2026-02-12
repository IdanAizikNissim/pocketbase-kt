package io.pocketbase.services

import io.ktor.client.call.body
import io.ktor.http.HttpMethod
import io.pocketbase.PocketBase
import io.pocketbase.dtos.BackupFileInfo
import io.pocketbase.dtos.File

class BackupService internal constructor(
    client: PocketBase,
) : BaseService(client) {
    suspend fun getFullList(
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): List<BackupFileInfo> {
        return client.send(
            path = "/api/backups",
            method = HttpMethod.Get,
            query = query,
            headers = headers,
        ).body()
    }

    suspend fun create(
        basename: String,
        body: Map<String, Any?> = emptyMap(),
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): Boolean {
        val enrichedBody = body + mapOf("name" to basename)

        client.send(
            path = "/api/backups",
            method = HttpMethod.Post,
            body = enrichedBody,
            query = query,
            headers = headers,
        )

        return true
    }

    suspend fun upload(
        body: Any? = null,
        files: List<File> = emptyList(),
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): Boolean {
        client.send(
            path = "/api/backups/upload",
            method = HttpMethod.Post,
            body = body,
            files = files,
            query = query,
            headers = headers,
        )

        return true
    }

    suspend fun delete(
        key: String,
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): Boolean {
        client.send(
            path = "/api/backups/$key",
            method = HttpMethod.Delete,
            query = query,
            headers = headers,
        )

        return true
    }

    suspend fun restore(
        key: String,
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): Boolean {
        client.send(
            path = "/api/backups/$key/restore",
            method = HttpMethod.Post,
            query = query,
            headers = headers,
        )

        return true
    }

    @Deprecated("Please use getDownloadURL()")
    fun getDownloadUrl(
        token: String,
        key: String,
    ): String = getDownloadURL(token, key)

    fun getDownloadURL(
        token: String,
        key: String,
    ): String {
        return client.buildUrl(
            path = "/api/backups/$key",
            queryParameters = mapOf("token" to token),
        )
    }
}
