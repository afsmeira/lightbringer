package pt.afsmeira.lightbringer.setup

import pt.afsmeira.lightbringer.model.{Character, DrawCard, Marshallable}
import pt.afsmeira.lightbringer.model.RichCards.RichMarshallableDrawCards

object Setup {
  type ValidCard = DrawCard with Marshallable
}

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

  val keyCardCount: Int = deduplicatedCards.count { card =>
    settings.keyCards.contains(card.code) || settings.keyCards.contains(card.name)
  }
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

  val isPoor: Boolean =
    (settings.requireTwoCharacters     && !hasTwoCharacters)     ||
    (settings.requireFourCostCharacter && !hasFourCostCharacter) ||
    (settings.requireEconomy           && !hasEconomy)           ||
    (settings.requireKeyCard           && keyCardCount == 0)     ||
    (settings.minCardsRequired > validCards.size)

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
