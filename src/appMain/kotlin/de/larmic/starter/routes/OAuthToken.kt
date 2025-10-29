package de.larmic.starter.routes

import de.larmic.starter.AppConfig
import de.larmic.starter.AuthState
import de.larmic.starter.client.HomeConnectClient
import de.larmic.starter.client.OAuthTokenResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

private suspend fun buildOAuthTokenResponse(): Pair<HttpStatusCode, Any> {
    val clientId = AppConfig.clientId
    val authStatus = AuthState.status()

    val deviceCode = when (authStatus) {
        is AuthState.Status.WaitingForManualTasks -> authStatus.deviceCode
        is AuthState.Status.Up -> return HttpStatusCode.OK to mapOf(
            "status" to "ALREADY_AUTHENTICATED",
            "message" to "OAuth tokens are already available."
        )
        else -> null
    }

    if (deviceCode.isNullOrBlank()) {
        return HttpStatusCode.BadRequest to mapOf(
            "status" to "ERROR",
            "message" to "No device_code available. Start device authorization first.",
            "hint" to "Call /oauth/init and follow the instructions"
        )
    }

    return try {
        val client = HomeConnectClient()
        val payload: OAuthTokenResponse = client.getOAuthToken(clientId, deviceCode)

        when {
            payload.error == "authorization_pending" -> HttpStatusCode.Accepted to payload
            payload.error != null -> HttpStatusCode.BadRequest to payload
            else -> {
                // Store the tokens in AuthState
                if (payload.accessToken != null && payload.refreshToken != null) {
                    AuthState.updateTokens(
                        accessToken = payload.accessToken,
                        refreshToken = payload.refreshToken,
                        expiresInSeconds = payload.expiresInToken
                    )
                }
                HttpStatusCode.OK to payload
            }
        }
    } catch (t: Throwable) {
        println("Token exchange failed: ${t.message}")
        HttpStatusCode.InternalServerError to mapOf("status" to "ERROR", "message" to (t.message ?: "Unknown error"))
    }
}

fun Route.oauthTokenRoute() {
    get("/oauth/token") {
        val (status, body) = buildOAuthTokenResponse()
        call.respond(status, body)
    }
}
