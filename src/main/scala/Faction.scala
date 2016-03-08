
sealed abstract class Faction(val name: String)

case object Neutral   extends Faction("Neutral")
case object Baratheon extends Faction("House Baratheon")
case object Greyjoy   extends Faction("House Greyjoy")
case object Lannister extends Faction("House Lannister")
case object Martell   extends Faction("House Martell")
case object TheWatch  extends Faction("The Night's Watch")
case object Stark     extends Faction("House Stark")
case object Targaryen extends Faction("House Targaryen")
case object Tyrell    extends Faction("House Tyrell")

object Faction {
  val values = List(
    Neutral,
    Baratheon,
    Greyjoy,
    Lannister,
    Martell,
    TheWatch,
    Stark,
    Targaryen,
    Tyrell
  )
}