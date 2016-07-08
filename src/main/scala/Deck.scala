import scala.util.Random

case class Deck(faction: Faction, agenda: Option[Agenda], cards: List[Card]) {

  val plotDeck: List[Plot] = cards.collect {
    case card: Plot => card
  }
  val drawDeck: List[DrawCard] = (cards diff plotDeck) collect {
    case drawCard: DrawCard => drawCard
  }

  val characters: List[Character] = drawDeck.collect {
    case card: Character => card
  }
  val events: List[Event] = drawDeck.collect {
    case card: Event => card
  }
  val attachments: List[Attachment] = drawDeck.collect {
    case card: Attachment => card
  }
  val locations: List[Location] = drawDeck.collect {
    case card: Location => card
  }

  val iconStats: Map[String, CountStat] = {
    val militaryCount = characters.count(_.military)
    val intrigueCount = characters.count(_.intrigue)
    val powerCount    = characters.count(_.power)

    Map(
      "Military" -> CountStat(militaryCount, drawDeck.size),
      "Intrigue" -> CountStat(intrigueCount, drawDeck.size),
      "Power"    -> CountStat(powerCount,    drawDeck.size)
    )
  }

  val strengthStats: Map[Int, Int] = {
    val maxStr = characters.maxBy(_.strength).strength
    (1 to maxStr).map(i => i -> characters.count(_.strength == i)).toMap
  }
  val averageStrength = strengthStats.map(kv => kv._1 * kv._2).sum.toDouble / characters.size

  val characterCostStats = costStats(characters)
  val averageCharacterCost = averageCountStat(characterCostStats, characters.size)

  val eventCostStats = costStats(events)
  val averageEventCost = averageCountStat(eventCostStats, events.size)

  val attachmentCostStats = costStats(attachments)
  val averageAttachmentCost = averageCountStat(attachmentCostStats, attachments.size)

  val locationCostStats = costStats(locations)
  val averageLocationCost = averageCountStat(locationCostStats, locations.size)

  val totalCostStats = costStats(drawDeck)
  val averageCost = averageCountStat(totalCostStats, drawDeck.size)

  val factionStats = drawDeck.groupBy(_.faction).mapValues(_.size)
  
  val incomeStats = plotStats(plotDeck, _.income)
  val averageIncome = averageCountStat(incomeStats, plotDeck.size)
  
  val initiativeStats = plotStats(plotDeck, _.initiative)
  val averageInitiative = averageCountStat(initiativeStats, plotDeck.size)
  
  val claimStats = plotStats(plotDeck, _.claim)
  val averageClaim = averageCountStat(claimStats, plotDeck.size)
  
  val reserveStats = plotStats(plotDeck, _.reserve)
  val averageReserve = averageCountStat(reserveStats, plotDeck.size)

  def costStats(cards: List[DrawCard]): Map[Int, CountStat] =
    (0 to cards.maxBy(_.printedCost).printedCost).map { i =>
      val count = cards.count(_.printedCost == i)
      (i, CountStat(count, cards.size))
    }.toMap

  def averageCountStat(costDistribution: Map[Int, CountStat], totalCards: Int): Double =
    costDistribution.map(kv => kv._1 * kv._2.count).sum.toDouble / characters.size

  def plotStats(plots: List[Plot], f :Plot => Int): Map[Int, CountStat] = {
    (0 to f(plots.maxBy(f))).map { i =>
      val count = plots.count(f(_) == i)
      (i, CountStat(count, plotDeck.size))
    }.toMap
  }

  def sampleHand: List[Card] = Random.shuffle(drawDeck).take(7)

  case class CountStat(count: Int, total: Int) {
    val pct: Double = 100 * count.toDouble / total.toDouble

    override def toString: String = f"$pct%.2f%% ($count out of $total)."
  }
}

