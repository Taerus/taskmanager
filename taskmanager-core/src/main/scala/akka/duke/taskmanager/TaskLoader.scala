package akka.duke.taskmanager

import akka.actor.{ActorRef, ActorRefFactory}


trait TaskLoader {

  val defaultSource: String = null

  /** Load a task and return its akka.actor.ActorRef
    *
    * @param taskId The identifier of the task
    * @param taskName The name of the task in the actor system
    * @param actorFactory The actor factory (the actor system or an actor context)
    * @param source the name of the source used to get the task
    * @return an option value containing the reference on the task if it successfully loaded, None otherwise.
    */
  def load(taskId: String, taskName: String, actorFactory: ActorRefFactory, source: String = defaultSource): Option[ActorRef]

  /** List all available task
    *
    * @param source the name of the source holding the tasks
    * @return the list of the tasks in the given source
    */
  def list(source: String = defaultSource): List[String]

  /** List all available task from multiple sources
    *
    * @param sources a list of sources holding the tasks
    * @return a map with a task list for every given source
    */
  final def list(sources: List[String]): Map[String, List[String]] = {
    sources.map(source => (source, list(source))).toMap
  }

}
