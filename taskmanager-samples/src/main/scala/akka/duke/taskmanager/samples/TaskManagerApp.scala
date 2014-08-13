package akka.duke.taskmanager.samples

import akka.actor.{ActorSystem, Props}
import akka.duke.taskmanager.ConfDef
import akka.duke.taskmanager.Task._
import akka.duke.taskmanager.TaskManager._
import com.typesafe.config.ConfigFactory


object TaskManagerApp extends App {
  import Thread.sleep

  val system = ActorSystem("taskmanager-system", ConfigFactory.parseString(""" akka.loglevel = "OFF" """))
  val taskManager = system.actorOf(Props[MyTaskManager], "taskmanager")

  println("==== helloWorld ====")
  taskManager ! AddTask("helloWorld", "helloWorld")
  taskManager ! TaskCmd(Start, "helloWorld")
  sleep(500)
  println("\\___________________\n")


  println("===== pausable =====")
  taskManager ! AddTask("pausable", "pausable")
  taskManager ! TaskCmd(Start, "pausable")
  sleep(2000)
  taskManager ! TaskCmd(Pause, "pausable")
  sleep(1000)
  taskManager ! TaskCmd(Start, "pausable")
  sleep(2000)
  taskManager ! TaskCmd(Stop, "pausable")
  sleep(500)
  println("\\___________________\n")


  println("=== simpleWorker ===")
  taskManager ! AddTask("simpleWorker", "simpleWorker")
  taskManager ! SetTaskConfig(ConfDef.parseString(""" name = "Guy" """), "simpleWorker")
  taskManager ! TaskCmd(Start, "simpleWorker")
  sleep(500)
  println("|------------------- ")
  taskManager ! SetTaskConfig(ConfDef.parseString(""" name = "Jack" """), "simpleWorker")
  taskManager ! TaskCmd(Start, "simpleWorker")
  sleep(500)
  println("\\___________________\n")

  system.shutdown()
  system.awaitTermination()

}
