package io.pocketbase.dtos

import kotlinx.serialization.Serializable

@Serializable
data class AdminModel(
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val avatar: Int = 0,
) : Model()
