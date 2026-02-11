package io.pocketbase.sample.client

import com.russhwolf.settings.Settings
import io.pocketbase.auth.AuthStore
import io.pocketbase.dtos.Model
import io.pocketbase.sample.data.User
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PersistentAuthStore : AuthStore() {
    private val settings = Settings()
    private val jsonSerializer = Json { ignoreUnknownKeys = true }

    init {
        val savedToken = settings.getString("pb_auth_token", "")
        val savedModelJson = settings.getString("pb_auth_model", "")

        if (savedToken.isNotEmpty() && savedModelJson.isNotEmpty()) {
            try {
                // Assuming User model for this sample app
                val model = jsonSerializer.decodeFromString<User>(savedModelJson)
                // Restore state using parent's save method
                super.save(savedToken, model)
            } catch (e: Exception) {
                // Ignore failure, start fresh
            }
        }
    }

    override fun save(newToken: String, newModel: Model) {
        super.save(newToken, newModel)
        settings.putString("pb_auth_token", newToken)
        if (newModel is User) {
            settings.putString("pb_auth_model", jsonSerializer.encodeToString(newModel))
        }
    }

    override fun clear() {
        super.clear()
        settings.remove("pb_auth_token")
        settings.remove("pb_auth_model")
    }
}
