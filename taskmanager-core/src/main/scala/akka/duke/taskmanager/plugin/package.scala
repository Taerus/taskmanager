package akka.duke.taskmanager

import org.json4s.JsonDSL._
import org.json4s.JsonAST.JValue
import org.json4s.DefaultFormats


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
                       entries: Map[String, PluginEntry] = Map.empty[String, PluginEntry]) {

    def merge(that: PluginDef): PluginDef = {
      val name = that.name orElse this.name
      val version = that.version orElse this.version
      val dependencies = {
        this.dependencies.map(d => d.name -> d).toMap ++
        that.dependencies.map(d => d.name -> d).toMap
      }.values.toList
      val entries = that.entries ++ this.entries

      PluginDef(name, version, dependencies, entries)
    }

    def toJson = {
      val a = 'lol
      ("name" -> name) ~
        ("version" -> version) ~
        ("dependencies" -> dependencies.map{_.toJson}) ~
        ("entries" -> entries.map { case (entryName, entry) =>
          entryName -> entry.toJson
        })
    }
  }

  case class PluginEntry(`class`: String, run: Option[String]) {
    run.foreach( _ => if(`class`.isEmpty) throw new RuntimeException("run method defined without class") )

    def toJson = {
      ("class" -> `class`) ~ ("run" -> run)
    }
  }

  case class PluginDependency(name: String, version: Option[String]) {
    def toJson = {
      ("name" -> name) ~ ("version" -> version)
    }
  }


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
