package io.pocketbase

import io.pocketbase.config.TestConfig
import io.pocketbase.dtos.AdminAuth
import kotlin.test.BeforeTest

abstract class TestService {
    protected val config = TestConfig.load()
    protected lateinit var pb: PocketBase
        private set

    @BeforeTest
    fun setup() {
//        HttpClientFactory.engine = pocketBaseMockEngine
        pb = PocketBase(config.host)
    }

    protected suspend fun authWithAdmin(): AdminAuth =
        with(config) {
            return pb.admins.authWithPassword(adminEmail, adminPassword)
        }
}
