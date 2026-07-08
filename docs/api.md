# API Plan

Base path: `/api/v1`.

## Principles

- JSON only.
- Exact-match checks only for the first version.
- Normalize input by trimming whitespace and lowercasing with `Locale.ROOT`.
- Return both original and normalized values so behavior is transparent.
- Make responses deterministic and cache-friendly.

## `GET /health`

Returns a small JSON health response for Kubernetes probes and uptime checks.

```json
{
  "status": "ok"
}
```

## `GET /api/v1/check/{username}`

Checks a single username.

Example:

```http
GET /api/v1/check/admin
```

```json
{
  "username": "admin",
  "normalized": "admin",
  "bad": true,
  "matched": "admin"
}
```

If the username is not blocked:

```json
{
  "username": "ivar",
  "normalized": "ivar",
  "bad": false,
  "matched": null
}
```

## `POST /api/v1/check`

Checks multiple usernames.

```json
{
  "usernames": ["admin", "ivar", "support"]
}
```

Response:

```json
{
  "results": [
    { "username": "admin", "normalized": "admin", "bad": true, "matched": "admin" },
    { "username": "ivar", "normalized": "ivar", "bad": false, "matched": null },
    { "username": "support", "normalized": "support", "bad": true, "matched": "support" }
  ]
}
```

## `GET /api/v1/meta`

Returns service and dataset metadata.

Response fields:

- `service`: service name
- `serviceVersion`: API service version
- `datasetVersion`: dataset version or upstream commit when configured, otherwise `unknown`
- `languages`: loaded dataset languages, when known
- `wordCount`: number of unique normalized blocked usernames
- `normalization`: normalization strategy
- `batchLimit`: max usernames per batch request

## Errors

Initial error shape:

```json
{
  "error": "message"
}
```

Important initial cases:

- `400` for empty username.
- `400` for malformed JSON.
- `400` if batch size exceeds the configured limit.
- `404` for unknown routes.
