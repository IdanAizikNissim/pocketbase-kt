package io.pocketbase.http

import kotlinx.serialization.json.JsonObject

data class ClientException(
    val statusCode: Int = 0,
    val url: String = "",
    val data: JsonObject? = null,
    val originError: String? = null,
) : Exception()
