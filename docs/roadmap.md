# Roadmap

## Milestone 0 — Repository scaffold

- Initialize git repository.
- Add root project files.
- Add docs skeleton.
- Add sbt/http4s project scaffold.
- Add sample development dataset.

## Milestone 1 — Local API

- Implement single username check.
- Implement batch username check.
- Implement metadata endpoint.
- Add basic tests.
- Document local usage.

## Milestone 2 — Dataset integration

- Decide how to consume `flurdy/bad_usernames`.
- Preserve upstream license information.
- Expose dataset version or commit in `/api/v1/meta`.

## Milestone 3 — Self-hosting

- Add production container build.
- Add example docker run / compose docs.
- Document environment configuration.

## Later possibilities

- Hosted public endpoint.
- Rate limiting.
- API keys if abuse requires them.
- Unicode spoof detection.
- Fuzzy matching.
- Custom tenant lists.
- Dashboard or web UI.
