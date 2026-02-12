package io.pocketbase.dtos

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class LogModel(
    val level: String? = null,
    val message: String? = null,
    val data: JsonObject? = null,
) : Model()
