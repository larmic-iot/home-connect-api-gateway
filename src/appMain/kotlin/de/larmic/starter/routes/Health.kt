package de.larmic.starter.routes

import de.larmic.starter.AuthState
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.healthRoutes() {
    get("/health") {
        call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
    }

    get("/health/ready") {
        val authStatus = AuthState.status()
        val isReady = authStatus == AuthState.Status.UP
        val httpStatus = if (isReady) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
        val overall = if (isReady) "READY" else "NOT_READY"

        call.respond(
            httpStatus,
            mapOf(
                "status" to overall,
                "checks" to mapOf(
                    "authorization" to authStatus.name
                )
            )
        )
    }

    get("/health/live") {
        call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
    }
}