package akka.duke.taskmanager.plugin

import java.io.{FileWriter, FileInputStream, File}
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import com.typesafe.scalalogging.slf4j.LazyLogging
import scala.io.Source


object Cache extends LazyLogging {

  implicit val formats = DefaultFormats

  private var cacheDir: File = null


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
    try {
      val dir = new File(path)
      if (!dir.exists && !dir.mkdirs()) { // attempts to create the directory if it doesn't exist
        throw new Exception("failed to create the cache directory")
      } else if (dir.isFile) {
        throw new Exception("the path point on a file")
      } else {
        cacheDir = dir
        logger.info("plugin cache directory set to " + dir.getPath)
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
      logger.debug("done")
    } catch {
      case e: Exception =>
        logger.error("failed to clear the cache !")
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
    if(!(file.exists && file.isDirectory)) return false

    if(jarDate < 0) return true

    val source = Source.fromFile(file)
    val json = parse(source.getLines().mkString)
    source.close()

    (json \ "last-modified").extractOpt[Long].fold(false)(_ == jarDate)
  }


  /** Optionally returns a plugin definition from the plugin cache folder.
    *
    * @param jarName the name of the plugin jar file
    * @param jarDate the last modification date of the plugin jar file
    * @param version the plugin version
    * @return an option value containing the plugin definition, or None if not present in the cache
    */
  def get(jarName: String, jarDate: Long, version: String = "_noVersion"): Option[PluginDef] = {
    if(!contains(jarName, version)) return None

    val file = new File(directory, s"$jarName/$version")
    val source = Source.fromFile(file)
    val json = parse(source.getLines().mkString)
    source.close()

    if((json \ "last-modified").extract[Long] != jarDate) return None

    json.extractOpt[PluginDef]
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

    val version = (json \ "version").extractOrElse[String]("_noVersion")
    logger.debug(s"version : $version")

    val jsonLastMod = new JObject( List("last-modified" -> new JInt(jarDate)) )
    val str = pretty(render(json merge jsonLastMod))
    logger.trace("plugin definition : \n" + str)

    try {
      val dir = new File(directory, jarName)
      if(!dir.exists) dir.mkdir()

      val file = new File(dir, version)
      if(!file.exists()) file.createNewFile()

      val fw = new FileWriter(file)
      fw.write(str)
      fw.close()

      logger.debug("done")
    } catch {
      case e: Exception =>
        logger.error(s"failed to save $jarName plugin definition !")
        logger.trace(s"cause :", e)
    }
  }

}
