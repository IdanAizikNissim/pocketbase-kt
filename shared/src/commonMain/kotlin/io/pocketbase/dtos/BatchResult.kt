package io.pocketbase.dtos

import kotlinx.serialization.Serializable

@Serializable
data class BatchResult(
    val status: Int,
    val result: RecordModel,
)
