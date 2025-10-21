package de.larmic.starter.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.curl.Curl
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import platform.posix.wait
import kotlin.io.println

/**
 * Ktor HTTP client using Curl engine to be platform-agnostic (works in Docker/Linux and macOS).
 */
private fun httpClient(): HttpClient = HttpClient(Curl) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
}

@Serializable
data class DeviceAuthorizationResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_uri") val verificationUri: String,
    @SerialName("verification_uri_complete") val verificationUriComplete: String? = null,
    @SerialName("expires_in") val expiresIn: Int? = null,
    val interval: Int? = null
)

class HomeConnectClient(
    private val baseUrl: String = "https://api.home-connect.com"
) {
    /**
     * Performs the Device Authorization request (OAuth 2.0 Device Code Flow, Step 1).
     * Returns the payload as DeviceAuthorizationResponse.
     * Logs useful information for the user to complete the flow manually.
     */
    suspend fun startDeviceAuthorization(
        clientId: String,
        scope: String = "IdentifyAppliance Monitor Settings Control"
    ): DeviceAuthorizationResponse {
        val bodyParams = Parameters.build {
            append("client_id", clientId)
            append("scope", scope)
        }.formUrlEncode()

        val client = httpClient()
        try {
            val response = client.post("$baseUrl/security/oauth/device_authorization") {
                contentType(ContentType.Application.FormUrlEncoded)
                headers { append("Accept", "application/json") }
                setBody(bodyParams)
            }

            val payload: DeviceAuthorizationResponse = response.body()

            // Log formatted output similar to example-requests.http
            println("═══════════════════════════════════════════")
            println("Open this URL:")
            println("(This initial step is only needed to authorize this app with Home Connect. Once authorized, a refresh token will be used automatically.)")
            println("${payload.verificationUri}?user_code=${payload.userCode}")
            println("═══════════════════════════════════════════")
            if (payload.expiresIn != null) println("You have ${payload.expiresIn} seconds")
            val wait = payload.interval ?: 5
            println("")
            println("After entering the code, execute Step 2 (wait ${wait} seconds)")

            // In containerized/headless environments we do not auto-open a browser. Logging is sufficient.
            return payload
        } finally {
            client.close()
        }
    }

}
