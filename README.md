# Bad Usernames API

Bad Usernames API is a small API-first service for checking whether a username should be blocked, reserved, or treated as unsafe.

It is intended to wrap the existing [`flurdy/bad_usernames`](https://github.com/flurdy/bad_usernames) dataset rather than replace it.

## Status

Early scaffold. The intended public hosted service is free, open source, and best-effort.

## Planned API

### Single username check

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

### Batch username check

```http
POST /api/v1/check
Content-Type: application/json

{
  "usernames": ["admin", "ivar", "support"]
}
```

### Metadata

```http
GET /api/v1/meta
```

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
curl http://localhost:8080/api/v1/check/admin
curl http://localhost:8080/api/v1/meta
```

Useful local targets:

```bash
make ci
make docker-build
make docker-run
```

For container and self-hosting examples, see `docker/README.md` and `docs/self-hosting.md`.

## Configuration

| Environment variable | Default | Description |
| --- | --- | --- |
| `BAD_USERNAMES_HOST` | `0.0.0.0` | HTTP bind host |
| `BAD_USERNAMES_PORT` | `8080` | HTTP bind port |
| `BAD_USERNAMES_DATASET_PATH` | `dev/sample-bad-usernames.json` | Dataset JSON path |
| `BAD_USERNAMES_BATCH_LIMIT` | `1000` | Max usernames per batch request |

## Licenses

- API service code: Apache-2.0
- Upstream dataset: CC0-1.0
