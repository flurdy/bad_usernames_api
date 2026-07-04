# Docker

The production image is built with `sbt-native-packager` and a multi-stage Dockerfile.

CI publishes `main` builds to `quay.io/flurdy/bad-usernames-api` as `latest` and semver-style `0.1.<circle-build-number>` tags.

## Build

```bash
make docker-build
```

Or call Docker directly:

```bash
docker build -t bad-usernames-api:local .
```

Pull the public image from Quay.io:

```bash
docker pull quay.io/flurdy/bad-usernames-api:latest
```

## Run with Docker

Use the sample dataset for local evaluation:

```bash
make docker-run
```

Or call Docker directly:

```bash
docker run --rm \
  -p 8080:8080 \
  -v "$PWD/dev/sample-bad-usernames.json:/data/bad-usernames.json:ro" \
  bad-usernames-api:local
```

Using the public Quay image:

```bash
docker run --rm \
  -p 8080:8080 \
  -v /path/to/bad-usernames.json:/data/bad-usernames.json:ro \
  quay.io/flurdy/bad-usernames-api:latest
```

For a real mounted dataset with the local image:

```bash
docker run --rm \
  -p 8080:8080 \
  -v /path/to/bad-usernames.json:/data/bad-usernames.json:ro \
  bad-usernames-api:local
```

Example using a different mounted path:

```bash
docker run --rm \
  -p 8080:8080 \
  -v /path/to/bad-usernames.json:/datasets/bad-usernames.json:ro \
  -e BAD_USERNAMES_DATASET_PATH=/datasets/bad-usernames.json \
  bad-usernames-api:local
```

## Run with Docker Compose

The repository includes `compose.yaml`.

Using the sample dataset:

```bash
docker compose up --build
```

Using a real dataset file:

```bash
BAD_USERNAMES_DATASET_FILE=/path/to/bad-usernames.json docker compose up --build
```

Optional compose overrides:

```bash
BAD_USERNAMES_HOST_PORT=18080 \
BAD_USERNAMES_BATCH_LIMIT=500 \
BAD_USERNAMES_DATASET_FILE=/path/to/bad-usernames.json \
docker compose up --build
```

## Dataset mount requirement

The runtime image does **not** include a dataset. By default it reads `/data/bad-usernames.json`; if that file is not mounted, startup fails instead of serving from a hidden fallback.

## Configuration

| Environment variable | Default in image | Description |
| --- | --- | --- |
| `BAD_USERNAMES_HOST` | `0.0.0.0` | HTTP bind host |
| `BAD_USERNAMES_PORT` | `8080` | HTTP bind port |
| `BAD_USERNAMES_DATASET_PATH` | `/data/bad-usernames.json` | Dataset JSON path inside the container |
| `BAD_USERNAMES_BATCH_LIMIT` | `1000` | Max usernames per batch request |

For broader self-hosting notes, see `docs/self-hosting.md`.

For local development without Docker, use:

```bash
make run
```
