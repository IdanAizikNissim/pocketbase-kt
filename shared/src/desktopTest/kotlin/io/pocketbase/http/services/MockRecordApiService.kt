package io.pocketbase.http.services

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.pocketbase.http.fromRequest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal class MockRecordApiService(
    private val json: Json,
) : MockApiService {
    private val records = mutableMapOf<String, JsonElement>()

    private val JsonElement.created
        get() = jsonObject["created"]?.jsonPrimitive?.content

    private val JsonElement.title
        get() = jsonObject["title"]?.jsonPrimitive?.content

    private val JsonElement.id
        get() = jsonObject["id"]?.jsonPrimitive?.content

    override suspend fun handle(
        request: HttpRequestData,
        handleScope: MockRequestHandleScope,
    ): HttpResponseData =
        with(request) {
            when (method) {
                HttpMethod.Get -> if (url.encodedPath.endsWith("records")) list(request, handleScope) else getOne(request, handleScope)
                HttpMethod.Post -> create(request, handleScope)
                HttpMethod.Patch -> update(request, handleScope)
                HttpMethod.Delete -> delete(request, handleScope)
                else -> handleScope.respondBadRequest()
            }
        }

    private fun HttpRequestData.getIdOrNull(): String? {
        val regex = "/api/collections/[^/]+/records/([^/]+)".toRegex()
        val matchResult = regex.find(url.encodedPath)
        return matchResult?.groups?.get(1)?.value
    }

    private fun getOne(
        request: HttpRequestData,
        handleScope: MockRequestHandleScope,
    ): HttpResponseData {
        val id = request.getIdOrNull() ?: return handleScope.respondBadRequest()
        val record = records[id] ?: return handleScope.respondBadRequest()

        return handleScope.respond(
            content = ByteReadChannel(recordMockJson(id, record.title ?: "", record.created ?: "")),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }

    private fun list(
        request: HttpRequestData,
        handleScope: MockRequestHandleScope,
    ): HttpResponseData {
        val filter = request.url.parameters["filter"]
        val title = "title = '(.+)'".toRegex().find(filter ?: "")?.groups?.get(1)?.value ?: return handleScope.respondBadRequest()
        val recordKey =
            records.keys.find { records[it]?.title == title } ?: return handleScope.respond(
                content =
                    ByteReadChannel(
                        """
                        {
                            "page": 1,
                            "perPage": 0,
                            "totalItems": 0,
                            "totalPages": 1,
                            "items": []
                        }
                        """.trimIndent(),
                    ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )

        return handleScope.respond(
            content =
                ByteReadChannel(
                    """
                    {
                        "page": 1,
                        "perPage": 100,
                        "totalItems": 1,
                        "totalPages": 1,
                        "items": [
                            ${recordMockJson(recordKey, title, records[recordKey]?.created ?: "")}
                        ]
                    }
                    """.trimIndent(),
                ),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }

    private suspend fun create(
        request: HttpRequestData,
        handleScope: MockRequestHandleScope,
    ): HttpResponseData {
        val record = json.fromRequest(request)
        val uuid = Uuid.random().toString()
        val created = Clock.System.now().toLocalDateTime(TimeZone.UTC).format()
        records[uuid] =
            record.jsonObject.toMutableMap().apply {
                put("created", JsonPrimitive(created))
            }.let { JsonObject(it) }

        return handleScope.respond(
            content =
                ByteReadChannel(
                    recordMockJson(
                        uuid = uuid,
                        title = record.title ?: "",
                        created = created,
                    ),
                ),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }

    private suspend fun update(
        request: HttpRequestData,
        handleScope: MockRequestHandleScope,
    ): HttpResponseData {
        val updatedRecord = json.fromRequest(request)
        val id = (request.getIdOrNull() ?: updatedRecord.id?.takeIf { it.isNotBlank() }) ?: return handleScope.respondBadRequest()
        records[id] ?: return handleScope.respondBadRequest()
        records[id] =
            updatedRecord.jsonObject.toMutableMap().apply {
                put("created", JsonPrimitive(records[id]?.created))
            }.let { JsonObject(it) }

        return handleScope.respond(
            content =
                ByteReadChannel(
                    recordMockJson(
                        uuid = id,
                        title = updatedRecord.title ?: "",
                        created = records[id]?.created ?: "",
                        updated = Clock.System.now().toLocalDateTime(TimeZone.UTC).format(),
                    ),
                ),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }

    private fun delete(
        request: HttpRequestData,
        handleScope: MockRequestHandleScope,
    ): HttpResponseData {
        val id = request.getIdOrNull() ?: return handleScope.respondBadRequest()
        val record = records.remove(id)

        return handleScope.respond(
            content =
                ByteReadChannel(
                    recordMockJson(
                        uuid = id,
                        title = record?.title ?: "",
                        created = records[id]?.created ?: "",
                    ),
                ),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }

    private fun recordMockJson(
        uuid: String,
        title: String,
        created: String,
        updated: String = created,
    ): String {
        return """
            {
              "@collectionId": "a98f514eb05f454",
              "@collectionName": "demo",
              "id": "$uuid",
              "updated": "$updated",
              "created": "$created",
              "title": "$title"
            }
            """.trimIndent()
    }

    private fun LocalDateTime.format(): String {
        return buildString {
            append(year.toString().padStart(4, '0'))
            append('-')
            append(monthNumber.toString().padStart(2, '0'))
            append('-')
            append(dayOfMonth.toString().padStart(2, '0'))
            append(' ')
            append(hour.toString().padStart(2, '0'))
            append(':')
            append(minute.toString().padStart(2, '0'))
            append(':')
            append(second.toString().padStart(2, '0'))
            append('.')
            append(nanosecond.toString().padStart(9, '0').substring(0, 3))
        }
    }
}
