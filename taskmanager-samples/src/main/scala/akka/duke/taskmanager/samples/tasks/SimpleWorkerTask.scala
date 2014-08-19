package akka.duke.taskmanager.samples.tasks

import akka.duke.taskmanager.Task.Terminate
import akka.duke.taskmanager.{PartialFunctionBuilder, Task}
import akka.duke.taskmanager.macros.SimpleWorker


class SimpleWorkerTask extends Task {

  val actorRefFactory = context
  val workerReceive = new PartialFunctionBuilder[Any, Unit]

  override def runReceive = workerReceive.result()

  var name = ""
  override def onApplyConfig() {
    name = config.getString("name")
  }

  def start() {
    HelloWorkerCall(name)
  }

  override def stop() {}

  @SimpleWorker
  class HelloWorker {
    def action(name: String) = {
      println(s"| Hello, $name !")
      name
    }
    def reaction(name: String) {
      ByeWorkerCall(name)
    }
  }

  @SimpleWorker
  class ByeWorker {
    def action(name: String) {
      println(s"| Bye, $name !")
    }
    def reaction() {
      self ! Terminate
    }
  }

}
