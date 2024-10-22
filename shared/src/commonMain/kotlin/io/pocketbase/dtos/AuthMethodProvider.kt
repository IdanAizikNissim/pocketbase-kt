package io.pocketbase.dtos

import kotlinx.serialization.Serializable

@Serializable
data class AuthMethodProvider(
    val name: String = "",
    val displayName: String = "",
    val state: String = "",
    val codeVerifier: String = "",
    val codeChallenge: String = "",
    val codeChallengeMethod: String = "",
    val authUrl: String = "",
)
