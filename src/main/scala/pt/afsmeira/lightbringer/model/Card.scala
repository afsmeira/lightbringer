package pt.afsmeira.lightbringer.model

/**
  * Basic characteristics that define a card of A Game of Thrones LCG 2nd edition.
  */
trait Card {
  /** Card name. */
  def name: String
  /** Card traits. */
  def traits: Seq[String]
  /** Small text that gives you a bit of context about the card. */
  def flavourText: Option[String]
  /** How many copies of this card can be in a deck. */
  def limit: Int
  /** Internal identification code. */
  def code: String
  /** Card number in its corresponding [[pt.afsmeira.lightbringer.model.Pack]]. */
  def number: Int
  /** The pack to which this card belongs. */
  def pack: Pack
}

/**
  * Characteristics of a card that has a gold cost to be played.
  */
trait Cost {
  /** The actual cost printed on the card. This is a string because it can be X. */
  def printedCost: String
  /** The integer value of `printedCost`. If `printedCost` is X, `cost` should be 0. */
  def cost: Int
}

/**
  * Faction characteristics of a card.
  */
trait Allegiance {
  /** The faction to which this card belongs. */
  def faction: Faction
  /** Whether this card is loyal. */
  def loyalty: Boolean
}

/**
  * Characteristics of a card that can be marshalled.
  */
trait Marshallable {
  /** Whether this card is unique. */
  def unique: Boolean
  /** Whether this card has the `Limited` keyword. */
  def limited: Boolean
  /** Whether this card provides any sort of economy benefits. */
  def economy: Boolean
  /** Additional income provided by this card. */
  def income: Option[Int]
  /** Whether this card has the `Bestow` keyword, and what is its value. */
  def bestow: Option[Int]
}

/**
  * Trait that defines whether an attachment is eligible to be attached to this target.
  */
trait AttachmentEligibility {
  /**
    * Check if the argument is eligible to be attached to who implements this trait.
    *
    * @param attachment The attachment to check
    * @return `true` if `attachment` is eligible, `false` otherwise.
    */
  def eligible(attachment: Attachment): Boolean
}

/**
  * Characteristics of a card that you can draw from the draw deck.
  *
  * @see [[pt.afsmeira.lightbringer.model.Card]]
  * @see [[pt.afsmeira.lightbringer.model.Cost]]
  * @see [[pt.afsmeira.lightbringer.model.Allegiance]]
  */
trait DrawCard extends Card with Cost with Allegiance

/**
  * A character in A Game of Thrones.
  *
  * @param military Whether this character has a military icon.
  * @param intrigue Whether this character has an intrigue icon.
  * @param power    Whether this character has a power icon.
  * @param strength The value of this character's strength.
  *
  * @see [[pt.afsmeira.lightbringer.model.Card]]
  * @see [[pt.afsmeira.lightbringer.model.Cost]]
  * @see [[pt.afsmeira.lightbringer.model.Allegiance]]
  * @see [[pt.afsmeira.lightbringer.model.Marshallable]]
  * @see [[pt.afsmeira.lightbringer.model.AttachmentEligibility]]
  */
case class Character(
  name: String,
  traits: Seq[String],
  flavourText: Option[String],
  limit: Int,
  code: String,
  number: Int,
  pack: Pack,

  printedCost: String,
  cost: Int,

  faction: Faction,
  loyalty: Boolean,

  unique: Boolean,
  limited: Boolean,
  economy: Boolean,
  income: Option[Int],
  bestow: Option[Int],

  military: Boolean,
  intrigue: Boolean,
  power: Boolean,
  strength: Int
) extends DrawCard with Marshallable with AttachmentEligibility {
  override def toString: String = s"Character: $name"

  /** Check if attachment restrictions match character's characteristics. */
  def eligible(attachment: Attachment): Boolean =
    (attachment.restrictions.unique && unique) &&
    attachment.restrictions.faction.forall(_ == faction) &&
    (attachment.restrictions.traits.isEmpty || attachment.restrictions.traits.exists(traits.contains))
}

/**
  * An Attachment in A Game of Thrones.
  *
  * @param restrictions The restrictions a card must fulfill in order to receive this attachment.
  *
  * @see [[pt.afsmeira.lightbringer.model.Card]]
  * @see [[pt.afsmeira.lightbringer.model.Cost]]
  * @see [[pt.afsmeira.lightbringer.model.Allegiance]]
  * @see [[pt.afsmeira.lightbringer.model.Marshallable]]
  */
case class Attachment(
  name: String,
  traits: Seq[String],
  flavourText: Option[String],
  limit: Int,
  code: String,
  number: Int,
  pack: Pack,

  printedCost: String,
  cost: Int,

  faction: Faction,
  loyalty: Boolean,

  unique: Boolean,
  limited: Boolean,
  economy: Boolean,
  income: Option[Int],
  bestow: Option[Int],

  restrictions: Attachment.Restrictions
) extends DrawCard with Marshallable {
  override def toString: String = s"Attachment: $name"
}

/**
  * Companion object that provides additional functionality for the [[pt.afsmeira.lightbringer.model.Attachment]] class.
  */
object Attachment {

  /**
    * Restrictions of an attachment.
    *
    * @param terminal Whether this attachment has the `Terminal` keyword.
    * @param unique   Whether this attachment can only be attached to unique cards.
    * @param opponent Whether this attachment can only be attached to an opponent's card.
    * @param faction  Faction that a card must have in order to receive the attachment. `None` means unrestricted.
    * @param traits   Traits that a card must have in order to receive the attachment. Empty sequence means
    *                 unrestricted.
    */
  case class Restrictions(
    terminal: Boolean,
    unique: Boolean,
    opponent: Boolean,
    faction: Option[Faction],
    traits: Seq[String]
  )
}

/**
  * A location in A Game of Thrones.
  *
  * @see [[pt.afsmeira.lightbringer.model.Card]]
  * @see [[pt.afsmeira.lightbringer.model.Cost]]
  * @see [[pt.afsmeira.lightbringer.model.Allegiance]]
  * @see [[pt.afsmeira.lightbringer.model.Marshallable]]
  */
case class Location(
  name: String,
  traits: Seq[String],
  flavourText: Option[String],
  limit: Int,
  code: String,
  number: Int,
  pack: Pack,

  printedCost: String,
  cost: Int,

  faction: Faction,
  loyalty: Boolean,

  unique: Boolean,
  limited: Boolean,
  economy: Boolean,
  income: Option[Int],
  bestow: Option[Int]
) extends DrawCard with Marshallable {
  override def toString: String = s"Location: $name"
}

/**
  * An event in A Game of Thrones.
  *
  * @see [[pt.afsmeira.lightbringer.model.Card]]
  * @see [[pt.afsmeira.lightbringer.model.Cost]]
  * @see [[pt.afsmeira.lightbringer.model.Allegiance]]
  */
case class Event(
  name: String,
  traits: Seq[String],
  flavourText: Option[String],
  limit: Int,
  code: String,
  number: Int,
  pack: Pack,

  printedCost: String,
  cost: Int,

  faction: Faction,
  loyalty: Boolean
) extends DrawCard {
  override def toString: String = s"Event: $name"
}

/**
  * A plot in A Game of Thrones.
  *
  * @param printedIncome The actual income printed on the card. This is a string because it can be X.
  * @param income        The integer value of `printedIncome`. If `printedIncome` is X, `income` should be 0.
  * @param initiative    Initiative value for this plot.
  * @param printedClaim  The actual claim printed on the card. This is a string because it can be X.
  * @param claim         The integer value of `printedClaim`. If `printedClaim` is X, `claim` should be 0.
  * @param reserve       Reserve value for this plot.
  *
  * @see [[pt.afsmeira.lightbringer.model.Card]]
  * @see [[pt.afsmeira.lightbringer.model.Allegiance]]
  */
case class Plot(
  name: String,
  traits: Seq[String],
  flavourText: Option[String],
  limit: Int,
  code: String,
  number: Int,
  pack: Pack,

  faction: Faction,
  loyalty: Boolean,

  printedIncome: String,
  income: Int,
  initiative: Int,
  printedClaim: String,
  claim: Int,
  reserve: Int
) extends Card with Allegiance {
  override def toString: String = s"Plot: $name"
}

/**
  * An agenda in A Game of Thrones.
  *
  * @see [[pt.afsmeira.lightbringer.model.Card]]
  */
case class Agenda(
  name: String,
  traits: Seq[String],
  flavourText: Option[String],
  limit: Int,
  code: String,
  number: Int,
  pack: Pack
) extends Card {
  override def toString: String = s"Agenda: $name"
}

/**
  * A title in A Game of Thrones.
  *
  * @see [[pt.afsmeira.lightbringer.model.Card]]
  */
case class Title(
  name: String,
  traits: Seq[String],
  flavourText: Option[String],
  limit: Int,
  code: String,
  number: Int,
  pack: Pack
) extends Card {
  override def toString: String = s"Title: $name"
}

/**
  * Object that provides enrichment classes over collections of cards.
  */
object RichCards {

  /**
    * Enrichment class for a sequence of draw deck cards that can be marshalled.
    *
    * @param cards The draw deck cards to be enriched.
    * @tparam T The specific type of the card sequence to enrich.
    *
    * @see [[pt.afsmeira.lightbringer.model.DrawCard]]
    * @see [[pt.afsmeira.lightbringer.model.Marshallable]]
    */
  implicit class RichMarshallableDrawCards[T <: DrawCard with Marshallable](val cards: Seq[T]) extends AnyVal {
    /** Returns a new sequence without any [[pt.afsmeira.lightbringer.model.Marshallable#unique]] duplicates. */
    def deduplicate: Seq[T] =
      cards.groupBy(_.unique).flatMap {
        case (false, nonUniques) => nonUniques
        case (true,  uniques)    => uniques.distinct
      }.toSeq
  }

  /**
    * Enrichment class for a sequence of draw deck cards.
    *
    * @param cards The draw deck cards to be enriched.
    * @tparam T The specific type of cards.
    */
  implicit class RichDrawCards[T <: DrawCard](val cards: Seq[T]) extends AnyVal {
    /** Returns a new sequence without any cards which [[pt.afsmeira.lightbringer.model.Cost#printedCost]] is `X`. */
    def filterCostX: Seq[T] = cards.filter(_.printedCost != "X")
  }
}
