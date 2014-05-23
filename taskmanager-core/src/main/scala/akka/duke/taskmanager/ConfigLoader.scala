package akka.duke.taskmanager

import com.typesafe.config.Config


trait ConfigLoader {

  def load(name: String): Config
  def load(id: String, name: String): Config
  def save(config: Config, id: String, name: String) {}

}
