package io.flurdy.badusernames.api

import cats.effect.IO
import io.flurdy.badusernames.core.DatasetMetadata
import io.flurdy.badusernames.core.UsernameChecker
import org.http4s.EntityEncoder
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.MediaType
import org.http4s.Response
import org.http4s.Status
import org.http4s.headers.`Content-Type`
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
      IO.pure(
        Response[IO](status = Status.Ok)
          .withEntity(rootPage)(using EntityEncoder.stringEncoder[IO])
          .withContentType(`Content-Type`(MediaType.text.html))
      )

    case GET -> Root / "health" =>
      Ok(HealthResponse("ok"))

    case GET -> Root / "api" / "v1" / "check" :? UsernameQueryParamMatcher(username) =>
      username match
        case Some(value) =>
          val result = checker.check(value)
          if result.normalized.isEmpty then BadRequest(ErrorResponse("username must not be empty"))
          else Ok(toResponse(result))
        case None =>
          BadRequest(ErrorResponse("username query parameter is required"))

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

  private val rootPage: String =
    """<!doctype html>
      |<html lang="en">
      |  <head>
      |    <meta charset="utf-8">
      |    <meta name="viewport" content="width=device-width, initial-scale=1">
      |    <title>Bad Usernames API</title>
      |    <style>
      |      body { max-width: 44rem; margin: 4rem auto; padding: 0 1rem; font-family: system-ui, sans-serif; line-height: 1.5; color: #172033; }
      |      h1 { margin-bottom: 0.25rem; }
      |      p { color: #4d5a70; }
      |      ul { padding-left: 1.25rem; }
      |      li { margin: 0.45rem 0; }
      |      code { background: #f1f4f8; padding: 0.12rem 0.3rem; border-radius: 0.25rem; }
      |      a { color: #1459c7; }
      |    </style>
      |  </head>
      |  <body>
      |    <h1>Bad Usernames API</h1>
      |    <p>A small API for checking blocked, reserved, or unsafe usernames.</p>
      |    <ul>
      |      <li><a href="/health"><code>GET /health</code></a> — service health</li>
      |      <li><a href="/api/v1/meta"><code>GET /api/v1/meta</code></a> — service and dataset metadata</li>
      |      <li><a href="/api/v1/check?username=admin"><code>GET /api/v1/check?username=admin</code></a> — check one username</li>
      |      <li><code>POST /api/v1/check</code> — check multiple usernames</li>
      |      <li><a href="https://github.com/flurdy/bad_usernames_api#readme">Documentation</a></li>
      |    </ul>
      |  </body>
      |</html>
      |""".stripMargin

  private def toResponse(result: io.flurdy.badusernames.core.UsernameCheckResult): CheckResponse =
    CheckResponse(
      username = result.username,
      normalized = result.normalized,
      bad = result.bad,
      matched = result.matched,
      matches = result.matches.map(matchResult =>
        MatchResponse(matchType = matchResult.matchType, term = matchResult.term)
      )
    )

private object UsernameQueryParamMatcher
    extends OptionalQueryParamDecoderMatcher[String]("username")

private object BuildInfo:
  val version: String = sys.props.getOrElse("bad-usernames-api.version", "0.1.0-SNAPSHOT")
