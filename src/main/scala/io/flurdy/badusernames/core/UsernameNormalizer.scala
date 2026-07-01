package io.flurdy.badusernames.core

import java.util.Locale

object UsernameNormalizer:
  def normalize(username: String): String =
    username.trim.toLowerCase(Locale.ROOT)
