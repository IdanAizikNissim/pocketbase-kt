package io.pocketbase.services

import io.ktor.client.call.body
import io.ktor.http.HttpMethod
import io.pocketbase.PocketBase
import io.pocketbase.dtos.CronJob

class CronService internal constructor(
    client: PocketBase,
) : BaseService(client) {
    suspend fun getFullList(
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): List<CronJob> {
        return client.send(
            path = "/api/crons",
            method = HttpMethod.Get,
            query = query,
            headers = headers,
        ).body()
    }

    suspend fun run(
        jobId: String,
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): Boolean {
        client.send(
            path = "/api/crons/$jobId",
            method = HttpMethod.Post,
            query = query,
            headers = headers,
        )

        return true
    }
}
