package de.larmic.starter.routes

import de.larmic.starter.AuthState
import de.larmic.starter.client.HomeConnectClient
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.request.queryString
import io.ktor.server.request.contentType
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import kotlin.io.println

fun Route.proxyRoutes() {
    route("/proxy/{path...}") {
        get {
            call.handleProxy(HttpMethod.Get)
        }
        put {
            call.handleProxy(HttpMethod.Put)
        }
        delete {
            call.handleProxy(HttpMethod.Delete)
        }
        post {
            call.handleProxy(HttpMethod.Post)
        }
    }
}

private suspend fun ApplicationCall.handleProxy(method: HttpMethod) {
    val authStatus = AuthState.status()

    val statusName = when (authStatus) {
        is AuthState.Status.StartingDeviceAuthorization -> "StartingDeviceAuthorization"
        is AuthState.Status.WaitingForManualTasks -> "WaitingForManualTasks"
        is AuthState.Status.Up -> "Up"
        is AuthState.Status.TokenExpired -> "TokenExpired"
    }

    if (authStatus !is AuthState.Status.Up) {
        this.respond(
            HttpStatusCode.ServiceUnavailable,
            mapOf(
                "status" to "ERROR",
                "message" to "Application is not ready: $statusName",
                "hint" to "Initialize OAuth flow until health/ready shows UP",
                "homeConnectApiSpec" to "https://apiclient.home-connect.com/"
            )
        )
        return
    }

    val tail = this.parameters.getAll("path")?.joinToString("/") ?: ""
    val query = this.request.queryString()
    val pathAndQuery = if (query.isBlank()) tail else "$tail?$query"

    val client = HomeConnectClient()

    val bodyText = when (method) {
        HttpMethod.Put, HttpMethod.Post -> this.receiveText()
        else -> null
    }
    val reqContentType: ContentType? = when (method) {
        HttpMethod.Put, HttpMethod.Post -> this.request.contentType()
        else -> null
    }

    try {
        val proxied = client.proxy(
            method = method,
            pathAndQuery = pathAndQuery,
            accessToken = authStatus.accessToken,
            bodyText = bodyText?.takeIf { it.isNotEmpty() },
            contentType = reqContentType
        )

        val respContentType = proxied.contentType?.let { ContentType.parse(it) }
        if (respContentType != null) {
            this.respondBytes(
                bytes = proxied.body,
                status = HttpStatusCode.fromValue(proxied.status),
                contentType = respContentType
            )
        } else {
            this.respondBytes(
                bytes = proxied.body,
                status = HttpStatusCode.fromValue(proxied.status)
            )
        }
    } catch (t: Throwable) {
        println("Proxy error: ${t.message}")
        this.respond(
            HttpStatusCode.BadGateway,
            mapOf(
                "status" to "ERROR",
                "message" to (t.message ?: "Proxy request failed"),
                "homeConnectApiSpec" to "https://apiclient.home-connect.com/"
            )
        )
    }
}
