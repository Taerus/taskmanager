package akka.duke.taskmanager

object Message {

  sealed trait MessageType
  trait Command   extends MessageType
  trait Request   extends MessageType
  trait Responce  extends MessageType
  trait Event     extends MessageType
  trait Error     extends MessageType


  case object SaveState                               extends Command
  case object LoadState                               extends Command

  case class BadRequest(err: String)                  extends Error with Responce
  case class CommandError(cmd: Command, err: String)  extends Error

}


