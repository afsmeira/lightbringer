package pt.afsmeira.lightbringer.model

import pt.afsmeira.lightbringer.model.Deck.{FieldStatistics, StatisticPoint}
import pt.afsmeira.lightbringer.utils.Tabulator

import scala.reflect.ClassTag
import scala.reflect._

case class Deck(house: Faction, agenda: Option[Agenda], cards: Seq[Card], name: Option[String]) {

  private val plotDeck: Seq[Plot] = cards.collect {
    case card: Plot => card
  }
  val drawDeck: Seq[DrawCard] = (cards diff plotDeck) collect {
    case drawCard: DrawCard => drawCard
  }

  private val characters: Seq[Character] = drawDeck.collect {
    case card: Character => card
  }
  private val events: Seq[Event] = drawDeck.collect {
    case card: Event => card
  }
  private val attachments: Seq[Attachment] = drawDeck.collect {
    case card: Attachment => card
  }
  private val locations: Seq[Location] = drawDeck.collect {
    case card: Location => card
  }

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

  private val iconStats: Seq[StatisticPoint[String]] = Seq(
    StatisticPoint("Military", characters.count(_.military), characters.size),
    StatisticPoint("Intrigue", characters.count(_.intrigue), characters.size),
    StatisticPoint("Power",    characters.count(_.power),    characters.size)
  )
  private val factionStats = drawDeck.groupBy(_.faction).map { case (faction, factionCards) =>
    val totalFactionCards = factionCards.size
    val factionPercentage = totalFactionCards.toDouble / drawDeck.size.toDouble

    StatisticPoint[String](faction.name, totalFactionCards, factionPercentage)
  }.toSeq

  private val strengthStats       = FieldStatistics[Character] (characters,  _.strength,    "Strength")
  private val characterCostStats  = FieldStatistics[Character] (characters,  _.printedCost, "Character Cost")
  private val eventCostStats      = FieldStatistics[Event]     (events,      _.printedCost, "Event Cost")
  private val attachmentCostStats = FieldStatistics[Attachment](attachments, _.printedCost, "Attachment Cost")
  private val locationCostStats   = FieldStatistics[Location]  (locations,   _.printedCost, "Location Cost")
  private val totalCostStats      = FieldStatistics[DrawCard]  (drawDeck,    _.printedCost, "Total Cost")

  private val incomeStats         = FieldStatistics[Plot](plotDeck, _.income,       "Income")
  private val initiativeStats     = FieldStatistics[Plot](plotDeck, _.initiative,   "Initiative")
  private val claimStats          = FieldStatistics[Plot](plotDeck, _.printedClaim, "Claim")
  private val reserveStats        = FieldStatistics[Plot](plotDeck, _.reserve,      "Reserve")


  private def listing[C <: Card : ClassTag](cards: Seq[C]): String = {
    val title   = s"${classTag[C].runtimeClass.getSimpleName} (${cards.size})"
    val listing = cards.groupBy(identity).toSeq.sortBy(_._1.name).map {
      case (card, cardCopies) => s"${cardCopies.size.toString}x ${card.name}"
    }
    if (listing.isEmpty) "" else (title +: listing).mkString("", "\n", "\n")
  }

  def fullReport: String = Seq(
    overview,
    factionStats.toTable("Faction"),
    iconStats.toTable("Icon"),
    strengthStats.toTable,
    characterCostStats.toTable,
    attachmentCostStats.toTable,
    locationCostStats.toTable,
    eventCostStats.toTable,
    totalCostStats.toTable,
    incomeStats.toTable,
    initiativeStats.toTable,
    claimStats.toTable,
    reserveStats.toTable
  ).filterNot(_.isEmpty).mkString("\n\n")
}

object Deck {
  case class FieldStatistics[C <: Card](cards: Seq[C], field: C => Int, name: String) {
    // maxBy is not safe on empty collections, so the argument is matched
    val distribution: Seq[StatisticPoint[Int]] = cards match {
      case Nil => Seq.empty
      case _   => (0 to field(cards.maxBy(field))).map { value =>
        val count = cards.count(field(_) == value)
        StatisticPoint (value, count, 100 * count.toDouble / cards.size.toDouble)
      }
    }

    val average: Double = {
      val (weightedTotal, total) = distribution.map { statPoint =>
        statPoint.value * statPoint.count -> statPoint.count
      } unzip match {
        case (partialWeightedTotals, totals) => partialWeightedTotals.sum -> totals.sum
      }
      weightedTotal.toDouble / total.toDouble
    }

    def toTable: String = if (distribution.isEmpty) "" else f"${distribution.toTable(name)}\nAverage: $average%.2f"
  }

  case class StatisticPoint[T](value: T, count: Int, percentage: Double)

  implicit class ConvertToTable(val distribution: Seq[StatisticPoint[_]]) extends AnyVal {
    def toTable(fieldName: String): String = {
      val sortedStats = distribution.sortBy {
        case StatisticPoint(value: Int, _, _) => f"$value%2d"
        case StatisticPoint(value, _, _)      => value.toString
      }
      val costRow        = fieldName +: sortedStats.map(_.value.toString) :+ "TOTAL"
      val cardinalityRow = "#"       +: sortedStats.map(_.count.toString) :+ distribution.map(_.count).sum
      val percentageRow  = "%"       +: sortedStats.map(statPoint => f"${statPoint.percentage}%.2f") :+ "100"

      Tabulator.format(Seq(costRow, cardinalityRow, percentageRow))
    }
  }
}
