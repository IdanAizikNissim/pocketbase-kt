package io.pocketbase.auth

import io.pocketbase.dtos.Model
import io.pocketbase.dtos.RecordModel
import io.pocketbase.http.json
import io.pocketbase.utils.SyncQueue
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

abstract class AsyncAuthStore<T : @Serializable Model> internal constructor(
    cls: KClass<T>,
    private val save: SaveFunc,
    private val clear: ClearFunc? = null,
    initial: String? = null,
) : AuthStore() {
    @OptIn(InternalSerializationApi::class)
    protected val serializer = cls.serializer()

    private val queue by lazy { SyncQueue() }

    init {
        loadInitial(initial)
    }

    override fun save(
        newToken: String,
        newModel: Model,
    ) {
        super.save(newToken, newModel)

        val token =
            TokenModel(
                token = newToken,
                model = newModel as T,
            )

        queue.enqueue {
            save(jsonEncode(token))
        }
    }

    override fun clear() {
        super.clear()
        queue.enqueue {
            clear?.invoke() ?: save("")
        }
    }

    private fun loadInitial(initial: String?) {
        if (initial.isNullOrEmpty()) return

        try {
            val raw = json.decodeFromString<JsonObject>(initial)
            val token = raw[TOKEN_KEY]?.jsonPrimitive?.content
            val rawModel = raw[MODEL_KEY]?.jsonObject

            if (token != null && rawModel != null) {
                extractModel(rawModel)?.let { model ->
                    save(token, model)
                }
            }
        } catch (e: Exception) {
            return
        }
    }

    abstract fun extractModel(rawModel: JsonObject): T?

    abstract fun jsonEncode(token: TokenModel<T>): String

    companion object {
        const val TOKEN_KEY = "token"
        const val MODEL_KEY = "model"
    }
}

class RecordAsyncAuthStore<T : RecordModel>(
    cls: KClass<T>,
    save: SaveFunc,
    clear: ClearFunc? = null,
    initial: String? = null,
) : AsyncAuthStore<T>(
        cls = cls,
        save = save,
        clear = clear,
        initial = initial,
    ) {
    private val tokenModelSerializer by lazy { TokenModel.serializer(serializer) }

    override fun extractModel(rawModel: JsonObject): T? =
        if (rawModel.containsKey("collectionId") &&
            rawModel.containsKey("collectionName") &&
            rawModel.containsKey("verified")
        ) {
            json.decodeFromString(serializer, rawModel.toString())
        } else {
            null
        }

    override fun jsonEncode(token: TokenModel<T>): String = json.encodeToString(tokenModelSerializer, token)
}

@Serializable
data class TokenModel<T : Model> internal constructor(
    val token: String,
    val model: T,
)

typealias SaveFunc = suspend (String) -> Unit
typealias ClearFunc = suspend () -> Unit
