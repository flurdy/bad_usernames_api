package io.flurdy.badusernames.core

import cats.effect.Sync
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.HCursor
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
    usernames: Set[String],
    metadata: DatasetMetadata
)

object UsernameDataset:
  private final case class RawBundle(
      language: String,
      usernames: List[String]
  )

  private object RawBundle:
    given Decoder[RawBundle] = deriveDecoder[RawBundle]

  private final case class RawDataset(
      version: Option[String],
      languages: List[String],
      usernames: List[String]
  )

  private given Decoder[RawDataset] = (cursor: HCursor) =>
    for
      usernames <- cursor.downField("usernames").as[Option[List[String]]]
      bundles <- cursor.downField("bundles").as[Option[List[RawBundle]]]
      version <- cursor.downField("version").as[Option[String]]
      languages <- cursor.downField("languages").as[Option[List[String]]]
      datasetUsernames <- usernames.orElse(bundles.map(_.flatMap(_.usernames))) match
        case Some(values) => Right(values)
        case None =>
          Left(
            DecodingFailure("dataset must contain a 'usernames' array or 'bundles'", cursor.history)
          )
    yield RawDataset(
      version = version.map(_.trim).filter(_.nonEmpty),
      languages = languages
        .getOrElse(bundles.toList.flatten.map(_.language))
        .map(_.trim)
        .filter(_.nonEmpty)
        .distinct,
      usernames = datasetUsernames
    )

  def load[F[_]: Sync](path: String, configuredVersion: Option[String] = None): F[UsernameDataset] =
    for
      content <- Sync[F].blocking(Files.readString(Path.of(path)))
      adjacentVersion <- readAdjacentVersion[F](path)
      dataset <- decodeDataset[F](path, content, configuredVersion, adjacentVersion)
    yield dataset

  private def readAdjacentVersion[F[_]: Sync](path: String): F[Option[String]] =
    Sync[F].blocking {
      val datasetPath = Path.of(path)
      val versionPaths = List(
        Path.of(s"$path.version"),
        datasetPath.resolveSibling(
          datasetPath.getFileName.toString.stripSuffix(".json") + ".version"
        )
      )
      versionPaths
        .find(Files.isRegularFile(_))
        .flatMap(versionPath => Some(Files.readString(versionPath).trim).filter(_.nonEmpty))
    }

  private def decodeDataset[F[_]: Sync](
      path: String,
      content: String,
      configuredVersion: Option[String],
      adjacentVersion: Option[String]
  ): F[UsernameDataset] =
    parse(content).flatMap(_.as[RawDataset]) match
      case Left(error) =>
        Sync[F].raiseError(
          new RuntimeException(
            s"Failed to load bad usernames dataset from $path: ${error.getMessage}",
            error
          )
        )
      case Right(rawDataset) =>
        val usernames =
          rawDataset.usernames.map(UsernameNormalizer.normalize).filter(_.nonEmpty).toSet
        Sync[F].pure(
          UsernameDataset(
            usernames = usernames,
            metadata = DatasetMetadata(
              version = configuredVersion
                .orElse(rawDataset.version)
                .orElse(adjacentVersion)
                .getOrElse("unknown"),
              languages = languagesFor(path, rawDataset.languages),
              wordCount = usernames.size
            )
          )
        )

  private def languagesFor(path: String, languages: List[String]): List[String] =
    if languages.nonEmpty then languages
    else
      val filename = Path.of(path).getFileName.toString
      val languagePattern = """bad_usernames\.([a-z]{2})\.json""".r
      filename match
        case languagePattern(language)                   => List(language)
        case "bad_usernames.json" | "bad-usernames.json" => List("en")
        case _                                           => Nil
