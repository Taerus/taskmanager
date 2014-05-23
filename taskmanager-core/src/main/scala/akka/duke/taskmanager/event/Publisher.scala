package akka.duke.taskmanager.event

import akka.actor.ActorRef
import scala.collection.mutable
import akka.duke.taskmanager.ComposableActor
import akka.duke.taskmanager.Message.{Request, Command, Event}


trait Publisher { this: ComposableActor =>
  import Publisher._

  val listeners = mutable.Set.empty[ActorRef]

  receiveBuilder += {
    case AddListener(listener)    => listeners += listener
    case RemoveListener(listener) => listeners -= listener
    case RemoveAllListeners       => listeners.clear()
    case GetListeners             => sender ! listeners
  }

//  final def publish(event: Event, forward: Boolean = false) {
//    listeners foreach { _ ! event }
//  }
//  final def publish(event: Event) {
//    publish(event, false)
//  }
//
  final def publish(event: Event, forward: Boolean = false) {
    if (forward) {
      listeners foreach { _ forward event }
    } else {
      listeners foreach { _ ! event }
    }
  }

}

object Publisher {

  case class  AddListener(listener: ActorRef)     extends Command
  case class  RemoveListener(listener: ActorRef)  extends Command
  case object RemoveAllListeners                  extends Command

  case object GetListeners                        extends Request

}


