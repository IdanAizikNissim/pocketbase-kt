package io.pocketbase.auth

import io.pocketbase.dtos.Model
import io.pocketbase.http.json
import io.pocketbase.utils.safeDecode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.ExperimentalTime

open class AuthStore {
    var model: Model? = null
        private set
    var token: String = ""
        private set

    private val eventChannel =
        Channel<AuthStoreEvent>(
            capacity = Channel.CONFLATED,
        )
    val onChange = eventChannel.receiveAsFlow()

    @OptIn(ExperimentalEncodingApi::class, ExperimentalTime::class)
    val isValid: Boolean
        get() {
            val parts = token.split(".")
            if (parts.size != 3) {
                return false
            }

            val tokenPart = normalizeBase64(parts[1])
            val json = json.decodeFromString<JsonObject>(Base64.safeDecode(tokenPart).decodeToString())
            val exp = json["exp"]?.jsonPrimitive?.intOrNull ?: (json["exp"]?.toString()?.toIntOrNull() ?: 0)

            return exp > kotlin.time.Clock.System.now().epochSeconds
        }

    open fun save(
        newToken: String,
        newModel: Model,
    ) {
        token = newToken
        model = newModel

        eventChannel.trySend(AuthStoreEvent(token, model))
    }

    open fun clear() {
        token = ""
        model = null

        eventChannel.trySend(AuthStoreEvent(token, model))
    }
}

data class AuthStoreEvent internal constructor(
    val token: String,
    val model: Model?,
)

internal fun normalizeBase64(base64: String): String {
    val noWhitespace = base64.replace("\\s".toRegex(), "")
    return noWhitespace.replace("=", "")
}
