package io.pocketbase.dtos

class File(
    val field: String,
    val data: ByteArray,
    val fileName: String? = null,
)
