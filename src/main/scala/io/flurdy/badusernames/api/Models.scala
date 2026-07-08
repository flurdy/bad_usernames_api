package io.flurdy.badusernames.api

import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder

final case class CheckResponse(
    username: String,
    normalized: String,
    bad: Boolean,
    matched: Option[String]
)

object CheckResponse:
  given Encoder[CheckResponse] = deriveEncoder[CheckResponse]

final case class BatchCheckRequest(usernames: List[String])

object BatchCheckRequest:
  given Decoder[BatchCheckRequest] = deriveDecoder[BatchCheckRequest]

final case class BatchCheckResponse(results: List[CheckResponse])

object BatchCheckResponse:
  given Encoder[BatchCheckResponse] = deriveEncoder[BatchCheckResponse]

final case class MetaResponse(
    service: String,
    serviceVersion: String,
    datasetVersion: String,
    languages: List[String],
    wordCount: Int,
    normalization: String,
    batchLimit: Int
)

object MetaResponse:
  given Encoder[MetaResponse] = deriveEncoder[MetaResponse]

final case class HealthResponse(status: String)

object HealthResponse:
  given Encoder[HealthResponse] = deriveEncoder[HealthResponse]

final case class ErrorResponse(error: String)

object ErrorResponse:
  given Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
