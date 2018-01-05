name := "js2"

version := "0.1"

scalaVersion := "2.11.11"

val akkaV = "2.4.20"
val akkaHttpV = "10.0.11"
val specsV = "3.8.4"
val slf4jV = "1.7.25"

libraryDependencies ++= Seq(
  "org.apache.httpcomponents" % "httpclient" % "4.5.2",
  "org.apache.httpcomponents" % "httpasyncclient" % "4.1.2",
  "com.typesafe.akka" %% "akka-http" % akkaHttpV,
  "org.apache.commons" % "commons-pool2" % "2.4.2",
  "com.typesafe.akka" %% "akka-actor" % akkaV,
  "org.slf4j" % "slf4j-api" % slf4jV,
  "org.slf4j" % "slf4j-simple" % slf4jV,
  "org.specs2" %% "specs2-core" % specsV % Test,
  "com.github.tomakehurst" % "wiremock" % "2.13.0" % Test,
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.0" % "test"
)
