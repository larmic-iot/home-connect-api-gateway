# HTTP Client examples

This folder contains JetBrains HTTP Client requests to interact with the Home Connect API.

Quick start
- Create a private environment file next to this README
  - Copy http-client.private.env.json.example to http-client.private.env.json
  - Put your Home Connect client ID into the file under the "default" environment
- Open example-requests.http in your IDE and run the requests in order
  1) Step 1: Request device code
  2) Step 2: Exchange device code for tokens (may need to retry until authorized)
  3) Step 3: List home appliances
- Optional: To refresh the token later, set @savedRefreshToken in the file or store it in your private env and run the refresh request

Notes
- The variable {{clientId}} is resolved from http-client.private.env.json and should not be committed.
- The http-client.private.env.json file is intentionally not provided; use the .example as a template.
