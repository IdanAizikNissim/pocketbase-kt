package io.pocketbase.dtos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecordSubscriptionEvent<T : RecordModel> internal constructor(
    val action: Action,
    val record: T? = null,
) {
    enum class Action {
        @SerialName("create")
        CREATE,

        @SerialName("update")
        UPDATE,

        @SerialName("delete")
        DELETE,
    }
}
