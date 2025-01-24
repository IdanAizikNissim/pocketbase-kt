package io.pocketbase.http.services

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Url
import kotlinx.serialization.json.Json

internal interface MockApiService {
    suspend fun handle(
        request: HttpRequestData,
        handleScope: MockRequestHandleScope,
    ): HttpResponseData
}

private val json =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

private val mockAdminApiService = MockAdminApiService(json)
private val mockRecordApiService = MockRecordApiService(json)

internal val HttpRequestData.mockService: MockApiService?
    get() =
        when {
            url.isAdminApi -> mockAdminApiService
            url.isRecordCRUDAction -> mockRecordApiService
            else -> null
        }

private val Url.isAdminApi get() =
    Regex("^/api/admins.*\$").matches(encodedPath)

private val Url.isRecordCRUDAction get() =
    Regex("^/api/collections/.+/records.*\$").matches(encodedPath)
