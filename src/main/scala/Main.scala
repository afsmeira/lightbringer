
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling._
import akka.stream.ActorMaterializer

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {

	implicit val system = ActorSystem()
	implicit val materializer = ActorMaterializer()

	import materializer.executionContext
	import CardProtocol._

	println("DO REQUEST")
	val cardMap = Await.result(Http().singleRequest(HttpRequest(uri = "http://thronesdb.com/api/public/cards/"))
    .flatMap(r => Unmarshal(r.entity).to[Array[Card]])
    .map(_.groupBy(_.code).mapValues(_.head)), 1 minute)

//    .result(1 minute)

	val deck = Await.result(Http().singleRequest(HttpRequest(uri = "http://thronesdb.com/api/public/decklist/1"))
		.flatMap(r => Unmarshal(r.entity).to[RawDeck])
		.map(Deck.fromRawDeck(_, cardMap)), 1 minute)
//		.result(1 minute)

  println("AVG CHAR COST " + deck.averageCharacterCost)
  println("CHAR COST STATS " + deck.characterCostStats)
	println("SAMPLE HAND " + deck.sampleHand)
}
