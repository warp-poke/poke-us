package utils

import java.util.UUID
import javax.inject._

import play.api.Configuration

case class Warp10Config(
  host: String,
  port: Int
)

case class PokeAPIConfig(
  baseURL: String,
  readTokenEndpoint: String,
  writeTokenEndpoint: String,
  internalAuthToken: String
)

case class PokeUsConfig(
  fetchReadTokenInterval: Int,
  fetchWriteTokenInterval: Int,
  fetchAlertsInterval: Int,
  maxRetriesForHook: Int,
  seriesToWatch: Seq[String]
)

@Singleton
class Config @Inject() (val configuration: Configuration) {
  val warp10 = Warp10Config(
    host = getString("warp10.host"),
    port = getInt("warp10.port")
  )

  val pokeAPI = PokeAPIConfig(
    baseURL = getString("pokeapi.baseURL"),
    readTokenEndpoint = getString("pokeapi.readTokenEndpoint"),
    writeTokenEndpoint = getString("pokeapi.writeTokenEndpoint"),
    internalAuthToken = getString("pokeapi.internalAuthToken")
  )

  val pokeUs = PokeUsConfig(
    fetchReadTokenInterval = getInt("pokeus.fetchReadTokenInterval"),
    fetchWriteTokenInterval = getInt("pokeus.fetchWriteTokenInterval"),
    fetchAlertsInterval = getInt("pokeus.fetchAlertsInterval"),
    maxRetriesForHook = getInt("pokeus.maxRetriesForHook"),
    seriesToWatch = configuration.get[Seq[String]]("pokeus.seriesToWatch")
  )

  private def getInt(path: String): Int = configuration.get[Int](path)
  private def getString(path: String): String = configuration.get[String](path)
}
