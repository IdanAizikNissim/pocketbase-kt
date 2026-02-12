package io.pocketbase.dtos

import kotlinx.serialization.Serializable

@Serializable
data class HourlyStats(
    val total: Int,
    val date: String,
)
