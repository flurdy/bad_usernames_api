package io.flurdy.badusernames

import cats.effect.IO
import io.circe.Json
import io.flurdy.badusernames.api.Routes
import io.flurdy.badusernames.core.DatasetMetadata
import io.flurdy.badusernames.core.UsernameChecker
import munit.CatsEffectSuite
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.headers.`Content-Type`
import org.http4s.implicits.*

class RoutesSuite extends CatsEffectSuite:

  private val metadata =
    DatasetMetadata(version = "test", languages = List("en"), wordCount = 2)

  private val app =
    Routes(UsernameChecker(Set("admin", "support")), metadata, batchLimit = 3).httpApp

  private def run(request: Request[IO]): IO[(Status, Json)] =
    app.run(request).flatMap(response => response.as[Json].map(json => (response.status, json)))

  private def field[A: io.circe.Decoder](json: Json, name: String): Either[Any, A] =
    json.hcursor.get[A](name)

  // GET /health

  test("GET health returns ok") {
    run(Request[IO](Method.GET, uri"/health")).map { (status, json) =>
      assertEquals(status, Status.Ok)
      assertEquals(field[String](json, "status"), Right("ok"))
    }
  }

  // GET /api/v1/check?username=...

  test("GET check returns bad=true for a matched username") {
    run(Request[IO](Method.GET, uri"/api/v1/check".withQueryParam("username", "admin"))).map {
      (status, json) =>
        assertEquals(status, Status.Ok)
        assertEquals(field[Boolean](json, "bad"), Right(true))
        assertEquals(field[String](json, "matched"), Right("admin"))
        assertEquals(field[String](json, "normalized"), Right("admin"))
        assertEquals(field[List[Json]](json, "matches").map(_.size), Right(1))
        assertEquals(
          json.hcursor.downField("matches").downArray.get[String]("matchType"),
          Right("exact")
        )
        assertEquals(
          json.hcursor.downField("matches").downArray.get[String]("term"),
          Right("admin")
        )
    }
  }

  test("GET check normalizes trim + lowercase before matching") {
    run(Request[IO](Method.GET, uri"/api/v1/check".withQueryParam("username", " Admin "))).map {
      (status, json) =>
        assertEquals(status, Status.Ok)
        assertEquals(field[Boolean](json, "bad"), Right(true))
        assertEquals(field[String](json, "normalized"), Right("admin"))
        assertEquals(field[String](json, "username"), Right(" Admin "))
    }
  }

  test("GET check returns bad=false with null matched for an allowed username") {
    run(Request[IO](Method.GET, uri"/api/v1/check".withQueryParam("username", "ivar"))).map {
      (status, json) =>
        assertEquals(status, Status.Ok)
        assertEquals(field[Boolean](json, "bad"), Right(false))
        assertEquals(field[Option[String]](json, "matched"), Right(None))
        assertEquals(field[List[Json]](json, "matches"), Right(Nil))
    }
  }

  test("GET check identifies advisory substring matches") {
    run(Request[IO](Method.GET, uri"/api/v1/check".withQueryParam("username", "admin123"))).map {
      (status, json) =>
        assertEquals(status, Status.Ok)
        assertEquals(field[Boolean](json, "bad"), Right(false))
        assertEquals(field[Option[String]](json, "matched"), Right(None))
        assertEquals(
          json.hcursor.downField("matches").downArray.get[String]("matchType"),
          Right("substring")
        )
        assertEquals(
          json.hcursor.downField("matches").downArray.get[String]("term"),
          Right("admin")
        )
    }
  }

  test("GET check returns 400 for a whitespace-only username") {
    run(Request[IO](Method.GET, uri"/api/v1/check".withQueryParam("username", " "))).map {
      (status, json) =>
        assertEquals(status, Status.BadRequest)
        assertEquals(field[String](json, "error"), Right("username must not be empty"))
    }
  }

  test("GET check returns 400 when username query parameter is missing") {
    run(Request[IO](Method.GET, uri"/api/v1/check")).map { (status, json) =>
      assertEquals(status, Status.BadRequest)
      assertEquals(field[String](json, "error"), Right("username query parameter is required"))
    }
  }

  test("GET check path username endpoint is not supported") {
    app.run(Request[IO](Method.GET, uri"/api/v1/check/admin")).map { response =>
      assertEquals(response.status, Status.NotFound)
    }
  }

  // POST /api/v1/check

  test("POST check returns a result per username") {
    val body = Json.obj(
      "usernames" -> Json.arr(
        Json.fromString("admin"),
        Json.fromString("ivar"),
        Json.fromString("support")
      )
    )
    run(Request[IO](Method.POST, uri"/api/v1/check").withEntity(body)).map { (status, json) =>
      assertEquals(status, Status.Ok)
      val results = json.hcursor.downField("results").values.map(_.toList).getOrElse(Nil)
      assertEquals(results.size, 3)
      assertEquals(field[Boolean](results.head, "bad"), Right(true))
      assertEquals(field[Boolean](results(1), "bad"), Right(false))
      assertEquals(field[Boolean](results(2), "bad"), Right(true))
    }
  }

  test("POST check returns 400 when the batch exceeds the limit") {
    val body = Json.obj(
      "usernames" -> Json.arr(
        Json.fromString("a"),
        Json.fromString("b"),
        Json.fromString("c"),
        Json.fromString("d")
      )
    )
    run(Request[IO](Method.POST, uri"/api/v1/check").withEntity(body)).map { (status, json) =>
      assertEquals(status, Status.BadRequest)
      assertEquals(field[String](json, "error"), Right("batch size must not exceed 3"))
    }
  }

  test("POST check returns 400 when a username normalizes to empty") {
    val body = Json.obj("usernames" -> Json.arr(Json.fromString("admin"), Json.fromString("  ")))
    run(Request[IO](Method.POST, uri"/api/v1/check").withEntity(body)).map { (status, json) =>
      assertEquals(status, Status.BadRequest)
      assertEquals(field[String](json, "error"), Right("usernames must not contain empty values"))
    }
  }

  test("POST check returns 400 for malformed JSON") {
    val request = Request[IO](Method.POST, uri"/api/v1/check")
      .withEntity("{ not valid json")
      .withContentType(`Content-Type`(MediaType.application.json))
    run(request).map { (status, json) =>
      assertEquals(status, Status.BadRequest)
      assertEquals(
        field[String](json, "error"),
        Right("request body must be JSON with a 'usernames' array")
      )
    }
  }

  // GET /api/v1/meta

  test("GET meta returns dataset and service metadata") {
    run(Request[IO](Method.GET, uri"/api/v1/meta")).map { (status, json) =>
      assertEquals(status, Status.Ok)
      assertEquals(field[Int](json, "wordCount"), Right(2))
      assertEquals(field[Int](json, "batchLimit"), Right(3))
      assertEquals(field[List[String]](json, "languages"), Right(List("en")))
      assertEquals(field[String](json, "datasetVersion"), Right("test"))
      assertEquals(field[String](json, "normalization"), Right("trim + lowercase(Locale.ROOT)"))
    }
  }

  // Root + unknown routes

  test("GET root returns a plain-text pointer") {
    app.run(Request[IO](Method.GET, uri"/")).flatMap { response =>
      response.as[String].map { body =>
        assertEquals(response.status, Status.Ok)
        assert(body.contains("/health"), s"expected root body to mention health, got: $body")
        assert(body.contains("/api/v1/meta"), s"expected root body to mention meta, got: $body")
      }
    }
  }

  test("unknown route returns 404") {
    app.run(Request[IO](Method.GET, uri"/does/not/exist")).map { response =>
      assertEquals(response.status, Status.NotFound)
    }
  }
