package io.pocketbase.dtos

import kotlinx.serialization.Serializable

@Serializable
abstract class RecordModel(
    val collectionId: String = "",
    val collectionName: String = "",
    internal var data: Data = Data(),
) : Model() {
    val isVerified
        get() = data.verified

    @Serializable
    data class Data internal constructor(
        val verified: Boolean = false,
    )
}
