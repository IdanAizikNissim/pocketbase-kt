package io.pocketbase.models

import io.pocketbase.dtos.RecordModel
import kotlinx.serialization.Serializable

@Serializable
data class SuperUser(
    val email: String = "",
) : RecordModel()
