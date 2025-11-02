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

        // Startup device flow polling configuration (milliseconds)
        // Defaults: initial=5000ms (5 seconds), interval=5000ms (5 seconds)
        // OAuth 2.0 Device Flow requires at least 5 seconds between polls
        initialPollDelayMs = getenv("DEVICE_FLOW_INITIAL_POLL_DELAY_MS")?.toLongOrNull() ?: 10000L
        pollIntervalMs = getenv("DEVICE_FLOW_POLL_INTERVAL_MS")?.toLongOrNull() ?: 5000L
        println("Device flow polling configured: initialPollDelayMs=$initialPollDelayMs, pollIntervalMs=$pollIntervalMs")
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun getenv(name: String): String? = cGetEnv(name)?.toKString()
