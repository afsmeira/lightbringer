package pt.afsmeira.lightbringer.setup

import pt.afsmeira.lightbringer.model._
import pt.afsmeira.lightbringer.model.RichCards.RichMarshallableDrawCards

object SetupAnalyzer {
  private val HandSize  = 7
  private val SetupGold = 8

  /**
    * Analyzes decks from a setup perspective, by generating a configurable number of "hands" and analyzing all possible
    * setups that "hand" can generate.
    *
    * @param deck The deck to analyze.
    * @return The best generated setups.
    */
  def analyze(deck: Deck): Seq[Setup] = (1 to Settings.Meta.setupRuns).map { _ =>
    val startingHand = deck.randomHand(HandSize)
    val setups = generateSetups(startingHand, mulligan = false)

    // Mulligan if all setups are poor
    if (setups.forall(_.isPoor)) {
      val mulliganHand = deck.randomHand(HandSize)
      generateSetups(mulliganHand, mulligan = true).max
    } else {
      setups.max
    }
  }

  /** Generate all setups possible for a given `hand`. */
  private def generateSetups(hand: Seq[DrawCard], mulligan: Boolean): Seq[Setup] = {
    val validCards = filterInvalidCards(hand)

    // Possible setups are all combinations of hand
    for {
      i           <- 1 to validCards.size
      combination <- validCards.combinations(i) if combination.count(_.limited) <= 1
      totalGold    = combination.deduplicate.map(_.cost).sum if totalGold <= SetupGold
    } yield Setup(combination, hand, mulligan, Settings.Setup)
  }

  /**
    * Filter setup invalid cards from `hand`.
    * <p>
    * Setup invalid cards are:
    *   - attachments that are not eligible for the other cards in `hand`
    *   - events or other cards that can't be marshalled
    *
    * @param hand Cards to generate a setup.
    * @return     Setup valid cards.
    */
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
