package akka.duke.taskmanager

import akka.actor.{ActorRef, ActorRefFactory}


trait TaskLoader {

  def load(TaskId: String, TaskName: String, actorFactory: ActorRefFactory): Option[ActorRef]

}
