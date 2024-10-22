package io.pocketbase.dtos

import kotlinx.serialization.Serializable

@Serializable
data class AuthMethodsList(
    val usernamePassword: Boolean = false,
    val emailPassword: Boolean = false,
    val onlyVerified: Boolean = false,
    val authProviders: List<AuthMethodProvider> = emptyList(),
)
