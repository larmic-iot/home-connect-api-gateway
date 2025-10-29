package de.larmic.starter.routes

import de.larmic.starter.BuildKonfig
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.openApiRoute() {
    get("/openapi.yaml") {
        // Ensure the browser renders the YAML instead of downloading it
        call.response.headers.append(HttpHeaders.ContentDisposition, "inline; filename=\"openapi.yaml\"")
        call.respondText(
            BuildKonfig.OPENAPI_YAML,
            contentType = ContentType.Text.Plain
        )
    }
}
