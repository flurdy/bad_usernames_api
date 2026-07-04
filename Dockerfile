# Stage 1: build and stage the Scala application
FROM sbtscala/scala-sbt:eclipse-temurin-17.0.15_6_1.11.7_3.3.7 AS builder

WORKDIR /opt/build
COPY . /opt/build

RUN sbt clean stage

# Stage 2: minimal runtime image
FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache bash && \
    mkdir -p /opt/app /data

WORKDIR /opt/app

COPY --from=builder /opt/build/target/universal/stage /opt/app

ENV BAD_USERNAMES_HOST=0.0.0.0 \
    BAD_USERNAMES_PORT=8080 \
    BAD_USERNAMES_DATASET_PATH=/data/bad-usernames.json \
    BAD_USERNAMES_BATCH_LIMIT=1000

EXPOSE 8080

ENTRYPOINT ["/opt/app/bin/bad-usernames-api"]
