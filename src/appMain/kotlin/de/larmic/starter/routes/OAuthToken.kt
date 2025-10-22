package de.larmic.starter.routes

import de.larmic.starter.AppConfig
import de.larmic.starter.AuthState
import de.larmic.starter.client.HomeConnectClient
import de.larmic.starter.client.OAuthTokenResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.oauthTokenRoute() {
    get("/auth/device/token") {
        val clientId = AppConfig.clientId

        // Prefer stored device_code from the last authorization start
        val deviceCode = AuthState.deviceCode

        if (deviceCode.isNullOrBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "status" to "ERROR",
                    "message" to "No device_code available. Start device authorization first.",
                    "hint" to "Call /auth/device/start and follow the instructions"
                )
            )
            return@get
        }

        try {
            val client = HomeConnectClient()
            val payload: OAuthTokenResponse = client.getOAuthToken(clientId, deviceCode)

            when {
                payload.error == "authorization_pending" -> call.respond(HttpStatusCode.Accepted, payload)
                payload.error != null -> call.respond(HttpStatusCode.BadRequest, payload)
                else -> call.respond(HttpStatusCode.OK, payload)
            }
        } catch (t: Throwable) {
            println("Token exchange failed: ${t.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("status" to "ERROR", "message" to (t.message ?: "Unknown error")))
        }
    }
}
