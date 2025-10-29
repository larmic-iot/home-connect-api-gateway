package de.larmic.starter.routes

import de.larmic.starter.BuildKonfig
import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.openApiRoute() {
    get("/openapi.yaml") {
        call.respondText(
            BuildKonfig.OPENAPI_YAML,
            contentType = ContentType.parse("application/yaml")
        )
    }
}
