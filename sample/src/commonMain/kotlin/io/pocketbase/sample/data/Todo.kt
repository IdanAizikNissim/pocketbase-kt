package io.pocketbase.sample.data

import io.pocketbase.dtos.RecordModel
import kotlinx.serialization.Serializable

@Serializable
class Todo(
    val text: String = "",
    val completed: Boolean = false,
    val attachment: String = "",
) : RecordModel()
