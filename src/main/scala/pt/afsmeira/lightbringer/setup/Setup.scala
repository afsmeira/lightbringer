package pt.afsmeira.lightbringer.setup

import pt.afsmeira.lightbringer.model.{Character, DrawCard, Marshallable}

object Setup {
  type ValidCard = DrawCard with Marshallable

  def deduplicate(cards: Seq[Setup.ValidCard]): Seq[Setup.ValidCard] =
    cards.groupBy(_.unique).flatMap {
      case (false, nonUniques) => nonUniques
      case (true, uniques)     => uniques.distinct
    }.toSeq
}

case class Setup(cards: Seq[Setup.ValidCard], settings: SetupSettings) extends Ordered[Setup] {

  private val deduplicatedCards: Seq[Setup.ValidCard] = Setup.deduplicate(cards)

  private val goldUsed   = deduplicatedCards.map(_.printedCost).sum + deduplicatedCards.count(_.economy)
  private val hasEconomy = deduplicatedCards.exists(_.economy)
  private val hasLimited = deduplicatedCards.exists(_.limited)

  private val keyCardCount = deduplicatedCards.count { card =>
    settings.keyCards.contains(card.code) || settings.keyCards.contains(card.name)
  }
  private val avoidableCardCount = deduplicatedCards.count { card =>
    settings.avoidableCards.contains(card.code) || settings.avoidableCards.contains(card.name)
  }

  private val characters = deduplicatedCards.collect {
    case card: Character => card
  }
  private val hasTwoCharacters       = characters.size >= 2
  private val hasFourCostCharacter   = characters.exists(_.printedCost >= 4)
  private val distinctCharacterCount = characters.size
  private val totalStrength          = characters.map(_.strength).sum

  val isPoor: Boolean =
    (settings.requireTwoCharacters     && !hasTwoCharacters) ||
    (settings.requireFourCostCharacter && !hasFourCostCharacter) ||
    (settings.requireEconomy           && !hasEconomy) ||
    (settings.requireKeyCard           && keyCardCount == 0) ||
    (settings.minCardsRequired > cards.size)

  override def compare(that: Setup): Int = Seq(
    this.cards.size.compareTo(that.cards.size),
    this.keyCardCount.compareTo(that.keyCardCount),
    this.goldUsed.compareTo(that.goldUsed),
    this.hasLimited.compareTo(that.hasLimited),
    that.avoidableCardCount.compareTo(this.avoidableCardCount),
    this.distinctCharacterCount.compareTo(that.distinctCharacterCount),
    this.totalStrength.compareTo(that.totalStrength)
  ).find(_ != 0).getOrElse(0)
}
