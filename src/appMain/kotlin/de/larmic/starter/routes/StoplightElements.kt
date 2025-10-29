package de.larmic.starter.routes

import de.larmic.starter.BuildKonfig
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.stoplightElementsRoute() {
    get("/index.html") {
        call.response.headers.append(HttpHeaders.ContentDisposition, "inline; filename=\"index.html\"")
        call.respondText(
            BuildKonfig.INDEX_HTML,
            contentType = ContentType.Text.Html
        )
    }
}