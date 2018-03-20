package actors

import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.UUID
import javax.inject._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

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
import utils.Config

object AlertAgent {
  case object Tick

  sealed trait AlertAgentError
  case object TokenUndefined extends AlertAgentError
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
    case State(None, None) => Logger.error(TokenUndefined.toString)
    case State(Some(token), None) => {
      fetchAlert(
        token,
        LocalDateTime.now.minusMinutes(50000) // fetch huge period to start
      )
    }
    case State(Some(token), Some(lastAlertDate)) => {
      fetchAlert(token, lastAlertDate)
    }
  }

  private def fetchAlert(token: Token, lastAlertDate: LocalDateTime) = {
    warpClient.fetch(
      token.token,
      Query(
        Selector("alert.http.status.owner"),
        FetchRange(lastAlertDate, LocalDateTime.now)
      )
    ).map { gtsList =>
      gtsList.map { gts =>
        stateAgent ! StateAgent.SetLastAlertDate(tsToLocalDateTime(gts.mostRecentPoint.ts.get))

        val alertsToEmit = gts.points.filter(_.value == GTSBooleanValue(true)).map { alertPoint =>
          Alert(
            ownerId = UUID.fromString(gts.labels("ownerId")),
            date = tsToLocalDateTime(alertPoint.ts.get),
            message = "alert.http.status.owner" // to update with label value?
          )
        }.toList

        pushAlerts(alertsToEmit)
      }
    }
  }

  private def pushAlerts(alerts: List[Alert]) = alerts.map(pushAlert(_))

  private def pushAlert(alert: Alert) = ???

  private def tsToLocalDateTime(ts: Long): LocalDateTime = {
    LocalDateTime.ofInstant(
      Instant.ofEpochMilli(ts / 1000), // get point timestamp and remove useless nanos precision
      ZoneId.of("UTC")
    )
  }
}
