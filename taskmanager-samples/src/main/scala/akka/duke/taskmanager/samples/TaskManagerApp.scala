package akka.duke.taskmanager.samples

import akka.actor.{ActorSystem, Props}
import akka.duke.taskmanager.Task.Start
import akka.duke.taskmanager.TaskManager.{AddTask, TaskCmd}


object TaskManagerApp extends App {

  val system = ActorSystem("taskmanager-system")
  val taskManager = system.actorOf(Props[MyTaskManager], "taskmanager")

  taskManager ! AddTask("helloWorld", "helloWorld")
  taskManager ! TaskCmd(Start, "helloWorld")

  Thread.sleep(1000)

  system.shutdown()
  system.awaitTermination()

}
