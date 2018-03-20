package models

import java.time.LocalDateTime
import java.util.UUID

case class Alert(
  date: LocalDateTime,
  message: String,
  ownerId: UUID
)
