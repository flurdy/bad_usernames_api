package io.flurdy.badusernames

import cats.effect.IO
import cats.effect.IOApp
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import io.flurdy.badusernames.api.Routes
import io.flurdy.badusernames.config.AppConfig
import io.flurdy.badusernames.core.UsernameChecker
import io.flurdy.badusernames.core.UsernameDataset
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp.Simple:
  private given Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    for
      config <- AppConfig.load[IO]
      host <- IO.fromOption(Host.fromString(config.host))(
        new IllegalArgumentException(s"Invalid BAD_USERNAMES_HOST: ${config.host}")
      )
      port <- IO.fromOption(Port.fromInt(config.port))(
        new IllegalArgumentException(s"Invalid BAD_USERNAMES_PORT: ${config.port}")
      )
      dataset <- UsernameDataset.load[IO](config.datasetPath)
      _ <- Logger[IO].info(
        s"Loaded ${dataset.metadata.wordCount} bad usernames from ${config.datasetPath}"
      )
      checker = UsernameChecker(dataset.words)
      routes = Routes(checker, dataset.metadata, config.batchLimit)
      _ <- Logger[IO].info(s"Starting Bad Usernames API on ${config.host}:${config.port}")
      _ <- EmberServerBuilder
        .default[IO]
        .withHost(host)
        .withPort(port)
        .withHttpApp(routes.httpApp)
        .build
        .useForever
    yield ()
