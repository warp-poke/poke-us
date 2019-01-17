package actors

import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.UUID
import javax.inject._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
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
import _root_.models.Hook
import _root_.models.Hook._
import _root_.models.HookInstances._
import _root_.models.State
import _root_.models.Token
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
  actorSystem: ActorSystem,
  @Named("stateAgent") stateAgent: ActorRef,
  configuration: Config,
  implicit val ec: ExecutionContext,
  wsClient: WSClient
) extends Actor {
  import AlertAgent._

  implicit val executionContext = actorSystem.dispatchers.lookup("alertAgent")
  implicit val actorMaterializer = ActorMaterializer()
  implicit val warpConfiguration = WarpConfiguration("https://" + configuration.warp10.host)
  val warpClient = WarpClient(
    configuration.warp10.host,
    configuration.warp10.port,
    "https"
  )(warpConfiguration, actorSystem, actorMaterializer)

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

    val selector = "~alert.http.status{}"

    warpClient.exec(
      List(
        Checks.last(
          token = readToken.token,
          selector = selector,
          duration = "15 m",
          value = "false",
          op = "eq",
          labels = Some("[ 'zone' ]")),
        Checks.deltaMapper(
          token = readToken.token,
          selector = selector,
          duration = "15 m",
          range = 5,
          value = "2",
          op = "ge",
          labels = Some("[ 'zone' ]"))
      ).mkString("")
    ).map { gtsList =>
      gtsList.map { gts =>
        val alertsToEmit = gts.points.map { alertPoint =>
          Alert(
            ownerId = UUID.fromString(gts.labels("owner_id")),
            date = tsToLocalDateTime(alertPoint.ts.get),
            message = s"${gts.classname} - ${gts.labels.toString}"
          )
        }.toList

        // for each (ownerId, ownedAlerts), send alerts
        alertsToEmit.groupBy(_.ownerId).map { case (ownerId, ownedAlerts) => {
          getUserHooks(ownerId).map { either =>
            either match { 
              case Left(e) => Logger.error(e.toString) ; throw new Exception(e.toString)
              case Right(hooks) => hooks.map { hook =>
                send(hook, ownedAlerts).map { either =>
                  either match {
                    case Right(_) => {
                      markAsNotified(writeToken, selector) // TODO add return to check response and do the next line consequently
                      stateAgent ! StateAgent.SetLastAlertDate(tsToLocalDateTime(gts.mostRecentPoint.ts.get))
                    }
                  }
                }
              }
            }
          }
        }}
      }
    }
  }

  def getUserHooks(ownerId: UUID): Future[Either[String, List[Hook]]] = {
    wsClient
      .url(configuration.pokeAPI.baseURL + "/users/" + ownerId + "/hooks")
      .addHttpHeaders("Accept" -> "application/json")
      .addHttpHeaders("Authorization" -> configuration.pokeAPI.internalAuthToken) // missing user auth
      .get
      .map { response =>
        if (response.status >= 200 && response.status <= 299) Right(response.json.as[List[Hook]])
        else Left("${response.status.toString} - ${response.body.toString}")
      }
  }

  def send(hook: Hook, alerts: List[Alert], nbRetries: Int = 0): Future[Either[String, Unit]] = {
    if (nbRetries <= configuration.pokeUs.maxRetriesForHook) {
      wsClient
        .url(hook.webhook)
        .withHttpHeaders("Content-Type" -> "application/json")
        .post(Json.toJson(alerts.map { alert =>
          hook.template.replace("@@BODY@@", s"${alert.date.toString}: ${alert.message} to ${alert.ownerId.toString}.")
        }.mkString("\n")))
        .map { response =>
          if (response.status >= 200 && response.status <= 299) Right(())
          else {
            Logger.error(s"${response.status.toString} - ${response.body.toString}, let's retry in ${nbRetries * 1}s...")
            actorSystem.scheduler.scheduleOnce(nbRetries * 1 seconds) {
              send(hook, alerts, nbRetries + 1)
            }
            Left("Retrying")
          }
        }
    } else Future.successful(Left(s"Stopping sending for ${hook.hook_id} after ${configuration.pokeUs.maxRetriesForHook.toString} retries."))
  }

  private def markAsNotified(writeToken: Token, selector: String) = {
    warpClient.exec(_root_.models.warpscripts.query_module.Query(writeToken.token, selector, "").markAsNotified.toString)
  }

  private def tsToLocalDateTime(ts: Long): LocalDateTime = {
    LocalDateTime.ofInstant(
      Instant.ofEpochMilli(ts / 1000), // get point timestamp and remove useless nanos precision
      ZoneId.of("UTC")
    )
  }
}
