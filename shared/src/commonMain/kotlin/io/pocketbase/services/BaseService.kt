package io.pocketbase.services

import io.pocketbase.PocketBase

abstract class BaseService internal constructor(
    val client: PocketBase,
)
