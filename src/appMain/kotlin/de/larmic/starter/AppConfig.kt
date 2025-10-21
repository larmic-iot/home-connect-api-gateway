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

    init {
        val value = getenv("HOME_CONNECT_CLIENT_ID")
        if (value.isNullOrBlank()) {
            println("FATAL: Environment variable HOME_CONNECT_CLIENT_ID is not set. Application cannot start without it.")
            println("Hint: export HOME_CONNECT_CLIENT_ID=YOUR_CLIENT_ID and retry")
            exitProcess(1)
        }
        clientId = value
        println("Loaded HOME_CONNECT_CLIENT_ID from environment.")
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun getenv(name: String): String? = cGetEnv(name)?.toKString()
