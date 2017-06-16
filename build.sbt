scalaVersion := "2.11.8"

name         := "lightbringer"
organization := "pt.afsmeira"
version      := "1.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.typesafe.akka"          %% "akka-http"            % "10.0.7",
  "com.typesafe.akka"          %% "akka-http-spray-json" % "10.0.7",
  "com.typesafe"               %  "config"               % "1.3.1",
  "com.github.scopt"           %% "scopt"                % "3.6.0",
  "ch.qos.logback"             %  "logback-classic"      % "1.1.7",
  "com.typesafe.scala-logging" %% "scala-logging"        % "3.5.0"
)

mainClass       in assembly := Some("pt.afsmeira.lightbringer.Main")
assemblyJarName in assembly := "Lightbringer.jar"
