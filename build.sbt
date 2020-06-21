name := "onvif-camera-snapshot-taker"

version := "1.0"

scalaVersion := "2.12.3"

resolvers += "Adobe" at "https://repo.adobe.com/nexus/content/repositories/public/"

unmanagedJars in Compile += file("libs/onvif-2016-03-16.jar")
unmanagedJars in Compile += file("libs/jdring-2.0.jar")

val akkaVersion = "2.5.4"

libraryDependencies += "com.typesafe" % "config" % "1.4.0"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.3"
libraryDependencies += "org.apache.httpcomponents" % "httpmime" % "4.5.3"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % "2.9.0"
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.0"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.0"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"
libraryDependencies += "commons-io" % "commons-io" % "2.5"
libraryDependencies += "org.apache.commons" % "commons-imaging" % "1.0-R1534292"
libraryDependencies += "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0"
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.6"
libraryDependencies += "com.github.hipjim" %% "scala-retry" % "0.2.2"
libraryDependencies += "com.coreoz" % "wisp" % "1.0.0"
libraryDependencies += "com.cronutils" % "cron-utils" % "6.0.2"
libraryDependencies += "com.jsuereth" %% "scala-arm" % "2.0"
libraryDependencies += "org.slf4j" % "jcl-over-slf4j" % "1.7.25"

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
dockerRepository := Some("nowicki.azurecr.io")
dockerUsername := Some("nowicki")
dockerExposedVolumes := Seq("/data")
dockerUpdateLatest := true
dockerBaseImage := "adoptopenjdk/openjdk11:debianslim-jre"

javaOptions in Universal ++= Seq(
  "-J-Xmx128m",
  "-J-Xms128m",
  "-Dconfig.override_with_env_vars=true",
  "-Dconfig.file=/data/onvif-camera-snapshot-taker/config.config"
)

import com.typesafe.sbt.packager.docker._
dockerCommands ++= Seq(
  Cmd("USER", "root"),
  Cmd("RUN apt-get update"),
  Cmd("RUN apt-get install -y software-properties-common"),
  Cmd("RUN add-apt-repository ppa:mc3man/trusty-media "),
  Cmd("RUN apt-get update || true"),
  Cmd("RUN apt-get install -y ffmpeg"),
  Cmd("RUN apt-get install -y frei0r-plugins"),
  Cmd("USER", "daemon")
)