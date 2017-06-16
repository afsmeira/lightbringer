package pt.afsmeira.lightbringer.model

/**
  * The faction of a card.
  */
sealed trait Faction {
  /** The faction's name. */
  def name: String
  /** The faction's keyword. */
  def keyword: Option[String]
}

/**
  * Companion object that provides concrete types of [[Faction]].
  */
object Faction {

  /** The Neutral faction. This is the faction of all cards that do not have an explicit faction. */
  case object Neutral extends Faction {
    val name: String = "Neutral"
    val keyword: Option[String] = None
  }
  case object Baratheon extends Faction {
    val name: String = "House Baratheon"
    val keyword: Option[String] = Some("baratheon")
  }
  case object Greyjoy extends Faction {
    val name: String = "House Greyjoy"
    val keyword: Option[String] = Some("greyjoy")
  }
  case object Lannister extends Faction {
    val name: String = "House Lannister"
    val keyword: Option[String] = Some("lannister")
  }
  case object Martell extends Faction {
    val name: String = "House Martell"
    val keyword: Option[String] = Some("martell")
  }
  case object TheWatch extends Faction {
    val name: String = "The Night's Watch"
    val keyword: Option[String] = Some("thenightswatch")
  }
  case object Stark extends Faction {
    val name: String = "House Stark"
    val keyword: Option[String] = Some("stark")
  }
  case object Targaryen extends Faction {
    val name: String = "House Targaryen"
    val keyword: Option[String] = Some("targaryen")
  }
  case object Tyrell extends Faction {
    val name: String = "House Tyrell"
    val keyword: Option[String] = Some("tyrell")
  }

  /** All possible values for [[pt.afsmeira.lightbringer.model.Faction]]. */
  val values = Seq(
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
