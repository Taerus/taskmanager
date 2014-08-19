package akka.duke.taskmanager

import com.typesafe.config.Config


trait ConfigLoader {

  def load(taskId: String): Config
  def load(taskId: String, name: String): Config
  def list(taskId: String): Set[String]
  def save(config: Config, taskId: String, name: String) {}

}
