package io.flurdy.badusernames.api

import cats.effect.IO
import io.flurdy.badusernames.core.DatasetMetadata
import io.flurdy.badusernames.core.UsernameChecker
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*

final class Routes(
    checker: UsernameChecker,
    datasetMetadata: DatasetMetadata,
    batchLimit: Int
):
  val httpRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root =>
      Ok("Bad Usernames API\n\nSee /health, /api/v1/meta and /api/v1/check/{username}.\n")

    case GET -> Root / "health" =>
      Ok(HealthResponse("ok"))

    case GET -> Root / "api" / "v1" / "check" / username =>
      val result = checker.check(username)
      if result.normalized.isEmpty then BadRequest(ErrorResponse("username must not be empty"))
      else Ok(toResponse(result))

    case request @ POST -> Root / "api" / "v1" / "check" =>
      request.attemptAs[BatchCheckRequest].value.flatMap {
        case Left(_) =>
          BadRequest(ErrorResponse("request body must be JSON with a 'usernames' array"))
        case Right(body) =>
          if body.usernames.size > batchLimit then
            BadRequest(ErrorResponse(s"batch size must not exceed $batchLimit"))
          else
            val checked = body.usernames.map(checker.check)
            if checked.exists(_.normalized.isEmpty) then
              BadRequest(ErrorResponse("usernames must not contain empty values"))
            else Ok(BatchCheckResponse(checked.map(toResponse)))
      }

    case GET -> Root / "api" / "v1" / "meta" =>
      Ok(
        MetaResponse(
          service = "Bad Usernames API",
          serviceVersion = BuildInfo.version,
          datasetVersion = datasetMetadata.version,
          languages = datasetMetadata.languages,
          wordCount = datasetMetadata.wordCount,
          normalization = "trim + lowercase(Locale.ROOT)",
          batchLimit = batchLimit
        )
      )
  }

  val httpApp: HttpApp[IO] = httpRoutes.orNotFound

  private def toResponse(result: io.flurdy.badusernames.core.UsernameCheckResult): CheckResponse =
    CheckResponse(
      username = result.username,
      normalized = result.normalized,
      bad = result.bad,
      matched = result.matched
    )

private object BuildInfo:
  val version: String = sys.props.getOrElse("bad-usernames-api.version", "0.1.0-SNAPSHOT")
