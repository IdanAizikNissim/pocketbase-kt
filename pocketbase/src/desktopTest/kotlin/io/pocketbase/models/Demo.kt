package io.pocketbase.models

import io.pocketbase.dtos.RecordModel
import kotlinx.serialization.Serializable

@Serializable
data class Demo(
    val title: String = "",
    val file: String? = null,
) : RecordModel()
