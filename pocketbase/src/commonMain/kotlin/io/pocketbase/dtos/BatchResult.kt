package io.pocketbase.dtos

import kotlinx.serialization.Serializable

@Serializable
data class BatchResult<T : RecordModel>(
    val status: Int?,
    val body: T? = null,
)
