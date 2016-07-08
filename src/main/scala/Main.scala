
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.{KeyManager, X509TrustManager, SSLContext}

import akka.actor.ActorSystem
import akka.http.scaladsl.{HttpsConnectionContext, Http}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
//import akka.http.scaladsl.unmarshalling._
import akka.stream.ActorMaterializer

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {

	implicit val system = ActorSystem()
	implicit val materializer = ActorMaterializer()

	import materializer.executionContext
	import CardProtocol._

  val trustfulSslContext: SSLContext = {
    object WideOpenX509TrustManager extends X509TrustManager {
      override def checkClientTrusted(chain: Array[X509Certificate], authType: String) = ()
      override def checkServerTrusted(chain: Array[X509Certificate], authType: String) = ()
      override def getAcceptedIssuers = Array[X509Certificate]()
    }

    val context = SSLContext.getInstance("TLS")
    context.init(Array[KeyManager](), Array(WideOpenX509TrustManager), null)
    context
  }

	private val context = new HttpsConnectionContext(trustfulSslContext)

	println("DO REQUEST")
	val cardMap = Await.result(Http().singleRequest(HttpRequest(uri = "https://thronesdb.com/api/public/cards/"), context)
    .flatMap(r => Unmarshal(r.entity).to[List[Card]])
    .map(_.groupBy(_.code).mapValues(_.head)), 1 minute)

//    .result(1 minute)

	val deck = Await.result(Http().singleRequest(HttpRequest(uri = "https://thronesdb.com/api/public/decklist/1"), context)
		.flatMap(r => Unmarshal(r.entity).to[Deck](DeckProtocol(cardMap), implicitly, implicitly)), 1 minute)
//		.result(1 minute)

  println("Deck: " + deck.cards.mkString("\n"))
  println("AVG CHAR COST " + deck.averageCharacterCost)
  println("CHAR COST STATS " + deck.characterCostStats)
	println("SAMPLE HAND " + deck.sampleHand)
}
