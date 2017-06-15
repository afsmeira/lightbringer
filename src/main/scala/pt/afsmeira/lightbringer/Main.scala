package pt.afsmeira.lightbringer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.stream.ActorMaterializer
import pt.afsmeira.lightbringer.model.{Card, Deck}
import pt.afsmeira.lightbringer.setup.Settings
import pt.afsmeira.lightbringer.utils.AGoTProtocol._
import pt.afsmeira.lightbringer.utils.{ConnectionUtils, FileUtils}
import scopt.OptionParser
import spray.json._

import scala.concurrent.Await
import scala.util.Try

object Main extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import materializer.executionContext

  implicit val cardProtocol = CardProtocol(Settings.Meta)
  val parser = new OptionParser[LightbringerArguments]("lightbringer") {
    opt[String]('o', "out")
      .action((o, args) => args.copy(outputToFile = Some(o)))
      .valueName("<file>")
      .text("Output Lightbringer analysis to a file")

    opt[Unit]('s', "setup-hands")
      .action((_, args) => args.copy(setupHandsReport = true))
      .text("Include each setup hand in output")

    arg[String]("deckId")
      .required()
      .action((d, args) => args.copy(deckId = d))
      .text("Identifier of the deck to analyse")
  }

  parser.parse(args, LightbringerArguments()) match {
    case Some(arguments) =>

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

        println(s"Fetching decklist ${arguments.deckId}...")
        Await.ready(
          ConnectionUtils.requestCards[Deck](s"https://thronesdb.com/api/public/decklist/${arguments.deckId}"),
          ConnectionUtils.RequestTimeout
        ).value.get
      }

      deckTry.foreach { deck =>
        val deckReport = deck.fullReport(arguments.setupHandsReport)

        arguments.outputToFile match {
          case Some(file) => FileUtils.writeReport(deckReport, file)
          case None => println(deckReport)
        }
      }
      deckTry.failed.foreach { e =>
        println(e.printStackTrace())
      }

      Http().shutdownAllConnectionPools() andThen { case _ => system.terminate() }

    case None =>
      Http().shutdownAllConnectionPools() andThen { case _ => system.terminate() }
  }

  case class LightbringerArguments(
    outputToFile: Option[String] = None,
    setupHandsReport: Boolean = false,
    deckId: String = ""
  )
}
