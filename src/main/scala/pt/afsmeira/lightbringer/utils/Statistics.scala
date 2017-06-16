package pt.afsmeira.lightbringer.utils

/**
  * Statistic distribution for a given field of a card.
  * <p>
  * Optionally, this statistic can also contain the distributions for the same field, but split by a dual predicate.
  *
  * @param cards         The cards to analyze.
  * @param field         The field to get the distribution.
  * @param fieldName     The name of the field.
  * @param dualPredicate The predicate the dual distribution fulfills.
  * @param dualPrefix    The name prefix of the dual field.
  * @tparam C The type of card this statistic refers to.
  */
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

  /** Generate a string table for this statistic. */
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

/**
  * A given statistic point.
  *
  * @tparam T The type of the field value.
  */
protected[utils] sealed trait StatisticPoint[T] {
  /** The field's value. */
  def value: T
  /** The field's meta value. Simply a placeholder for extension and name redifinition. */
  protected[utils] def metaValue: Double
}

/**
  * A statistic point with a value and it's average.
  *
  * @param average The field's average.
  */
case class AverageStatisticPoint[T](value: T, average: Double) extends StatisticPoint[T] {
  protected[utils] def metaValue: Double = average
}

/**
  * A statistic point with a value, it's count and percentage.
  *
  * @param count      The field's count.
  * @param percentage The field's percentage.
  */
case class PercentageStatisticPoint[T](value: T, count: Int, percentage: Double) extends StatisticPoint[T] {
  protected[utils] def metaValue: Double = percentage

  def toLine: String = f"$value: $count ($percentage%.2f%%)"
}

/**
  * Object that provides enrichment classes over collections of [[StatisticPoint]].
  */
object RichStatistics {

  private[utils] def headerRow(title: String, sortedStats: Seq[StatisticPoint[_]]): Seq[String] =
    title +: sortedStats.map(_.value.toString)

  private[utils] def metaValueRow(title: String, sortedStats: Seq[StatisticPoint[_]]): Seq[String] =
    title +: sortedStats.map(statPoint => f"${statPoint.metaValue}%.2f")

  private[utils] def cardinalityRow(title: String, sortedStats: Seq[PercentageStatisticPoint[_]]): Seq[String] =
    title +: sortedStats.map(_.count.toString) :+ sortedStats.map(_.count).sum.toString

  implicit class RichAverageStatisticPoints(val simpleStatisticPoints: Seq[AverageStatisticPoint[_]]) extends AnyVal {
    /**
      * Output the information in a sequence of [[pt.afsmeira.lightbringer.utils.AverageStatisticPoint]] in a table
      * format.
      */
    def toTable(fieldName: String, averageRowTitle: String): String = Tabulator.format(
      Seq(headerRow(fieldName, simpleStatisticPoints), metaValueRow(averageRowTitle, simpleStatisticPoints))
    )
  }

  implicit class RichPercentageStatisticPoints(val points: Seq[PercentageStatisticPoint[_]]) extends AnyVal {
    /**
      * Output the information in a sequence of [[pt.afsmeira.lightbringer.utils.PercentageStatisticPoint]] in a table
      * format.
      */
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
