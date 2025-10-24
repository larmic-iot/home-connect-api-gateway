package de.larmic.starter.routes

import de.larmic.starter.AuthState
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
)

@Serializable
data class ReadinessCheckDetail(
    val authorization: String,
)

@Serializable
data class ReadinessResponse(
    val status: String,
    val checks: ReadinessCheckDetail,
)

fun Route.healthRoutes() {
    get("/health") {
        call.respond(HttpStatusCode.OK, HealthResponse(status = "UP"))
    }

    get("/health/ready") {
        val authStatus = AuthState.status()

        val statusName = when (authStatus) {
            is AuthState.Status.StartingDeviceAuthorization -> "STARTING_DEVICE_AUTHORIZATION"
            is AuthState.Status.WaitingForManualTasks -> "WAITING_FOR_MANUAL_TASKS"
            is AuthState.Status.Up -> "UP"
            is AuthState.Status.TokenExpired -> "TOKEN_EXPIRED"
        }

        val ready = authStatus is AuthState.Status.Up
        val httpStatus = if (ready) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
        val overall = if (ready) "READY" else "NOT_READY"

        call.respond(
            httpStatus,
            ReadinessResponse(
                status = overall,
                checks = ReadinessCheckDetail(
                    authorization = statusName
                )
            )
        )
    }

    get("/health/live") {
        call.respond(HttpStatusCode.OK, HealthResponse(status = "UP"))
    }
}