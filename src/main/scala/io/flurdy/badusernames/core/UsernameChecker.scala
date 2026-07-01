package io.flurdy.badusernames.core

final case class UsernameCheckResult(
    username: String,
    normalized: String,
    bad: Boolean,
    matched: Option[String]
)

final class UsernameChecker(blockedUsernames: Set[String]):
  def check(username: String): UsernameCheckResult =
    val normalized = UsernameNormalizer.normalize(username)
    val matched = Option.when(blockedUsernames.contains(normalized))(normalized)

    UsernameCheckResult(
      username = username,
      normalized = normalized,
      bad = matched.isDefined,
      matched = matched
    )
