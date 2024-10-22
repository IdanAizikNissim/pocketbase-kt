package io.pocketbase.dtos

import kotlinx.serialization.Serializable

@Serializable
abstract class Model {
    val id: String = ""
    val created: String = ""
    val updated: String = ""
}
