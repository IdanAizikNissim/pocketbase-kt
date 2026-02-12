package io.pocketbase.dtos

import kotlinx.serialization.Serializable

@Serializable
data class CronJob(
    val id: String,
    val expression: String,
)
