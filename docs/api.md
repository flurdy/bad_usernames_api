# API Plan

Base path: `/api/v1`.

## Principles

- JSON only.
- Exact matches decide the `bad` field.
- Substring matches are advisory only and do not set `bad` by themselves.
- Normalize input by trimming whitespace and lowercasing with `Locale.ROOT`.
- Return both original and normalized values so behavior is transparent.
- Make responses deterministic and cache-friendly.

## `GET /`

Returns a small HTML landing page with links to the main API endpoints and documentation.

## `GET /health`

Returns a small JSON health response for Kubernetes probes and uptime checks.

```json
{
  "status": "ok"
}
```

## `GET /api/v1/check?username={username}`

Checks a single username passed as a query parameter. This avoids treating the username as a path segment, because usernames can contain Unicode, spaces, slashes, or other characters that are awkward in paths.

Example:

```http
GET /api/v1/check?username=admin
```

```json
{
  "username": "admin",
  "normalized": "admin",
  "bad": true,
  "matched": "admin",
  "matches": [
    { "matchType": "exact", "term": "admin" }
  ]
}
```

If the username is not blocked:

```json
{
  "username": "ivar",
  "normalized": "ivar",
  "bad": false,
  "matched": null,
  "matches": []
}
```

If the username contains a blocked term but is not an exact match, `bad` remains `false` and the advisory substring match is returned in `matches`:

```json
{
  "username": "admin123",
  "normalized": "admin123",
  "bad": false,
  "matched": null,
  "matches": [
    { "matchType": "substring", "term": "admin" }
  ]
}
```

Substring matches can be false-positive-prone, for example place names containing blocked terms, so clients should treat `matchType: "substring"` as explainable advisory data rather than an exact block decision.

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
    {
      "username": "admin",
      "normalized": "admin",
      "bad": true,
      "matched": "admin",
      "matches": [{ "matchType": "exact", "term": "admin" }]
    },
    {
      "username": "ivar",
      "normalized": "ivar",
      "bad": false,
      "matched": null,
      "matches": []
    },
    {
      "username": "support",
      "normalized": "support",
      "bad": true,
      "matched": "support",
      "matches": [{ "matchType": "exact", "term": "support" }]
    }
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

- `400` for missing or empty username.
- `400` for malformed JSON.
- `400` if batch size exceeds the configured limit.
- `404` for unknown routes.
