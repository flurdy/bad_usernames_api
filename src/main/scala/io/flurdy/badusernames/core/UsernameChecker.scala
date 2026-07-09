package io.flurdy.badusernames.core

final case class UsernameMatch(
    matchType: String,
    term: String
)

object UsernameMatch:
  val Exact: String = "exact"
  val Substring: String = "substring"

final case class UsernameCheckResult(
    username: String,
    normalized: String,
    bad: Boolean,
    matched: Option[String],
    matches: List[UsernameMatch]
)

final class UsernameChecker(blockedUsernames: Set[String]):
  private val sortedBlockedUsernames = blockedUsernames.toList.sorted

  def check(username: String): UsernameCheckResult =
    val normalized = UsernameNormalizer.normalize(username)
    val matched = Option.when(blockedUsernames.contains(normalized))(normalized)
    val matches = matched match
      case Some(term) => List(UsernameMatch(UsernameMatch.Exact, term))
      case None =>
        sortedBlockedUsernames
          .filter(term => normalized.contains(term))
          .map(term => UsernameMatch(UsernameMatch.Substring, term))

    UsernameCheckResult(
      username = username,
      normalized = normalized,
      bad = matched.isDefined,
      matched = matched,
      matches = matches
    )
