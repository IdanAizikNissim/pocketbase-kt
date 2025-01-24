package io.pocketbase.utils.encoder

import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.create
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters

internal actual object DefaultUrlEncoder : UrlEncoder {
    @OptIn(BetaInteropApi::class)
    actual override fun encode(value: String): String {
        val allowedCharacterSet = NSCharacterSet.URLQueryAllowedCharacterSet
        return NSString.create(string = value).stringByAddingPercentEncodingWithAllowedCharacters(allowedCharacterSet) ?: value
    }
}
