package io.pocketbase.http.services

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.pocketbase.http.fromRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal class MockAdminApiService(
    private val json: Json,
) : MockApiService {
    private var adminId: String? = null
    private var adminToken: String? = null

    override suspend fun handle(
        request: HttpRequestData,
        handleScope: MockRequestHandleScope,
    ): HttpResponseData =
        with(request) {
            return when {
                url.encodedPath.endsWith("auth-with-password") ->
                    authWithPassword(
                        json = json.fromRequest(request),
                        handleScope = handleScope,
                    )
                else -> handleScope.respondBadRequest()
            }
        }

    private fun authWithPassword(
        json: JsonElement,
        handleScope: MockRequestHandleScope,
    ): HttpResponseData {
        val identity = json.jsonObject["identity"]?.jsonPrimitive?.content
        val password = json.jsonObject["password"]?.jsonPrimitive?.content

        return if (identity == null || password == null) {
            handleScope.respondBadRequest()
        } else {
            val id = Uuid.random().toString()
            val token = Uuid.random().toString()
            adminId = id
            adminToken = token

            handleScope.respond(
                content =
                    ByteReadChannel(
                        """
                        {
                          "token": "$token",
                          "admin": {
                            "id": "$id",
                            "created": "2022-06-22 07:13:09.735Z",
                            "updated": "2022-06-22 07:13:09.735Z",
                            "email": "$identity",
                            "avatar": 0
                          }
                        }
                        """.trimIndent(),
                    ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
    }
}
