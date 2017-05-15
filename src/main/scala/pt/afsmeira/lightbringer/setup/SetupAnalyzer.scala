package pt.afsmeira.lightbringer.setup

import pt.afsmeira.lightbringer.model._

object SetupAnalyzer {
  private val HandSize  = 7
  private val SetupGold = 8

  def analyze(deck: Deck): String = {
    (1 to Settings.Meta.setupRuns).map { _ =>
      val startingHand = deck.randomHand(HandSize)
      val setups = generateSetups(startingHand)

      // Mulligan if all setups are poor
      val (finalHand, bestSetup) = if (setups.forall(_.isPoor)) {
        val mulliganHand = deck.randomHand(HandSize)
        (mulliganHand, generateSetups(mulliganHand).max)
      } else {
        (startingHand, setups.max)
      }

      // TODO Don't do setup visualization here
      finalHand.toString + "\n" + bestSetup.toString
    }.mkString("\n")
  }

  private def generateSetups(hand: Seq[DrawCard]): Seq[Setup] = {
    val validCards = filterInvalidCards(hand)

    for {
      i           <- 1 to validCards.size
      combination <- validCards.combinations(i) if combination.count(_.limited) <= 1
      totalGold    = Setup.deduplicate(combination).map(_.printedCost).sum if totalGold <= SetupGold
    } yield Setup(combination, Settings.Setup)
  }

  private def filterInvalidCards(hand: Seq[DrawCard]): Seq[Setup.ValidCard] = {

    val characters = hand.collect {
      case character: Character => character
    }
    val attachments = hand.collect {
      case attachment: Attachment => attachment
    }

    val invalidAttachments = attachments.filterNot { attachment =>
      characters.exists(_.eligible(attachment)) && !attachment.restrictions.opponent
    }

    // Filter out invalid attachments AND events
    (hand diff invalidAttachments) collect {
      case card: Setup.ValidCard => card
    }
  }
}
