package io.pocketbase.services

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.pocketbase.PocketBase
import io.pocketbase.dtos.AdminAuth
import io.pocketbase.dtos.AdminModel

class AdminService internal constructor(
    client: PocketBase,
) : BaseCRUDService<AdminModel>(
        client = client,
        cls = AdminModel::class,
        baseCrudPath = "/api/admins",
    ) {
    override suspend fun update(
        id: String,
        body: AdminModel?,
        expand: String?,
        fields: String?,
        query: Map<String, Any?>,
        headers: Map<String, String>,
    ): AdminModel =
        super.update(id, body, expand, fields, query, headers).also {
            onAdminModelChanged(id) {
                client.authStore.save(client.authStore.token, it)
            }
        }

    override suspend fun delete(
        id: String,
        body: AdminModel?,
        query: Map<String, Any?>,
        headers: Map<String, String>,
    ) {
        super.delete(id, body, query, headers).also {
            onAdminModelChanged(id) {
                client.authStore.clear()
            }
        }
    }

    suspend fun authWithPassword(
        email: String,
        password: String,
        body: Map<String, Any?> = emptyMap(),
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): AdminAuth {
        val enrichedBody =
            body +
                mapOf(
                    "identity" to email,
                    "password" to password,
                )

        return client.send(
            path = "$baseCrudPath/auth-with-password",
            method = HttpMethod.Post,
            body = enrichedBody,
            query = query,
            headers = headers,
        ).authResponse()
    }

    suspend fun authRefresh(
        body: Map<String, Any?> = emptyMap(),
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): AdminAuth {
        return client.send(
            path = "$baseCrudPath/auth-refresh",
            method = HttpMethod.Post,
            body = body,
            query = query,
            headers = headers,
        ).authResponse()
    }

    suspend fun requestPasswordReset(
        email: String,
        body: Map<String, Any?> = emptyMap(),
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) {
        val enrichedBody =
            body +
                mapOf(
                    "email" to email,
                )

        client.send(
            path = "$baseCrudPath/request-password-reset",
            method = HttpMethod.Post,
            body = enrichedBody,
            query = query,
            headers = headers,
        )
    }

    suspend fun confirmPasswordReset(
        passwordResetToken: String,
        password: String,
        passwordConfirm: String,
        body: Map<String, Any?> = emptyMap(),
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) {
        val enrichedBody =
            body +
                mapOf(
                    "token" to passwordResetToken,
                    "password" to password,
                    "passwordConfirm" to passwordConfirm,
                )

        client.send(
            path = "$baseCrudPath/confirm-password-reset",
            method = HttpMethod.Post,
            body = enrichedBody,
            query = query,
            headers = headers,
        )
    }

    private fun onAdminModelChanged(
        id: String,
        onChange: (AdminModel) -> Unit,
    ) {
        (client.authStore.model as? AdminModel)?.let {
            if (it.id == id) {
                onChange(it)
            }
        }
    }

    private suspend fun HttpResponse.authResponse(): AdminAuth {
        val auth = body<AdminAuth>()
        client.authStore.save(auth.token, auth.admin)

        return auth
    }
}
