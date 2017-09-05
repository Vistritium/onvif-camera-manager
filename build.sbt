name := "onvif-camera-snapshot-taker"

version := "1.0"

scalaVersion := "2.12.3"

resolvers += "Adobe" at "https://repo.adobe.com/nexus/content/repositories/public/"

unmanagedJars in Compile += file("libs/onvif-2016-03-16.jar")
unmanagedJars in Compile += file("libs/jdring-2.0.jar")

libraryDependencies += "com.typesafe" % "config" % "1.3.1"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.3"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % "2.9.0"
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.0"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.4"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.0"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"
libraryDependencies += "commons-io" % "commons-io" % "2.5"
libraryDependencies += "org.apache.commons" % "commons-imaging" % "1.0-R1534292"

enablePlugins(JavaAppPackaging)

javaOptions in Universal ++= Seq (
  "-J-Xmx64m",
  "-J-Xms64m"
)