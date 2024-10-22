package io.pocketbase.http

import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

internal suspend fun Json.fromRequest(request: HttpRequestData): JsonElement {
    return parseToJsonElement(request.body.toByteArray().decodeToString())
}
