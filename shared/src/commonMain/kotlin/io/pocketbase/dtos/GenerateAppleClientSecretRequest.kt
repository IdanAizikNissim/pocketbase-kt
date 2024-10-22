package io.pocketbase.dtos

import kotlinx.serialization.Serializable

@Serializable
data class GenerateAppleClientSecretRequest(
    val clientId: String,
    val teamId: String,
    val keyId: String,
    val privateKey: String,
    val duration: Long,
)
