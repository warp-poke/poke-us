package actors

import java.time.LocalDateTime
import javax.inject._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

import play.api.libs.ws._

import akka._
import akka.actor._
import akka.stream.ActorMaterializer

import models.State
import models.Token

object StateAgent {
  sealed trait StateAgentMessage
  case class SetToken(token: Token) extends StateAgentMessage
  case class SetLastAlertDate(date: LocalDateTime) extends StateAgentMessage
  case object GetState extends StateAgentMessage
  case class SetState(state: State) extends StateAgentMessage
}

class StateAgent @Inject()() extends Actor {
  import StateAgent._

  def receive = active(State(None, None))

  def active(state: State): Receive = {
    case SetToken(token: Token) => context.become(active(State(Some(token), state.lastAlertDate)))
    case SetLastAlertDate(date: LocalDateTime) => {
      val dateToSet = state.lastAlertDate match {
        case None => date
        case Some(currentDate) => {
          if (date isAfter currentDate) date
          else                          currentDate
        }
      }
      context.become(active(State(state.token, Some(dateToSet))))
    }
    case GetState => sender ! state
    case SetState(state: State) => context.become(active(state))
  }
}
