package akka.duke.taskmanager

import akka.actor.ActorRef
import akka.duke.taskmanager.event.Listener
import akka.duke.taskmanager.event.Publisher.AddListener
import akka.duke.taskmanager.Message.Event
import scala.swing.Swing


class SwingActorListener(reaction: PartialFunction[Event, Unit], publishers: ActorRef*) extends ComposableActor with Listener {

  publishers.foreach ( _ ! AddListener(self) )

  def handleEvent(event: Event) {
    if (reaction isDefinedAt event) {
      Swing.onEDT {
        reaction(event)
      }
    }
  }

}
