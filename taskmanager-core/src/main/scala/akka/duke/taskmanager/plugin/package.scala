package akka.duke.taskmanager

import org.json4s.JsonDSL._
import org.json4s.JsonAST.JValue
import org.json4s.{FieldSerializer, DefaultFormats}


package object plugin {

  implicit val formats = DefaultFormats

  def pluginDef2JSON(pluginDef: PluginDef): JValue = {
    import pluginDef._

    ("name" -> name) ~
      ("version" -> version.toOption) ~
      ("dependencies" -> dependencies.map { d =>
        ("name" -> d.name) ~ ("version" -> d.version)
      }) ~
      ("entries" -> entries.map { case (entryName, e) =>
        entryName -> ( ("class" -> e.`class`) ~ ("run" -> e.run) ~ ("isTask" -> e.isTask) )
      })
  }

  case class PluginDef(name: Option[String],
                       version: Version,
                       dependencies: List[PluginDependency],
                       entries: Map[String, PluginEntry]) {

    def this(name: Option[String],
             version: String = null,
             dependencies: List[PluginDependency],
             entries: Map[String, PluginEntry] = Map.empty[String, PluginEntry]) = {
      this(name, Version(version), dependencies, entries)
    }

    def merge(that: PluginDef): PluginDef = {
      val name = that.name orElse this.name
      val version = that.version match {
        case Version.noVersion => this.version
        case _ => that.version
      }
      val dependencies = {
        this.dependencies.map(d => d.name -> d).toMap ++
        that.dependencies.map(d => d.name -> d).toMap
      }.values.toList
      val entries = that.entries ++ this.entries

      PluginDef(name, version, dependencies, entries)
    }

    def toJson = {
      ("name" -> name) ~
        ("version" -> version.toOption) ~
        ("dependencies" -> dependencies.map{_.toJson}) ~
        ("entries" -> entries.map { case (entryName, entry) =>
          entryName -> entry.toJson
        })
    }
  }

  case class PluginEntry(`class`: String, run: Option[String], isTask: Boolean = false) {
    run.foreach( _ => if(`class`.isEmpty) throw new RuntimeException("run method defined without class") )

    val isRunnable = run.nonEmpty

    def toJson = {
      ("class" -> `class`) ~ ("run" -> run) ~ ("isTask" -> isTask)
    }
  }

  case class PluginDependency(name: String, version: Option[String]) {
    def toJson = {
      ("name" -> name) ~ ("version" -> version)
    }
  }


  case class JarId(jarName: String, jarDate: Long)

  object JarId {
    implicit val byDateFirst = Ordering[(Long, String)].on[JarId] {
      id => (id.jarDate, id.jarName)
    }
  }
  
  
  case class PluginId(name: String, version: Version)

  object PluginId {
    implicit val byVersionFirst = Ordering[(Long, Long, String)].on[PluginId] {
      id => (id.version.major, id.version.minor, id.name)
    }
  }
  

  case class Version(major: Int, minor: Int = 0) {
    import Version._

    override def toString = this match {
      case Version(Integer.MIN_VALUE, _) => "_noVersion"
      case Version(_, 0) =>  s"$major.0"
      case _ => s"$major.$minor"
    }

    def toOption: Option[String] = this match {
      case `noVersion` => None
      case _ => Option(this.toString)
    }
  }

  object Version {
    val noVersion = Version(Integer.MIN_VALUE)

    private val patern = """\D*(\d)(?:\.(\d+))?\D.*""".r

    def apply(version: String): Version = {
      version match {
        case "_noVersion" | null => noVersion
        case patern(major, null) => Version(major.toInt, 0)
        case patern(major, minor)=> Version(major.toInt, minor.toInt)
        case _ => throw new Exception(s"invalid plugin version : '$version'")
      }
    }
  }


  def path(jarName: String, tempId: Long = -1L): String = {
    if(tempId < 0) {
      s"plugins/$jarName.jar"
    } else {
      f"plugins/~$jarName$tempId%019d.jar"
    }
  }

}
