package pt.afsmeira.lightbringer.model

trait Card {
  def name: String
  def traits: Option[Seq[String]]
  def flavourText: Option[String]
  def limit: Int
  def code: String
  def number: Int
  def pack: Pack
}

trait Cost {
  def cost: String
  def printedCost: Int
}

trait Allegiance {
  def faction: Faction
  def loyalty: Boolean
}

trait Marshallable {
  def unique: Boolean
  def limited: Boolean
  def economy: Boolean
  def income: Option[Int]
  def bestow: Option[Int]
}

trait DrawCard extends Card with Cost with Allegiance

case class Character(
  name: String,
  traits: Option[Seq[String]],
  flavourText: Option[String],
  limit: Int,
  code: String,
  number: Int,
  pack: Pack,

  cost: String,
  printedCost: Int,

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
) extends DrawCard with Marshallable {
  override def toString: String = s"Character: $name"
}

case class Attachment(
  name: String,
  traits: Option[Seq[String]],
  flavourText: Option[String],
  limit: Int,
  code: String,
  number: Int,
  pack: Pack,

  cost: String,
  printedCost: Int,

  faction: Faction,
  loyalty: Boolean,

  unique: Boolean,
  limited: Boolean,
  economy: Boolean,
  income: Option[Int],
  bestow: Option[Int]
) extends DrawCard with Marshallable {
  override def toString: String = s"Attachment: $name"
}

case class Location(
  name: String,
  traits: Option[Seq[String]],
  flavourText: Option[String],
  limit: Int,
  code: String,
  number: Int,
  pack: Pack,

  cost: String,
  printedCost: Int,

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

case class Event(
  name: String,
  traits: Option[Seq[String]],
  flavourText: Option[String],
  limit: Int,
  code: String,
  number: Int,
  pack: Pack,

  cost: String,
  printedCost: Int,

  faction: Faction,
  loyalty: Boolean
) extends DrawCard {
  override def toString: String = s"Event: $name"
}

case class Plot(
  name: String,
  traits: Option[Seq[String]],
  flavourText: Option[String],
  limit: Int,
  code: String,
  number: Int,
  pack: Pack,

  faction: Faction,
  loyalty: Boolean,

  income: Int,
  initiative: Int,
  claim: String,
  printedClaim: Int,
  reserve: Int
) extends Card with Allegiance {
  override def toString: String = s"Plot: $name"
}

case class Agenda(
  name: String,
  traits: Option[Seq[String]],
  flavourText: Option[String],
  limit: Int,
  code: String,
  number: Int,
  pack: Pack
) extends Card {
  override def toString: String = s"Agenda: $name"
}

case class Title(
  name: String,
  traits: Option[Seq[String]],
  flavourText: Option[String],
  limit: Int,
  code: String,
  number: Int,
  pack: Pack
) extends Card {
  override def toString: String = s"Title: $name"
}
