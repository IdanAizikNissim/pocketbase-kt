package io.pocketbase.dtos

import kotlinx.serialization.Serializable

@Serializable
data class RecordAuth<T : RecordModel> internal constructor(
    val token: String = "",
    val record: T,
)
