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
  - Open gateway-local.http and execute the POST /oauth/* steps, then try proxy requests

Notes
- The variable {{clientId}} is resolved from http-client.private.env.json and should not be committed.
- The http-client.private.env.json file is intentionally not provided; use the .example as a template.
