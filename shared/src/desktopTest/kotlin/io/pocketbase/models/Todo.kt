package io.pocketbase.models

import io.pocketbase.dtos.RecordModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Todo(
    @SerialName("list_id") val listId: String,
    val text: String,
    val completed: String? = null,
) : RecordModel()
