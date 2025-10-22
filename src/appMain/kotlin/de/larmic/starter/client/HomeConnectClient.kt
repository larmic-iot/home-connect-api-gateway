package de.larmic.starter.client

import de.larmic.starter.AuthState
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

 @Serializable
 data class OAuthTokenResponse(
     @SerialName("access_token") val accessToken: String? = null,
     @SerialName("refresh_token") val refreshToken: String? = null,
     @SerialName("token_type") val tokenType: String? = null,
     @SerialName("expires_in") val expiresInToken: Int? = null,
     val scope: String? = null,
     val error: String? = null,
     @SerialName("error_description") val errorDescription: String? = null,
     @SerialName("error_uri") val errorUri: String? = null
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
            println("After entering the code, execute Step 2 (wait $wait seconds)")

            AuthState.updateDeviceCode(payload.deviceCode)
            return payload
        } finally {
            client.close()
        }
    }

    /**
     * Step 2: Exchange device_code for tokens.
     * Returns OAuthTokenResponse which contains either access/refresh tokens or error information (e.g., authorization_pending).
     */
    suspend fun getOAuthToken(
        clientId: String,
        deviceCode: String
    ): OAuthTokenResponse {
        val bodyParams = Parameters.build {
            append("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
            append("client_id", clientId)
            append("device_code", deviceCode)
        }.formUrlEncode()

        val client = httpClient()
        try {
            val response = client.post("$baseUrl/security/oauth/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                headers { append("Accept", "application/json") }
                setBody(bodyParams)
            }

            val payload: OAuthTokenResponse = response.body()

            when {
                payload.error == "authorization_pending" -> {
                    println("Waiting for authorization... Run this request again!")
                }
                payload.error != null -> {
                    println("Error: ${payload.error}${payload.errorDescription?.let { ": $it" } ?: ""}")
                }
                else -> {
                    println("═══════════════════════════════════════════")
                    println("✓ Success!")
                    println("═══════════════════════════════════════════")
                    println("Access Token: ${payload.accessToken}")
                    println("Refresh Token: ${payload.refreshToken}")
                    println("═══════════════════════════════════════════")

                    // Persist tokens in global AuthState with expiry and timestamp
                    val access = payload.accessToken
                    if (access != null) {
                        AuthState.updateTokens(
                            accessToken = access,
                            refreshToken = payload.refreshToken,
                            expiresInSeconds = payload.expiresInToken
                        )
                    } else {
                        println("Warning: Successful token response without access_token. Nothing stored.")
                    }
                }
            }

            return payload
        } finally {
            client.close()
        }
    }

    /**
     * Refresh access token using the refresh_token grant.
     * On success, updates AuthState with new access token and optionally new refresh token.
     * If the response omits refresh_token, the previous one is retained in AuthState.
     */
    suspend fun refreshToken(
        clientId: String,
        refreshToken: String
    ): OAuthTokenResponse {
        val bodyParams = Parameters.build {
            append("grant_type", "refresh_token")
            append("client_id", clientId)
            append("refresh_token", refreshToken)
        }.formUrlEncode()

        val client = httpClient()
        try {
            val response = client.post("$baseUrl/security/oauth/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                headers { append("Accept", "application/json") }
                setBody(bodyParams)
            }

            val payload: OAuthTokenResponse = response.body()

            if (payload.error != null) {
                println("Refresh error: ${payload.error}${payload.errorDescription?.let { ": $it" } ?: ""}")
                return payload
            }

            val newAccess = payload.accessToken
            val newRefresh = payload.refreshToken ?: refreshToken
            if (newAccess != null) {
                AuthState.updateTokens(
                    accessToken = newAccess,
                    refreshToken = newRefresh,
                    expiresInSeconds = payload.expiresInToken
                )
                println("✓ Token refreshed and stored in memory.")
            } else {
                println("Warning: Refresh response without access_token. AuthState not updated.")
            }

            return payload
        } finally {
            client.close()
        }
    }
}
