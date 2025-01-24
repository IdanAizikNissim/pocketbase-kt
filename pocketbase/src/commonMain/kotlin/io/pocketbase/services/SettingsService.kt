package io.pocketbase.services

import io.ktor.client.call.body
import io.ktor.http.HttpMethod
import io.pocketbase.PocketBase
import io.pocketbase.dtos.AuthProvider
import io.pocketbase.dtos.Backups
import io.pocketbase.dtos.EmailTemplate
import io.pocketbase.dtos.GenerateAppleClientSecretRequest
import io.pocketbase.dtos.Logs
import io.pocketbase.dtos.Meta
import io.pocketbase.dtos.S3
import io.pocketbase.dtos.Settings
import io.pocketbase.dtos.Smtp
import io.pocketbase.dtos.Token
import io.pocketbase.http.json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class SettingsService internal constructor(
    client: PocketBase,
) : BaseService(client) {
    suspend fun getAll(): Settings {
        return client.send(
            path = "/api/settings",
            method = HttpMethod.Get,
        ).body<JsonObject>().toSettings()
    }

    suspend fun update(settings: Settings): Settings {
        return client.send(
            path = "/api/settings",
            method = HttpMethod.Patch,
            body = settings.toJsonObject().toString(),
        ).body<JsonObject>().toSettings()
    }

    suspend fun testS3(filesystem: String) {
        client.send(
            path = "/api/settings/test/s3",
            method = HttpMethod.Post,
            body =
                buildJsonObject {
                    put("filesystem", filesystem)
                }.toString(),
        )
    }

    suspend fun testEmail(
        email: String,
        template: String,
    ) {
        client.send(
            path = "/api/settings/test/email",
            method = HttpMethod.Post,
            body =
                buildJsonObject {
                    put("email", email)
                    put("template", template)
                }.toString(),
        )
    }

    suspend fun generateAppleClientSecret(body: GenerateAppleClientSecretRequest): String {
        return client.send(
            path = "/api/settings/apple/generate-client-secret",
            method = HttpMethod.Post,
            body = body,
        ).body<JsonObject>().getValue("secret").jsonPrimitive.content
    }

    private fun JsonObject.toSettings(): Settings {
        val meta = json.decodeFromJsonElement<Meta>(getValue("meta").jsonObject)
        val logs = json.decodeFromJsonElement<Logs>(getValue("logs").jsonObject)
        val backups = json.decodeFromJsonElement<Backups>(getValue("backups").jsonObject)
        val smtp = json.decodeFromJsonElement<Smtp>(getValue("smtp").jsonObject)
        val s3 = json.decodeFromJsonElement<S3>(getValue("s3").jsonObject)

        val tokens = mutableMapOf<String, Token>()
        keys
            .filter { it.endsWith("Token") }
            .forEach {
                try {
                    tokens[it] = json.decodeFromJsonElement<Token>(getValue(it).jsonObject)
                } catch (_: Exception) {
                }
            }

        val authProviders = mutableMapOf<String, AuthProvider>()
        keys
            .filter { it.endsWith("Auth") }
            .forEach {
                try {
                    authProviders[it] = json.decodeFromJsonElement<AuthProvider>(getValue(it).jsonObject)
                } catch (_: Exception) {
                }
            }

        return Settings(
            meta = meta,
            logs = logs,
            backups = backups,
            smtp = smtp,
            s3 = s3,
            tokens = tokens,
            authProviders = authProviders,
        )
    }

    private fun Settings.toJsonObject(): JsonObject {
        return buildJsonObject {
            meta?.let { put("meta", it.toJsonObject()) }
            logs?.let { put("logs", it.toJsonObject()) }
            backups?.let { put("backups", it.toJsonObject()) }
            smtp?.let { put("smtp", it.toJsonObject()) }
            s3?.let { put("s3", it.toJsonObject()) }
            tokens.forEach { token ->
                put(token.key, token.value.toJsonObject())
            }
            authProviders.forEach { authProvider ->
                put(authProvider.key, authProvider.value.toJsonObject())
            }
        }
    }

    private fun Meta.toJsonObject(): JsonObject {
        return buildJsonObject {
            put("appName", appName)
            put("appUrl", appURL)
            hideControls?.let { put("hideControls", it) }
            put("senderName", senderName)
            put("senderAddress", senderAddress)
            verificationTemplate?.let { put("verificationTemplate", it.toJsonObject()) }
            resetPasswordTemplate?.let { put("resetPasswordTemplate", it.toJsonObject()) }
            confirmEmailChangeTemplate?.let { put("confirmEmailChangeTemplate", it.toJsonObject()) }
        }
    }

    private fun EmailTemplate.toJsonObject(): JsonObject {
        return buildJsonObject {
            put("body", body)
            put("subject", subject)
            put("actionUrl", actionURL)
        }
    }

    private fun Logs.toJsonObject(): JsonObject {
        return buildJsonObject {
            put("maxDays", maxDays)
        }
    }

    private fun Backups.toJsonObject(): JsonObject {
        return buildJsonObject {
            cron?.let { put("cron", it) }
            cronMaxKeep?.let { put("cronMaxKeep", it) }
            s3?.let { put("s3", it.toJsonObject()) }
        }
    }

    private fun Smtp.toJsonObject(): JsonObject {
        return buildJsonObject {
            put("enabled", enabled)
            put("host", host)
            put("port", port)
            username?.let { put("username", it) }
            password?.let { put("password", it) }
            authMethod?.let { put("authMethod", it) }
            tls?.let { put("tls", it) }
            localName?.let { put("localName", it) }
        }
    }

    private fun S3.toJsonObject(): JsonObject {
        return buildJsonObject {
            put("enabled", enabled)
            put("bucket", bucket)
            put("region", region)
            put("endpoint", endpoint)
            put("accessKey", accessKey)
            put("secret", secret)
            forcePathStyle?.let { put("forcePathStyle", it) }
        }
    }

    private fun Token.toJsonObject(): JsonObject {
        return buildJsonObject {
            put("secret", secret)
            put("duration", duration)
        }
    }

    private fun AuthProvider.toJsonObject(): JsonObject {
        return buildJsonObject {
            put("enabled", enabled)
            put("clientId", clientId)
            put("clientSecret", clientSecret)
            authURL?.let { put("authUrl", it) }
            tokenURL?.let { put("tokenUrl", it) }
            userApiURL?.let { put("userApiUrl", it) }
        }
    }
}
