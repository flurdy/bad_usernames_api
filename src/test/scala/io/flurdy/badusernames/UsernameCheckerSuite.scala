package io.flurdy.badusernames

import io.flurdy.badusernames.core.UsernameChecker
import munit.FunSuite

class UsernameCheckerSuite extends FunSuite:
  test("checks normalized exact matches") {
    val checker = UsernameChecker(Set("admin", "support"))

    val result = checker.check(" Admin ")

    assertEquals(result.bad, true)
    assertEquals(result.normalized, "admin")
    assertEquals(result.matched, Some("admin"))
  }

  test("allows usernames not present in the dataset") {
    val checker = UsernameChecker(Set("admin", "support"))

    val result = checker.check("ivar")

    assertEquals(result.bad, false)
    assertEquals(result.matched, None)
  }
