scalaVersion := "2.11.8"

name := "lightbringer"

libraryDependencies ++= Seq(
	"com.typesafe.akka" %% "akka-http-experimental"            % "2.4.11",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.11",
  "com.typesafe"      %  "config"                            % "1.3.1"
)
