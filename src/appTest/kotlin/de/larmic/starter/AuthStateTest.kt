package de.larmic.starter

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
        assertEquals(AuthState.Status.STARTING_DEVICE_AUTHORIZATION, status)
    }

    @Test
    fun `status is WAITING_FOR_MANUAL_TASKS when deviceCode set but tokens missing`() {
        AuthState.updateDeviceCode("device-code-123")
        val status = AuthState.status()
        assertEquals(AuthState.Status.WAITING_FOR_MANUAL_TASKS, status)
    }

    @Test
    fun `status is UP when tokens present and not expired`() {
        AuthState.updateDeviceCode("device-code-123")
        // Set a long expiry so it's not expired
        AuthState.updateTokens(accessToken = "acc", refreshToken = "ref", expiresInSeconds = 3600)
        val status = AuthState.status()
        assertEquals(AuthState.Status.UP, status)
    }

    @Test
    fun `status is TOKEN_EXPIRED when current time exceeds issuedAt plus expiresIn`() {
        AuthState.updateDeviceCode("device-code-123")
        // expiresInSeconds = 0 means it's immediately expired
        AuthState.updateTokens(accessToken = "acc", refreshToken = "ref", expiresInSeconds = 0)
        val status = AuthState.status()
        assertEquals(AuthState.Status.TOKEN_EXPIRED, status)
    }
}
