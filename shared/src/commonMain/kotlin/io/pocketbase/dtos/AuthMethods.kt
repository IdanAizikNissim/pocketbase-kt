package io.pocketbase.dtos

import kotlinx.serialization.Serializable

@Serializable
data class AuthMethods(
    val password: Password,
    val oauth2: Oauth2,
    val mfa: MFA,
    val otp: OTP,
) {
    @Serializable
    data class Password(
        val enabled: Boolean,
        val identityFields: List<String>,
    )

    @Serializable
    data class Oauth2(
        val enabled: Boolean,
        val providers: List<AuthMethodProvider> = emptyList(),
    )

    @Serializable
    data class MFA(
        val enabled: Boolean,
        val duration: Long,
    )

    @Serializable
    data class OTP(
        val enabled: Boolean,
        val duration: Long,
    )
}
