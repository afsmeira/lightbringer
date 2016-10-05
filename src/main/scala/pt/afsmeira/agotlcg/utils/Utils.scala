package pt.afsmeira.agotlcg.utils

import java.io.PrintWriter
import java.nio.file.{Files, Paths}
import java.security.cert.X509Certificate
import java.time.temporal.ChronoUnit
import javax.net.ssl.{KeyManager, SSLContext, X509TrustManager}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, ResponseEntity, Uri}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.http.scaladsl.{Http, HttpsConnectionContext}
import akka.stream.Materializer

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

object ConnectionUtils {

  private object WideOpenX509TrustManager extends X509TrustManager {
    override def checkClientTrusted(chain: Array[X509Certificate], authType: String) = ()
    override def checkServerTrusted(chain: Array[X509Certificate], authType: String) = ()
    override def getAcceptedIssuers = Array[X509Certificate]()
  }

  private val WideOpenConnectionContext: HttpsConnectionContext = {
    val trustfulSSLContext: SSLContext = SSLContext.getInstance("TLS")
    trustfulSSLContext.init(Array[KeyManager](), Array(WideOpenX509TrustManager), null)

    new HttpsConnectionContext(trustfulSSLContext)
  }

  val RequestTimeout = 1 minute

  def requestCards[T](uri: Uri)(
    implicit unmarshaller: Unmarshaller[ResponseEntity, T],
    actorSystem: ActorSystem,
    materializer: Materializer
  ): T = {
    import materializer.executionContext

    Await.result(
      Http().singleRequest(
        HttpRequest(uri = uri),
        WideOpenConnectionContext
      ).flatMap { response =>
        Unmarshal(response.entity).to[T]
      },
      RequestTimeout
    )
  }
}

object FileUtils {

  val CardsFile = ".cards.json"

  def validCardsFile: Boolean = {
    val path = Paths.get(CardsFile)
    val instant = java.time.Instant.now().minus(20, ChronoUnit.DAYS)

    Files.exists(path) && Files.getLastModifiedTime(path).toInstant.isAfter(instant)
  }

  def writeCardsFile(cards: String): Unit = new PrintWriter(CardsFile) {
    try {
      println(cards)
    } finally {
      close()
    }
  }

  def readCardsFile: String = Source.fromFile(CardsFile).mkString
}