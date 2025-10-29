package de.larmic.starter.routes

import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

// Embedded OpenAPI YAML. No external file is required at runtime.
private val OPENAPI_YAML: String = """
openapi: 3.0.3
info:
  title: Home Connect API Gateway
  description: |
    Ein leichtgewichtiges Gateway, das den OAuth Device Code Flow erleichtert und
    Anfragen an die Home Connect API proxyt. Diese Spezifikation beschreibt die
    Gateway-Routen, nicht die originale Home Connect API selbst.

    Hinweis zur Home Connect API Spezifikation: https://apiclient.home-connect.com/
  version: 1.0.0
servers:
  - url: http://localhost:8080
    description: Lokaler Entwicklungsserver
externalDocs:
  description: Home Connect API Spezifikation
  url: https://apiclient.home-connect.com/
paths:
  /openapi.yaml:
    get:
      summary: Liefert diese OpenAPI YAML
      operationId: getOpenApiYaml
      responses:
        '200':
          description: OpenAPI YAML
          content:
            application/yaml:
              schema:
                type: string
              example: |
                # OpenAPI YAML Inhalt
  /health:
    get:
      summary: Liveness Check
      operationId: getHealth
      responses:
        '200':
          description: Server lebt
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/HealthResponse'
              examples:
                default:
                  value:
                    status: UP
  /health/live:
    get:
      summary: Liveness Check (Alias)
      operationId: getHealthLive
      responses:
        '200':
          description: Server lebt
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/HealthResponse'
              examples:
                default:
                  value:
                    status: UP
  /health/ready:
    get:
      summary: Readiness Check inkl. OAuth Status
      operationId: getHealthReady
      responses:
        '200':
          description: Bereit
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/ReadinessResponse'
              examples:
                ready:
                  summary: Tokens vorhanden
                  value:
                    status: READY
                    checks:
                      authorization: UP
        '503':
          description: Nicht bereit
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/ReadinessResponse'
              examples:
                starting:
                  value:
                    status: NOT_READY
                    checks:
                      authorization: STARTING_DEVICE_AUTHORIZATION
                waiting:
                  value:
                    status: NOT_READY
                    checks:
                      authorization: WAITING_FOR_MANUAL_TASKS
                expired:
                  value:
                    status: NOT_READY
                    checks:
                      authorization: TOKEN_EXPIRED
  /oauth/init:
    get:
      summary: Startet den OAuth Device Authorization Flow (Schritt 1)
      operationId: getOauthInit
      responses:
        '200':
          description: Device Authorization gestartet
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/DeviceAuthorizationResponse'
              examples:
                default:
                  value:
                    device_code: 8adf1c...
                    user_code: ABCD-EFGH
                    verification_uri: https://my.home-connect.com/security/user/oauth/device
                    verification_uri_complete: https://my.home-connect.com/security/user/oauth/device?user_code=ABCD-EFGH
                    expires_in: 1800
                    interval: 5
        '500':
          description: Fehler beim Starten
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/ErrorResponse'
  /oauth/token:
    get:
      summary: Tauscht device_code gegen Tokens (Schritt 2)
      operationId: getOauthToken
      responses:
        '200':
          description: Erfolg (Tokens geliefert)
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/OAuthTokenResponse'
              examples:
                success:
                  value:
                    access_token: eyJhbGciOi...
                    refresh_token: r1-xyz...
                    token_type: bearer
                    expires_in: 3600
                    scope: IdentifyAppliance Monitor Settings Control
        '202':
          description: Autorisierung ausstehend
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/OAuthTokenResponse'
              examples:
                pending:
                  value:
                    error: authorization_pending
                    error_description: User has not completed the verification yet
        '400':
          description: Fehlerhafte Anfrage oder Fehlerantwort
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/OAuthTokenResponse'
              examples:
                error:
                  value:
                    error: invalid_grant
                    error_description: device_code expired
        '500':
          description: Serverfehler
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/ErrorResponse'
  /oauth/token/refresh:
    get:
      summary: Erneuert das Access Token mit refresh_token
      operationId: getOauthTokenRefresh
      responses:
        '200':
          description: Erfolg (neues Access Token)
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/OAuthTokenResponse'
        '400':
          description: Kein refresh_token oder Fehler vom OAuth-Server
          content:
            application/json:
              schema:
                oneOf:
                  - ${'$'}ref: '#/components/schemas/ErrorResponse'
                  - ${'$'}ref: '#/components/schemas/OAuthTokenResponse'
              examples:
                missingToken:
                  value:
                    status: ERROR
                    message: No refresh_token available. Obtain tokens first via device authorization.
                    hint: Run /oauth/init then /oauth/token until success
        '500':
          description: Serverfehler
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/ErrorResponse'
  /proxy/{path}:
    get:
      summary: Proxy GET zur Home Connect API
      operationId: proxyGet
      parameters:
        - name: path
          in: path
          required: true
          description: Pfad relativ zu /api der Home Connect API (z. B. appliances)
          schema:
            type: string
      responses:
        '200':
          description: Antwort der Home Connect API
          content:
            application/json:
              schema:
                type: object
              examples:
                note:
                  summary: Hinweis
                  value:
                    info: 'Antwortstruktur variiert je nach Home Connect Endpoint.'
        '503':
          description: Gateway nicht bereit
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/ProxyErrorResponse'
        '502':
          description: Fehler beim Proxy
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/ProxyErrorResponse'
    put:
      summary: Proxy PUT zur Home Connect API
      operationId: proxyPut
      parameters:
        - ${'$'}ref: '#/components/parameters/ProxyPath'
      requestBody:
        required: false
        content:
          application/json:
            schema:
              type: object
      responses:
        '200':
          description: Antwort der Home Connect API
        '503':
          description: Gateway nicht bereit
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/ProxyErrorResponse'
        '502':
          description: Fehler beim Proxy
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/ProxyErrorResponse'
    post:
      summary: Proxy POST zur Home Connect API
      operationId: proxyPost
      parameters:
        - ${'$'}ref: '#/components/parameters/ProxyPath'
      requestBody:
        required: false
        content:
          application/json:
            schema:
              type: object
      responses:
        '200':
          description: Antwort der Home Connect API
        '503':
          description: Gateway nicht bereit
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/ProxyErrorResponse'
        '502':
          description: Fehler beim Proxy
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/ProxyErrorResponse'
    delete:
      summary: Proxy DELETE zur Home Connect API
      operationId: proxyDelete
      parameters:
        - ${'$'}ref: '#/components/parameters/ProxyPath'
      responses:
        '200':
          description: Antwort der Home Connect API
        '503':
          description: Gateway nicht bereit
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/ProxyErrorResponse'
        '502':
          description: Fehler beim Proxy
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/ProxyErrorResponse'
components:
  parameters:
    ProxyPath:
      name: path
      in: path
      required: true
      description: Pfad relativ zu /api der Home Connect API (z. B. appliances)
      schema:
        type: string
  schemas:
    HealthResponse:
      type: object
      properties:
        status:
          type: string
          enum: [UP]
      required: [status]
    ReadinessResponse:
      type: object
      properties:
        status:
          type: string
          enum: [READY, NOT_READY]
        checks:
          type: object
          properties:
            authorization:
              type: string
              enum:
                - STARTING_DEVICE_AUTHORIZATION
                - WAITING_FOR_MANUAL_TASKS
                - UP
                - TOKEN_EXPIRED
          required: [authorization]
      required: [status, checks]
    ErrorResponse:
      type: object
      properties:
        status:
          type: string
        message:
          type: string
        hint:
          type: string
      required: [status, message]
    ProxyErrorResponse:
      allOf:
        - ${'$'}ref: '#/components/schemas/ErrorResponse'
        - type: object
          properties:
            homeConnectApiSpec:
              type: string
              example: https://apiclient.home-connect.com/
    DeviceAuthorizationResponse:
      type: object
      properties:
        device_code:
          type: string
        user_code:
          type: string
        verification_uri:
          type: string
        verification_uri_complete:
          type: string
          nullable: true
        expires_in:
          type: integer
          nullable: true
        interval:
          type: integer
          nullable: true
      required: [device_code, user_code, verification_uri]
    OAuthTokenResponse:
      type: object
      properties:
        access_token:
          type: string
          nullable: true
        refresh_token:
          type: string
          nullable: true
        token_type:
          type: string
          nullable: true
        expires_in:
          type: integer
          nullable: true
        scope:
          type: string
          nullable: true
        error:
          type: string
          nullable: true
        error_description:
          type: string
          nullable: true
        error_uri:
          type: string
          nullable: true
"""

fun Route.openApiRoute() {
    get("/openapi.yaml") {
        call.respondText(OPENAPI_YAML, contentType = ContentType.parse("application/yaml"))
    }
}
