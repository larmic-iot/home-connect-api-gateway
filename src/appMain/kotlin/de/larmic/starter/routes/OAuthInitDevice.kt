package de.larmic.starter.routes

import de.larmic.starter.AppConfig
import de.larmic.starter.client.DeviceAuthorizationResponse
import de.larmic.starter.client.HomeConnectClient
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.deviceAuthorizationRoute() {
    get("/auth/init") {
        val clientId = AppConfig.clientId
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
