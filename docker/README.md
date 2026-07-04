# Docker

The production image is built with `sbt-native-packager` and a multi-stage Dockerfile.

Build locally:

```bash
make docker-build
```

Or call Docker directly:

```bash
docker build -t bad-usernames-api:local .
```

Run with a mounted dataset:

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

The runtime image does **not** include a dataset. By default it reads `/data/bad-usernames.json`; if that file is not mounted, startup fails instead of serving from a hidden fallback.

Configuration:

| Environment variable | Default in image | Description |
| --- | --- | --- |
| `BAD_USERNAMES_HOST` | `0.0.0.0` | HTTP bind host |
| `BAD_USERNAMES_PORT` | `8080` | HTTP bind port |
| `BAD_USERNAMES_DATASET_PATH` | `/data/bad-usernames.json` | Dataset JSON path inside the container |
| `BAD_USERNAMES_BATCH_LIMIT` | `1000` | Max usernames per batch request |

Example using a different mounted path:

```bash
docker run --rm \
  -p 8080:8080 \
  -v /path/to/bad-usernames.json:/datasets/bad-usernames.json:ro \
  -e BAD_USERNAMES_DATASET_PATH=/datasets/bad-usernames.json \
  bad-usernames-api:local
```

For local development without Docker, use the sample dataset explicitly:

```bash
BAD_USERNAMES_DATASET_PATH=dev/sample-bad-usernames.json sbt run
```
