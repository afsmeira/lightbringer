package pt.afsmeira.agotlcg.model

import pt.afsmeira.agotlcg.utils.Tabulator

import scala.reflect.ClassTag
import scala.reflect._
import scala.util.Random

case class Deck(faction: Faction, agenda: Option[Agenda], cards: Seq[Card], name: Option[String]) {

  import Deck.CountStat

  val plotDeck: Seq[Plot] = cards.collect {
    case card: Plot => card
  }
  val drawDeck: Seq[DrawCard] = (cards diff plotDeck) collect {
    case drawCard: DrawCard => drawCard
  }

  val characters: Seq[Character] = drawDeck.collect {
    case card: Character => card
  }
  val events: Seq[Event] = drawDeck.collect {
    case card: Event => card
  }
  val attachments: Seq[Attachment] = drawDeck.collect {
    case card: Attachment => card
  }
  val locations: Seq[Location] = drawDeck.collect {
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
  
  val claimStats = plotStats(plotDeck, _.printedClaim)
  val averageClaim = averageCountStat(claimStats, plotDeck.size)
  
  val reserveStats = plotStats(plotDeck, _.reserve)
  val averageReserve = averageCountStat(reserveStats, plotDeck.size)

  def costStats(cards: Seq[DrawCard]): Map[Int, CountStat] =
    (0 to cards.maxBy(_.printedCost).printedCost).map { i =>
      val count = cards.count(_.printedCost == i)
      (i, CountStat(count, cards.size))
    }.toMap

  def averageCountStat(costDistribution: Map[Int, CountStat], totalCards: Int): Double =
    costDistribution.map(kv => kv._1 * kv._2.count).sum.toDouble / characters.size

  def plotStats(plots: Seq[Plot], f: Plot => Int): Map[Int, CountStat] = {
    (0 to f(plots.maxBy(f))).map { i =>
      val count = plots.count(f(_) == i)
      (i, CountStat(count, plotDeck.size))
    }.toMap
  }

  def sampleHand: Seq[Card] = Random.shuffle(drawDeck).take(7)

  def generalStatsReport: String = "Overall Stats\n" +
    Tabulator.format(Seq(
      Seq("Characters", "Attachments", "Locations", "Events", "TOTAL"),
      Seq(characters.size, attachments.size, locations.size, events.size, drawDeck.size)
    ))

  def factionStatsReport: String = "Faction Stats\n" +
    Tabulator.format(Seq(
      factionStats.keys.toSeq,
      factionStats.values.toSeq
    ))

  def overview: String = Seq(
    s"${faction.name}\n${agenda.map(_.name).getOrElse("")}",
    listing(plotDeck),
    listing(characters),
    listing(attachments),
    listing(locations),
    listing(events)
  ).mkString("\n\n")

  def iconStatsReport: String = "Icon Stats\n" + iconStats.toTable

  def characterCostStatsReport: String = "Character Cost Stats\n" + characterCostStats.toTable

  def attachmentCostStatsReport: String = "Attachment Cost Stats\n" + attachmentCostStats.toTable

  def locationCostStatsReport: String = "Location Cost Stats\n" + locationCostStats.toTable

  def eventCostStatsReport: String = "Event Cost Stats\n" + eventCostStats.toTable

  def plotIncomeStatsReport: String = "Plot Income Stats\n" + incomeStats.toTable

  def plotInitiativeStatsReport: String = "Plot Initiative Stats\n" + initiativeStats.toTable

  def plotClaimStatsReport: String = "Plot Claim Stats\n" + claimStats.toTable

  def plotReserveStatsReport: String = "Plot Reserve Stats\n" + reserveStats.toTable + "\n"

  def fullReport: String = Seq(overview, generalStatsReport, factionStatsReport, iconStatsReport, characterCostStatsReport,
    attachmentCostStatsReport, locationCostStatsReport, eventCostStatsReport, plotIncomeStatsReport,
    plotInitiativeStatsReport, plotClaimStatsReport, plotReserveStatsReport).mkString("\n\n")


  def listing[T <: Card : ClassTag](cardList: Seq[T]): String = {
    val title   = s"${classTag[T].runtimeClass.getSimpleName} (${cardList.size})"
    val listing = cardList.groupBy(identity).toSeq.sortBy(_._1.name).map {
      case (card, cardReps) => s"${cardReps.size.toString}x ${card.name}"
    }

    (title +: listing) mkString "\n"
  }
}

object Deck {
  case class CountStat(count: Int, total: Int) {
    val pct: Double = 100 * count.toDouble / total.toDouble

    override def toString: String = f"$pct%.2f%% ($count out of $total)."
  }

  implicit class ConvertToTable(val stats: Map[_, CountStat]) extends AnyVal {
    def toTable: String = {
      val sortedStats = stats.toSeq.sortBy {
        case (key: Int, _) => f"$key%2d"
        case (key, _) => key.toString
      }
      val costRow        = "Cost" +: sortedStats.map(_._1.toString) :+ "TOTAL"
      val cardinalityRow = "#" +: sortedStats.map(_._2.count.toString) :+ stats.values.head.total.toString
      val pctRow         = "%" +: sortedStats.map(stat => f"${stat._2.pct}%.2f") :+ "100"

      Tabulator.format(Seq(costRow, cardinalityRow, pctRow))
    }
  }
}

