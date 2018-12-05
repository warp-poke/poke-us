package global

import javax.inject._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}

import play.api._
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.libs.ws.WSClient

import akka.actor.{ActorSystem, ActorRef}
import com.google.inject.AbstractModule

import actors.AlertAgent
import actors.AlertAgent.Tick
import actors.StateAgent
import actors.StateAgent._
import models.Token
import models.TokenInstances._
import utils.Config

@Singleton
class Global @Inject() (
  @Named("alertAgent") alertAgent: ActorRef,
  @Named("stateAgent") stateAgent: ActorRef,
  actorSystem: ActorSystem,
  app: Application,
  configuration: Config,
  wsClient: WSClient
)(implicit ec: ExecutionContext) {
  Logger.info(s"STARTING POKE-US...")
  // refresh read token each `fetchReachTokenInterval.minutes`
  actorSystem.scheduler.schedule(1.seconds, configuration.pokeUs.fetchReadTokenInterval.minutes) {
    wsClient
      .url(configuration.pokeAPI.readTokenEndpoint)
      .addHttpHeaders("Accept" -> "application/json")
      .addHttpHeaders("Authorization" -> configuration.pokeAPI.internalAuthToken)
      .get
      .map(response => stateAgent ! StateAgent.SetReadToken((response.json).as[Token]))
  }

  // refresh write token each `fetchWriteTokenInterval.minutes`
  actorSystem.scheduler.schedule(1.seconds, configuration.pokeUs.fetchWriteTokenInterval.minutes) {
    wsClient
      .url(configuration.pokeAPI.readTokenEndpoint)
      .addHttpHeaders("Accept" -> "application/json")
      .addHttpHeaders("Authorization" -> configuration.pokeAPI.internalAuthToken)
      .get
      .map(response => stateAgent ! StateAgent.SetWriteToken((response.json).as[Token]))
  }

  // fetch alerts and manage them each `fetchAlertsInterval.seconds`
  actorSystem.scheduler.schedule(4.seconds, configuration.pokeUs.fetchAlertsInterval.seconds) {
    alertAgent ! Tick
  }

  Logger.info(s"STARTED.")
}

class GlobalModule extends AbstractModule with AkkaGuiceSupport {
  def configure() = {
    bindActor[AlertAgent]("alertAgent")
    bindActor[StateAgent]("stateAgent")
    bind(classOf[Global]).asEagerSingleton
  }
}
