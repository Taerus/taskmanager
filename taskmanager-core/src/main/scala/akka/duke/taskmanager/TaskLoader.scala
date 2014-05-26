package akka.duke.taskmanager

import akka.actor.{ActorRef, ActorRefFactory}


trait TaskLoader {

  def load(taskId: String, taskName: String, actorFactory: ActorRefFactory): Option[ActorRef]

}
