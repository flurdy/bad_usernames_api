# Bad Usernames API

Bad Usernames API is a small API-first service for checking whether a username should be blocked, reserved, or treated as unsafe.

It is intended to wrap the existing [`flurdy/bad_usernames`](https://github.com/flurdy/bad_usernames) dataset rather than replace it.

## Status

Early scaffold. The intended public hosted service is free, open source, and best-effort.

## API

Base path: `/api/v1`.

### Service root

```http
GET /
```

Returns a small HTML landing page with documentation and endpoint links.

### Health

```http
GET /health
```

```json
{
  "status": "ok"
}
```

### Single username check

```http
GET /api/v1/check?username=admin
```

Exact matches set `bad` to `true` and are reported both in `matched` and `matches`:

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

Allowed usernames return `bad: false` with no matches:

```json
{
  "username": "ivar",
  "normalized": "ivar",
  "bad": false,
  "matched": null,
  "matches": []
}
```

Substring matches are advisory only. They do not set `bad` by themselves, because they can produce false positives:

```http
GET /api/v1/check?username=admin123
```

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

### Batch username check

```http
POST /api/v1/check
Content-Type: application/json

{
  "usernames": ["admin", "ivar", "support-team"]
}
```

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
      "username": "support-team",
      "normalized": "support-team",
      "bad": false,
      "matched": null,
      "matches": [{ "matchType": "substring", "term": "support" }]
    }
  ]
}
```

### Metadata

```http
GET /api/v1/meta
```

Returns service version, dataset version, loaded languages, word count, normalization strategy, and batch limit.

## Development

Requirements:

- Java 11 or newer
- sbt

Run locally:

```bash
make run
```

Or call sbt directly:

```bash
BAD_USERNAMES_DATASET_PATH=dev/sample-bad-usernames.json sbt run
```

Then open:

```bash
curl http://localhost:8080/health
curl 'http://localhost:8080/api/v1/check?username=admin'
curl http://localhost:8080/api/v1/meta
```

Useful local targets:

```bash
make ci
make ci-status
make docker-build
make docker-run
```

For real-dataset testing, use `BAD_USERNAMES_DATASET_PATH=data/bad-usernames.json make run`; `/api/v1/meta` will report the vendored upstream commit from `data/bad-usernames.version`.

For container and self-hosting examples, see `docker/README.md` and `docs/self-hosting.md`. `main` builds publish a public image at `quay.io/flurdy/badusernames.flurdy.io`.

## Configuration

| Environment variable | Default | Description |
| --- | --- | --- |
| `BAD_USERNAMES_HOST` | `0.0.0.0` | HTTP bind host |
| `BAD_USERNAMES_PORT` | `8080` | HTTP bind port |
| `BAD_USERNAMES_DATASET_PATH` | `dev/sample-bad-usernames.json` | Dataset JSON path |
| `BAD_USERNAMES_DATASET_VERSION` | unset | Optional dataset version/commit exposed in `/api/v1/meta` |
| `BAD_USERNAMES_BATCH_LIMIT` | `1000` | Max usernames per batch request |

## Licenses

- API service code: Apache-2.0
- Upstream dataset: CC0-1.0
