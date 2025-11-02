# Home Connect API Gateway

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Gradle build](https://github.com/larmic-iot/home-connect-api-gateway/actions/workflows/gradle-build.yml/badge.svg)](https://github.com/larmic-iot/home-connect-api-gateway/actions/workflows/gradle-build.yml)
[![Docker build and push](https://github.com/larmic-iot/home-connect-api-gateway/actions/workflows/docker-build-push.yml/badge.svg)](https://github.com/larmic-iot/home-connect-api-gateway/actions/workflows/docker-build-push.yml)
[![Docker hub image](https://img.shields.io/docker/image-size/larmic/home-connect-api-gateway?label=dockerhub)](https://hub.docker.com/repository/docker/larmic/home-connect-api-gateway)
![Docker Image Version (latest by date)](https://img.shields.io/docker/v/larmic/home-connect-api-gateway)

Home Connect API Gateway - Docker container providing a REST API adapter for BSH Home Connect devices (Bosch, Siemens, Neff, Gaggenau)

---

Quick start (Docker)
- Prerequisite: Home Connect client ID.
- Start container:
  - Linux/macOS:
    - `docker run -p 8080:8080 -e HOME_CONNECT_CLIENT_ID=YOUR_CLIENT_ID --name home-connect-gw larmic/home-connect-api-gateway:latest`
  - Windows PowerShell:
    - `docker run -p 8080:8080 -e HOME_CONNECT_CLIENT_ID=$Env:HOME_CONNECT_CLIENT_ID --name home-connect-gw larmic/home-connect-api-gateway:latest`
- Open OpenAPI UI: http://localhost:8080 (serves docs).
- Automatic OAuth device flow on startup:
  - On start, the app calls the Home Connect Device Authorization endpoint and prints the verification URL and user code.
  - After an initial delay it will poll for the access token periodically until successful.
  - You can still trigger the steps manually via the HTTP endpoints below if desired.
- Manual OAuth endpoints (optional):
  1. `POST http://localhost:8080/oauth/init` → get `user_code` and `verification_uri`.
  2. Open the verification URL and enter the code.
  3. Poll tokens: `POST http://localhost:8080/oauth/token` until success (no `authorization_pending`).
  4. Optional refresh: `POST http://localhost:8080/oauth/token/refresh`.
- Check readiness: `GET http://localhost:8080/health/ready` (should report READY/UP).
- Use proxy:
  - Example: `GET http://localhost:8080/proxy/api/homeappliances`
  - Send your body with POST/PUT via the `/proxy/...` endpoint.

Configuration
- Required environment variables:
  - `HOME_CONNECT_CLIENT_ID` — your Home Connect client ID (mandatory)
- Optional environment variables (seconds):
  - `DEVICE_FLOW_INITIAL_POLL_DELAY_SECONDS` — delay before the first token poll; default 10s if not set. Docker image default: 10s.
  - `DEVICE_FLOW_POLL_INTERVAL_SECONDS` — interval between subsequent polls; default 5s if not set. Docker image default: 5s.

Notes
- The service requires `HOME_CONNECT_CLIENT_ID` at startup.
- Example HTTP requests are in `misc/http/` (see `home-connect-api.http` and `gateway-local.http`).

