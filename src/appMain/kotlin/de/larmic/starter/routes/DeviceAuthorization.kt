package de.larmic.starter.routes

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
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import platform.posix.getenv as cGetEnv

// Use Curl engine universally for Kotlin/Native to ensure platform-agnostic behavior (Docker-friendly).
private fun httpClient(): HttpClient = HttpClient(Curl) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
}

@Serializable
private data class DeviceAuthorizationResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_uri") val verificationUri: String,
    @SerialName("verification_uri_complete") val verificationUriComplete: String? = null,
    @SerialName("expires_in") val expiresIn: Int? = null,
    val interval: Int? = null
)

fun Route.deviceAuthorizationRoute() {
    get("/auth/device/start") {
        val clientId = getenv("HOME_CONNECT_CLIENT_ID")
        if (clientId.isNullOrBlank()) {
            call.respond(
                mapOf(
                    "status" to "ERROR",
                    "message" to "Environment variable HOME_CONNECT_CLIENT_ID is not set.",
                    "hint" to "export HOME_CONNECT_CLIENT_ID=YOUR_CLIENT_ID and retry"
                )
            )
            return@get
        }

        val scope = "IdentifyAppliance Monitor Settings Control"

        val bodyParams = Parameters.build {
            append("client_id", clientId)
            append("scope", scope)
        }.formUrlEncode()

        val client = httpClient()
        try {
            val response = client.post("https://api.home-connect.com/security/oauth/device_authorization") {
                contentType(ContentType.Application.FormUrlEncoded)
                headers { append("Accept", "application/json") }
                setBody(bodyParams)
            }

            val payload: DeviceAuthorizationResponse = response.body()

            // Log formatted output similar to example-requests.http
            println("═══════════════════════════════════════════")
            println("Open this URL:")
            println(payload.verificationUri)
            println("")
            println("Enter this code:")
            println(payload.userCode)
            println("═══════════════════════════════════════════")
            if (payload.expiresIn != null) println("You have ${payload.expiresIn} seconds")
            val wait = payload.interval ?: 5
            println("")
            println("After entering the code, execute Step 2 (wait ${wait} seconds)")

            // Try to open the browser with verification_uri_complete (fallback to verification_uri)
            val targetUrl = payload.verificationUriComplete ?: payload.verificationUri
            val opened = openInBrowser(targetUrl)
            println("Open browser attempt: ${if (opened) "OK" else "FAILED"} -> $targetUrl")

            call.respond(payload)
        } catch (t: Throwable) {
            println("Device authorization failed: ${t.message}")
            call.respond(mapOf("status" to "ERROR", "message" to (t.message ?: "Unknown error")))
        } finally {
            client.close()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun getenv(name: String): String? = cGetEnv(name)?.toKString()

// Attempt to open URL using common commands available on macOS, Linux, and Windows.
private fun openInBrowser(url: String): Boolean {
    val commands = listOf(
        // macOS
        listOf("open", url),
        // Linux (most desktops)
        listOf("xdg-open", url),
        // Windows (via cmd start)
        listOf("cmd", "/c", "start", url),
        // Windows older variants
        listOf("rundll32", "url.dll,FileProtocolHandler", url)
    )

    for (cmd in commands) {
        val ok = tryRun(cmd)
        if (ok) return true
    }
    return false
}

private fun tryRun(cmd: List<String>): Boolean = try {
    // Kotlin/Native doesn't have ProcessBuilder; use posix system by joining the command.
    // Note: Quotes may not be perfect across shells, but good enough for typical URLs.
    val command = cmd.joinToString(" ") { it }
    val code = platform.posix.system(command)
    code == 0
} catch (_: Throwable) { false }
