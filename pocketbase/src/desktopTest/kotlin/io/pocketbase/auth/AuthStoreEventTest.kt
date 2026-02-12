package io.pocketbase.auth

import io.pocketbase.dtos.Model
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthStoreEventTest {

    @Test
    fun testToStringRedaction() {
        val sensitiveToken = "super_secret_token_12345"
        val model = object : Model() {} // Create a dummy model
        val event = AuthStoreEvent(sensitiveToken, model)

        val stringRepresentation = event.toString()

        // Verify the token is NOT present in the string representation
        assertFalse(
            stringRepresentation.contains(sensitiveToken),
            "The sensitive token should not be exposed in toString()"
        )

        // Verify that it contains a redacted version (e.g., token=*** or similar)
        // I will check for "token=***" or similar common patterns, or just that it contains "token=" and not the token.
        // Actually, to be robust, I'll just check "token=***" as that is what I plan to implement.
        assertTrue(
            stringRepresentation.contains("token=***"),
            "The token should be redacted as *** in toString()"
        )
    }
}
