package io.pocketbase.dtos

import kotlinx.serialization.Serializable

@Serializable
data class AdminAuth internal constructor(
    val token: String = "",
    val admin: AdminModel,
)
