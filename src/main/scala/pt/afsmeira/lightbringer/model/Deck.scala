package pt.afsmeira.lightbringer.model

import pt.afsmeira.lightbringer.model.RichCards.{RichDrawCards, RichMarshallableDrawCards}
import pt.afsmeira.lightbringer.setup.{SetupAnalyzer, SetupStatistics}
import pt.afsmeira.lightbringer.utils.{FieldStatistics, PercentageStatisticPoint}
import pt.afsmeira.lightbringer.utils.RichStatistics.RichPercentageStatisticPoints

import scala.reflect.ClassTag
import scala.reflect._
import scala.util.Random

/**
  * A deck for playing A Game of Thrones LCG 2nd edition.
  *
  * @param house  The house or faction of this deck.
  * @param agenda The agenda it uses.
  * @param cards  The cards in this deck, including plots.
  * @param name   The deck's name
  */
case class Deck(house: Faction, agenda: Option[Agenda], cards: Seq[Card], name: Option[String]) {

  val plotDeck: Seq[Plot] = cards.collect {
    case card: Plot => card
  }
  /* The actual draw deck, ie, the deck cards except for the plots. */
  val drawDeck: Seq[DrawCard] = (cards diff plotDeck) collect {
    case drawCard: DrawCard => drawCard
  }

  /** Get `handSize` random cards from the `drawDeck`. */
  def randomHand(handSize: Int): Seq[DrawCard] = Random.shuffle(drawDeck).take(handSize)

  val characters: Seq[Character] = drawDeck.collect {
    case card: Character => card
  }
  val deduplicatedCharacters: Seq[Character] = characters.deduplicate

  val events: Seq[Event] = drawDeck.collect {
    case card: Event => card
  }
  val attachments: Seq[Attachment] = drawDeck.collect {
    case card: Attachment => card
  }
  val locations: Seq[Location] = drawDeck.collect {
    case card: Location => card
  }

  /** String listing of the deck contents. */
  private val overview: String = Seq(
    house.name,
    agenda.map(_.name).mkString,
    s"Draw Deck: ${drawDeck.size} cards\n",
    listing(plotDeck),
    listing(characters),
    listing(attachments),
    listing(locations),
    listing(events)
  ).filterNot(_.isEmpty).mkString("\n")

  val iconStats: Seq[PercentageStatisticPoint[String]] = Seq(
    PercentageStatisticPoint(
      "Military",
      deduplicatedCharacters.count(_.military),
      100 * deduplicatedCharacters.count(_.military).toDouble / deduplicatedCharacters.size.toDouble
    ),
    PercentageStatisticPoint(
      "Intrigue",
      deduplicatedCharacters.count(_.intrigue),
      100 * deduplicatedCharacters.count(_.intrigue).toDouble / deduplicatedCharacters.size.toDouble
    ),
    PercentageStatisticPoint(
      "Power",
      deduplicatedCharacters.count(_.power),
      100 * deduplicatedCharacters.count(_.power).toDouble / deduplicatedCharacters.size.toDouble
    )
  )

  val factionStats: Seq[PercentageStatisticPoint[_]] =
    drawDeck.groupBy(_.faction).map { case (faction, factionCards) =>
      val totalFactionCards = factionCards.size
      val factionPercentage = 100 * totalFactionCards.toDouble / drawDeck.size.toDouble

      PercentageStatisticPoint[String](faction.name, totalFactionCards, factionPercentage)
    }.toSeq

  val strengthStats       = FieldStatistics[Character] (deduplicatedCharacters,  _.strength, "Strength")
  val characterCostStats  = FieldStatistics[Character] (characters.filterCostX,  _.cost,     "Character Cost")
  val eventCostStats      = FieldStatistics[Event]     (events.filterCostX,      _.cost,     "Event Cost")
  val attachmentCostStats = FieldStatistics[Attachment](attachments.filterCostX, _.cost,     "Attachment Cost")
  val locationCostStats   = FieldStatistics[Location]  (locations.filterCostX,   _.cost,     "Location Cost")
  val totalCostStats      = FieldStatistics[DrawCard]  (drawDeck.filterCostX,    _.cost,     "Total Cost")

  val incomeStats     = FieldStatistics[Plot](plotDeck.filter(_.printedIncome != "X"), _.income, "Income")
  val initiativeStats = FieldStatistics[Plot](plotDeck, _.initiative,   "Initiative")
  val claimStats      = FieldStatistics[Plot](plotDeck, _.claim, "Claim")
  val reserveStats    = FieldStatistics[Plot](plotDeck, _.reserve,      "Reserve")


  private def listing[C <: Card : ClassTag](cards: Seq[C]): String = {
    val title   = s"${classTag[C].runtimeClass.getSimpleName} (${cards.size})"
    val listing = cards.groupBy(identity).toSeq.sortBy(_._1.name).map {
      case (card, cardCopies) => s"${cardCopies.size.toString}x ${card.name}"
    }
    if (listing.isEmpty) "" else (title +: listing).mkString("", "\n", "\n")
  }

  val setupStats: SetupStatistics = SetupStatistics(SetupAnalyzer.analyze(this))

  /**
    * Generate the deck report, as a string.
    *
    * @param setupHandsReport Whether the report should output all setup hands.
    * @return The deck report.
    */
  def fullReport(setupHandsReport: Boolean): String = {
    val duplicatesTitle = "# (Duplicates not counted)"
    val costTitle = "# (Cost X not counted)"

    Seq(
      overview,
      "\nDECK STATISTICS",
      factionStats.toTable("Faction"),
      iconStats.toTable("Icon", duplicatesTitle, percentageRowTitle = "% of characters", sumsTo100 = false),
      strengthStats.toTable(duplicatesTitle),
      characterCostStats.toTable(costTitle),
      attachmentCostStats.toTable(costTitle),
      locationCostStats.toTable(costTitle),
      eventCostStats.toTable(costTitle),
      totalCostStats.toTable(costTitle),
      incomeStats.toTable(costTitle),
      initiativeStats.toTable(),
      claimStats.toTable(),
      reserveStats.toTable(),
      setupStats.fullReport(setupHandsReport)
    ).filterNot(_.isEmpty).mkString("\n", "\n\n", "\n")
  }
}
