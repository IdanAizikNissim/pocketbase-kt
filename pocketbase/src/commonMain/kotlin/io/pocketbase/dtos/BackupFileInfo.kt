package io.pocketbase.dtos

import kotlinx.serialization.Serializable

@Serializable
data class BackupFileInfo(
    val key: String,
    val size: Long,
    val modified: String,
)
