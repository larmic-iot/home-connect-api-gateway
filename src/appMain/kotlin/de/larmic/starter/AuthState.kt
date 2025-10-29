package de.larmic.starter

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.time

/**
 * In-memory holder for OAuth tokens. Ephemeral; will be lost on application restart.
 */
object AuthState {
    sealed class Status {
        data object StartingDeviceAuthorization : Status()
        data class WaitingForManualTasks(val deviceCode: String, val verificationUrl: String) : Status()
        data class Up(val accessToken: String, val refreshToken: String) : Status()
        data class TokenExpired(val refreshToken: String) : Status()
    }

    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var expiresInSeconds: Int? = null
    private var issuedAdMillis: Long? = null
    private var deviceCode: String? = null
    private var verificationUrl: String? = null

    @OptIn(ExperimentalForeignApi::class)
    fun updateTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Int?) {
        AuthState.accessToken = accessToken
        AuthState.refreshToken = refreshToken
        AuthState.expiresInSeconds = expiresInSeconds
        issuedAdMillis = time(null) * 1000L
        println("Stored OAuth tokens in memory (issuedAt=${time(null) * 1000L}).")
    }

    fun updateDeviceCode(deviceCode: String, verificationUrl: String) {
        AuthState.deviceCode = deviceCode
        AuthState.verificationUrl = verificationUrl
        println("Stored device_code in memory for later token exchange.")
        println("Stored verification URL for later use: $verificationUrl")
    }

    /**
     * Determine the current authentication status.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun status(): Status {
        // 1) No device code yet -> starting device authorization
        if (deviceCode.isNullOrBlank() || verificationUrl.isNullOrBlank())
            return Status.StartingDeviceAuthorization

        // 2) Device code exists but tokens not set yet -> waiting for manual tasks
        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank())
            return Status.WaitingForManualTasks(deviceCode!!, verificationUrl!!)

        // 3) Tokens exist; check expiry if we have metadata
        val issuedAt = issuedAdMillis
        val expires = expiresInSeconds
        if (issuedAt != null && expires != null) {
            val now = time(null) * 1000L
            val expiresAt = issuedAt + expires.toLong() * 1000L
            if (now >= expiresAt) return Status.TokenExpired(refreshToken!!)
        }

        // 4) Otherwise we are up - return with guaranteed tokens
        return Status.Up(accessToken!!, refreshToken!!)
    }

    /**
     * Helper for tests to clear in-memory state between test cases.
     */
    fun clear() {
        accessToken = null
        refreshToken = null
        expiresInSeconds = null
        issuedAdMillis = null
        deviceCode = null
        verificationUrl = null
    }
}