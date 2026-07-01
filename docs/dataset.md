# Dataset Plan

The API wraps the existing `flurdy/bad_usernames` dataset. The dataset remains the source of truth.

## Initial loading approach

The first version reads a local JSON file from `BAD_USERNAMES_DATASET_PATH`.

Expected development shape:

```json
{
  "version": "dev-sample",
  "languages": ["en"],
  "words": ["admin", "support"]
}
```

This shape may be adapted once the upstream dataset integration is finalized.

## Dataset handling rules

- Load once at startup.
- Normalize words during loading.
- Drop empty words.
- Store words in an immutable `Set[String]`.
- Fail startup if the configured dataset cannot be loaded.

## Future integration options

Options to decide later:

1. Git submodule pointing at `flurdy/bad_usernames`.
2. Vendored release snapshot.
3. Build-time fetch from a dataset release artifact.
4. Runtime mounted file for self-hosters.

For the first real release, mounted file or vendored release snapshot is probably simplest.
