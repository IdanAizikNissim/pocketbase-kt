package io.pocketbase.dtos

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalSerializationApi::class)
abstract class Model {
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @Serializable(with = ExcludeIfNullSerializer::class)
    val id: String? = null

    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @Serializable(with = ExcludeIfNullSerializer::class)
    val created: String? = null

    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @Serializable(with = ExcludeIfNullSerializer::class)
    val updated: String? = null
}
