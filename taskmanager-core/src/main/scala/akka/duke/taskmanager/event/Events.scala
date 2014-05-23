package akka.duke.taskmanager.event

import akka.duke.taskmanager.Message.Event


object Events {

  case class InfoEvent(msg: String)     extends Event
  case class WarningEvent(msg: String)  extends Event
  case class ErrorEvent(msg: String)    extends Event

}
