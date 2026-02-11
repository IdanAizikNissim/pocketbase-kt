package io.pocketbase.sample.data

import io.pocketbase.dtos.RecordModel
import kotlinx.serialization.Serializable

@Serializable
class User(
    val email: String = "",
    val name: String = "",
    val avatar: String = "",
    // Write-only fields for creation
    val password: String? = null,
    val passwordConfirm: String? = null,
    val emailVisibility: Boolean = true
) : RecordModel()
