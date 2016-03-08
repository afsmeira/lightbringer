
sealed abstract class CardType(val typeName: String)

case object AgendaType     extends CardType("Agenda")
case object AttachmentType extends CardType("Attachment")
case object CharacterType  extends CardType("Character")
case object EventType      extends CardType("Event")
case object LocationType   extends CardType("Location")
case object PlotType       extends CardType("Plot")
case object TitleType      extends CardType("Title")

object CardType {
  val values = List(
    AgendaType,
    AttachmentType,
    CharacterType,
    EventType,
    LocationType,
    PlotType,
    TitleType
  )
}
