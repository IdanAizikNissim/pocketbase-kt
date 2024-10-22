package io.pocketbase.utils.encoder

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal actual object DefaultUrlEncoder : UrlEncoder {
    actual override fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}
