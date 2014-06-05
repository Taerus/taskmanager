package akka.duke.taskmanager

import akka.actor.{Props, ActorRef, ActorRefFactory}
import org.springframework.context.ApplicationContext


trait SpringTaskManager extends TaskManager {
  val taskLoader = new TaskLoader {
    def load(taskId: String, taskName: String, actorFactory: ActorRefFactory, source: String = null): Option[ActorRef] = {
      Option(actorFactory.actorOf(Props(classOf[SpringActorProducer], taskId), taskName))
    }

    def list(source: String): List[String] = {
      Context[ApplicationContext]("springContext").getBeanNamesForType(classOf[Task]).toList
    }
  }
}
