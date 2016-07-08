scalaVersion := "2.11.7"

name := "agot"

libraryDependencies ++= Seq(
	"com.typesafe.akka" %% "akka-http-experimental"            % "2.4.2",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.2"
)
