package io.pocketbase.http

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondBadRequest
import io.pocketbase.http.services.mockService

val pocketBaseMockEngine =
    MockEngine { request ->
        request.mockService?.handle(request, this) ?: respondBadRequest()
    }
