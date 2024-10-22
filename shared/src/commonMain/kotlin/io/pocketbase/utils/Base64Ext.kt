package io.pocketbase.utils

import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val BASE_64_LENGTH_MULTIPLIER = 4
private const val BASE_64_PADDING_SIGN_BYTE: Byte = 61 // "="

@ExperimentalEncodingApi
internal fun Base64.safeDecode(
    source: String,
    startIndex: Int = 0,
    endIndex: Int = source.length,
): ByteArray {
    val bytes = source.substring(startIndex, endIndex).toByteArray(Charsets.ISO_8859_1)
    return safeDecode(bytes)
}

@ExperimentalEncodingApi
internal fun Base64.safeDecode(source: ByteArray): ByteArray {
    val remainder = source.size % BASE_64_LENGTH_MULTIPLIER
    val dataWithPadding =
        if (remainder == 0) {
            source
        } else {
            val extraPaddingSize = BASE_64_LENGTH_MULTIPLIER - remainder
            val paddingBytes = ByteArray(extraPaddingSize) { BASE_64_PADDING_SIGN_BYTE }
            source + paddingBytes
        }

    return decode(dataWithPadding)
}
