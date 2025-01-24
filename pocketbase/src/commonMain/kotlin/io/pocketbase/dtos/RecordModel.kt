package io.pocketbase.dtos

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalSerializationApi::class)
abstract class RecordModel(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @Serializable(with = ExcludeIfNullSerializer::class)
    val collectionId: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @Serializable(with = ExcludeIfNullSerializer::class)
    val collectionName: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    internal var data: Data? = null,
) : Model() {
    val isVerified
        get() = data?.verified == true

    @Serializable
    data class Data internal constructor(
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val verified: Boolean? = null,
    )
}
