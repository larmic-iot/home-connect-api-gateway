package de.larmic.starter

import de.larmic.starter.client.HomeConnectClient
import de.larmic.starter.routes.deviceAuthorizationRoute
import de.larmic.starter.routes.healthRoutes
import de.larmic.starter.routes.oauthTokenRoute
import de.larmic.starter.routes.tokenRefreshRoute
import de.larmic.starter.routes.proxyRoutes
import de.larmic.starter.routes.openApiRoute
import de.larmic.starter.routes.stoplightElementsRoute
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.*
import kotlinx.coroutines.*

fun main() {
    // Trigger AppConfig initialization (will exit if HOME_CONNECT_CLIENT_ID is missing)
    val clientId = AppConfig.clientId
    println("Starting Server with HOME_CONNECT_CLIENT_ID=$clientId ...")

    // Start background device authorization + polling flow
    GlobalScope.launch {
        try {
            val client = HomeConnectClient()
            println("[Startup] Starting device authorization flow ...")
            client.startDeviceAuthorization(clientId)

            // Wait initial delay before first token polling
            val initialDelay = AppConfig.initialPollDelayMs
            val interval = AppConfig.pollIntervalMs
            val initialSec = initialDelay / 1000
            val intervalSec = interval / 1000
            println("[Startup] Will start polling for token after ${initialSec}s, then every ${intervalSec}s ...")
            delay(initialDelay)

            while (true) {
                when (val st = AuthState.status()) {
                    is AuthState.Status.WaitingForManualTasks -> {
                        val result = client.getOAuthToken(clientId, st.deviceCode)
                        if (result.accessToken != null) {
                            println("[Startup] Token acquired successfully. Startup polling will stop.")
                            break
                        }
                        // Continue polling
                        delay(interval)
                    }
                    is AuthState.Status.Up -> {
                        println("[Startup] Already authenticated. No need to poll for token.")
                        break
                    }
                    is AuthState.Status.TokenExpired -> {
                        // We only handle initial acquisition here; token refresh handled via route or later logic
                        println("[Startup] Token is marked expired while polling; will keep polling for a fresh token ...")
                        delay(interval)
                    }
                    is AuthState.Status.StartingDeviceAuthorization -> {
                        // In case device auth not yet started for some reason, try again
                        println("[Startup] Device authorization not started; retrying start ...")
                        client.startDeviceAuthorization(clientId)
                        delay(interval)
                    }
                }
            }
        } catch (t: Throwable) {
            println("[Startup] Device authorization flow failed: ${t.message}")
        }
    }

    embeddedServer(CIO, port = 8080) {
        install(ContentNegotiation) {
            json()
        }

        routing {
            healthRoutes()
            deviceAuthorizationRoute()
            oauthTokenRoute()
            tokenRefreshRoute()
            openApiRoute()
            stoplightElementsRoute()
            proxyRoutes()
        }
    }.start(wait = true)
}