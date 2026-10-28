# Operations Plan

## Initial service posture

The hosted service, if launched, should be described as free and best-effort. No SLA should be implied.

## Configuration

Initial environment variables:

- `BAD_USERNAMES_HOST`
- `BAD_USERNAMES_PORT`
- `BAD_USERNAMES_DATASET_PATH`
- `BAD_USERNAMES_BATCH_LIMIT`

## Container runtime

The production Docker image does not include a dataset. Operators must mount a dataset file and point `BAD_USERNAMES_DATASET_PATH` at it. The default container path is `/data/bad-usernames.json`; if it is not present, startup should fail rather than serving from an implicit fallback.

CI publishes `main` builds to `quay.io/flurdy/badusernames.flurdy.io` with `latest` and semver-style `1.0.<circle-build-number>` tags. CircleCI needs Quay credentials in `DOCKER_LOGIN` and `DOCKER_PASSWORD`.

Paperboy deploys this service automatically through Flux. The `infrastructure/badusernames-source` image repository scans Quay every 15 minutes; `apps/badusernames-policy` selects the highest `1.0.x` tag. The shared image-update automation runs every 5 minutes and updates the marked image in the GitOps repository, which Flux then reconciles. Publishing an eligible tag can therefore trigger production deployment without a separate manual rollout. Disclose that consequence when seeking publication approval. Manual reconciliation, restarts, or deployment changes still need explicit approval.

`make docker-build` pulls the current base images, and the Dockerfile upgrades installed Alpine packages. For a security rebuild, use a fresh build without cached layers:

```bash
docker build --pull --no-cache -t bad-usernames-api:security .
docker run --rm --entrypoint java bad-usernames-api:security -version
docker run --rm --entrypoint sh bad-usernames-api:security -c 'apk list --installed --no-network; ls /opt/app/lib/org.http4s.*'
```

For the October 2026 eMudhra incident, [Adoptium identifies Temurin 17.0.21+9 as patched](https://adoptium.net/news/2026/10/temurin-october-2026-emudhra). Container publication can lag binary publication: `--pull` alone does not prove the runtime is patched. Before rollout, verify the built image's Java version and trust store, resolved Ember version (0.23.35 or newer), and Alpine package versions. Temurin's mitigation removes `emSign ECC Root CA - G3` and `AAA Certificate Services`; `emSign Root CA - G1` remains trusted under Mozilla's policy. Record the exact published image tag and digest, then verify that automatic deployment reaches that digest and normal `/health`, `/api/v1/meta`, and `/api/v1/check?username=admin` responses still work. Never send malformed-request probes to production.

See `docker/README.md` and `docs/self-hosting.md` for local build, Docker run, and Compose examples.

## Health and observability

Initial endpoints:

- `/` basic human-readable service info.
- `/api/v1/meta` machine-readable service/dataset metadata.

Possible later endpoints:

- `/health`
- `/ready`
- metrics endpoint

## Caching

Exact username checks are deterministic once a dataset is loaded. Later versions can add cache headers, especially for `GET /api/v1/check?username=...` and `/api/v1/meta`.

## Abuse controls

Do not add accounts or API keys initially. Start with a batch limit and reverse-proxy-level rate limiting if needed.
