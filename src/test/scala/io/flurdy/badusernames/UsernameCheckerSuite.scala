package io.flurdy.badusernames

import io.flurdy.badusernames.core.UsernameChecker
import io.flurdy.badusernames.core.UsernameMatch
import munit.FunSuite

class UsernameCheckerSuite extends FunSuite:
  test("checks normalized exact matches") {
    val checker = UsernameChecker(Set("admin", "support"))

    val result = checker.check(" Admin ")

    assertEquals(result.bad, true)
    assertEquals(result.normalized, "admin")
    assertEquals(result.matched, Some("admin"))
    assertEquals(result.matches, List(UsernameMatch(UsernameMatch.Exact, "admin")))
  }

  test("allows usernames not present in the dataset") {
    val checker = UsernameChecker(Set("admin", "support"))

    val result = checker.check("ivar")

    assertEquals(result.bad, false)
    assertEquals(result.matched, None)
    assertEquals(result.matches, Nil)
  }

  test("flags substring matches as advisory without marking username bad") {
    val checker = UsernameChecker(Set("admin", "support"))

    val result = checker.check("admin123")

    assertEquals(result.bad, false)
    assertEquals(result.matched, None)
    assertEquals(result.matches, List(UsernameMatch(UsernameMatch.Substring, "admin")))
  }

  test("documents false-positive-prone substring matches as advisory") {
    val checker = UsernameChecker(Set("cunt"))

    val result = checker.check("Scunthorpe")

    assertEquals(result.normalized, "scunthorpe")
    assertEquals(result.bad, false)
    assertEquals(result.matched, None)
    assertEquals(result.matches, List(UsernameMatch(UsernameMatch.Substring, "cunt")))
  }
