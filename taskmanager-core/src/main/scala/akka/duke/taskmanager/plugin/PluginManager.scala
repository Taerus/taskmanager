package akka.duke.taskmanager.plugin

import scala.collection.mutable
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.StandardCopyOption._
import scala.collection.JavaConverters._
import java.util.jar.JarFile
import scala.collection.immutable.SortedSet
import scala.concurrent.duration._


object PluginManager {

  // (jarName, id)
  type Id = (String, Long)

  // pluginName -> pluginDef
  type PluginDefMap = Map[String, PluginDef]

  case class PluginDef(className: String, dependencies: Array[String])

  object RefreshPolicy extends Enumeration {
    type RefreshPolicy = Value
    val Dynamic, Manual, TimeoutSchedule, Timeout = Value
  }
  import RefreshPolicy._

  // jarName -> ..tempIds
  private val tempIds = mutable.HashMap.empty[String, SortedSet[Long]]

  // ( jarName, tempId ) -> [pluginName -> PluginDef(className, ..dependencies)]
  private val pluginDefMaps = mutable.HashMap.empty[Id, PluginDefMap]

  // ( jarName, tempId ) -> ..depTreeIds
  private val depTreeIds = mutable.HashMap.empty[Id, SortedSet[Long]]

  // ( jarName, depTreeId ) -> classLoader
  private val classLoaders = mutable.HashMap.empty[Id, PluginClassLoader]

  // ( jarName, depTreeId ) -> [depJarName -> depTempId]
  //  private val depTrees = mutable.HashMap.empty[Id, Map[String, Long]]


  private var _refreshPolicy = Dynamic
  private var _timeout: Duration = 1.minute
  private var lastRefresh = 0L
  private var _pluginDirVersion = -1L -> Vector.empty[Id]
  private val added = mutable.Set.empty[Id]
  private val removed = mutable.Set.empty[Id]
  refresh()
  update()


  def refreshPolicy = _refreshPolicy

  def refreshPolicy_=(policy: RefreshPolicy) {
    if (_refreshPolicy == TimeoutSchedule && policy != TimeoutSchedule) {
      stopRefreshScheduler()
    } else if (policy == TimeoutSchedule) {
      startRefreshScheduler()
    }
    _refreshPolicy = policy
  }

  def timeout = _timeout

  def timeout_=(value: Duration) {
    _timeout = timeout
    if (_refreshPolicy == TimeoutSchedule) {
      stopRefreshScheduler()
      startRefreshScheduler()
    }
  }

  def startRefreshScheduler() {
    // TODO
  }

  def stopRefreshScheduler() {
    // TODO
  }

  def pluginDirVersion: Long = {
    if (needRefresh) refresh()
    _pluginDirVersion._1
  }

  def needRefresh: Boolean = {
    _refreshPolicy == Dynamic ||
      (_refreshPolicy == Timeout && (System.currentTimeMillis - lastRefresh > _timeout.toMillis))
  }

  def refresh(): Long = {
    lastRefresh = System.currentTimeMillis
    var (version, lastIds) = _pluginDirVersion
    val ids = pluginIds()
    if (lastIds != ids) {
      val newIds = ids.toSet -- lastIds
      newIds.foreach(createTemp)
      added ++= newIds
      removed ++= lastIds.toSet -- ids
      added --= removed
      version = System.currentTimeMillis
      _pluginDirVersion = (version, ids)
    }
    version
  }

  def needUpdate: Boolean = {
    added.nonEmpty || removed.nonEmpty
  }

  def update() {
    for ((jarName, tempId) <- added) {
      val newIds = tempIds.get(jarName).fold(SortedSet(tempId))(_ + tempId)
      tempIds(jarName) = newIds
      pluginDefMaps += (jarName, tempId) -> scanJarForPlugins(jarName, tempId)
    }

    for ((jarName, id) <- removed) {
      tempIds.get(jarName).foreach {
        ids =>
          val newIds = ids - id
          if (newIds.isEmpty) {
            tempIds -= jarName
          } else {
            tempIds(jarName) = newIds
          }
      }
    }
  }

  def isLast(id: Id): Boolean = {
    isLast(id._1, id._2)
  }

  def isLast(jarName: String, tempId: Long): Boolean = {
    tempIds.get(jarName).fold(false)(_.last == tempId)
  }

  def lastId(jarName: String): Long = {
    tempIds.get(jarName).fold(-1L)(_.last)
  }

  def exists(jarName: String, tempId: Long = -1L): Boolean = {
    tempIds.get(jarName).fold(false) {
      ids =>
        if (tempId < 0) ids.nonEmpty else ids.contains(tempId)
    }
  }

  def pluginDirectory: File = {
    val pluginDir = new File("plugins")
    if (!(pluginDir.exists && pluginDir.isDirectory)) {
      pluginDir.mkdir()
    }

    pluginDir
  }

  def scanJarForPlugins(jarName: String, tempId: Long): PluginDefMap = {
    scanJarForPlugins(new File(path(jarName, tempId)))
  }

  // pluginName -> PluginDef(className, ..dependencies)
  def scanJarForPlugins(file: File): PluginDefMap = {
    val jar = new JarFile(file)
    val classList = jar.entries().asScala.toVector
      .filter(_.getName.endsWith(".class"))
      .map(_.getName.replace("/", ".")
      .stripSuffix(".class"))
    val classLoader = new URLClassLoader(Array(file.toURI.toURL))

    val pluginDefMap = for {
      (clazz, className) <- classList.map(cn => (classLoader.loadClass(cn), cn))
      if clazz.isAnnotationPresent(classOf[Plugin])
    } yield {
      val plugin = clazz.getAnnotation(classOf[Plugin])
      plugin.name -> PluginDef(className, plugin.dependencies)
    }

    classLoader.close()

    pluginDefMap.toMap
  }

  def load(jarName: String): Boolean = {
    if (needRefresh) refresh()
    if (needUpdate) update()
    createClassLoader(jarName).nonEmpty
  }

  def createClassLoader(jarName: String): Option[PluginClassLoader] = {
    val tempId = lastId(jarName)

    buildDependencyTree(jarName) map {
      dependencies =>
        val classLoader = new PluginClassLoader(jarName, tempId)
        classLoader ++= dependencies

        classLoaders += (jarName, newDepTreeId(jarName, tempId)) -> classLoader
        classLoader
    }
  }

  def newDepTreeId(jarName: String, tempId: Long): Long = {
    val depTreeId = System.currentTimeMillis
    depTreeIds += (jarName, tempId) -> depTreeIds.get(jarName, tempId).fold(SortedSet(depTreeId))(_ + depTreeId)
    depTreeId
  }

  def buildDependencyTree(jarName: String): Option[Set[Id]] = {
    val depNames = mutable.Set(jarName)
    buildDependencyTree(jarName, depNames)
  }

  def buildDependencyTree(jarName: String, depNames: mutable.Set[String]): Option[Set[Id]] = {
    this.dependencies(jarName).map {
      dependencies =>
        (for {
          dependency <- dependencies
          if !(depNames contains dependency)
        } yield {
          if (!exists(dependency)) {
            return None
          }
          depNames += dependency
          buildDependencyTree(dependency, depNames).getOrElse(return None) + ((dependency, lastId(dependency)))
        }).flatten
    }
  }

  def dependencies(jarName: String, pluginName: String = null, tempId: Long = -1): Option[Set[String]] = {
    val id = {
      if (exists(jarName, tempId)) {
        if (tempId < 0) lastId(jarName) else tempId
      } else {
        return None
      }
    }

    pluginDefMaps.get(jarName, id).map {
      pluginDefMap =>
        if (pluginName == null) {
          pluginDefMap.values.foldLeft(Set.empty[String]) {
            (set, pluginDef) => set ++ pluginDef.dependencies
          }
        } else {
          pluginDefMap.get(pluginName).fold[Set[String]](return None)(_.dependencies.toSet)
        }
    }
  }

  def getClass(jarName: String, pluginName: String, tempId: Long = -1): Option[Class[_]] = {
    val id = {
      if (exists(jarName, tempId)) {
        if (tempId < 0) lastId(jarName) else tempId
      } else {
        return None
      }
    }

    depTreeIds.get(jarName, id).map {
      depTreeIds =>
        val classLoader = classLoaders(jarName, depTreeIds.last)
        val className = pluginDefMaps.get(jarName, id).fold[String](return None) {
          pluginDefMap =>
            pluginDefMap.get(pluginName).fold[String](return None)(_.className)
        }
        classLoader.loadClass(className)
    }
  }


  // ####################################################################################################################
  // #                                                listing methods                                                   #
  // ####################################################################################################################

  def pluginFiles(showTemp: Boolean = false): Vector[File] = {
    var plugins = pluginDirectory
      .listFiles()
      .filter(_.getName.toLowerCase.endsWith(".jar"))
    if (!showTemp) {
      plugins = plugins.filterNot(_.getName.startsWith("~"))
    }
    plugins.toVector
  }

  def pluginIds(showTemp: Boolean = false): Vector[Id] = {
    pluginFiles(showTemp).map(f => (f.getName.stripSuffix(".jar"), f.lastModified))
  }

  def availablePlugins(showTemp: Boolean = false): Vector[String] = {
    pluginFiles(showTemp).map(_.getName.stripSuffix(".jar"))
  }

  // jarName -> [tempId -> ..pluginNames]
  def plugins: Map[String, Map[Long, Vector[String]]] = {
    tempIds.map {
      case (jarName, ids) =>
        jarName -> ids.map {
          id =>
            id -> pluginDefMaps(jarName, id).keys.toVector
        }.toMap[Long, Vector[String]]
    }.toMap
  }


  // ####################################################################################################################
  // #                                              temp files management                                               #
  // ####################################################################################################################

  def createTemp(id: Id): File = {
    createTemp(id._1, id._2)
  }

  def createTemp(jarName: String, tempId: Long): File = {
    val jarPath = new File(path(jarName)).toPath
    val tmpPath = new File(path(jarName, tempId)).toPath
    Files.copy(jarPath, tmpPath, REPLACE_EXISTING).toFile
  }

  def deleteTemp(id: Id): Boolean = {
    deleteTemp(id._1, id._2)
  }

  def deleteTemp(jarName: String, tempId: Long = -1L): Boolean = {
    if (tempId < 0 && exists(jarName)) {
      return tempIds(jarName) forall (deleteTemp(jarName, _))
    }

    val file = new File(path(jarName, tempId))
    /*if( exists(jarName, tempId) ) {
      println(s"$jarName is loaded, please unload it before deleting its temp file.")
      false
    } else */ if (file.delete()) {
      println(s"${file.getName} deleted")
      true
    } else {
      println(s"failed to delete ${file.getName}")
      false
    }
  }

  def clean() {
    val extractor = """^~(.+)([0-9]{19})\.jar$""".r
    pluginFiles(showTemp = true)
      .map(_.getName)
      .filter(_.startsWith("~"))
      .foreach {
      fileName =>
        extractor.findFirstIn(fileName) foreach {
          case extractor(jarName, tempIdStr) => deleteTemp(jarName, tempIdStr.toLong)
        }
    }
  }

}
