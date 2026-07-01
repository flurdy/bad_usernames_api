# Docker

Container packaging is planned for the self-hosting milestone.

For now, run locally with:

```bash
BAD_USERNAMES_DATASET_PATH=dev/sample-bad-usernames.json sbt run
```

A production Dockerfile should be added after deciding whether to use sbt-native-packager, an assembly JAR, or another release packaging approach.
