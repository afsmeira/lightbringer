package pt.afsmeira.lightbringer.utils


case class FieldStatistics[C](
  cards: Seq[C],
  field: C => Int,
  fieldName: String,
  dualPredicate: Option[C => Boolean] = None,
  dualPrefix: Option[String] = None
) {
  // `maxBy` is not safe on empty collections, so the argument is matched
  // Also, we need to get the max out of all the cards, but build the distribution for the statistic itself and its dual
  private def getDistribution(filteredCards: Seq[C]): Seq[PercentageStatisticPoint[Int]] = filteredCards match {
    case Nil if dualPredicate.isEmpty => Seq.empty
    case _   => (0 to field(cards.maxBy(field))).map { value =>
      val count = filteredCards.count(field(_) == value)
      val percentage = if (filteredCards.isEmpty) 0.0 else 100 * count.toDouble / filteredCards.size.toDouble
      PercentageStatisticPoint(value, count, percentage)
    }
  }

  private def getAverage(distribution: Seq[PercentageStatisticPoint[Int]]): Double = {
    val (weightedTotal, total) = distribution.map { statPoint =>
      statPoint.value * statPoint.count -> statPoint.count
    }.unzip match {
      case (partialWeightedTotals, totals) => partialWeightedTotals.sum -> totals.sum
    }
    weightedTotal.toDouble / total.toDouble
  }

  val distribution: Seq[PercentageStatisticPoint[Int]] = dualPredicate.map { predicate =>
    getDistribution(cards.filterNot(predicate))
  }.getOrElse(getDistribution(cards))
  val dualDistribution: Option[Seq[PercentageStatisticPoint[Int]]] = dualPredicate.map { predicate =>
    getDistribution(cards.filter(predicate))
  }

  val average: Double = getAverage(distribution)
  val dualAverage: Option[Double] = dualDistribution.map(getAverage)

  def toTable(
    cardinalityRowTitle: String = "#",
    percentageRowTitle:  String = "%",
    sumsTo100: Boolean = true
  ): String = {
    import RichStatistics.RichPercentageStatisticPoints

    val sortedStats     = distribution.sort
    val dualSortedStats = dualDistribution.map(_.sort)

    val dualPrefix = this.dualPrefix.getOrElse("")
    val dualCardinalityRow = dualSortedStats.map { stats =>
      RichStatistics.cardinalityRow(s"($dualPrefix) $cardinalityRowTitle", stats)
    }.getOrElse(Seq.empty)
    val dualPercentageRow = dualSortedStats.map { stats =>
      RichStatistics.metaValueRow(s"($dualPrefix) $percentageRowTitle", stats) :+ (if (sumsTo100) f"${stats.map(_.metaValue).sum}%.2f" else "N/A")
    }.getOrElse(Seq.empty)

    val rows = Seq(
      RichStatistics.headerRow(fieldName, sortedStats) :+ "TOTAL",
      RichStatistics.cardinalityRow(cardinalityRowTitle, sortedStats),
      RichStatistics.metaValueRow(percentageRowTitle, sortedStats) :+ (if (sumsTo100) f"${sortedStats.map(_.metaValue).sum}%.2f" else "N/A"),
      dualCardinalityRow,
      dualPercentageRow
    ).filter(_.nonEmpty)

    Tabulator.format(rows) +
      f"\nAverage: $average%.2f" +
      dualAverage.map { average =>
        f"\n($dualPrefix) Average: $average%.2f"
      }.getOrElse("")
  }
}

protected[utils] sealed trait StatisticPoint[T] {
  def value: T
  protected[utils] def metaValue: Double
}
case class AverageStatisticPoint[T](value: T, average: Double) extends StatisticPoint[T] {
  protected[utils] def metaValue: Double = average
}
case class PercentageStatisticPoint[T](value: T, count: Int, percentage: Double) extends StatisticPoint[T] {
  protected[utils] def metaValue: Double = percentage

  def toLine: String = f"$value: $count ($percentage%.2f%%)"
}

object RichStatistics {

  private[utils] def headerRow(title: String, sortedStats: Seq[StatisticPoint[_]]): Seq[String] =
    title +: sortedStats.map(_.value.toString)

  private[utils] def metaValueRow(title: String, sortedStats: Seq[StatisticPoint[_]]): Seq[String] =
    title +: sortedStats.map(statPoint => f"${statPoint.metaValue}%.2f")

  private[utils] def cardinalityRow(title: String, sortedStats: Seq[PercentageStatisticPoint[_]]): Seq[String] =
    title +: sortedStats.map(_.count.toString) :+ sortedStats.map(_.count).sum.toString

  implicit class RichAverageStatisticPoints(val simpleStatisticPoints: Seq[AverageStatisticPoint[_]]) extends AnyVal {
    def toTable(fieldName: String, averageRowTitle: String): String = Tabulator.format(
      Seq(headerRow(fieldName, simpleStatisticPoints), metaValueRow(averageRowTitle, simpleStatisticPoints))
    )
  }

  implicit class RichPercentageStatisticPoints(val points: Seq[PercentageStatisticPoint[_]]) extends AnyVal {
    def toTable(
      fieldName: String,
      cardinalityRowTitle: String = "#",
      percentageRowTitle : String = "%",
      sumsTo100: Boolean = true
    ): String = {
      val sortedStats = points.sort

      Tabulator.format(Seq(
        headerRow(fieldName, sortedStats) :+ "TOTAL",
        cardinalityRow(cardinalityRowTitle, sortedStats),
        metaValueRow(percentageRowTitle, sortedStats) :+ (if (sumsTo100) f"${sortedStats.map(_.metaValue).sum}%.2f" else "N/A")
      ))
    }

    def sort: Seq[PercentageStatisticPoint[_]] = points.sortBy {
      case PercentageStatisticPoint(value: Int, _, _) => f"$value%2d"
      case s => s.value.toString
    }
  }
}
