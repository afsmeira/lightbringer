scalaVersion := "2.11.8"

name := "lightbringer"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"            % "10.0.7",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.7",
  "com.typesafe"      %  "config"               % "1.3.1",
  "com.github.scopt"  %% "scopt"                % "3.6.0"
)
