package de.larmic.starter

import de.larmic.starter.routes.deviceAuthorizationRoute
import de.larmic.starter.routes.healthRoutes
import de.larmic.starter.routes.helloWorld
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.*

fun main() {
    println("Starting Server...")

    embeddedServer(CIO, port = 8080) {
        install(ContentNegotiation) {
            json()
        }

        routing {
            helloWorld()
            healthRoutes()
            deviceAuthorizationRoute()
        }
    }.start(wait = true)
}