package de.larmic.starter

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.time

/**
 * In-memory holder for OAuth tokens. Ephemeral; will be lost on application restart.
 */
object AuthState {
    enum class Status {
        STARTING_DEVICE_AUTHORIZATION,
        WAITING_FOR_MANUAL_TASKS,
        UP,
        TOKEN_EXPIRED
    }

    var accessToken: String? = null
        private set
    var refreshToken: String? = null
        private set
    var expiresInSeconds: Int? = null
        private set
    var issuedAdMillis: Long? = null
        private set
    var deviceCode: String? = null
        private set

    @OptIn(ExperimentalForeignApi::class)
    fun updateTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Int?) {
        AuthState.accessToken = accessToken
        AuthState.refreshToken = refreshToken
        AuthState.expiresInSeconds = expiresInSeconds
        issuedAdMillis = time(null) * 1000L
        println("Stored OAuth tokens in memory (issuedAt=${time(null) * 1000L}).")
    }

    fun updateDeviceCode(deviceCode: String) {
        AuthState.deviceCode = deviceCode
        println("Stored device_code in memory for later token exchange.")
    }

    /**
     * Determine the current authentication status.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun status(): Status {
        // 1) No device code yet -> starting device authorization
        if (deviceCode.isNullOrBlank()) return Status.STARTING_DEVICE_AUTHORIZATION

        // 2) Device code exists but tokens not set yet -> waiting for manual tasks
        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) return Status.WAITING_FOR_MANUAL_TASKS

        // 3) Tokens exist; check expiry if we have metadata
        val issuedAt = issuedAdMillis
        val expires = expiresInSeconds
        if (issuedAt != null && expires != null) {
            val now = time(null) * 1000L
            val expiresAt = issuedAt + expires.toLong() * 1000L
            if (now >= expiresAt) return Status.TOKEN_EXPIRED
        }

        // 4) Otherwise we are up
        return Status.UP
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
    }
}
