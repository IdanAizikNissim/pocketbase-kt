package io.pocketbase.services

import io.ktor.client.call.body
import io.ktor.http.HttpMethod
import io.pocketbase.PocketBase
import io.pocketbase.dtos.CollectionModel

class CollectionService internal constructor(
    client: PocketBase,
) : BaseCRUDService<CollectionModel>(
        client = client,
        cls = CollectionModel::class,
        baseCrudPath = "/api/collections",
    ) {
    suspend fun `import`(
        collections: List<CollectionModel>,
        deleteMissing: Boolean = false,
        body: Map<String, Any?> = emptyMap(),
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): Boolean {
        val enrichedBody =
            body +
                mapOf(
                    "collections" to collections,
                    "deleteMissing" to deleteMissing,
                )

        client.send(
            path = "$baseCrudPath/import",
            method = HttpMethod.Put,
            body = enrichedBody,
            query = query,
            headers = headers,
        )

        return true
    }

    suspend fun getScaffolds(
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): Map<String, CollectionModel> {
        return client.send(
            path = "$baseCrudPath/meta/scaffolds",
            method = HttpMethod.Get,
            query = query,
            headers = headers,
        ).body()
    }

    suspend fun truncate(
        collectionIdOrName: String,
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): Boolean {
        client.send(
            path = "$baseCrudPath/$collectionIdOrName/truncate",
            method = HttpMethod.Delete,
            query = query,
            headers = headers,
        )

        return true
    }
}
