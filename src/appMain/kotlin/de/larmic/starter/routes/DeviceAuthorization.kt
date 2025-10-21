package de.larmic.starter.routes

import de.larmic.starter.client.DeviceAuthorizationResponse
import de.larmic.starter.client.HomeConnectClient
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv as cGetEnv

fun Route.deviceAuthorizationRoute() {
    get("/auth/device/start") {
        val clientId = getenv("HOME_CONNECT_CLIENT_ID")
        if (clientId.isNullOrBlank()) {
            call.respond(
                mapOf(
                    "status" to "ERROR",
                    "message" to "Environment variable HOME_CONNECT_CLIENT_ID is not set.",
                    "hint" to "export HOME_CONNECT_CLIENT_ID=YOUR_CLIENT_ID and retry"
                )
            )
            return@get
        }

        val scope = "IdentifyAppliance Monitor Settings Control"

        try {
            val client = HomeConnectClient()
            val payload: DeviceAuthorizationResponse = client.startDeviceAuthorization(clientId, scope)
            call.respond(payload)
        } catch (t: Throwable) {
            println("Device authorization failed: ${t.message}")
            call.respond(mapOf("status" to "ERROR", "message" to (t.message ?: "Unknown error")))
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun getenv(name: String): String? = cGetEnv(name)?.toKString()
