package io.pocketbase.services

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.utils.io.core.toByteArray
import io.pocketbase.PocketBase
import io.pocketbase.dtos.File
import io.pocketbase.dtos.ResultList
import io.pocketbase.http.ClientException
import io.pocketbase.http.json
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

abstract class BaseCRUDService<T : @Serializable Any> internal constructor(
    client: PocketBase,
    protected val cls: KClass<T>,
    protected val baseCrudPath: String,
) : BaseService(client) {
    @OptIn(InternalSerializationApi::class)
    protected val serializer = cls.serializer()

    private val resultListSerializer = ResultList.serializer(serializer)

    suspend fun getFullList(
        batch: Int = 1000,
        expand: String? = null,
        filter: String? = null,
        sort: String? = null,
        fields: String? = null,
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): List<T> {
        val result = mutableListOf<T>()

        val request =
            y { request: suspend (Int) -> List<T> ->
                { page ->
                    val list =
                        getList(
                            page = page,
                            perPage = batch,
                            skipTotal = true,
                            expand = expand,
                            filter = filter,
                            sort = sort,
                            fields = fields,
                            query = query,
                            headers = headers,
                        )

                    result += list.items

                    if (list.items.size == list.perPage) {
                        request(page + 1)
                    } else {
                        result
                    }
                }
            }

        return request(1)
    }

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
    ): ResultList<T> {
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
            path = baseCrudPath,
            query = enrichedQuery,
            headers = headers,
        ).list()
    }

    suspend fun getOne(
        id: String,
        expand: String? = null,
        fields: String? = null,
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): T {
        if (id.isBlank()) {
            throw ClientException(
                url = baseCrudPath,
                statusCode = 404,
                data = null,
            )
        }

        val enrichedQuery =
            query +
                mapOfNotNull(
                    "expand" to expand,
                    "fields" to fields,
                )

        return client.send(
            path = "$baseCrudPath/$id",
            query = enrichedQuery,
            headers = headers,
        ).result()
    }

    suspend fun getFirstListItem(
        filter: String,
        expand: String? = null,
        fields: String? = null,
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): T? {
        return getList(
            perPage = 1,
            skipTotal = true,
            filter = filter,
            expand = expand,
            fields = fields,
            query = query,
            headers = headers,
        ).items.firstOrNull()
    }

    suspend fun create(
        body: T? = null,
        expand: String? = null,
        fields: String? = null,
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        files: List<File> = emptyList(),
    ): T {
        val enrichedQuery =
            query +
                mapOfNotNull(
                    "expand" to expand,
                    "fields" to fields,
                )

        val enrichedFiles =
            body?.let {
                files.takeIf { it.isEmpty() } ?: (
                    files +
                        listOf(
                            File(
                                field = "@jsonPayload",
                                data = body.encodeToString().toByteArray(),
                            ),
                        )
                )
            } ?: files

        return client.send(
            path = baseCrudPath,
            method = HttpMethod.Post,
            body = body,
            query = enrichedQuery,
            headers = headers,
            files = enrichedFiles,
        ).result()
    }

    open suspend fun update(
        id: String,
        body: T? = null,
        expand: String? = null,
        fields: String? = null,
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        files: List<File> = emptyList(),
    ): T {
        val enrichedQuery =
            query +
                mapOfNotNull(
                    "expand" to expand,
                    "fields" to fields,
                )

        return client.send(
            path = "$baseCrudPath/$id",
            method = HttpMethod.Patch,
            body = body,
            query = enrichedQuery,
            headers = headers,
            files = files,
        ).result()
    }

    open suspend fun delete(
        id: String,
        body: T? = null,
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) {
        client.send(
            path = "$baseCrudPath/$id",
            method = HttpMethod.Delete,
            body = body,
            query = query,
            headers = headers,
        )
    }

    private suspend fun HttpResponse.result(): T = json.decodeFromString(serializer, bodyAsText())

    private suspend fun HttpResponse.list(): ResultList<T> = json.decodeFromString(resultListSerializer, bodyAsText())

    private fun T.encodeToString() = json.encodeToString(serializer, this)
}

private fun <T, R> y(f: (suspend (T) -> R) -> suspend (T) -> R): suspend (T) -> R {
    return { x -> f(y(f))(x) }
}

internal fun <K, V> mapOfNotNull(vararg pairs: Pair<K, V?>): Map<K, V> {
    return pairs.mapNotNull { (key, value) ->
        value?.let { key to it }
    }.toMap()
}
