package pt.afsmeira.agotlcg.utils

import pt.afsmeira.agotlcg.model._
import spray.json._

object AGoTProtocol extends DefaultJsonProtocol {

  // This object is not just a JSON Reader because the Unmarshal.to() method requires a full JSON format
  implicit object CardProtocol extends RootJsonFormat[Card] {
    override def read(json: JsValue): Card = {

      def readAgenda(json: JsValue) = Agenda(
        fromField[String](json, "name"),
        fromField[Option[String]](json, "traits").map(_.split('.').map(_.trim).toList),
        fromField[Option[String]](json, "flavor"),
        fromField[Option[Int]](json, "deck_limit").getOrElse(3),
        fromField[String](json, "code"),
        fromField[Int](json, "position"),
        fromField[Pack](json, "pack_name")
      )

      def readAttachment(json: JsValue) = Attachment(
        fromField[String](json, "name"),
        fromField[Option[String]](json, "traits").map(_.split('.').map(_.trim).toList),
        fromField[Option[String]](json, "flavor"),
        fromField[Option[Int]](json, "deck_limit").getOrElse(3),
        fromField[String](json, "code"),
        fromField[Int](json, "position"),
        fromField[Pack](json, "pack_name"),
        fromField[Option[Int]](json, "cost").map(_.toString).getOrElse("X"),
        fromField[Option[Int]](json, "cost").getOrElse(0),
        fromField[Faction](json, "faction_name"),
        fromField[Boolean](json, "is_loyal"),
        fromField[Boolean](json, "is_unique")
      )

      def readCharacter(json: JsValue) = Character(
        fromField[String](json, "name"),
        fromField[Option[String]](json, "traits").map(_.split('.').map(_.trim).toList),
        fromField[Option[String]](json, "flavor"),
        fromField[Option[Int]](json, "deck_limit").getOrElse(3),
        fromField[String](json, "code"),
        fromField[Int](json, "position"),
        fromField[Pack](json, "pack_name"),
        fromField[Option[Int]](json, "cost").map(_.toString).getOrElse("X"),
        fromField[Option[Int]](json, "cost").getOrElse(0),
        fromField[Faction](json, "faction_name"),
        fromField[Boolean](json, "is_loyal"),
        fromField[Boolean](json, "is_unique"),
        fromField[Boolean](json, "is_military"),
        fromField[Boolean](json, "is_intrigue"),
        fromField[Boolean](json, "is_power"),
        fromField[Int](json, "strength")
      )

      def readEvent(json: JsValue) = Event(
        fromField[String](json, "name"),
        fromField[Option[String]](json, "traits").map(_.split('.').map(_.trim).toList),
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

      def readLocation(json: JsValue) = Location(
        fromField[String](json, "name"),
        fromField[Option[String]](json, "traits").map(_.split('.').map(_.trim).toList),
        fromField[Option[String]](json, "flavor"),
        fromField[Option[Int]](json, "deck_limit").getOrElse(3),
        fromField[String](json, "code"),
        fromField[Int](json, "position"),
        fromField[Pack](json, "pack_name"),
        fromField[Option[Int]](json, "cost").map(_.toString).getOrElse("X"),
        fromField[Option[Int]](json, "cost").getOrElse(0),
        fromField[Faction](json, "faction_name"),
        fromField[Boolean](json, "is_loyal"),
        fromField[Boolean](json, "is_unique")
      )

      def readPlot(json: JsValue) = Plot(
        fromField[String](json, "name"),
        fromField[Option[String]](json, "traits").map(_.split('.').map(_.trim).toList),
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
        fromField[Option[String]](json, "traits").map(_.split('.').map(_.trim).toList),
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
    override def write(obj: Deck): JsValue = JsObject.empty

    override def read(json: JsValue): Deck = {
      val faction   = fromField[Faction](json, "faction_name")
      val agenda    = fromField[Option[String]](json, "agenda_code").map(cardMap(_).asInstanceOf[Agenda])
      val cards = fromField[Map[String, Int]](json, "slots").collect {
        case (code, copies) if !agenda.exists(_.code == code) => List.fill(copies)(cardMap(code))
      }.flatten.toList

      Deck(faction, agenda, cards)
    }
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
