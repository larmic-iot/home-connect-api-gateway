package de.larmic.starter

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthStateTest {

    @BeforeTest
    fun setUp() {
        AuthState.clear()
    }

    @AfterTest
    fun tearDown() {
        AuthState.clear()
    }

    @Test
    fun `status is STARTING_DEVICE_AUTHORIZATION when deviceCode not set`() {
        // deviceCode is null by default
        val status = AuthState.status()
        assertEquals(AuthState.Status.StartingDeviceAuthorization, status)
    }

    @Test
    fun `status is WAITING_FOR_MANUAL_TASKS when deviceCode set but tokens missing`() {
        val url = "https://verify.example?user_code=ABC"
        val deviceCode = "device-code-123"
        AuthState.updateDeviceCode(deviceCode, url)

        val status = AuthState.status()
        assertTrue(status is AuthState.Status.WaitingForManualTasks)
        assertEquals(deviceCode, status.deviceCode)
        assertEquals(url, status.verificationUrl)
    }

    @Test
    fun `status is UP when tokens present and not expired`() {
        AuthState.updateDeviceCode("device-code-123", "https://verify.example?user_code=ABC")
        AuthState.updateTokens(accessToken = "acc", refreshToken = "ref", expiresInSeconds = 3600)

        val status = AuthState.status()
        assertTrue(status is AuthState.Status.Up)
        assertEquals("acc", status.accessToken)
        assertEquals("ref", status.refreshToken)
    }

    @Test
    fun `status is TOKEN_EXPIRED when current time exceeds issuedAt plus expiresIn`() {
        AuthState.updateDeviceCode("device-code-123", "https://verify.example?user_code=ABC")
        AuthState.updateTokens(accessToken = "acc", refreshToken = "ref", expiresInSeconds = 0)

        val status = AuthState.status()
        assertTrue(status is AuthState.Status.TokenExpired)
        assertEquals("ref", status.refreshToken)
    }
}
