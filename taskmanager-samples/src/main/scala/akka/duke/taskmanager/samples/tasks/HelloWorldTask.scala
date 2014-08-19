package akka.duke.taskmanager.samples.tasks

import akka.duke.taskmanager.Task
import akka.duke.taskmanager.Task.Terminate


class HelloWorldTask extends Task {

  def start() {
    println("| Hello World !")
    self ! Terminate
  }

}
