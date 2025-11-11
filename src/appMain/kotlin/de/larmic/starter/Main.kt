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
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.*
import kotlinx.coroutines.*

fun Application.module() {
    // Create an application-bound scope for background jobs
    val appScope = CoroutineScope(coroutineContext + SupervisorJob())

    // Start background device authorization + polling flow when application starts
    monitor.subscribe(ApplicationStarted) {
        appScope.launch(Dispatchers.Default) {
            try {
                val clientId = AppConfig.clientId
                val client = HomeConnectClient()
                log.info("[Startup] Starting device authorization flow ...")
                client.startDeviceAuthorization(clientId)

                // Wait initial delay before first token polling
                val initialDelay = AppConfig.initialPollDelayMs
                val interval = AppConfig.pollIntervalMs
                val initialSec = initialDelay / 1000
                val intervalSec = interval / 1000
                log.info("[Startup] Will start polling for token after ${initialSec}s, then every ${intervalSec}s ...")
                delay(initialDelay)

                while (isActive) {
                    when (val st = AuthState.status()) {
                        is AuthState.Status.WaitingForManualTasks -> {
                            log.info("[Startup] Acquire token. No need to poll for token.")
                            val result = client.getOAuthToken(clientId, st.deviceCode)
                            if (result.accessToken != null) {
                                log.info("[Startup] Token acquired successfully. Startup polling will stop.")
                                break
                            }
                            // Continue polling
                            delay(interval)
                        }
                        is AuthState.Status.Up -> {
                            log.info("[Startup] Already authenticated. No need to poll for token.")
                            break
                        }
                        is AuthState.Status.TokenExpired -> {
                            // We only handle initial acquisition here; token refresh handled via route or later logic
                            log.info("[Startup] Token is marked expired while polling; will keep polling for a fresh token ...")
                            delay(interval)
                        }
                        is AuthState.Status.StartingDeviceAuthorization -> {
                            // In case device auth not yet started for some reason, try again
                            log.info("[Startup] Device authorization not started; retrying start ...")
                            client.startDeviceAuthorization(clientId)
                            delay(interval)
                        }
                    }
                }
            } catch (t: Throwable) {
                log.error("[Startup] Device authorization flow failed: ${t.message}", t)
            }
        }
    }

    // Cancel background jobs when application stops
    monitor.subscribe(ApplicationStopping) {
        appScope.cancel("Application stopping")
    }

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
}

fun main() {
    // Trigger AppConfig initialization (will exit if HOME_CONNECT_CLIENT_ID is missing)
    val clientId = AppConfig.clientId
    println("Starting Server with HOME_CONNECT_CLIENT_ID=$clientId ...")

    embeddedServer(CIO, port = 8080, module = Application::module)
        .start(wait = true)
}