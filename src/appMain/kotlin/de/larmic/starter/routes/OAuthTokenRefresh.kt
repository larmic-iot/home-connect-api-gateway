package de.larmic.starter.routes

import de.larmic.starter.AppConfig
import de.larmic.starter.AuthState
import de.larmic.starter.client.OAuthTokenResponse
import de.larmic.starter.client.HomeConnectClient
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.tokenRefreshRoute() {
    get("/oauth/token/refresh") {
        val clientId = AppConfig.clientId
        val authStatus = AuthState.status()

        val refreshToken = when (authStatus) {
            is AuthState.Status.Up -> authStatus.refreshToken
            is AuthState.Status.TokenExpired -> authStatus.refreshToken
            is AuthState.Status.StartingDeviceAuthorization,
            is AuthState.Status.WaitingForManualTasks -> null
        }

        if (refreshToken.isNullOrBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "status" to "ERROR",
                    "message" to "No refresh_token available. Obtain tokens first via device authorization.",
                    "hint" to "Run /oauth/init then /oauth/token until success"
                )
            )
            return@get
        }

        try {
            val client = HomeConnectClient()
            val payload: OAuthTokenResponse = client.refreshToken(clientId, refreshToken)

            if (payload.error != null) {
                call.respond(HttpStatusCode.BadRequest, payload)
            } else {
                call.respond(HttpStatusCode.OK, payload)
            }
        } catch (t: Throwable) {
            println("Token refresh failed: ${t.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("status" to "ERROR", "message" to (t.message ?: "Unknown error")))
        }
    }
}
