package akka.duke.taskmanager.samples

import akka.duke.taskmanager.samples.tasks.{PausableTask, SimpleWorkerTask, HelloWorldTask}

import scala.collection.mutable


object TaskRegistery {

  private val taskMap = mutable.HashMap (
    "helloWorld"    -> classOf[HelloWorldTask],
    "pausable"      -> classOf[PausableTask],
    "simpleWorker"  -> classOf[SimpleWorkerTask]
  )

  def apply(taskId: String) = taskMap.get(taskId)

  def taskIds = taskMap.keys.toList

}
