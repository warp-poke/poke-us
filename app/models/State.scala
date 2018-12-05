package models

import java.time.LocalDateTime

case class State(
  readToken: Option[Token],
  writeToken: Option[Token],
  lastAlertDate: Option[LocalDateTime]
)
