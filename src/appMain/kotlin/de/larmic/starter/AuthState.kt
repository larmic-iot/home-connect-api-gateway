package de.larmic.starter

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.time

/**
 * In-memory holder for OAuth tokens. Ephemeral; will be lost on application restart.
 */
object AuthState {
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
}
