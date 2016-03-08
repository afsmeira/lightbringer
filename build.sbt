scalaVersion := "2.11.6"

name := "agot"

libraryDependencies ++= Seq(
	"com.typesafe.akka" %% "akka-http-experimental" % "2.0-M2",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.0-M2"
)
