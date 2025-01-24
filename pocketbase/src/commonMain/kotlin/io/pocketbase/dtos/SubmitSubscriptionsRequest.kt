package io.pocketbase.dtos

import kotlinx.serialization.Serializable

@Serializable
internal data class SubmitSubscriptionsRequest(
    val clientId: String?,
    val subscriptions: List<String>,
)
