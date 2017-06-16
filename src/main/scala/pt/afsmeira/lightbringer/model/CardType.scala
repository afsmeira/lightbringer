package pt.afsmeira.lightbringer.model

/**
  * Trait that defines the type of a card.
  * <p>
  * This trait is used to help in unmarshalling a JSON object that represents a card.
  */
sealed trait CardType

/**
  * Companion object that provides concrete types of [[CardType]].
  */
object CardType {

  case object Agenda     extends CardType
  case object Attachment extends CardType
  case object Character  extends CardType
  case object Event      extends CardType
  case object Location   extends CardType
  case object Plot       extends CardType
  case object Title      extends CardType

  /** All possible values for [[pt.afsmeira.lightbringer.model.CardType]]. */
  val values = Seq(
    Agenda,
    Attachment,
    Character,
    Event,
    Location,
    Plot,
    Title
  )
}
