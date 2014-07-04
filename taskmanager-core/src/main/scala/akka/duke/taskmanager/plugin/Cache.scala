package akka.duke.taskmanager.plugin

import java.io._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import com.typesafe.scalalogging.slf4j.LazyLogging
import scala.io.Source
import scala.collection.mutable


object Cache extends LazyLogging {

  implicit val formats = DefaultFormats

  private var cacheDir: File = null

  private var pluginNames: mutable.HashMap[String, String] = null
  private var pluginNamesFile: File = null


  /** Returns the plugin cache folder path */
  def directory = {
    if(cacheDir == null) throw new Exception("the cache directory is not defined yet")
    cacheDir
  }


  /** Set the path of the plugin cache folder, and create the directory if he doesn't exist yet
    *
    * @param path the path of the plugin cache folder
    */
  def directory_=(path: String) {
    val dir = new File(path)
    if (!dir.exists && !dir.mkdirs()) { // attempts to create the directory if it doesn't exist
      throw new Exception("failed to create the cache directory")
    } else if (dir.isFile) {
      throw new Exception("the path point on a file")
    } else {
      cacheDir = dir
      logger.info("plugin cache directory set to " + dir.getPath)

      pluginNamesFile = new File(dir, "pluginNames")
      if(!pluginNamesFile.exists) {
        pluginNames = mutable.HashMap.empty[String, String]
      } else {
        loadPluginNames()
      }
    }
  }


  /** Remove all plugin definitions saved in the cache */
  def clear() {
    def rmDir(dir: File) {
      dir.listFiles().foreach { f =>
        if(f.isDirectory) {
          rmDir(f)
        } else {
          f.delete()
        }
      }
    }

    logger.debug("clearing plugin cache...")
    try {
      if(directory.exists() && directory.isDirectory) {
        rmDir(directory)
      }
      logger.debug("  done")
    } catch {
      case e: Exception =>
        logger.error("  failed to clear the cache !")
        logger.trace(s"cause :", e)
    }
  }


  /** Tests if a plugin definition is present in the plugin cache folder.
    *
    * @param jarName the name of the plugin jar file
    * @param version the plugin version
    * @param jarDate the last modification date of the plugin jar file
    * @return true if the plugin definition is found, false otherwise
    */
  def contains(jarName: String, version: String = "_noVersion", jarDate: Long = -1): Boolean = {
    val dir = new File(directory, jarName)
    if(!(dir.exists && dir.isDirectory)) return false

    val file = new File(dir, version)
    if(!(file.exists && file.isFile)) return false

    if(jarDate < 0) return true

    val (lastDate, json) = readFile(file) { lines => (lines.next().toLong, parse(lines.mkString)) }

    !(lastDate != jarDate && (json \ jarDate.toString).extractOpt.isEmpty)
//    (json \ "last-modified").extractOpt[Long].fold(false)(_ == jarDate)
  }


  /** Optionally returns a plugin definition from the plugin cache folder.
    *
    * @param jarName the name of the plugin jar file
    * @param jarDate the last modification date of the plugin jar file
    * @param version the plugin version
    * @return an option value containing the plugin definition, or None if not present in the cache
    */
  def get(jarName: String, jarDate: Long, version: String = "_noVersion"): Option[PluginDef] = {
    logger.debug(s"searching $jarName definition from the cache...")

    def failLog(cause: String) {
      logger.debug("  no definition found")
      logger.trace("  cause : {}", cause)
    }

    val pluginName = getPluginName(jarName)
    if(!contains(pluginName, version)) {
      failLog("no definition file found")
      return None
    }

    val file = new File(directory, s"$pluginName/$version")
    val (lastDate, json) = readFile(file) { lines =>
//      if(lines.next().toLong != jarDate) {
//        failLog("definition date doesn't match")
//        return None
//      }

      (lines.next().toLong, parse(lines.mkString))
    }

    val pluginDefMapOpt = json.extractOpt[Map[String, PluginDef]]
    val pluginDef = pluginDefMapOpt.map{ _.get(jarDate.toString) }.flatten

    if(pluginDefMapOpt.isDefined) {
       pluginDef match {
        case Some(_) => logger.debug("  definition loaded")
        case None    => failLog("definition not found")
      }
    } else {
      failLog("failed to parse the plugin definition")
    }

    pluginDef
  }


  /** Save a plugin definition in the plugin cache folder.
    *
    * @param pluginDef the plugin definition
    * @param jarName the name of the plugin jar file
    * @param jarDate the last modification date of the plugin jar file
    */
  def save(pluginDef: PluginDef, jarName: String, jarDate: Long) {
    save(pluginDef2JSON(pluginDef), jarName, jarDate)
  }


  /** Save a plugin definition in the plugin cache folder.
    *
    * @param json the JSON tree representing the plugin definition
    * @param jarName the name of the plugin jar file
    * @param jarDate the last modification date of the plugin jar file
    */
  def save(json: JValue, jarName: String, jarDate: Long) {
    logger.debug(s"saving $jarName plugin definition...")

    val pluginName = (json \ "name").extractOrElse[String](jarName)
    logger.debug(s"  name : $pluginName")

    val version = (json \ "version").extractOrElse[String]("_noVersion")
    logger.debug(s"  version : $version")

    val dir = new File(directory, pluginName)
    val file = new File(dir, version)

//    if(file.exists()) {
//      val cacheDate = readFile(file) { _.next().toLong }
//      if(jarDate <= cacheDate) {
//        logger.debug(s"  a most recent definition is present for ${jarName}_$version")
//        return
//      }
//    }

    logger.trace("plugin definition : \n" + pretty(render(json)))

    try {
      if(!dir.exists) dir.mkdir()

      var date = jarDate
      var newJson = JObject(jarDate.toString -> json :: Nil)
      if(!file.exists()) {
        file.createNewFile()
      } else {
        val oldJson = readFile(file) { lines =>
          date = math.max(date, lines.next().toLong)
          parse(lines.mkString)
        }.asInstanceOf[JObject]
        newJson = oldJson merge newJson
      }

      val str = pretty(render(newJson))

      val fw = new FileWriter(file)
      fw.write(date + "\n")
      fw.write(str)
      fw.close()

      setPluginName(jarName, pluginName)

      logger.debug("done")
    } catch {
      case e: Exception =>
        logger.error(s"failed to save $jarName plugin definition !")
        logger.trace(s"cause :", e)
    }
  }

  private def readFile[T](file: File)(f: Iterator[String] => T): T = {
    val source = Source.fromFile(file)
    val result = f(source.getLines())
    source.close()

    result
  }

  private def getPluginName(jarName: String): String = {
    pluginNames.getOrElse(jarName, jarName)
  }

  private def setPluginName(jarName: String, pluginName: String) {
    if(jarName != pluginName) {
      pluginNames(jarName) = pluginName
      savePluginNames()
    }
  }

  private def loadPluginNames() {
    val fis = new FileInputStream(pluginNamesFile)
    val ois = new ObjectInputStream(fis)
    pluginNames = ois.readObject().asInstanceOf[mutable.HashMap[String, String]]
    ois.close()
    fis.close()
  }

  private def savePluginNames() {
    val fos = new FileOutputStream(pluginNamesFile)
    val oos = new ObjectOutputStream(fos)
    oos.writeObject(pluginNames)
    oos.close()
    fos.close()
  }

}
