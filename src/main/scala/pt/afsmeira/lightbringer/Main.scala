package pt.afsmeira.lightbringer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.stream.ActorMaterializer
import pt.afsmeira.lightbringer.model.{Card, Deck}
import pt.afsmeira.lightbringer.setup.{Settings, SetupAnalyzer}
import pt.afsmeira.lightbringer.utils.AGoTProtocol._
import pt.afsmeira.lightbringer.utils.{ConnectionUtils, FileUtils}
import spray.json._

import scala.concurrent.Await
import scala.util.Try

object Main extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import materializer.executionContext

  implicit val cardProtocol = CardProtocol(Settings.Meta)
  val decklistId = args(0)

  val cardMapTry = if (FileUtils.validCardsFile) {
    Try(FileUtils.readCardsFile.parseJson.convertTo[Seq[Card]].groupBy(_.code).mapValues(_.head))
  } else {
    println("Local card information is outdated or inexistent. Fetching from remote...")
    Await.ready(
      ConnectionUtils.requestCards[String]("https://thronesdb.com/api/public/cards/"),
      ConnectionUtils.RequestTimeout
    ).value.get.map { cards =>
      FileUtils.writeCardsFile(cards)
      cards.parseJson.convertTo[Seq[Card]].groupBy(_.code).mapValues(_.head)
    }
  }

  val deckTry = cardMapTry.flatMap { cardMap =>
    implicit val deckProtocol = DeckProtocol(cardMap)

    println(s"Fetching decklist $decklistId...")
    Await.ready(
      ConnectionUtils.requestCards[Deck](s"https://thronesdb.com/api/public/decklist/$decklistId"),
      ConnectionUtils.RequestTimeout
    ).value.get
  }

  deckTry.foreach(deck => println(deck.fullReport))
  deckTry.failed.foreach { e =>
    println(s"Decklist with ID $decklistId was not able to be retrieved. Are you sure it exists?")
  }

  Http().shutdownAllConnectionPools() andThen { case _ => system.terminate() }
}
