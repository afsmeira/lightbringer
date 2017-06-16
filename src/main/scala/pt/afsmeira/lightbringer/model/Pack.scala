package pt.afsmeira.lightbringer.model

/**
  * A cycle represents an history arch in the publishing of A Game of Thrones LCG 2nd edition.
  */
sealed trait Cycle

/**
  * Companion object that provides concrete types of [[Cycle]].
  */
object Cycle {
  case object Core           extends Cycle
  case object Deluxe         extends Cycle
  case object Draft          extends Cycle
  case object Westeros       extends Cycle
  case object WarOfFiveKings extends Cycle
  case object BloodAndGold   extends Cycle
}

/**
  * A pack is a group of cards released at the same time, and it belongs to a cycle. A cycle can have several packs.
  */
sealed trait Pack {
  /** The name of the pack. */
  def name: String
  /** The cycle to which the pack belongs. */
  def cycle: Cycle
}

/**
  * Companion object that provides concrete types of [[Pack]].
  */
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

  case object AllMenAreFools extends Pack {
    val name: String = "All Men Are Fools"
    val cycle: Cycle = Cycle.BloodAndGold
  }
  case object GuardingTheRealm extends Pack {
    val name: String = "Guarding the Realm"
    val cycle: Cycle = Cycle.BloodAndGold
  }
  case object TheFallOfAstapor extends Pack {
    val name: String = "The Fall of Astapor"
    val cycle: Cycle = Cycle.BloodAndGold
  }
  case object TheRedWedding extends Pack {
    val name: String = "The Red Wedding"
    val cycle: Cycle = Cycle.BloodAndGold
  }
  case object OberynsRevenge extends Pack {
    val name: String = "Oberyn's Revenge"
    val cycle: Cycle = Cycle.BloodAndGold
  }
  case object TheBrotherhoodWithoutBanners extends Pack {
    val name: String = "The Brotherhood Without Banners"
    val cycle: Cycle = Cycle.BloodAndGold
  }

  case object WatchersOnTheWall extends Pack {
    val name: String = "Watchers on the Wall"
    val cycle: Cycle = Cycle.Deluxe
  }

  case object ValyrianDraftSet extends Pack {
    val name: String = "Valyrian Draft Set"
    val cycle: Cycle = Cycle.Draft
  }

  /** All possible values for [[pt.afsmeira.lightbringer.model.Pack]]. */
  val values = Seq(
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
    AllMenAreFools,
    GuardingTheRealm,
    TheFallOfAstapor,
    TheRedWedding,
    OberynsRevenge,
    TheBrotherhoodWithoutBanners,
    WatchersOnTheWall,
    ValyrianDraftSet
  )
}
