package io.pocketbase

import io.pocketbase.http.LogLevel
import io.pocketbase.http.Protocol
import kotlin.time.Duration.Companion.seconds

data class ClientConfig(
    val baseUrl: String,
    val protocol: Protocol = Protocol.HTTP,
    val port: Int?,
    val lang: String = "en-US",
    val reconnectionTime: Long = 3.seconds.inWholeMilliseconds,
    val maxReconnectionRetries: Long = Long.MAX_VALUE,
    val logLevel: LogLevel = LogLevel.NONE,
) {
    companion object {
        fun from(url: String): ClientConfig {
            val protocolPattern = "^(https?://)?".toRegex()
            val protocolMatch = protocolPattern.find(url)
            val protocol =
                protocolMatch?.value?.removeSuffix("://")?.let {
                    when (it) {
                        "http" -> Protocol.HTTP
                        "https" -> Protocol.HTTPS
                        else -> null
                    }
                } ?: Protocol.HTTP

            val withoutProtocol = url.removePrefix(protocolMatch?.value ?: "")
            val hostAndPort = withoutProtocol.split(":")

            val host =
                if (hostAndPort.isNotEmpty()) {
                    hostAndPort[0].let {
                        if (it.endsWith("/")) it.dropLast(1) else it
                    }
                } else {
                    ""
                }
            val port = if (hostAndPort.size > 1) hostAndPort[1].toIntOrNull() else null

            return ClientConfig(
                baseUrl = host,
                protocol = protocol,
                port = port,
            )
        }
    }
}
