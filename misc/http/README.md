# HTTP Client examples

This folder contains JetBrains HTTP Client requests for both the original Home Connect API and this gateway.

Files
- home-connect-api.http — requests to the official Home Connect API (device flow, token, sample GET)
- gateway-local.http — requests to this gateway (POST /oauth/* and proxy examples)

Quick start
- Create a private environment file next to this README
  - Copy http-client.private.env.json.example to http-client.private.env.json
  - Put your Home Connect client ID into the file under the "default" environment
- For direct API testing:
  - Open home-connect-api.http and run the requests in order
- For gateway testing (after starting Docker container):
  - The gateway now starts the OAuth device flow automatically on startup and will poll for a token.
  - You can still use the manual endpoints in gateway-local.http if you want to control the flow yourself.
  - Open gateway-local.http and execute the POST /oauth/* steps, then try proxy requests

Notes
- Polling can be configured via env vars (seconds): DEVICE_FLOW_INITIAL_POLL_DELAY_SECONDS and DEVICE_FLOW_POLL_INTERVAL_SECONDS.
- The variable {{clientId}} is resolved from http-client.private.env.json and should not be committed.
- The http-client.private.env.json file is intentionally not provided; use the .example as a template.
