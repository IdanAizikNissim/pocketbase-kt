package io.pocketbase.dtos

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
abstract class Model {
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @Serializable(with = ExcludeIfNullSerializer::class)
    val id: String? = null
    val created: String = ""
    val updated: String = ""
}
