package io.pocketbase.sample.client

import io.pocketbase.PocketBase
import io.pocketbase.sample.SERVER_URL

object PocketBaseSingleton {
    val client: PocketBase by lazy {
        PocketBase(SERVER_URL)
    }
}
