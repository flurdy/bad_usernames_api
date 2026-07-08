package io.flurdy.badusernames

import cats.effect.IO
import io.flurdy.badusernames.core.UsernameDataset
import munit.CatsEffectSuite

import java.nio.file.Files

class UsernameDatasetSuite extends CatsEffectSuite:

  test("loads the API dataset shape with explicit metadata") {
    val path = Files.createTempFile("bad-usernames-api-shape", ".json")
    Files.writeString(
      path,
      """{
        |  "version": "test-version",
        |  "languages": ["en", "no"],
        |  "usernames": [" Admin ", "admin", "", "Support"]
        |}
        |""".stripMargin
    )

    UsernameDataset.load[IO](path.toString).map { dataset =>
      assertEquals(dataset.usernames, Set("admin", "support"))
      assertEquals(dataset.metadata.version, "test-version")
      assertEquals(dataset.metadata.languages, List("en", "no"))
      assertEquals(dataset.metadata.wordCount, 2)
    }
  }

  test("loads the upstream flurdy/bad_usernames shape") {
    val path = Files.createTempFile("bad-usernames", ".json")
    Files.writeString(
      path,
      """{
        |  "usernames": ["abuse", "Root", "support"]
        |}
        |""".stripMargin
    )
    Files.writeString(
      path.resolveSibling(path.getFileName.toString + ".version"),
      "upstream-commit\n"
    )

    UsernameDataset.load[IO](path.toString).map { dataset =>
      assertEquals(dataset.usernames, Set("abuse", "root", "support"))
      assertEquals(dataset.metadata.version, "upstream-commit")
      assertEquals(dataset.metadata.wordCount, 3)
    }
  }

  test("loads a bundled combined snapshot") {
    val path = Files.createTempFile("bad-usernames-bundles", ".json")
    Files.writeString(
      path,
      """{
        |  "version": "bundled-version",
        |  "bundles": [
        |    {"language": "en", "usernames": ["Admin", "support"]},
        |    {"language": "no", "usernames": ["admin", "bruker"]}
        |  ]
        |}
        |""".stripMargin
    )

    UsernameDataset.load[IO](path.toString).map { dataset =>
      assertEquals(dataset.usernames, Set("admin", "support", "bruker"))
      assertEquals(dataset.metadata.version, "bundled-version")
      assertEquals(dataset.metadata.languages, List("en", "no"))
      assertEquals(dataset.metadata.wordCount, 3)
    }
  }

  test("configured dataset version overrides file metadata") {
    val path = Files.createTempFile("bad-usernames-version", ".json")
    Files.writeString(path, """{"version":"file-version","usernames":["admin"]}""")

    UsernameDataset.load[IO](path.toString, Some("configured-version")).map { dataset =>
      assertEquals(dataset.metadata.version, "configured-version")
    }
  }

  test("loads the vendored upstream snapshot with its adjacent version file") {
    UsernameDataset.load[IO]("data/bad-usernames.json").map { dataset =>
      assert(dataset.usernames.contains("admin"))
      assert(dataset.usernames.contains("support"))
      assertEquals(dataset.metadata.wordCount, 104)
      assertEquals(dataset.metadata.languages, List("de", "en", "no"))
      assertEquals(
        dataset.metadata.version,
        "flurdy/bad_usernames@ecf2524f2dbe43b774dc8bffb9f229aebd540b43"
      )
    }
  }
