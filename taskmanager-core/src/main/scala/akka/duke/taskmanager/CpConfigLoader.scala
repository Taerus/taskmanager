package akka.duke.taskmanager

import com.typesafe.config.{ConfigRenderOptions, ConfigFactory, Config}
import java.io.{PrintWriter, File}


class CpConfigLoader(confDir: String = "config") extends ConfigLoader {

  def load(taskId: String) = {
    val f = new File(s"$confDir/$taskId.conf")
    if (f.exists && f.isFile) {
      ConfigFactory.parseFile(f)
    } else {
      ConfigFactory.empty()
    }
  }

  def load(taskId: String, name: String) = {
    val f = new File(s"$confDir/$taskId/$name.conf")
    if(f.exists && f.isFile) {
      ConfigFactory.parseFile(f)
    } else {
      ConfigFactory.empty()
    }
  }

  def list(taskId: String) = {
    val dir = new File(s"$confDir/$taskId")
    if(dir.exists && dir.isDirectory) {
      dir.list.map(_.stripSuffix(".conf")).toSet
    } else {
      Set()
    }
  }

  override def save(config: Config, taskId: String, name: String = "_default_") {
    val f = new File(s"$confDir/$taskId/$name.conf")
    f.getParentFile.mkdirs()

    val writer = new PrintWriter(f)
    writer.print(config.root.render(ConfigRenderOptions.defaults.setOriginComments(false)))
    writer.close()
  }

}
