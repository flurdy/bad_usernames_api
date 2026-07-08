package io.flurdy.badusernames.config

import cats.effect.Sync

final case class AppConfig(
    host: String,
    port: Int,
    datasetPath: String,
    datasetVersion: Option[String],
    batchLimit: Int
)

object AppConfig:
  def load[F[_]: Sync]: F[AppConfig] =
    Sync[F].delay {
      AppConfig(
        host = sys.env.getOrElse("BAD_USERNAMES_HOST", "0.0.0.0"),
        port = sys.env.get("BAD_USERNAMES_PORT").flatMap(_.toIntOption).getOrElse(8080),
        datasetPath =
          sys.env.getOrElse("BAD_USERNAMES_DATASET_PATH", "dev/sample-bad-usernames.json"),
        datasetVersion =
          sys.env.get("BAD_USERNAMES_DATASET_VERSION").map(_.trim).filter(_.nonEmpty),
        batchLimit = sys.env.get("BAD_USERNAMES_BATCH_LIMIT").flatMap(_.toIntOption).getOrElse(1000)
      )
    }
