package io.pocketbase.services

import io.ktor.client.call.body
import io.pocketbase.PocketBase
import io.pocketbase.dtos.HealthCheck

class HealthService internal constructor(
    client: PocketBase,
) : BaseService(client) {
    suspend fun check(
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): HealthCheck {
        return client.send(
            path = "/api/health",
            query = query,
            headers = headers,
        ).body()
    }
}
