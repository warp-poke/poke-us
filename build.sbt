name := """poke-us"""

organization := "com.clever-cloud"

version := "1.0.3"

scalaVersion := "2.12.8"

resolvers += "Clever Cloud" at "http://maven.clever-cloud.com/"
resolvers += "clevercloud-bintray" at "http://dl.bintray.com/clevercloud/maven"

libraryDependencies ++= Seq(
  guice,
  jdbc,
  ws,
  "com.typesafe.play" %% "anorm" % "2.5.3",
  "com.clevercloud" %% "akka-warp10-scala-client" % "1.0.1",
  "name.delafargue" %% "anorm-pg-entity" % "0.1.0-SNAPSHOT",
  "org.postgresql" % "postgresql" % "42.1.4",
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
