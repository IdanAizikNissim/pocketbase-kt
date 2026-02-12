package io.pocketbase.services

import io.ktor.client.call.body
import io.pocketbase.PocketBase
import io.pocketbase.dtos.HourlyStats
import io.pocketbase.dtos.LogModel
import io.pocketbase.dtos.ResultList
import io.pocketbase.http.ClientException

class LogService internal constructor(
    client: PocketBase,
) : BaseService(client) {
    suspend fun getList(
        page: Int = 1,
        perPage: Int = 30,
        skipTotal: Boolean = false,
        expand: String? = null,
        filter: String? = null,
        sort: String? = null,
        fields: String? = null,
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): ResultList<LogModel> {
        val enrichedQuery =
            query +
                mapOfNotNull(
                    "page" to page,
                    "perPage" to perPage,
                    "expand" to expand,
                    "filter" to filter,
                    "sort" to sort,
                    "fields" to fields,
                    "skipTotal" to skipTotal,
                )

        return client.send(
            path = "/api/logs",
            query = enrichedQuery,
            headers = headers,
        ).body()
    }

    suspend fun getOne(
        id: String,
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): LogModel {
        if (id.isBlank()) {
            throw ClientException(
                url = "/api/logs",
                statusCode = 404,
                data = null,
            )
        }

        return client.send(
            path = "/api/logs/$id",
            query = query,
            headers = headers,
        ).body()
    }

    suspend fun getStats(
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): List<HourlyStats> {
        return client.send(
            path = "/api/logs/stats",
            query = query,
            headers = headers,
        ).body()
    }
}
