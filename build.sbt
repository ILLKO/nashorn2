name := "js2"

version := "0.1"

scalaVersion := "2.11.11"

val akkaV = "2.4.20"
val sttpV = "1.1.4"
//val akkaHttpV = "10.0.11"
val specsV = "3.8.4"
val slf4jV = "1.7.25"
val sprayV = "1.3.4"

libraryDependencies ++= Seq(
  "com.softwaremill.sttp" %% "core" % sttpV,
  "com.softwaremill.sttp" %% "akka-http-backend" % sttpV,
  "com.softwaremill.sttp" %% "async-http-client-backend-future" % "1.1.3",
  "com.softwaremill.sttp" %% "okhttp-backend" % "1.1.3",

  "io.spray" %% "spray-client" % sprayV,
  "io.spray" %% "spray-routing" % sprayV,

//  "com.typesafe.akka" %% "akka-http" % akkaHttpV,
  "org.apache.commons" % "commons-pool2" % "2.4.2",
  "com.typesafe.akka" %% "akka-actor" % akkaV,
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.7.5",
  "org.slf4j" % "slf4j-api" % slf4jV,
  "org.slf4j" % "slf4j-simple" % slf4jV,
  "org.specs2" %% "specs2-core" % specsV % Test,
  "com.github.tomakehurst" % "wiremock" % "2.13.0" % Test,
  "com.google.guava" % "guava" % "21.0" % Test,
  "com.github.blemale" %% "scaffeine" % "2.1.0" % Test,

    "com.storm-enroute" %% "scalameter" % "0.9" % Test

//  "io.gatling" %% "gatling-core" % "2.3.0",
//  "io.gatling" %% "gatling-http" % "2.3.0",
//  "io.gatling" %% "gatling-app" % "2.3.0"

  //  "net.databinder.dispatch" %% "dispatch-core" % "0.11.0" % "test",
//  "org.apache.httpcomponents" % "httpclient" % "4.5.2",
//  "org.apache.httpcomponents" % "httpasyncclient" % "4.1.2"
)
