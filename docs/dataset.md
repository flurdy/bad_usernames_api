# Dataset Plan

The API wraps the existing [`flurdy/bad_usernames`](https://github.com/flurdy/bad_usernames) dataset. The dataset remains the source of truth.

## Dataset shape

The API dataset shape uses `usernames` consistently. A combined snapshot should include a merged top-level list and may include per-language bundles:

```json
{
  "version": "flurdy/bad_usernames@<commit>",
  "languages": ["de", "en", "no"],
  "usernames": ["admin", "root", "support"],
  "bundles": [
    { "language": "de", "usernames": ["admin"] },
    { "language": "en", "usernames": ["admin", "root", "support"] },
    { "language": "no", "usernames": ["admin"] }
  ]
}
```

The top-level `usernames` list is canonical for runtime checks. If a dataset omits the top-level list but includes `bundles`, the loader merges the bundle usernames at startup.

Single upstream language files are also valid:

```json
{
  "usernames": ["admin", "support"]
}
```

## Vendored snapshot

This repository includes a vendored combined snapshot for real-dataset testing:

- `data/bad-usernames.json` — generated from upstream `bad_usernames.*.json` language files
- `data/bad-usernames.version` — upstream commit recorded as `flurdy/bad_usernames@...`

The snapshot is provided for development, tests, and simple deployments that want a checked-in dataset file. The production Docker image still does **not** include a dataset; operators should mount their chosen snapshot at runtime.

## Dataset version metadata

`GET /api/v1/meta` returns `datasetVersion` using this precedence:

1. `BAD_USERNAMES_DATASET_VERSION`, when set.
2. The dataset JSON `version` field, when present.
3. An adjacent version file named either `<dataset>.version` or `<dataset-without-.json>.version`.
4. `unknown`.

For mounted upstream datasets, set `BAD_USERNAMES_DATASET_VERSION=flurdy/bad_usernames@<commit>` or mount an adjacent version file so `/meta` exposes the dataset commit.

## Dataset handling rules

- Load once at startup.
- Normalize usernames during loading.
- Drop empty usernames.
- De-duplicate usernames after normalization.
- Store usernames in an immutable `Set[String]`.
- Fail startup if the configured dataset cannot be loaded or parsed.

## Runtime integration options

Supported first-release approaches:

1. Runtime mounted upstream JSON file, plus `BAD_USERNAMES_DATASET_VERSION` or adjacent `.version` file.
2. Checked-in combined snapshot from `data/bad-usernames.json` for simple local/self-hosted deployments.

Future automation may add release artifact fetching or a Git submodule, but the service code should continue to consume a plain JSON file at startup.
