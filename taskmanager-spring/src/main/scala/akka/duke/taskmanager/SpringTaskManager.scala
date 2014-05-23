package akka.duke.taskmanager

import akka.actor.{Props, ActorRef, ActorRefFactory}


trait SpringTaskManager extends TaskManager {
  val taskLoader = new TaskLoader {
    def load(TaskId: String, TaskName: String, actorFactory: ActorRefFactory): Option[ActorRef] = {
      Option(actorFactory.actorOf(Props(classOf[SpringActorProducer], TaskId), TaskName))
    }
  }
}
