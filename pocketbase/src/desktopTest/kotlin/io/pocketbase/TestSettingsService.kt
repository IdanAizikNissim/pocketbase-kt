package io.pocketbase

import kotlinx.coroutines.test.runTest
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import kotlin.test.Test

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TestSettingsService : TestService() {
    @Test
    fun test_0_listSettings() =
        runTest {
            authWithAdmin()
            pb.settings.getAll()
        }
}
