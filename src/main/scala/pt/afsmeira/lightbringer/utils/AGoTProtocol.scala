package pt.afsmeira.lightbringer.utils

import pt.afsmeira.lightbringer.model._
import pt.afsmeira.lightbringer.setup.MetaSettings
import spray.json._

object AGoTProtocol extends DefaultJsonProtocol {

  // This object is not just a JSON Reader because the Unmarshal.to() method requires a full JSON format
  case class CardProtocol(settings: MetaSettings) extends RootJsonFormat[Card] {
    override def read(json: JsValue): Card = {

      val LimitedKeyword : String = "Limited."
      val TerminalKeyword: String = "Terminal."

      def readIncome(text: Option[String]): Option[Int] = {
        /**
          * This pattern will match a `+` or a `-` followed by a single digit and then the word `Income`.
          * Before or after this group there can be any number of characters, including line breaks.
          */
        val IncomePattern = """((.|\n|\r)*)((\+|\-)\d) Income((.|\n)*)""".r

        text.flatMap {
          case IncomePattern(_, _, income, _, _, _) => Some(income.toInt)
          case _ => None
        }
      }

      def readBestow(text: Option[String]): Option[Int] = {
        /**
          * This pattern will the word `Bestow` followed by a number within parenthesis.
          * Before or after this group there can be any number of characters, including line breaks.
          */
        val BestowPattern = """((.|\n|\r)*)Bestow \((\d)\)((.|\n)*)""".r

        text.flatMap {
          case BestowPattern(_, _, bestow, _, _) => Some(bestow.toInt)
          case _ => None
        }
      }

      def readAttachmentRestrictions(text: Option[String]): Attachment.Restrictions = {
        /**
          * This pattern will match any number of letters between tags `<i>` and `</i>`.
          */
        val traitsPattern = """<i>([A-z]*)<\/i>""".r
        val firstLine = text.flatMap(_.split("\n").headOption)

        val terminal = firstLine.exists(_.contains(TerminalKeyword))
        val unique   = firstLine.exists(_.toLowerCase.contains("unique"))
        val opponent = firstLine.exists(_.toLowerCase.contains("opponent"))
        val faction  = firstLine.flatMap { line =>
          Faction.values.find(_.keyword.exists(line.contains))
        }
        val traits = for {
          line       <- firstLine.toSeq
          matchBlock <- traitsPattern.findAllMatchIn(line)
        } yield matchBlock.group(1)

        Attachment.Restrictions(terminal, unique, opponent, faction, traits)
      }

      def readAgenda(json: JsValue) = Agenda(
        fromField[String](json, "name"),
        fromField[String](json, "traits").split('.').map(_.trim).filter(!_.isEmpty),
        fromField[Option[String]](json, "flavor"),
        fromField[Option[Int]](json, "deck_limit").getOrElse(3),
        fromField[String](json, "code"),
        fromField[Int](json, "position"),
        fromField[Pack](json, "pack_name")
      )

      def readAttachment(json: JsValue) = {
        val name   = fromField[String](json, "name")
        val code   = fromField[String](json, "code")
        val text   = fromField[Option[String]](json, "text")
        val income = readIncome(text)

        Attachment(
          name,
          fromField[String](json, "traits").split('.').map(_.trim).filter(!_.isEmpty),
          fromField[Option[String]](json, "flavor"),
          fromField[Option[Int]](json, "deck_limit").getOrElse(3),
          code,
          fromField[Int](json, "position"),
          fromField[Pack](json, "pack_name"),
          fromField[Option[Int]](json, "cost").map(_.toString).getOrElse("X"),
          fromField[Option[Int]](json, "cost").getOrElse(0),
          fromField[Faction](json, "faction_name"),
          fromField[Boolean](json, "is_loyal"),
          fromField[Boolean](json, "is_unique"),
          text.exists(_.contains(LimitedKeyword)),
          income.exists(_ > 0) || settings.economyCards.contains(code) || settings.economyCards.contains(name),
          income,
          readBestow(text),
          readAttachmentRestrictions(text)
        )
      }

      def readCharacter(json: JsValue) = {
        val name   = fromField[String](json, "name")
        val code   = fromField[String](json, "code")
        val text   = fromField[Option[String]](json, "text")
        val income = readIncome(text)

        Character(
          name,
          fromField[String](json, "traits").split('.').map(_.trim).filter(!_.isEmpty),
          fromField[Option[String]](json, "flavor"),
          fromField[Option[Int]](json, "deck_limit").getOrElse(3),
          code,
          fromField[Int](json, "position"),
          fromField[Pack](json, "pack_name"),
          fromField[Option[Int]](json, "cost").map(_.toString).getOrElse("X"),
          fromField[Option[Int]](json, "cost").getOrElse(0),
          fromField[Faction](json, "faction_name"),
          fromField[Boolean](json, "is_loyal"),
          fromField[Boolean](json, "is_unique"),
          text.exists(_.contains(LimitedKeyword)),
          income.exists(_ > 0) || settings.economyCards.contains(code) || settings.economyCards.contains(name),
          income,
          readBestow(text),
          fromField[Boolean](json, "is_military"),
          fromField[Boolean](json, "is_intrigue"),
          fromField[Boolean](json, "is_power"),
          fromField[Int](json, "strength")
        )
      }

      def readEvent(json: JsValue) = Event(
        fromField[String](json, "name"),
        fromField[String](json, "traits").split('.').map(_.trim).filter(!_.isEmpty),
        fromField[Option[String]](json, "flavor"),
        fromField[Option[Int]](json, "deck_limit").getOrElse(3),
        fromField[String](json, "code"),
        fromField[Int](json, "position"),
        fromField[Pack](json, "pack_name"),
        fromField[Option[Int]](json, "cost").map(_.toString).getOrElse("X"),
        fromField[Option[Int]](json, "cost").getOrElse(0),
        fromField[Faction](json, "faction_name"),
        fromField[Boolean](json, "is_loyal")
      )

      def readLocation(json: JsValue) = {
        val name   = fromField[String](json, "name")
        val code   = fromField[String](json, "code")
        val text   = fromField[Option[String]](json, "text")
        val income = readIncome(text)

        Location(
          name,
          fromField[String](json, "traits").split('.').map(_.trim).filter(!_.isEmpty),
          fromField[Option[String]](json, "flavor"),
          fromField[Option[Int]](json, "deck_limit").getOrElse(3),
          code,
          fromField[Int](json, "position"),
          fromField[Pack](json, "pack_name"),
          fromField[Option[Int]](json, "cost").map(_.toString).getOrElse("X"),
          fromField[Option[Int]](json, "cost").getOrElse(0),
          fromField[Faction](json, "faction_name"),
          fromField[Boolean](json, "is_loyal"),
          fromField[Boolean](json, "is_unique"),
          text.exists(_.contains(LimitedKeyword)),
          income.exists(_ > 0) || settings.economyCards.contains(code) || settings.economyCards.contains(name),
          income,
          readBestow(text)
        )
      }

      def readPlot(json: JsValue) = Plot(
        fromField[String](json, "name"),
        fromField[String](json, "traits").split('.').map(_.trim).filter(!_.isEmpty),
        fromField[Option[String]](json, "flavor"),
        fromField[Option[Int]](json, "deck_limit").getOrElse(3),
        fromField[String](json, "code"),
        fromField[Int](json, "position"),
        fromField[Pack](json, "pack_name"),
        fromField[Faction](json, "faction_name"),
        fromField[Boolean](json, "is_loyal"),
        fromField[Int](json, "income"),
        fromField[Int](json, "initiative"),
        fromField[Option[Int]](json, "claim").map(_.toString).getOrElse("X"),
        fromField[Option[Int]](json, "claim").getOrElse(0),
        fromField[Int](json, "reserve")
      )

      def readTitle(json: JsValue) = Title(
        fromField[String](json, "name"),
        fromField[String](json, "traits").split('.').map(_.trim).filter(!_.isEmpty),
        fromField[Option[String]](json, "flavor"),
        fromField[Option[Int]](json, "deck_limit").getOrElse(3),
        fromField[String](json, "code"),
        fromField[Int](json, "position"),
        fromField[Pack](json, "pack_name")
      )

      fromField[CardType](json, "type_name") match {
        case CardType.Agenda     => readAgenda(json)
        case CardType.Attachment => readAttachment(json)
        case CardType.Character  => readCharacter(json)
        case CardType.Event      => readEvent(json)
        case CardType.Location   => readLocation(json)
        case CardType.Plot       => readPlot(json)
        case CardType.Title      => readTitle(json)
      }
    }

    override def write(obj: Card): JsValue = JsObject.empty
  }

  case class DeckProtocol(cardMap: Map[String, Card]) extends RootJsonFormat[Deck] {
    override def read(json: JsValue): Deck = {
      val faction = fromField[Faction](json, "faction_name")
      val agenda  = fromField[Option[String]](json, "agenda_code").map(cardMap(_).asInstanceOf[Agenda])
      val cards   = fromField[Map[String, Int]](json, "slots").collect {
        case (code, copies) if !agenda.exists(_.code == code) => List.fill(copies)(cardMap(code))
      }.flatten.toSeq
      val name = fromField[Option[String]](json, "name")

      Deck(faction, agenda, cards, name)
    }

    override def write(obj: Deck): JsValue = JsObject.empty
  }

  implicit val cardTypeReader: JsonReader[CardType] = JsonReader.func2Reader[CardType] { json =>
    CardType.values.find(_.toString == json.convertTo[String]).get
  }
  implicit val packReader: JsonReader[Pack] = JsonReader.func2Reader[Pack] { json =>
    Pack.values.find(_.name == json.convertTo[String]).get
  }
  implicit val factionReader: JsonReader[Faction] = JsonReader.func2Reader[Faction] { json =>
    Faction.values.find(_.name == json.convertTo[String]).get
  }
}
