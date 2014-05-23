package akka.duke.taskmanager.event

import akka.duke.taskmanager.ComposableActor
import akka.duke.taskmanager.Message.Event


trait Listener { this: ComposableActor =>

  receiveBuilder += {
    case e: Event => handleEvent(e)
  }

  def handleEvent(event: Event): Unit

}
