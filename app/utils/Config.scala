package utils

import java.util.UUID
import javax.inject._

import play.api.Configuration

case class Warp10Config(
  host: String,
  port: Int
)

case class PokeAPIConfig(
  readTokenEndpoint: String,
  writeTokenEndpoint: String,
  internalAuthToken: String
)

case class PokeUsConfig(
  fetchReadTokenInterval: Int,
  fetchWriteTokenInterval: Int,
  fetchAlertsInterval: Int
)

@Singleton
class Config @Inject() (val configuration: Configuration) {
  val warp10 = Warp10Config(
    host = getString("warp10.host"),
    port = getInt("warp10.port")
  )

  val pokeAPI = PokeAPIConfig(
    readTokenEndpoint = getString("pokeapi.readTokenEndpoint"),
    writeTokenEndpoint = getString("pokeapi.writeTokenEndpoint"),
    internalAuthToken = getString("pokeapi.internalAuthToken")
  )

  val pokeUs = PokeUsConfig(
    fetchReadTokenInterval = getInt("pokeus.fetchReadTokenInterval"),
    fetchWriteTokenInterval = getInt("pokeus.fetchWriteTokenInterval"),
    fetchAlertsInterval = getInt("pokeus.fetchAlertsInterval")
  )

  private def getInt(path: String): Int = configuration.get[Int](path)
  private def getString(path: String): String = configuration.get[String](path)
}
