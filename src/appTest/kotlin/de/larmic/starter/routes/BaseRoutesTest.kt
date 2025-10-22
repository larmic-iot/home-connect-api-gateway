package de.larmic.starter.routes

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.Route
import io.ktor.server.testing.ApplicationTestBuilder

fun ApplicationTestBuilder.setupTest(
    configure: Route.() -> Unit,
): HttpClient {
    install(ContentNegotiation) { json() }
    routing(configure)

    return createClient {
        install(ClientContentNegotiation) {
            json()
        }
    }
}