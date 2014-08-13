package akka.duke.taskmanager.samples

import akka.duke.taskmanager.samples.tasks.HelloWorldTask

import scala.collection.mutable


object TaskRegistery {

  private val taskMap = mutable.HashMap (
    "helloWorld" -> classOf[HelloWorldTask]
  )

  def apply(taskId: String) = taskMap.get(taskId)

  def taskIds = taskMap.keys.toList

}
