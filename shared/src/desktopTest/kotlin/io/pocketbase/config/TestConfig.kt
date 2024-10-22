package io.pocketbase.config

import io.pocketbase.PocketBase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TestConfig(
    @SerialName("host")
    val host: String,
    @SerialName("admin_email")
    val adminEmail: String,
    @SerialName("admin_password")
    val adminPassword: String,
) {
    companion object {
        fun load(resource: String = "config.json"): TestConfig {
            val jsonString = PocketBase::class.java.classLoader?.getResource(resource)?.readBytes()?.decodeToString()
            return Json.decodeFromString(jsonString!!)
        }
    }
}
