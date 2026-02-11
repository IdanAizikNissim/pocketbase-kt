package io.pocketbase.models

import io.pocketbase.dtos.RecordModel
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val username: String = "",
    val email: String = "",
    val name: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
) : RecordModel()
