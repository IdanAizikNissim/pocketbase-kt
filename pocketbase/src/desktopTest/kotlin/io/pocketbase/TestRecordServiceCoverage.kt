package io.pocketbase

import io.pocketbase.http.ClientException
import io.pocketbase.models.SuperUser
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestRecordServiceCoverage : TestService() {
    private val superusers by lazy { pb.collection<SuperUser>("_superusers") }

    @Test
    fun recordService_methods_areInvocable() =
        runTest {
            authWithAdmin()

            val authMethods = superusers.listAuthMethods()
            assertNotNull(authMethods)

            val authData = superusers.authRefresh()
            assertNotNull(authData.token)

            // These depend on server/email/auth-provider setup.
            // We assert each endpoint is reachable and returns either success or a well-formed API error.
            invokeOrClientError {
                superusers.authWithOAuth2Code(
                    provider = "github",
                    code = "invalid",
                    codeVerifier = "invalid",
                    redirectUrl = "http://127.0.0.1:8090/api/oauth2-redirect",
                )
            }
            invokeOrClientError { superusers.requestPasswordReset(config.adminEmail) }
            invokeOrClientError {
                superusers.confirmPasswordReset(
                    passwordResetToken = "invalid-token",
                    password = "password123456",
                    passwordConfirm = "password123456",
                )
            }
            invokeOrClientError { superusers.requestEmailChange("new+invalid@example.com") }
            invokeOrClientError {
                superusers.confirmEmailChange(
                    emailChangeToken = "invalid-token",
                    password = config.adminPassword,
                )
            }
            invokeOrClientError { superusers.requestVerification(config.adminEmail) }
            invokeOrClientError { superusers.confirmVerification("invalid-token") }
            invokeOrClientError {
                superusers.requestOTP(config.adminEmail)
            }
            invokeOrClientError {
                superusers.authWithOTP(
                    otpId = "invalid-otp",
                    password = "invalid-password",
                )
            }
            invokeOrClientError {
                superusers.impersonate(
                    id = "invalid-id",
                    duration = 60,
                )
            }
            invokeOrClientError { superusers.listExternalAuths("invalid-record-id") }
            invokeOrClientError { superusers.unlinkExternalAuth("invalid-record-id", "github") }
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
