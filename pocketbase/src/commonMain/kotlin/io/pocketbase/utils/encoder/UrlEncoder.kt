package io.pocketbase.utils.encoder

internal interface UrlEncoder {
    fun encode(value: String): String
}
