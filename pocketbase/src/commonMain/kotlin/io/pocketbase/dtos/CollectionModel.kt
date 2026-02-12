package io.pocketbase.dtos

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable
data class CollectionModel(
    val name: String? = null,
    val type: String? = null,
    val system: Boolean? = null,
    val schema: JsonArray? = null,
    val indexes: JsonArray? = null,
    val listRule: String? = null,
    val viewRule: String? = null,
    val createRule: String? = null,
    val updateRule: String? = null,
    val deleteRule: String? = null,
    val fields: JsonArray? = null,
) : Model()
