package pt.afsmeira.lightbringer.setup

import pt.afsmeira.lightbringer.utils.{AverageStatisticPoint, FieldStatistics, PercentageStatisticPoint}
import pt.afsmeira.lightbringer.utils.RichStatistics.{RichAverageStatisticPoints, RichFieldStatistics, RichPercentageStatisticPoint}

case class SetupStatistics(setups: Seq[Setup]) {

  private val mulliganPredicate: Option[Setup => Boolean] = Some(_.mulligan)
  private val mulliganPrefix: Option[String] = Some("Mulligan")

  val usedCardsStats =
    FieldStatistics[Setup](setups, _.validCards.size, "Used Cards", mulliganPredicate, mulliganPrefix)
  val goldStats =
    FieldStatistics[Setup](setups, _.goldUsed, "Used Gold", mulliganPredicate, mulliganPrefix)

  val distinctCharacters =
    FieldStatistics[Setup](setups, _.distinctCharacterCount, "Distinct Characters", mulliganPredicate, mulliganPrefix)
  val totalStrength =
    FieldStatistics[Setup](setups, _.totalStrength, "Total Character Strength", mulliganPredicate, mulliganPrefix)

  val keyCardCountStats =
    FieldStatistics[Setup](setups, _.keyCardCount, "Key Cards", mulliganPredicate, mulliganPrefix)
  val avoidableCardCountStats =
    FieldStatistics[Setup](setups, _.avoidableCardCount, "Avoidable Cards", mulliganPredicate, mulliganPrefix)

  val poorStats = PercentageStatisticPoint[String](
    "Poor Setups",
    setups.count(_.isPoor),
    100 * setups.count(_.isPoor).toDouble / setups.size.toDouble
  )
  val economyStats = PercentageStatisticPoint[String](
    "Setups w/ Economy",
    setups.count(_.hasEconomy),
    100 * setups.count(_.hasEconomy).toDouble / setups.size.toDouble
  )
  val limitedStats = PercentageStatisticPoint[String](
    "Setups w/ Limited",
    setups.count(_.hasLimited),
    100 * setups.count(_.hasLimited).toDouble / setups.size.toDouble
  )

  val totalIconSpread: Seq[AverageStatisticPoint[String]] = Seq(
    AverageStatisticPoint[String]("Military", setups.map(_.characters.count(_.military)).sum / setups.size.toDouble),
    AverageStatisticPoint[String]("Intrigue", setups.map(_.characters.count(_.intrigue)).sum / setups.size.toDouble),
    AverageStatisticPoint[String]("Power",    setups.map(_.characters.count(_.power)).sum    / setups.size.toDouble)
  )
  val totalIconStrength: Seq[AverageStatisticPoint[String]] = Seq(
    AverageStatisticPoint[String]("Military", setups.map(_.characters.filter(_.military).map(_.strength).sum).sum / setups.size.toDouble),
    AverageStatisticPoint[String]("Intrigue", setups.map(_.characters.filter(_.intrigue).map(_.strength).sum).sum / setups.size.toDouble),
    AverageStatisticPoint[String]("Power",    setups.map(_.characters.filter(_.power).map(_.strength).sum).sum    / setups.size.toDouble)
  )

  def fullReport(setupHandsReport: Boolean): String = Seq(
    "\n\nSETUP STATISTICS",
    poorStats.toLine,
    economyStats.toLine,
    limitedStats.toLine,
    usedCardsStats.toTable(),
    goldStats.toTable(),
    keyCardCountStats.toTable(),
    avoidableCardCountStats.toTable(),
    distinctCharacters.toTable(),
    totalStrength.toTable(),
    totalIconSpread.toTable("Icons", "Average number of characters per icon"),
    totalIconStrength.toTable("Icons", "Average icon strength"),
    if (setupHandsReport) this.setupHandsReport else ""
  ).mkString("\n\n")

  private val setupHandsReport: String = {
    val header = "\nSETUP CARDS\n\n"

    val body = setups.map { setup =>
      val unusedCards = setup.originalHand.diff(setup.validCards)

      setup.originalHand.map { card =>
        if (!unusedCards.contains(card)) {
          "*" + card.name + "*"
        } else {
          card.name
        }
      }.mkString(", ")
    }.mkString("\n")

    header + body
  }
}
