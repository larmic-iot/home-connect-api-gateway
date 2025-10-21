package de.larmic.starter

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.time

/**
 * In-memory holder for OAuth tokens. Ephemeral; will be lost on application restart.
 */
object AuthState {
    private var _accessToken: String? = null
    private var _refreshToken: String? = null
    private var _expiresInSeconds: Int? = null
    private var _issuedAtMillis: Long? = null

    @OptIn(ExperimentalForeignApi::class)
    fun updateTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Int?) {
        val nowSeconds = time(null).toLong() // epoch seconds
        val nowMillis = nowSeconds * 1000L
        _accessToken = accessToken
        _refreshToken = refreshToken
        _expiresInSeconds = expiresInSeconds
        _issuedAtMillis = nowMillis
        println("Stored OAuth tokens in memory (issuedAt=${nowMillis}).")
    }
}
