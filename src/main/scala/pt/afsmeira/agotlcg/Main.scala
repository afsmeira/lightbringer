package pt.afsmeira.agotlcg


import java.io.PrintWriter
import java.nio.file.{Files, Paths}
import java.security.cert.X509Certificate
import java.time.temporal.ChronoUnit
import javax.net.ssl.{KeyManager, SSLContext, X509TrustManager}

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.{Http, HttpsConnectionContext}
import akka.stream.ActorMaterializer
import pt.afsmeira.agotlcg.model.{Card, Deck}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

object Main extends App {

	implicit val system = ActorSystem()
	implicit val materializer = ActorMaterializer()

	import materializer.executionContext
	import pt.afsmeira.agotlcg.utils.AGoTProtocol._
	import spray.json._

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

  val path = Paths.get(".cards.json")
  val instant = java.time.Instant.now().minus(20, ChronoUnit.DAYS)

  val cardMap = if (Files.exists(path) && Files.getLastModifiedTime(path).toInstant.isAfter(instant)) {
    Source.fromFile(".cards.json").mkString.parseJson.convertTo[List[Card]].groupBy(_.code).mapValues(_.head)
  } else {
    val cardsStr = Await.result(
      Http().singleRequest(HttpRequest(uri = "https://thronesdb.com/api/public/cards/"), context).flatMap { response =>
        Unmarshal(response.entity).to[String]
      }, 1 minute)

    new PrintWriter(".cards.json") {
      try {
        println(cardsStr)
      } finally {
        close()
      }
    }
    cardsStr.parseJson.convertTo[List[Card]].groupBy(_.code).mapValues(_.head)
  }

	val deck = Await.result(Http().singleRequest(HttpRequest(uri = "https://thronesdb.com/api/public/decklist/1"), context)
		.flatMap(r => Unmarshal(r.entity).to[Deck](DeckProtocol(cardMap), implicitly, implicitly)), 1 minute)

  println(deck.fullReport)
}
