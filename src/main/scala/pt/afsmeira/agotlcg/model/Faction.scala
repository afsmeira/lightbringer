package pt.afsmeira.agotlcg.model

sealed trait Faction {
  def name: String
}

object Faction {

  case object Neutral extends Faction {
    val name: String = "Neutral"
  }
  case object Baratheon extends Faction {
    val name: String = "House Baratheon"
  }
  case object Greyjoy extends Faction {
    val name: String = "House Greyjoy"
  }
  case object Lannister extends Faction {
    val name: String = "House Lannister"
  }
  case object Martell extends Faction {
    val name: String = "House Martell"
  }
  case object TheWatch extends Faction {
    val name: String = "The Night's Watch"
  }
  case object Stark extends Faction {
    val name: String = "House Stark"
  }
  case object Targaryen extends Faction {
    val name: String = "House Targaryen"
  }
  case object Tyrell extends Faction {
    val name: String = "House Tyrell"
  }

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
