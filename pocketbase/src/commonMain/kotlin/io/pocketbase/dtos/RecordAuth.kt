package io.pocketbase.dtos

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class RecordAuth<T : RecordModel> internal constructor(
    val token: String = "",
    val record: T,
    val meta: JsonObject? = null,
)
