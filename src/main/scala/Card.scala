
trait Card {
  def name: String
  def traits: Option[List[String]]
  def flavourText: Option[String]
  def limit: Int
  def code: String
  def number: Int
  def pack: Pack
  def illustrator: String
}

trait Cost {
	def cost: String
	def printedCost: Int
}

trait Allegiance {
	def faction: Faction
	def loyalty: Boolean
}

trait Uniqueness {
	def unique: Boolean
}

case class Character(
	name: String,
	traits: Option[List[String]],
	flavourText: Option[String],
	limit: Int,
  code: String,
  number: Int,
  pack: Pack,
	illustrator: String,

	cost: String,
	printedCost: Int,

	faction: Faction,
	loyalty: Boolean,

	unique: Boolean,

	military: Boolean,
	intrigue: Boolean,
	power: Boolean,
	strength: Int
) extends Card with Cost with Allegiance with Uniqueness

case class Attachment(
  name: String,
	traits: Option[List[String]],
	flavourText: Option[String],
	limit: Int,
  code: String,
  number: Int,
  pack: Pack,
	illustrator: String,

	cost: String,
	printedCost: Int,

	faction: Faction,
	loyalty: Boolean,

	unique: Boolean
) extends Card with Cost with Allegiance with Uniqueness

case class Location(
	name: String,
	traits: Option[List[String]],
	flavourText: Option[String],
	limit: Int,
  code: String,
  number: Int,
  pack: Pack,
	illustrator: String,

	cost: String,
	printedCost: Int,

	faction: Faction,
	loyalty: Boolean,

	unique: Boolean
) extends Card with Cost with Allegiance with Uniqueness

case class Event(
	name: String,
	traits: Option[List[String]],
	flavourText: Option[String],
	limit: Int,
  code: String,
  number: Int,
  pack: Pack,
	illustrator: String,

	cost: String,
	printedCost: Int,

	faction: Faction,
	loyalty: Boolean
) extends Card with Cost with Allegiance

case class Plot(
	name: String,
	traits: Option[List[String]],
	flavourText: Option[String],
	limit: Int,
  code: String,
  number: Int,
  pack: Pack,
	illustrator: String,

	faction: Faction,
	loyalty: Boolean,

	income: Int,
	initiative: Int,
	claim: Int,
	reserve: Int
) extends Card with Allegiance

case class Agenda(
	name: String,
	traits: Option[List[String]],
	flavourText: Option[String],
	limit: Int,
  code: String,
  number: Int,
  pack: Pack,
	illustrator: String
) extends Card

case class Title(
  name: String,
	traits: Option[List[String]],
	flavourText: Option[String],
	limit: Int,
	code: String,
	number: Int,
	pack: Pack,
	illustrator: String
) extends Card
