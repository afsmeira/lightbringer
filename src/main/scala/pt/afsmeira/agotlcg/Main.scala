package pt.afsmeira.agotlcg

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.stream.ActorMaterializer
import pt.afsmeira.agotlcg.model.{Card, Deck}
import pt.afsmeira.agotlcg.utils.AGoTProtocol._
import pt.afsmeira.agotlcg.utils.{ConnectionUtils, FileUtils}
import spray.json._

object Main extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val cardMap = if (FileUtils.validCardsFile) {
    FileUtils.readCardsFile.parseJson.convertTo[List[Card]].groupBy(_.code).mapValues(_.head)
  } else {
    val cards = ConnectionUtils.requestCards[String]("https://thronesdb.com/api/public/cards/")
    FileUtils.writeCardsFile(cards)
    cards.parseJson.convertTo[Seq[Card]].groupBy(_.code).mapValues(_.head)
  }

  implicit val deckProtocol = DeckProtocol(cardMap)

  val deck = ConnectionUtils.requestCards[Deck]("https://thronesdb.com/api/public/decklist/5993")
  println(deck.fullReport)
}
