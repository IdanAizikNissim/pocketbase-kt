package io.pocketbase.http

import kotlinx.serialization.json.Json

internal val json =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
