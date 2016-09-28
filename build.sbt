scalaVersion := "2.11.8"

name := "agot"

libraryDependencies ++= Seq(
	"com.typesafe.akka" %% "akka-http-experimental"            % "2.4.8",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.8"
)
