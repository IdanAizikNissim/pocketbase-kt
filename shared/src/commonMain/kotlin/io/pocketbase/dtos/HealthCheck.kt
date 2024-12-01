package io.pocketbase.dtos

import kotlinx.serialization.Serializable

@Serializable
data class HealthCheck(
    val code: Int,
    val message: String,
    val data: Data,
) {
    @Serializable
    data class Data(
        val canBackup: Boolean? = null,
    )
}
