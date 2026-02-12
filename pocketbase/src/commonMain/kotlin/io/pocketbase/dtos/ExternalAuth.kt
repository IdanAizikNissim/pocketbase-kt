package io.pocketbase.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ExternalAuth(
    val collectionRef: String? = null,
    val recordRef: String? = null,
    val provider: String? = null,
    val providerId: String? = null,
) : RecordModel()
