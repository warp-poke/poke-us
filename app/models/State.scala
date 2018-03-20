package models

import java.time.LocalDateTime

case class State(
  token: Option[Token],
  lastAlertDate: Option[LocalDateTime]
)
