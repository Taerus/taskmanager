package akka.duke.taskmanager

import org.json4s.JsonDSL._
import org.json4s.JsonAST.JValue
import org.json4s.DefaultFormats
import java.lang.reflect.Method


package object plugin {

  implicit val formats = DefaultFormats

  def pluginDef2JSON(pluginDef: PluginDef): JValue = {
    import pluginDef._

    ("name" -> name) ~
      ("version" -> version) ~
      ("dependencies" -> dependencies.map { d =>
        ("name" -> d.name) ~ ("version" -> d.version)
      }) ~
      ("entries" -> entries.map { case (entryName, e) =>
        entryName -> ( ("class" -> e.`class`) ~ ("run" -> e.run) )
      })
  }

  case class PluginDef(name: Option[String],
                       version: Option[String],
                       dependencies: List[PluginDependency],
                       entries: Map[String, PluginEntry])

  case class PluginEntry(`class`: String, run: Option[String]) {
    run.foreach( _ => if(`class`.isEmpty) throw new RuntimeException("run method defined without class") )
  }

  case class PluginDependency(name: String, version: Option[String])


  // (jarName, jarDate)
  case class JarId(jarName: String, jarDate: Long)

  def path(jarName: String, tempId: Long = -1L): String = {
    if(tempId < 0) {
      s"plugins/$jarName.jar"
    } else {
      f"plugins/~$jarName$tempId%019d.jar"
    }
  }

}
