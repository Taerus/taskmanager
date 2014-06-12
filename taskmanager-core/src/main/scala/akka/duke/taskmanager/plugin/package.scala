package akka.duke.taskmanager

import java.io._
import scala.io.Source


package object plugin {

  // (jarName, id)
  type Id = (String, Long)

  // pluginName -> pluginDef
  type PluginDefMap = Map[String, PluginDef]
  object PluginDefMapUtil {
    def empty() = Map.empty[String, PluginDef]
  }

  case class PluginDef(className: Option[String], dependencies: Array[String], runMethodName: Option[String] = None)


  val cacheDir = new File("plugins/.cache")

  def clearCache() {
    def rmDir(dir: File) {
      dir.listFiles().foreach { f =>
        if(f.isDirectory) {
          rmDir(f)
        } else {
          f.delete()
        }
      }
    }
    if(cacheDir.exists() && cacheDir.isDirectory) {
      rmDir(cacheDir)
    }
  }

  def savePluginDefMap(name: String, version: Long, pluginDefMap: PluginDefMap, path: String = "plugins/.cache/") {
    if(!cacheDir.exists()) {
      cacheDir.mkdirs()
    }
    val f = new File(s"$path/$name")
    if(!f.exists()) f.createNewFile()
    val fw = new FileWriter(f)
    fw.write(s"#${version.toHexString}\n")
    pluginDefMap.foreach { case (pn, PluginDef(cn, ds, rn)) =>
      fw.write(pn + "\n")
      cn.foreach(v => fw.write(s" c> $v\n"))
      rn.foreach(v => fw.write(s" r> $v\n"))
      ds.foreach(v => fw.write(s" d> $v\n"))
    }
    fw.flush()
    fw.close()
  }

  def loadPluginDefMap(name: String, version: Long, path: String = "plugins/.cache/"): Option[PluginDefMap] = {
    try {
      val br = new BufferedReader(new FileReader(s"$path/$name"))
      val ret = if (br.readLine() == s"#${version.toHexString}") {
        PluginDefParser.parseFile(s"$path/$name")
      } else {
        None
      }
      br.close()
      ret
    } catch {
      case e: Exception => None
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
