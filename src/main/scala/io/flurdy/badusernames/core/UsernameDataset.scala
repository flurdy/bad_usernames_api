package io.flurdy.badusernames.core

import cats.effect.Sync
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser.parse

import java.nio.file.Files
import java.nio.file.Path

final case class DatasetMetadata(
    version: String,
    languages: List[String],
    wordCount: Int
)

final case class UsernameDataset(
    words: Set[String],
    metadata: DatasetMetadata
)

object UsernameDataset:
  private final case class RawDataset(
      version: String,
      languages: List[String],
      words: List[String]
  )

  private given Decoder[RawDataset] = deriveDecoder[RawDataset]

  def load[F[_]: Sync](path: String): F[UsernameDataset] =
    for
      content <- Sync[F].blocking(Files.readString(Path.of(path)))
      dataset <- decodeDataset[F](path, content)
    yield dataset

  private def decodeDataset[F[_]: Sync](path: String, content: String): F[UsernameDataset] =
    parse(content).flatMap(_.as[RawDataset]) match
      case Left(error) =>
        Sync[F].raiseError(
          new RuntimeException(
            s"Failed to load bad usernames dataset from $path: ${error.getMessage}",
            error
          )
        )
      case Right(rawDataset) =>
        val words = rawDataset.words.map(UsernameNormalizer.normalize).filter(_.nonEmpty).toSet
        Sync[F].pure(
          UsernameDataset(
            words = words,
            metadata = DatasetMetadata(
              version = rawDataset.version,
              languages = rawDataset.languages,
              wordCount = words.size
            )
          )
        )
