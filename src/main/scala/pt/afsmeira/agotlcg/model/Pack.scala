package pt.afsmeira.agotlcg.model

sealed trait Cycle

object Cycle {
  case object Core           extends Cycle
  case object Deluxe         extends Cycle
  case object Draft          extends Cycle
  case object Westeros       extends Cycle
  case object WarOfFiveKings extends Cycle
}

sealed trait Pack {
  def name: String
  def cycle: Cycle
}

object Pack {
  case object CoreSet extends Pack {
    val name: String = "Core Set"
    val cycle: Cycle = Cycle.Core
  }

  case object TakingTheBlack extends Pack {
    val name: String = "Taking the Black"
    val cycle: Cycle = Cycle.Westeros
  }
  case object TheRoadToWinterfell extends Pack {
    val name: String = "The Road to Winterfell"
    val cycle: Cycle = Cycle.Westeros
  }
  case object TheKingsPeace extends Pack {
    val name: String = "The King's Peace"
    val cycle: Cycle = Cycle.Westeros
  }
  case object NoMiddleGroud extends Pack {
    val name: String = "No Middle Ground"
    val cycle: Cycle = Cycle.Westeros
  }
  case object CalmOverWesteros extends Pack {
    val name: String = "Calm over Westeros"
    val cycle: Cycle = Cycle.Westeros
  }
  case object TrueSteel extends Pack {
    val name : String = "True Steel"
    val cycle: Cycle = Cycle.Westeros
  }

  case object WolvesOfTheNorth extends Pack {
    val name: String = "Wolves of the North"
    val cycle: Cycle = Cycle.Deluxe
  }

  case object AcrossTheSevenKingdoms extends Pack {
    val name: String = "Across the Seven Kingdoms"
    val cycle: Cycle = Cycle.WarOfFiveKings
  }
  case object CalledToArms extends Pack {
    val name: String = "Called to Arms"
    val cycle: Cycle = Cycle.WarOfFiveKings
  }
  case object ForFamilyHonor extends Pack {
    val name: String = "For Family Honor"
    val cycle: Cycle = Cycle.WarOfFiveKings
  }
  case object ThereIsMyClaim extends Pack {
    val name: String = "There Is My Claim"
    val cycle: Cycle = Cycle.WarOfFiveKings
  }
  case object GhostsOfHarrenhal extends Pack {
    val name: String = "Ghosts of Harrenhal"
    val cycle: Cycle = Cycle.WarOfFiveKings
  }
  case object TyrionsChain extends Pack {
    val name: String = "Tyrion's Chain"
    val cycle: Cycle = Cycle.WarOfFiveKings
  }

  case object LionsOfCasterlyRock extends Pack {
    val name: String = "Lions of Casterly Rock"
    val cycle: Cycle = Cycle.Deluxe
  }

  case object ValyrianDraftSet extends Pack {
    val name: String = "Valyrian Draft Set"
    val cycle: Cycle = Cycle.Draft
  }

  val values = List(
    CoreSet,
    TakingTheBlack,
    TheRoadToWinterfell,
    TheKingsPeace,
    NoMiddleGroud,
    CalmOverWesteros,
    TrueSteel,
    WolvesOfTheNorth,
    AcrossTheSevenKingdoms,
    CalledToArms,
    ForFamilyHonor,
    ThereIsMyClaim,
    GhostsOfHarrenhal,
    TyrionsChain,
    LionsOfCasterlyRock,
    ValyrianDraftSet
  )
}
