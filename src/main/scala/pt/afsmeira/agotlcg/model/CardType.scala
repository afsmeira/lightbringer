package pt.afsmeira.agotlcg.model


sealed trait CardType

object CardType {

  case object Agenda     extends CardType
  case object Attachment extends CardType
  case object Character  extends CardType
  case object Event      extends CardType
  case object Location   extends CardType
  case object Plot       extends CardType
  case object Title      extends CardType

  val values = List(
    Agenda,
    Attachment,
    Character,
    Event,
    Location,
    Plot,
    Title
  )
}
