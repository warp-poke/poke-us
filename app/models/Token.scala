package models

import java.time.ZonedDateTime

import play.api.libs.json._

case class Token(
  token: String,
  expires_at: ZonedDateTime
)

object TokenInstances {
  implicit val tokenReads = Json.reads[Token]
}
