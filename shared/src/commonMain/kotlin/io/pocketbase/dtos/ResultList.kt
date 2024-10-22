package io.pocketbase.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ResultList<T : @Serializable Any>(
    val page: Int = 0,
    val perPage: Int = 0,
    val totalItems: Int = 0,
    val totalPages: Int = 0,
    val items: List<T> = emptyList(),
)
