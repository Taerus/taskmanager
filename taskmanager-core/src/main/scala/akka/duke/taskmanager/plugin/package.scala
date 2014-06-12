package akka.duke.taskmanager


package object plugin {

  // (jarName, id)
  type Id = (String, Long)

  // pluginName -> pluginDef
  type PluginDefMap = Map[String, PluginDef]
  object PluginDefMapUtil {
    def empty() = Map.empty[String, PluginDef]
  }

  case class PluginDef(className: Option[String], dependencies: Array[String], runMethodName: Option[String] = None)

  def path(jarName: String, tempId: Long = -1L): String = {
    if(tempId < 0) {
      s"plugins/$jarName.jar"
    } else {
      f"plugins/~$jarName$tempId%019d.jar"
    }
  }

}
