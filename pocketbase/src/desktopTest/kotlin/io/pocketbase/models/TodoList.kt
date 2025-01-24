package io.pocketbase.models

import io.pocketbase.dtos.RecordModel
import kotlinx.serialization.Serializable

@Serializable
data class TodoList(
    val title: String,
) : RecordModel()
