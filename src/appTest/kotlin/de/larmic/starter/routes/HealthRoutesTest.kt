package de.larmic.starter.routes

import de.larmic.starter.AuthState
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.testApplication
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        install(ContentNegotiation) { json() }
        routing { healthRoutes() }

        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(Regex("""\"status\"\s*:\s*\"UP\"""").containsMatchIn(response.bodyAsText()))
    }

    @Test
    fun get_health_live_returns_UP() = testApplication {
        install(ContentNegotiation) { json() }
        routing { healthRoutes() }

        val response = client.get("/health/live")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(Regex("""\"status\"\s*:\s*\"UP\"""").containsMatchIn(response.bodyAsText()))
    }

    @Test
    fun get_health_ready_not_ready_starting_device_authorization_when_no_deviceCode() = testApplication {
        install(ContentNegotiation) { json() }
        routing { healthRoutes() }

        val response = client.get("/health/ready")
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        val body = response.bodyAsText()
        assertTrue(Regex("""\"status\"\s*:\s*\"NOT_READY\"""").containsMatchIn(body))
        assertTrue(Regex("""\"authorization\"\s*:\s*\"STARTING_DEVICE_AUTHORIZATION\"""").containsMatchIn(body))
    }

    @Test
    fun get_health_ready_not_ready_waiting_for_manual_tasks_when_deviceCode_but_tokens_missing() = testApplication {
        install(ContentNegotiation) { json() }
        routing { healthRoutes() }

        AuthState.updateDeviceCode("device-code-123")

        val response = client.get("/health/ready")
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        val body = response.bodyAsText()
        assertTrue(Regex("""\"status\"\s*:\s*\"NOT_READY\"""").containsMatchIn(body))
        assertTrue(Regex("""\"authorization\"\s*:\s*\"WAITING_FOR_MANUAL_TASKS\"""").containsMatchIn(body))
    }

    @Test
    fun get_health_ready_not_ready_token_expired_when_tokens_expired() = testApplication {
        install(ContentNegotiation) { json() }
        routing { healthRoutes() }

        AuthState.updateDeviceCode("device-code-123")
        // expires immediately
        AuthState.updateTokens(accessToken = "acc", refreshToken = "ref", expiresInSeconds = 0)

        val response = client.get("/health/ready")
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        val body = response.bodyAsText()
        assertTrue(Regex("""\"status\"\s*:\s*\"NOT_READY\"""").containsMatchIn(body))
        assertTrue(Regex("""\"authorization\"\s*:\s*\"TOKEN_EXPIRED\"""").containsMatchIn(body))
    }

    @Test
    fun get_health_ready_ready_up_when_tokens_valid() = testApplication {
        install(ContentNegotiation) { json() }
        routing { healthRoutes() }

        AuthState.updateDeviceCode("device-code-123")
        AuthState.updateTokens(accessToken = "acc", refreshToken = "ref", expiresInSeconds = 3600)

        val response = client.get("/health/ready")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(Regex("""\"status\"\s*:\s*\"READY\"""").containsMatchIn(body))
        assertTrue(Regex("""\"authorization\"\s*:\s*\"UP\"""").containsMatchIn(body))
    }
}
