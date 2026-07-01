# Operations Plan

## Initial service posture

The hosted service, if launched, should be described as free and best-effort. No SLA should be implied.

## Configuration

Initial environment variables:

- `BAD_USERNAMES_HOST`
- `BAD_USERNAMES_PORT`
- `BAD_USERNAMES_DATASET_PATH`
- `BAD_USERNAMES_BATCH_LIMIT`

## Health and observability

Initial endpoints:

- `/` basic human-readable service info.
- `/api/v1/meta` machine-readable service/dataset metadata.

Possible later endpoints:

- `/health`
- `/ready`
- metrics endpoint

## Caching

Exact username checks are deterministic once a dataset is loaded. Later versions can add cache headers, especially for `GET /api/v1/check/{username}` and `/api/v1/meta`.

## Abuse controls

Do not add accounts or API keys initially. Start with a batch limit and reverse-proxy-level rate limiting if needed.
