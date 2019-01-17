package models

import java.time.OffsetDateTime
import java.util.UUID
import javax.inject._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.{Failure, Success}

import play.api.libs.json._
import play.api.libs.ws._

import anorm._
import anorm.SqlParser._
import pgentity.pg_entity._

import models.enums._

object hook_kind {

  sealed trait HookKind {
    def name = this.toString.toUpperCase
  }
  case object SLACK_WEBHOOK extends HookKind

  object HookKind {
    val values = List(
      SLACK_WEBHOOK
    )
    def apply(s: String) = values.find(_.name == s.toUpperCase)
  }

  implicit val hookKindEnum = new EnumAdt[HookKind] {
    val values = HookKind.values
    def valueAsString(x: HookKind) = x.name
  }
  implicit val hookKindReads  = jsonReads[HookKind]
  implicit val hookKindWrites = jsonWrites[HookKind]

  implicit val hookKindToStatement = toStatement[HookKind]
  implicit val hookKindFromColumn = fromColumn[HookKind]
}

case class HookInput(
  user_id: Hook.UserId,
  label: String,
  kind: hook_kind.HookKind,
  webhook: String
)

case class Hook(
  hook_id: Hook.HookId,
  user_id: Hook.UserId,
  label: String,
  kind: hook_kind.HookKind,
  webhook: String
) {
  import hook_kind._

  val template = kind match {
    case SLACK_WEBHOOK => """{"text": "@@BODY@@"}"""
    case _ => "Error: this kind's template is not defined."
  }
}

object Hook {
  type HookId = UUID
  type UserId = UUID
}

object HookInstances {
  implicit val hookInputReads = Json.reads[HookInput]
  implicit val hookReads = Json.reads[Hook]
  implicit val hookWrites = Json.writes[Hook]
  implicit val hookEntity = new PgEntity[Hook] {
    val tableName = "hook"
    val columns = List(
      PgField("hook_id", Some("uuid"), true),
      PgField("user_id"),
      PgField("label"),
      PgField("kind"),
      PgField("webhook")
    )
    def parser(prefix: String): RowParser[Hook] = {
      get[UUID](prefix + "hook_id") ~
      get[UUID](prefix + "user_id") ~
      get[String](prefix + "label") ~
      get[hook_kind.HookKind](prefix + "kind") ~
      get[String](prefix + "webhook") map {
        case hook_id ~ user_id ~ label ~ kind ~ webhook => {
          Hook(hook_id, user_id, label, kind, webhook)
        }
      }
    }
  }
}