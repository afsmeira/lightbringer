
sealed abstract class Pack(id: Int, val name: String, cycle: String)

case object Core           extends Pack(1, "Core Set",         "Core")
case object TakingTheBlack extends Pack(2, "Taking the Black", "Westeros")

object Pack {
  val values = List(Core, TakingTheBlack)
}
