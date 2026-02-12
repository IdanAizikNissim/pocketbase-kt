package io.pocketbase

import io.pocketbase.dtos.GenerateAppleClientSecretRequest
import io.pocketbase.http.ClientException
import kotlinx.coroutines.test.runTest
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TestSettingsService : TestService() {
    @Test
    fun test_0_listSettings() =
        runTest {
            authWithAdmin()
            val settings = pb.settings.getAll()
            assertNotNull(settings)
        }

    @Test
    fun test_1_updateSettings() =
        runTest {
            authWithAdmin()
            val settings = pb.settings.getAll()
            val updated = pb.settings.update(settings)
            assertNotNull(updated)
        }

    @Test
    fun test_2_testEndpointsAndAppleSecret() =
        runTest {
            authWithAdmin()

            invokeOrClientError { pb.settings.testS3() }
            invokeOrClientError { pb.settings.testS3(filesystem = "backups") }
            invokeOrClientError {
                pb.settings.testEmail(
                    email = config.adminEmail,
                    template = "verification",
                )
            }
            invokeOrClientError {
                pb.settings.testEmail(
                    collectionIdOrName = "_superusers",
                    toEmail = config.adminEmail,
                    emailTemplate = "verification",
                )
            }
            invokeOrClientError {
                pb.settings.generateAppleClientSecret(
                    GenerateAppleClientSecretRequest(
                        clientId = "invalid-client",
                        teamId = "invalid-team",
                        keyId = "invalid-key",
                        privateKey = "invalid-private-key",
                        duration = 300,
                    ),
                )
            }
        }

    private suspend fun invokeOrClientError(block: suspend () -> Any?) {
        try {
            block()
        } catch (e: ClientException) {
            assertTrue(e.statusCode >= 400)
        } catch (e: Exception) {
            assertTrue(e.message != null)
        }
    }
}
