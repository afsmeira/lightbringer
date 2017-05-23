package pt.afsmeira.lightbringer.utils


case class FieldStatistics[C](cards: Seq[C], field: C => Int, name: String) {
  // maxBy is not safe on empty collections, so the argument is matched
  val distribution: Seq[StatisticPoint[Int]] = cards match {
    case Nil => Seq.empty
    case _   => (0 to field(cards.maxBy(field))).map { value =>
      val count = cards.count(field(_) == value)
      StatisticPoint(value, count, 100 * count.toDouble / cards.size.toDouble)
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

  def toTable: String = {
    import RichStatistics.RichStatisticPoints

    if (distribution.isEmpty) "" else f"${distribution.toTable(name)}\nAverage: $average%.2f"
  }
}

case class StatisticPoint[T](value: T, count: Int, percentage: Double)

object RichStatistics {

  implicit class RichStatisticPoints(val distribution: Seq[StatisticPoint[_]]) extends AnyVal {
    def toTable(
      fieldName: String,
      cardinalityRowName: String = "#",
      percentageRowName : String = "%",
      sumsTo100: Boolean = true
    ): String = {
      val sortedStats = distribution.sortBy {
        case StatisticPoint(value: Int, _, _) => f"$value%2d"
        case StatisticPoint(value, _, _) => value.toString
      }
      val costRow        = fieldName          +: sortedStats.map(_.value.toString) :+ "TOTAL"
      val cardinalityRow = cardinalityRowName +: sortedStats.map(_.count.toString) :+ distribution.map(_.count).sum
      val percentageRow  = percentageRowName  +: sortedStats.map(statPoint => f"${statPoint.percentage}%.2f") :+
        (if (sumsTo100) "100" else "N/A")

      Tabulator.format(Seq(costRow, cardinalityRow, percentageRow))
    }
  }
}
