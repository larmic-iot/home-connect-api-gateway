package de.larmic.starter

import de.larmic.starter.routes.deviceAuthorizationRoute
import de.larmic.starter.routes.healthRoutes
import de.larmic.starter.routes.oauthTokenRoute
import de.larmic.starter.routes.tokenRefreshRoute
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.*

fun main() {
    // Trigger AppConfig initialization (will exit if HOME_CONNECT_CLIENT_ID is missing)
    val clientId = AppConfig.clientId
    println("Starting Server with HOME_CONNECT_CLIENT_ID=$clientId ...")

    embeddedServer(CIO, port = 8080) {
        install(ContentNegotiation) {
            json()
        }

        routing {
            healthRoutes()
            deviceAuthorizationRoute()
            oauthTokenRoute()
            tokenRefreshRoute()
        }
    }.start(wait = true)
}