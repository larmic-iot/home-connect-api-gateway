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
            BuildKonfig.STOPLIGHT_INDEX_HTML,
            contentType = ContentType.Text.Html
        )
    }

    get("/assets/stoplight-elements/web-components.min.js") {
        call.response.headers.append(HttpHeaders.ContentDisposition, "inline; filename=\"web-components.min.js\"")
        call.respondText(
            BuildKonfig.STOPLIGHT_JS,
            contentType = ContentType.Text.JavaScript
        )
    }

    get("/assets/stoplight-elements/styles.min.css") {
        call.response.headers.append(HttpHeaders.ContentDisposition, "inline; filename=\"styles.min.css\"")
        call.respondText(
            BuildKonfig.STOPLIGHT_CSS,
            contentType = ContentType.Text.CSS
        )
    }
}