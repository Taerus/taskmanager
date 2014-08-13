package akka.duke.taskmanager

import com.typesafe.config.{ConfigRenderOptions, ConfigFactory, Config}
import java.io.{PrintWriter, File}


class CpConfigLoader( confDir: String = "config",
                      format: String = "#id-#name"
                    ) extends ConfigLoader {

  override def load(name: String) = loadConf(name)

  override def load(id: String, name: String) = loadConf(getName(id, name))

  override def save(config: Config, id: String, name: String = "_default_") {
    val f = new File(getDir(confDir) + getName(id, name) + ".conf")
    f.getParentFile.mkdirs()

    val writer = new PrintWriter(f)
    writer.print(config.root.render(ConfigRenderOptions.defaults.setOriginComments(false)))
    writer.close()
  }

  private def loadConf(name: String): Config = {
    val path = s"${confDir.stripSuffix("/")}/$name.conf"
    println(path)
    val file = new File(path)
    val conf = if(file.exists()) {
      ConfigFactory.parseFile(file)
    } else {
      ConfigFactory.empty()
    }
    conf withFallback ConfigFactory.parseResources(path)
  }

  private def getDir(dir: String): String = {
    dir.trim match {
      case "" | null => ""
      case str if str.endsWith("/") => str
      case str => str + "/"
    }
  }

  private def getName(id: String, name: String): String = {
    format.trim.replace("#id", id.trim).replace("#name", name.trim)
  }

}
