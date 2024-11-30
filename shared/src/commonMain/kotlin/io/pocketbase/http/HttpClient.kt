package io.pocketbase.http

import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.fullPath
import io.pocketbase.ClientConfig
import io.pocketbase.auth.AuthStore
import io.pocketbase.dtos.File
import io.pocketbase.http.Protocol.HTTP
import io.pocketbase.http.Protocol.HTTPS
import kotlinx.serialization.Serializable

internal class HttpClient(
    authStore: AuthStore,
    private val config: ClientConfig,
) {
    private val client =
        HttpClientBuilder(HttpClientFactory()).build(
            baseUrl = config.baseUrl,
            protocol = config.protocol,
            port = config.port,
            lang = config.lang,
            httpTimeout = config.httpTimeout,
            logLevel = config.logLevel,
            authStore = authStore,
        )

    fun buildUrl(
        path: String,
        queryParameters: Map<String, Any?> = emptyMap(),
        includeHost: Boolean = true,
    ): String =
        io.ktor.http.buildUrl {
            protocol = config.protocol.urlProtocol
            host = config.baseUrl
            config.port?.let { port = it }
            pathSegments = path.split("/")
            queryParameters.forEach {
                parameters.append(it.key, it.value.toString().encodeURLParameter())
            }
        }.let {
            it.toString().takeIf { includeHost } ?: it.fullPath
        }

    suspend fun send(
        path: String,
        method: HttpMethod = HttpMethod.Get,
        headers: Map<String, String> = emptyMap(),
        query: Map<String, Any?> = emptyMap(),
        body: @Serializable Any? = null,
        files: List<File> = emptyList(),
    ): HttpResponse {
        return if (files.isEmpty()) {
            client.request(path) {
                this.method = method
                headers.forEach { (key, value) -> header(key, value) }
                query.forEach { (key, value) -> parameter(key, value) }

                body?.let {
                    contentType(ContentType.Application.Json)
                    setBody(it)
                }
            }
        } else {
            client.submitFormWithBinaryData(
                url = path,
                formData =
                    formData {
                        files.forEach {
                            append(
                                key = it.field,
                                value = it.data,
                                headers =
                                    Headers.build {
                                        it.fileName?.let { filename ->
                                            append(HttpHeaders.ContentDisposition, "filename=$filename")
                                        }
                                    },
                            )
                        }
                    },
            ) {
                this.method = method
                headers.forEach { (key, value) -> header(key, value) }
                query.forEach { (key, value) -> parameter(key, value) }
            }
        }
    }
}

private val Protocol.urlProtocol: URLProtocol
    get() =
        when (this) {
            HTTP -> URLProtocol.HTTP
            HTTPS -> URLProtocol.HTTPS
        }
