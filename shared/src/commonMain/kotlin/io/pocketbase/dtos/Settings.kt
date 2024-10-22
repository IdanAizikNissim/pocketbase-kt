package io.pocketbase.dtos

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    val meta: Meta? = null,
    val logs: Logs? = null,
    val backups: Backups? = null,
    val smtp: Smtp? = null,
    val s3: S3? = null,
    val tokens: Map<String, Token> = emptyMap(),
    val authProviders: Map<String, AuthProvider> = emptyMap(),
)

@Serializable
data class Meta(
    val appName: String,
    val appUrl: String,
    val hideControls: Boolean? = null,
    val senderName: String,
    val senderAddress: String,
    val verificationTemplate: EmailTemplate? = null,
    val resetPasswordTemplate: EmailTemplate? = null,
    val confirmEmailChangeTemplate: EmailTemplate? = null,
)

@Serializable
data class EmailTemplate(
    val body: String,
    val subject: String,
    val actionUrl: String,
    val hidden: Boolean,
)

@Serializable
data class Logs(
    val maxDays: Long,
)

@Serializable
data class Smtp(
    val enabled: Boolean,
    val host: String,
    val port: Long,
    val username: String? = null,
    val password: String? = null,
    val authMethod: String? = null,
    val tls: Boolean? = null,
    val localName: String? = null,
)

@Serializable
data class S3(
    val enabled: Boolean,
    val bucket: String,
    val region: String,
    val endpoint: String,
    val accessKey: String,
    val secret: String,
    val forcePathStyle: Boolean? = null,
)

@Serializable
data class Backups(
    val cron: String? = null,
    val cronMaxKeep: Long? = null,
    val s3: S3? = null,
)

@Serializable
data class Token(
    val secret: String,
    val duration: Long,
)

@Serializable
data class AuthProvider(
    val enabled: Boolean,
    val clientId: String,
    val clientSecret: String,
    val authUrl: String?,
    val tokenUrl: String?,
    val userApiUrl: String?,
)
