package de.larmic.starter.routes

import de.larmic.starter.AuthState
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthRoutesTest {

    @BeforeTest
    fun setUp() {
        AuthState.clear()
    }

    @AfterTest
    fun tearDown() {
        AuthState.clear()
    }

    @Test
    fun get_health_returns_UP() = testApplication {
        val client = setupTest { healthRoutes() }

        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)

        val healthResponse = response.body<HealthResponse>()
        assertEquals("UP", healthResponse.status)
    }

    @Test
    fun get_health_live_returns_UP() = testApplication {
        val client = setupTest { healthRoutes() }

        val response = client.get("/health/live")
        assertEquals(HttpStatusCode.OK, response.status)

        val healthResponse = response.body<HealthResponse>()
        assertEquals("UP", healthResponse.status)
    }

    @Test
    fun get_health_ready_not_ready_starting_device_authorization_when_no_deviceCode() = testApplication {
        val client = setupTest { healthRoutes() }

        val response = client.get("/health/ready")
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)

        val readiness = response.body<ReadinessResponse>()
        assertEquals("NOT_READY", readiness.status)
        assertEquals("STARTING_DEVICE_AUTHORIZATION", readiness.checks.authorization)
    }

    @Test
    fun get_health_ready_not_ready_waiting_for_manual_tasks_when_deviceCode_but_tokens_missing() = testApplication {
        val client = setupTest { healthRoutes() }

        AuthState.updateDeviceCode("device-code-123")

        val response = client.get("/health/ready")
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)

        val readiness = response.body<ReadinessResponse>()
        assertEquals("NOT_READY", readiness.status)
        assertEquals("WAITING_FOR_MANUAL_TASKS", readiness.checks.authorization)
    }

    @Test
    fun get_health_ready_not_ready_token_expired_when_tokens_expired() = testApplication {
        val client = setupTest { healthRoutes() }

        AuthState.updateDeviceCode("device-code-123")
        // expires immediately
        AuthState.updateTokens(accessToken = "acc", refreshToken = "ref", expiresInSeconds = 0)

        val response = client.get("/health/ready")
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)

        val readiness = response.body<ReadinessResponse>()
        assertEquals("NOT_READY", readiness.status)
        assertEquals("TOKEN_EXPIRED", readiness.checks.authorization)
    }

    @Test
    fun get_health_ready_ready_up_when_tokens_valid() = testApplication {
        val client = setupTest { healthRoutes() }

        AuthState.updateDeviceCode("device-code-123")
        AuthState.updateTokens(accessToken = "acc", refreshToken = "ref", expiresInSeconds = 3600)

        val response = client.get("/health/ready")
        assertEquals(HttpStatusCode.OK, response.status)

        val readiness = response.body<ReadinessResponse>()
        assertEquals("READY", readiness.status)
        assertEquals("UP", readiness.checks.authorization)
    }
}