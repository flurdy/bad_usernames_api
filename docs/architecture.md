# Architecture Plan

Bad Usernames API should stay deliberately small.

## Runtime shape

1. Read configuration from environment variables.
2. Load the username dataset at startup.
3. Normalize all dataset usernames into an in-memory `Set[String]`.
4. Serve HTTP requests using http4s.
5. For each check, normalize input and perform an exact set lookup.
6. If no exact match exists, scan the in-memory set for advisory substring matches.

## Main components

- `config.AppConfig` — environment-backed configuration.
- `core.UsernameNormalizer` — deterministic username normalization.
- `core.UsernameDataset` — dataset loading and metadata.
- `core.UsernameChecker` — pure exact-match and advisory substring check logic.
- `api.Models` — request/response JSON models.
- `api.Routes` — http4s routes.
- `Main` — application wiring.

## Deliberate constraints

The first version should not include:

- database storage
- user accounts
- API keys
- fuzzy matching beyond exact substring advisories
- unicode spoof detection
- tenant-specific rules
- background refresh

Those can be introduced later if real usage justifies them.

## Deployment expectation

The service should be runnable as:

- a local sbt application during development
- a containerized service for self-hosting
- a small hosted service at the planned public hostname
