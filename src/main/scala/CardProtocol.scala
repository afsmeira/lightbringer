import spray.json._

object CardProtocol extends DefaultJsonProtocol {
  implicit object SomeStuff extends RootJsonFormat[Card] {
    override def write(obj: Card): JsValue = JsObject.empty

    override def read(json: JsValue): Card = {
      val cardType = CardType.values.find(_.typeName == fromField[String](json, "type_name")).get

      cardType match {
        case AgendaType => agenda(card(json))
        case AttachmentType => attachment(card(json), cost(json), allegiance(json), uniqueness(json))
        case CharacterType => character(json, card(json), cost(json), allegiance(json), uniqueness(json))
        case EventType => event(card(json), cost(json), allegiance(json))
        case LocationType => location(card(json), cost(json), allegiance(json), uniqueness(json))
        case PlotType => plot(json, card(json), allegiance(json))
        case TitleType => title(card(json))
      }
    }

    def card(json: JsValue) =
      new Card {
        val name = fromField[String](json, "name")
        val traits = fromField[Option[String]](json, "traits").map(_.split('.').map(_.trim).toList)
        val flavourText = fromField[Option[String]](json, "flavor")
        val limit = fromField[Int](json, "deck_limit")
        val code = fromField[String](json, "code")
        val number = fromField[Int](json, "position")
        val pack = Pack.values.find(_.name == fromField[String](json, "pack_name")).get
        val illustrator = fromField[String](json, "illustrator")
      }

    def cost(json: JsValue) = {
      val costTuple = json.asJsObject.getFields("cost") match {
        case Seq(JsNumber(costNum)) => (costNum.toString(), costNum.toInt)
        case _ => ("X", 0)
      }
      new Cost {
        val cost = costTuple._1
        val printedCost = costTuple._2
      }
    }

    def allegiance(json: JsValue) =
      new Allegiance {
        val faction = Faction.values.find(_.name == fromField[String](json, "faction_name")).get
        val loyalty = fromField[Boolean](json, "is_loyal")
      }

    def uniqueness(json: JsValue) =
      new Uniqueness {
        val unique: Boolean = fromField[Boolean](json, "is_unique")
      }

    def agenda(card: Card) =
      Agenda(
        card.name,
        card.traits,
        card.flavourText,
        card.limit,
        card.code,
        card.number,
        card.pack,
        card.illustrator
      )

    def attachment(card: Card, cost: Cost, allegiance: Allegiance, uniqueness: Uniqueness) =
      Attachment(
        card.name,
        card.traits,
        card.flavourText,
        card.limit,
        card.code,
        card.number,
        card.pack,
        card.illustrator,
        cost.cost,
        cost.printedCost,
        allegiance.faction,
        allegiance.loyalty,
        uniqueness.unique
      )

    def character(json: JsValue, card: Card, cost: Cost, allegiance: Allegiance, uniqueness: Uniqueness) =
      Character(
        card.name,
        card.traits,
        card.flavourText,
        card.limit,
        card.code,
        card.number,
        card.pack,
        card.illustrator,
        cost.cost,
        cost.printedCost,
        allegiance.faction,
        allegiance.loyalty,
        uniqueness.unique,
        fromField[Boolean](json, "is_military"),
        fromField[Boolean](json, "is_intrigue"),
        fromField[Boolean](json, "is_power"),
        fromField[Int](json, "strength")
      )

    def event(card: Card, cost: Cost, allegiance: Allegiance) =
      Event(
        card.name,
        card.traits,
        card.flavourText,
        card.limit,
        card.code,
        card.number,
        card.pack,
        card.illustrator,
        cost.cost,
        cost.printedCost,
        allegiance.faction,
        allegiance.loyalty
      )

    def location(card: Card, cost: Cost, allegiance: Allegiance, uniqueness: Uniqueness) =
      Location(
        card.name,
        card.traits,
        card.flavourText,
        card.limit,
        card.code,
        card.number,
        card.pack,
        card.illustrator,
        cost.cost,
        cost.printedCost,
        allegiance.faction,
        allegiance.loyalty,
        uniqueness.unique
      )

    def plot(json: JsValue, card: Card, allegiance: Allegiance) =
      Plot(
        card.name,
        card.traits,
        card.flavourText,
        card.limit,
        card.code,
        card.number,
        card.pack,
        card.illustrator,
        allegiance.faction,
        allegiance.loyalty,
        fromField[Int](json, "income"),
        fromField[Int](json, "initiative"),
        fromField[Int](json, "claim"),
        fromField[Int](json, "reserve")
      )

    def title(card: Card) =
      Title(
        card.name,
        card.traits,
        card.flavourText,
        card.limit,
        card.code,
        card.number,
        card.pack,
        card.illustrator
      )
  }

  implicit object DeckProtocol extends RootJsonFormat[RawDeck] {
    override def write(obj: RawDeck): JsValue = JsObject.empty

    override def read(json: JsValue): RawDeck =
      RawDeck(
        fromField[String](json, "faction_name"),
        fromField[Option[String]](json, "agenda_code"),
        fromField[Map[String, Int]](json, "slots")
      )
  }
}