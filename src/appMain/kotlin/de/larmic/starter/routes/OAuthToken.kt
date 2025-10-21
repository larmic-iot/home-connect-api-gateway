package de.larmic.starter.routes

import de.larmic.starter.client.HomeConnectClient
import de.larmic.starter.client.OAuthTokenResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv as cGetEnv

fun Route.oauthTokenRoute() {
    get("/auth/device/token") {
        val clientId = getenv("HOME_CONNECT_CLIENT_ID")
        if (clientId.isNullOrBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "status" to "ERROR",
                    "message" to "Environment variable HOME_CONNECT_CLIENT_ID is not set.",
                    "hint" to "export HOME_CONNECT_CLIENT_ID=YOUR_CLIENT_ID and retry"
                )
            )
            return@get
        }

        val deviceCode = call.request.queryParameters["device_code"]
        if (deviceCode.isNullOrBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "status" to "ERROR",
                    "message" to "Missing query parameter: device_code",
                    "hint" to "Use value returned from /auth/device/start"
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

@OptIn(ExperimentalForeignApi::class)
private fun getenv(name: String): String? = cGetEnv(name)?.toKString()
