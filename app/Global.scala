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
  Logger.info(s"START POKE-US")
  actorSystem.scheduler.schedule(1.seconds, configuration.pokeUs.fetchReadTokenInterval.minutes) {
    wsClient
      .url(configuration.pokeAPI.readTokenEndpoint)
      .addHttpHeaders("Accept" -> "application/json")
      .addHttpHeaders("Authorization" -> configuration.pokeAPI.internalAuthToken)
      .get
      .map(response => stateAgent ! StateAgent.SetToken((response.json).as[Token]))
  }

  actorSystem.scheduler.schedule(4.seconds, configuration.pokeUs.fetchAlertsInterval.seconds) {
    alertAgent ! Tick
  }
}

class GlobalModule extends AbstractModule with AkkaGuiceSupport {
  def configure() = {
    bindActor[AlertAgent]("alertAgent")
    bindActor[StateAgent]("stateAgent")
    bind(classOf[Global]).asEagerSingleton
  }
}
