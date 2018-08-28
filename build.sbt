name := """poke-us"""

organization := "com.clever-cloud"

version := "1.0.0"

scalaVersion := "2.12.6"

resolvers += "Clever Cloud" at "http://maven.clever-cloud.com/"
resolvers += "clevercloud-bintray" at "http://dl.bintray.com/clevercloud/maven"
resolvers += "cityzendata-bintray" at "http://dl.bintray.com/cityzendata/maven"
resolvers += "hbs-bintray" at "http://dl.bintray.com/hbs/maven"
resolvers += Resolver.bintrayRepo("cakesolutions", "maven")

libraryDependencies ++= Seq(
  evolutions,
  guice,
  jdbc,
  ws,
  "com.typesafe.play" %% "anorm" % "2.5.3",
  "org.postgresql" % "postgresql" % "42.1.4",
  "io.warp10" % "token" % "1.0.10-29-gd8b6b0d",
  "com.github.nitram509" % "jmacaroons" % "0.3.1",
  "de.mkammerer" % "argon2-jvm" % "2.3",
  "net.cakesolutions" %% "scala-kafka-client-akka" % "1.0.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
  "com.clevercloud" %% "akka-warp10-scala-client" % "2.5.12_10.1.1_1.0.2",
  "name.delafargue" %% "anorm-pg-entity" % "0.1.0-SNAPSHOT",
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
