package models

case class State(
  readToken: Option[Token],
  writeToken: Option[Token]
)
