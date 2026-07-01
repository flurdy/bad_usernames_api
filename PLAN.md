# Bad Usernames API Plan

## Project Shape

Bad Usernames API is a small API-first service for checking whether a username should be blocked, reserved, or treated as unsafe.

The service should wrap the existing `flurdy/bad_usernames` dataset rather than replacing it.

- Dataset repo: `bad_usernames`
- API repo: `bad_usernames_api`
- Public service name: Bad Usernames
- Likely hostname: `badusernames.flurdy.io`
- Initial posture: free, open source, best-effort hosted API

## Licensing

- Dataset: CC0-1.0
- API service code: Apache-2.0

The dataset is intentionally very open because it is just a reusable word list. The API code has more implementation craft and should use Apache-2.0.

## Scope

Start narrow:

- Exact username checks against the dataset
- English-first default list
- Selected multilingual reserved/system words where they are high-value
- JSON responses
- Self-hostable service
- Minimal root/docs response so the hosted URL is not blank

Defer:

- User accounts
- API keys
- Paid tiers
- Dashboard
- SLA
- Custom tenant lists
- Fuzzy matching
- Unicode spoof detection
- Web UI beyond basic docs

## API Sketch

Single username check:

```http
GET /api/v1/check/{username}
```

Example response:

```json
{
  "username": "admin",
  "bad": true,
  "matched": "admin"
}
```

Batch check:

```http
POST /api/v1/check
Content-Type: application/json

{
  "usernames": ["admin", "ivar", "support"]
}
```

Metadata:

```http
GET /api/v1/meta
```

Useful metadata:

- dataset version or commit
- loaded language lists
- word count
- service version

## Implementation Notes

Likely implementation:

- Scala
- http4s
- JSON parser/encoder from the chosen http4s stack
- Load dataset into memory at startup
- Normalize checks with lowercase and trim first
- Keep behavior deterministic and easy to cache

Avoid over-designing the first version. The useful first milestone is a fast hosted/readme-documented API over the existing JSON data.

## README-First Launch

Start with GitHub documentation rather than a web UI.

The README should explain:

- What problem the service solves
- Hosted API status: free and best-effort
- Self-hosting instructions
- Example requests/responses
- Link to the CC0 dataset repo
- License: Apache-2.0

Commercial options can remain a future possibility without shaping the first version.
