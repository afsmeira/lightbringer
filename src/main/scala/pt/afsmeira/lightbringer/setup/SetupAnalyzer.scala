package pt.afsmeira.lightbringer.setup

import pt.afsmeira.lightbringer.model._

object SetupAnalyzer {
  private val HandSize  = 7
  private val SetupGold = 8
  private val TotalRuns = 5

  def analyze(deck: Deck): String = {
    (1 to TotalRuns).map { _ =>
      val setups = generateSetups(deck.randomHand(HandSize))

      // Mulligan if all setups are poor
      if (setups.forall(_.isPoor)) {
        generateSetups(deck.randomHand(HandSize)).max
      } else {
        setups.max
      }
    }.mkString("\n")
  }

  private def generateSetups(cards: Seq[DrawCard]): Seq[Setup] = {
    // TODO somehow filter out attachments that can't be used for setup
    val validCards = cards.collect {
      case card : Setup.ValidCard => card
    }

    for {
      i           <- 1 to validCards.size
      combination <- validCards.combinations(i) if combination.count(_.limited) <= 1
      totalGold    = Setup.deduplicate(combination).map(_.printedCost).sum if totalGold <= SetupGold
    } yield Setup(combination, Settings.Setup)
  }
}
