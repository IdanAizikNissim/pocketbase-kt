package io.pocketbase

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TestHealthService : TestService() {
    @Test
    fun healthCheck() =
        runTest {
            val results = pb.healthCheck.check()
            assertEquals(200, results.code)

            val aliasResults = pb.health.check()
            assertEquals(200, aliasResults.code)
        }
}
