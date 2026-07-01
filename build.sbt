ThisBuild / organization := "io.flurdy"
ThisBuild / scalaVersion := "3.5.2"
ThisBuild / version := "0.1.0-SNAPSHOT"

val http4sVersion = "0.23.30"
val circeVersion = "0.14.10"
val log4catsVersion = "2.7.0"
val munitCatsEffectVersion = "2.0.0"
val logbackVersion = "1.5.16"

lazy val root = (project in file("."))
  .settings(
    name := "bad-usernames-api",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion % Runtime,
      "org.typelevel" %% "munit-cats-effect" % munitCatsEffectVersion % Test
    )
  )
