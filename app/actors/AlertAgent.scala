package actors

import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.UUID
import javax.inject._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

import play.api.libs.json._
import play.api.libs.ws._
import play.api.Logger

import akka._
import akka.actor._
import akka.stream.ActorMaterializer

import com.clevercloud.warp10client._
import com.clevercloud.warp10client.models._
import com.clevercloud.warp10client.models.gts_module._

import _root_.models.Alert
import _root_.models.Token
import _root_.models.State
import _root_.models.warpscripts._
import utils.Config

object AlertAgent {
  case object Tick

  sealed trait AlertAgentError
  case object TokensUndefined extends AlertAgentError
  case object ReadTokenUndefined extends AlertAgentError
  case object WriteTokenUndefined extends AlertAgentError
}

class AlertAgent @Inject() (
  system: ActorSystem,
  @Named("stateAgent") stateAgent: ActorRef,
  configuration: Config,
  implicit val ec: ExecutionContext,
  wsClient: WSClient
) extends Actor {
  import AlertAgent._

  implicit val executionContext = system.dispatchers.lookup("alertAgent")
  implicit val actorMaterializer = ActorMaterializer()
  implicit val warpConfiguration = WarpConfiguration("https://" + configuration.warp10.host)
  val warpClient = WarpClient(
    configuration.warp10.host,
    configuration.warp10.port,
    "https"
  )(warpConfiguration, system, actorMaterializer)

  def receive = {
    case Tick => stateAgent ! StateAgent.GetState
    case State(None, None, None) => Logger.error(TokensUndefined.toString)
    case State(Some(readToken), None, None) => Logger.error(WriteTokenUndefined.toString)
    case State(None, Some(writeToken), None) => Logger.error(ReadTokenUndefined.toString)
    case State(Some(readToken), Some(writeToken), None) => processAlerts(readToken, writeToken, LocalDateTime.now.minusMinutes(50000))
    case State(Some(readToken), Some(writeToken), Some(lastAlertDate)) => processAlerts(readToken, writeToken, lastAlertDate)
  }

  private def processAlerts(readToken: Token, writeToken: Token, lastAlertDate: LocalDateTime) = {
    Logger.debug(s"""
      Fetching from ${FetchRange(lastAlertDate, LocalDateTime.now).toString} with
        - readToken: ${readToken.toString};
        - writeToken: ${writeToken.toString}.
    """)
    warpClient.exec(Checks.last(
      token = readToken.token,
      selector = "~alert.http.status{}",
      duration = "15 m",
      value = "false",
      op = "eq"
    )).map { gtsList =>
      gtsList.map { gts =>
        stateAgent ! StateAgent.SetLastAlertDate(tsToLocalDateTime(gts.mostRecentPoint.ts.get))

        val alertsToEmit = gts.points.filter(_.value == GTSBooleanValue(false)).map { alertPoint =>
          Alert(
            ownerId = UUID.fromString(gts.labels("owner_id")),
            date = tsToLocalDateTime(alertPoint.ts.get),
            message = s"${gts.classname} - ${gts.labels.toString}"
          )
        }.toList

        pushAlerts(alertsToEmit).map { _ match {
          case Left(e) => Logger.error(e); pushAlerts(alertsToEmit)
          case Right(_) => markAsNotified(writeToken, gts) ; Logger.info("Marked as notified.")
        }}
      }
    }
  }

  case class SlackWebhookPayload(text: String)
  implicit val slackWebhookPayloadWriter = Json.writes[SlackWebhookPayload]

  private def pushAlerts(alerts: List[Alert]): Future[Either[String, Unit]] = {
    wsClient
      .url("https://hooks.slack.com/services/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")
      .withHttpHeaders("Content-Type" -> "application/json")
      .post(Json.toJson(SlackWebhookPayload(alerts.map { alert =>
        s"${alert.date.toString}: ${alert.message} to ${alert.ownerId.toString}."
      }.mkString("\n"))))
      .map { response =>
        if (response.status >= 200 && response.status <= 299) Right(())
        else                                                  Left(s"${response.status} - ${response.body}.")
      }
  }

  private def markAsNotified(writeToken: Token, gts: GTS) = ???

  private def tsToLocalDateTime(ts: Long): LocalDateTime = {
    LocalDateTime.ofInstant(
      Instant.ofEpochMilli(ts / 1000), // get point timestamp and remove useless nanos precision
      ZoneId.of("UTC")
    )
  }
}
