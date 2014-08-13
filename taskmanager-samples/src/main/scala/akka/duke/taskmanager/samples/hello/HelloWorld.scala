package akka.duke.taskmanager.samples.hello

import akka.actor.{Cancellable, ActorSystem, ActorRefFactory, Props}
import akka.duke.taskmanager.{Task, TaskLoader, TaskManager, Pausable}
import akka.duke.taskmanager.TaskManager.{TaskCmd, AddTask}
import akka.duke.taskmanager.Task.{Stop, Pause, Start}

import scala.concurrent.duration._

class HelloWorldTask extends Task with Pausable {

  var sch: Cancellable = _

  def start() {
    println("Hello World Task is starting")
    startOrResumeTask()
  }

  override def stop() {
    println("Hello World Task is stopping")
    sch.cancel()
  }

  def pause() {
    println("Hello World Task is pausing")
    sch.cancel()
  }

  def resume() {
    println("Hello World Task is resuming")
    startOrResumeTask()
  }

  def startOrResumeTask() {
    //Akka dispatcher to run our task using the same thread pool as actors use.
    import context.dispatcher
    sch = context.system.scheduler.schedule(Duration.Zero, 1.second) {
      println("Hello World !")
    }
  }
}


class HelloWorldTaskManager extends TaskManager {

  override val taskLoader: TaskLoader = new TaskLoader {

    override def load(taskId: String, taskName: String, actorFactory: ActorRefFactory, source: String) = {
      Option(actorFactory.actorOf(Props(classOf[HelloWorldTask]), taskName))
    }

    override def list(source: String) = List("hello")
  }
}


object HelloWorldApp extends App  {

  val system = ActorSystem("BatchManagerSystem")
  val taskManager = system.actorOf(Props[HelloWorldTaskManager], "helloManager")

  taskManager ! AddTask(null, "hello")
  taskManager ! TaskCmd(Start, "hello")

  Thread.sleep(5000)

  taskManager ! TaskCmd(Pause, "hello")

  Thread.sleep(5000)

  taskManager ! TaskCmd(Start, "hello")

  Thread.sleep(5000)

  taskManager ! TaskCmd(Stop, "hello")

  system.awaitTermination()

}