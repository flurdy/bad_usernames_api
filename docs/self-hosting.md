# Self-hosting

Bad Usernames API can run as a small containerized service. The container image does not include a dataset; mount one at runtime and point `BAD_USERNAMES_DATASET_PATH` at the mounted file.

## Requirements

- Docker, or Docker Compose
- A dataset JSON file using the `usernames` shape described in `docs/dataset.md`

For local evaluation you can use `dev/sample-bad-usernames.json`. For real-dataset testing, use `data/bad-usernames.json` or mount your own upstream snapshot.

## Docker run

Pull the public image:

```bash
docker pull quay.io/flurdy/badusernames.flurdy.io:latest
```

Or build the local image:

```bash
make docker-build
```

Run it with a mounted dataset:

```bash
docker run --rm \
  -p 8080:8080 \
  -v /path/to/bad-usernames.json:/data/bad-usernames.json:ro \
  -e BAD_USERNAMES_DATASET_VERSION=flurdy/bad_usernames@<commit> \
  quay.io/flurdy/badusernames.flurdy.io:latest
```

If using the locally built image, replace the image name with `bad-usernames-api:local`.

If you mount the dataset somewhere else, set `BAD_USERNAMES_DATASET_PATH` to match:

```bash
docker run --rm \
  -p 8080:8080 \
  -v /path/to/bad-usernames.json:/datasets/bad-usernames.json:ro \
  -e BAD_USERNAMES_DATASET_PATH=/datasets/bad-usernames.json \
  bad-usernames-api:local
```

## Docker Compose

The repository includes `compose.yaml` for local/self-hosting examples.

Using the sample dataset:

```bash
docker compose up --build
```

Using a real dataset file:

```bash
BAD_USERNAMES_DATASET_FILE=/path/to/bad-usernames.json docker compose up --build
```

Optional overrides:

```bash
BAD_USERNAMES_HOST_PORT=18080 \
BAD_USERNAMES_BATCH_LIMIT=500 \
BAD_USERNAMES_DATASET_FILE=/path/to/bad-usernames.json \
docker compose up --build
```

## Configuration

| Environment variable | Container default | Description |
| --- | --- | --- |
| `BAD_USERNAMES_HOST` | `0.0.0.0` | HTTP bind host inside the container |
| `BAD_USERNAMES_PORT` | `8080` | HTTP bind port inside the container |
| `BAD_USERNAMES_DATASET_PATH` | `/data/bad-usernames.json` | Dataset JSON path inside the container |
| `BAD_USERNAMES_DATASET_VERSION` | unset | Optional dataset version/commit exposed in `/api/v1/meta` |
| `BAD_USERNAMES_BATCH_LIMIT` | `1000` | Max usernames per batch request |

Compose-only host-side variables:

| Environment variable | Default | Description |
| --- | --- | --- |
| `BAD_USERNAMES_HOST_PORT` | `8080` | Host port mapped to container port 8080 |
| `BAD_USERNAMES_DATASET_FILE` | `./dev/sample-bad-usernames.json` | Host dataset file mounted read-only into the container |

## Startup behavior

The service loads the dataset once at startup. If the mounted dataset file is missing, unreadable, or invalid, startup fails. This is intentional: self-hosted deployments should fail fast rather than serve from an implicit or stale fallback dataset.

`/api/v1/meta` reports `datasetVersion`. For upstream mounted files, set `BAD_USERNAMES_DATASET_VERSION` or mount an adjacent version file (`bad-usernames.json.version` or `bad-usernames.version`) containing the upstream commit.

## Smoke test

Once running:

```bash
curl http://localhost:8080/api/v1/meta
curl http://localhost:8080/api/v1/check/admin
```
