package io.pocketbase.utils.encoder

internal expect object DefaultUrlEncoder : UrlEncoder {
    override fun encode(value: String): String
}
