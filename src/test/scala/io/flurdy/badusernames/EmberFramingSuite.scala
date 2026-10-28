package io.flurdy.badusernames

import cats.effect.IO
import cats.effect.Ref
import cats.effect.Resource
import com.comcast.ip4s.*
import io.flurdy.badusernames.api.Routes
import io.flurdy.badusernames.core.DatasetMetadata
import io.flurdy.badusernames.core.UsernameChecker
import munit.CatsEffectSuite
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder

import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets.US_ASCII

class EmberFramingSuite extends CatsEffectSuite:

  private val app = Routes(
    UsernameChecker(Set("admin")),
    DatasetMetadata(version = "test", languages = List("en"), wordCount = 1),
    batchLimit = 3
  ).httpApp

  private def exchange(request: String): IO[(String, List[String])] =
    for
      received <- Ref.of[IO, List[String]](Nil)
      response <- EmberServerBuilder
        .default[IO]
        .withHost(host"127.0.0.1")
        .withPort(port"0")
        .withHttpApp(HttpApp[IO] { request =>
          received.update(_ :+ request.uri.path.renderString) *> app.run(request)
        })
        .build
        .use { server =>
          Resource.fromAutoCloseable(IO.blocking(new Socket())).use { socket =>
            IO.blocking {
              socket.connect(new InetSocketAddress("127.0.0.1", server.address.getPort), 5000)
              socket.setSoTimeout(5000)
              socket.getOutputStream.write(request.getBytes(US_ASCII))
              socket.getOutputStream.flush()
              new String(socket.getInputStream.readAllBytes(), US_ASCII)
            }
          }
        }
      paths <- received.get
    yield (response, paths)

  private val body = "{\"usernames\":[\"admin\"]}"
  private val chunkedBody = s"${body.length.toHexString}\r\n$body\r\n0\r\n\r\n"

  List(
    "content length first" -> "Content-Length: 5\r\nTransfer-Encoding: chunked",
    "transfer encoding first" -> "Transfer-Encoding: chunked\r\nContent-Length: 5",
    "mixed case" -> "tRaNsFeR-EnCoDiNg: ChUnKeD\r\ncOnTeNt-LeNgTh: 5"
  ).foreach { (name, headers) =>
    test(s"rejects ambiguous framing and closes the connection: $name") {
      val request =
        s"POST /api/v1/check HTTP/1.1\r\nHost: localhost\r\n$headers\r\nContent-Type: application/json\r\n\r\n" +
          chunkedBody + "GET /health HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n"
      exchange(request).map { (response, paths) =>
        assert(response.startsWith("HTTP/1.1 400 "), response)
        assert(!response.contains("HTTP/1.1 200 "), response)
        assertEquals(paths, Nil)
      }
    }
  }

  List(
    "content length" -> (s"Content-Length: ${body.length}", body),
    "chunked" -> ("Transfer-Encoding: chunked", chunkedBody)
  ).foreach { case (name, (header, payload)) =>
    test(s"accepts a batch with $name framing") {
      val request =
        s"POST /api/v1/check HTTP/1.1\r\nHost: localhost\r\n$header\r\nContent-Type: application/json\r\nConnection: close\r\n\r\n$payload"
      exchange(request).map { (response, paths) =>
        assert(response.startsWith("HTTP/1.1 200 "), response)
        assert(response.contains("\"bad\":true"), response)
        assertEquals(paths, List("/api/v1/check"))
      }
    }
  }
