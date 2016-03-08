import scala.reflect.ClassTag
import scala.util.Random

class Deck(faction: Faction, agenda: Option[Agenda], cards: List[Card]) {

  val plotDeck: List[Plot] = filterType[Plot](cards)
  val drawDeck: List[Card with Cost with Allegiance] = ((cards diff plotDeck) diff filterType[Agenda](cards)).asInstanceOf[List[Card with Cost with Allegiance]]

  val characters: List[Character] = filterType[Character](drawDeck)
  val events: List[Event] = filterType[Event](drawDeck)
  val attachments: List[Attachment] = filterType[Attachment](drawDeck)
  val locations: List[Location] = filterType[Location](drawDeck)

  val iconStats: Map[String, CountStat] = {
    val militaryCount = characters.count(_.military)
    val intrigueCount = characters.count(_.intrigue)
    val powerCount    = characters.count(_.power)

    Map(
      "Military" -> CountStat(militaryCount, 100 * militaryCount / drawDeck.size),
      "Intrigue" -> CountStat(intrigueCount, 100 * intrigueCount / drawDeck.size),
      "Power"    -> CountStat(powerCount, 100 * powerCount / drawDeck.size)
    )
  }

  val strengthStats: Map[Int, Int] = {
    val maxStr = characters.maxBy(_.strength).strength
    (1 to maxStr).map(i => i -> characters.count(_.strength == i)).toMap
  }

  val characterCostStats = costStats(characters)
  val averageCharacterCost = averageCountStat(characterCostStats)

  val eventCostStats = costStats(events)
  val averageEventCost = averageCountStat(eventCostStats)

  val attachmentCostStats = costStats(attachments)
  val averageAttachmentCost = averageCountStat(attachmentCostStats)

  val locationCostStats = costStats(locations)
  val averageLocationCost = averageCountStat(locationCostStats)

    // TODO calculate costs for all of the deck
  val totalCostStats = costStats(drawDeck)
  val averageCost = averageCountStat(totalCostStats)

    // TODO calculate faction stats for all of the deck
  val factionStats = drawDeck.groupBy(_.faction).mapValues(_.size)
  
  val incomeStats = plotStats(plotDeck, _.income)
  val incomeAverage = averageCountStat(incomeStats)
  
  val initiativeStats = plotStats(plotDeck, _.initiative)
  val initiativeAverage = averageCountStat(initiativeStats)
  
  val claimStats = plotStats(plotDeck, _.claim)
  val claimAverage = averageCountStat(claimStats)
  
  val reserveStats = plotStats(plotDeck, _.reserve)
  val reserveAverage = averageCountStat(reserveStats)

  def costStats(cards: List[Cost]): Map[Int, CountStat] =
    (0 to cards.maxBy(_.printedCost).printedCost).map { i =>
      val count = cards.count(_.printedCost == i)
      (i, CountStat(count, 100 * count / cards.size))
    }.toMap

  def averageCountStat(costDistribution: Map[Int, CountStat]): Double =
    costDistribution.map(kv => kv._1 * kv._2.count).sum.toDouble / plotDeck.size

  def plotStats(plots: List[Plot], f :(Plot) => Int): Map[Int, CountStat] = {
    (0 to f(plots.maxBy(f))).map { i =>
      val count = plots.count(f(_) == i)
      (i, CountStat(count, 100 * count / plotDeck.size))
    }.toMap
  }

  def filterType[T <: Card : ClassTag](cards: List[Card]): List[T] = {
    val clazz = implicitly[ClassTag[T]].runtimeClass
    cards flatMap {
      case card: T if clazz.isInstance(card) => Some(card)
      case _ => None
    }
  }

  def sampleHand: List[Card] = Random.shuffle(drawDeck).take(7)

  case class CountStat(count: Int, pct: Double)
}

case class RawDeck(factionName: String, agendaCode: Option[String], slots: Map[String, Int])

object Deck {
  def fromRawDeck(rawDeck: RawDeck, cardMap: Map[String, Card]): Deck = {
    val agenda = rawDeck.agendaCode.map(cardMap(_).asInstanceOf[Agenda])
    val faction = Faction.values.find(_.name == rawDeck.factionName).get

    val x = rawDeck.slots.keySet.map(cardMap(_)).toList

    new Deck(faction, agenda, x)
  }
}
