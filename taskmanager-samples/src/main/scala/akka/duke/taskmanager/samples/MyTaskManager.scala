package akka.duke.taskmanager.samples

import akka.actor.{Props, ActorRef, ActorRefFactory}
import akka.duke.taskmanager.{TaskLoader, TaskManager}


class MyTaskManager extends TaskManager {

  val taskLoader = new TaskLoader {

    def load(taskId: String, taskName: String, actorFactory: ActorRefFactory, source: String) = {
      TaskRegistery(taskId).map { taskClass =>
        actorFactory.actorOf(Props(taskClass.newInstance()), taskName)
      }
    }

    def list(source: String): List[String] = {
      TaskRegistery.taskIds
    }

  }

}
