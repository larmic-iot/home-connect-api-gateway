package de.larmic.starter

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlin.system.exitProcess
import platform.posix.getenv as cGetEnv

/**
 * Global application configuration loaded once at startup.
 */
object AppConfig {
    val clientId: String
    val initialPollDelayMs: Long
    val pollIntervalMs: Long

    init {
        val value = getenv("HOME_CONNECT_CLIENT_ID")
        if (value.isNullOrBlank()) {
            println("FATAL: Environment variable HOME_CONNECT_CLIENT_ID is not set. Application cannot start without it.")
            println("Hint: export HOME_CONNECT_CLIENT_ID=YOUR_CLIENT_ID and retry")
            exitProcess(1)
        }
        clientId = value
        println("Loaded HOME_CONNECT_CLIENT_ID from environment.")

        // Startup device flow polling configuration (seconds)
        // Defaults: initial=10s, interval=5s
        // Note: OAuth 2.0 Device Flow commonly uses >= 5 seconds between polls
        val initialSeconds = getenv("DEVICE_FLOW_INITIAL_POLL_DELAY_SECONDS")?.toLongOrNull() ?: 10L
        val intervalSeconds = getenv("DEVICE_FLOW_POLL_INTERVAL_SECONDS")?.toLongOrNull() ?: 5L
        initialPollDelayMs = initialSeconds * 1000L
        pollIntervalMs = intervalSeconds * 1000L
        println("Device flow polling configured: initialDelay=${initialSeconds}s, interval=${intervalSeconds}s")
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun getenv(name: String): String? = cGetEnv(name)?.toKString()
