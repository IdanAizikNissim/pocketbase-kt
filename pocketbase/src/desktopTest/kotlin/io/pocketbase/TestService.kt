package io.pocketbase

import io.pocketbase.config.TestConfig
import io.pocketbase.dtos.RecordAuth
import io.pocketbase.models.SuperUser
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

    protected suspend fun authWithAdmin(): RecordAuth<SuperUser> =
        with(config) {
            return pb
                .collection<SuperUser>("_superusers")
                .authWithPassword(adminEmail, adminPassword)
        }
}
