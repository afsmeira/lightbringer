package pt.afsmeira.lightbringer.setup

import pt.afsmeira.lightbringer.model.{Character, DrawCard, Marshallable}
import pt.afsmeira.lightbringer.model.RichCards.RichMarshallableDrawCards

object Setup {
  /** Type of a valid setup card. */
  type ValidCard = DrawCard with Marshallable
}

/**
  * Represents a valid setup, as per rule specifications.
  * <p>
  * The setup is comparable according to the configured parameters, so that it is possible to choose the best setup for
  * a given hand of cards.
  *
  * @param validCards   The cards to setup.
  * @param originalHand The original hand of cards the setup was chosen from.
  * @param mulligan     Whether the setup results of a mulligan.
  * @param settings     The settings for this setup.
  */
case class Setup(
  validCards: Seq[Setup.ValidCard],
  originalHand: Seq[DrawCard],
  mulligan: Boolean,
  settings: SetupSettings
) extends Ordered[Setup] {

  private val deduplicatedCards: Seq[Setup.ValidCard] = validCards.deduplicate

  private val virtualGoldUsed: Int = deduplicatedCards.map(_.cost).sum + deduplicatedCards.count(_.economy)
  val goldUsed  : Int = deduplicatedCards.map(_.cost).sum
  val hasEconomy: Boolean = deduplicatedCards.exists(_.economy)
  val hasLimited: Boolean = deduplicatedCards.exists(_.limited)

  /** Number of cards in the key card list. */
  val keyCardCount: Int = deduplicatedCards.count { card =>
    settings.keyCards.contains(card.code) || settings.keyCards.contains(card.name)
  }
  /** Number of cards in the avoidable card list. */
  val avoidableCardCount: Int = deduplicatedCards.count { card =>
    settings.avoidableCards.contains(card.code) || settings.avoidableCards.contains(card.name) || card.bestow.isDefined
  }

  val characters: Seq[Character] = deduplicatedCards.collect {
    case card: Character => card
  }
  private val hasTwoCharacters     = characters.size >= 2
  private val hasFourCostCharacter = characters.exists(_.cost >= 4)
  val distinctCharacterCount: Int  = characters.size
  val totalStrength: Int           = characters.map(_.strength).sum

  /** Whether a setup is poor, according to configurations. */
  val isPoor: Boolean =
    (settings.requireTwoCharacters     && !hasTwoCharacters)     ||
    (settings.requireFourCostCharacter && !hasFourCostCharacter) ||
    (settings.requireEconomy           && !hasEconomy)           ||
    (settings.requireKeyCard           && keyCardCount == 0)     ||
    (settings.minCardsRequired > validCards.size)

  /**
    * Compare two setups using the following priorities:
    *   - number of cards used
    *   - number of key cards used
    *   - amount of gold used (cards that provide income count towards this)
    *   - use of limited cards (as a whole)
    *   - number of avoidable cards used
    *   - distinct characters used
    *   - total strength
    */
  override def compare(that: Setup): Int = Seq(
    this.validCards.size.compareTo(that.validCards.size),
    this.keyCardCount.compareTo(that.keyCardCount),
    this.virtualGoldUsed.compareTo(that.virtualGoldUsed),
    this.hasLimited.compareTo(that.hasLimited),
    that.avoidableCardCount.compareTo(this.avoidableCardCount),
    this.distinctCharacterCount.compareTo(that.distinctCharacterCount),
    this.totalStrength.compareTo(that.totalStrength)
  ).find(_ != 0).getOrElse(0)

  // TODO implement a better toString
  override def toString: String = validCards.toString
}
