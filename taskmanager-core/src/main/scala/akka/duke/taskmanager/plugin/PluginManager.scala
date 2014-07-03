package akka.duke.taskmanager.plugin

import scala.collection.mutable
import java.io.File
import java.nio.file.StandardCopyOption._
import scala.collection.JavaConverters._
import java.util.jar.JarFile
import scala.collection.immutable.SortedSet
import scala.concurrent.duration._
import akka.duke.taskmanager.util.DirWatcher
import DirWatcher._
import akka.actor.{Cancellable, ActorSystem}
import scala.concurrent.ExecutionContext.Implicits.global
import org.json4s._
import org.json4s.native.JsonMethods._
import com.typesafe.scalalogging.slf4j.LazyLogging
import com.typesafe.config.{ConfigRenderOptions, ConfigFactory}
import akka.duke.taskmanager.util.DirWatcher
import java.nio.file.Files


object PluginManager extends LazyLogging {

  object RefreshPolicy extends Enumeration {
    type RefreshPolicy = Value
    val Dynamic, Manual, TimeoutSchedule, Timeout = Value
  }
  import RefreshPolicy.RefreshPolicy

  object UpdatePolicy extends Enumeration {
    type UpdatePolicy = Value
    val OnRefresh, OnUse, Manual = Value
  }
  import UpdatePolicy.UpdatePolicy

  // jarName -> ..jarDates
  private val jarIds = mutable.HashMap.empty[String, SortedSet[Long]]

  // ( jarName, tempId ) -> [pluginName -> PluginDef(className, ..dependencies)]
  private val pluginDefMap = mutable.HashMap.empty[JarId, PluginDef]

  // ( jarName, tempId ) -> ..depTreeIds
  private val depTreeIds = mutable.HashMap.empty[JarId, SortedSet[Long]]

  // ( jarName, depTreeId ) -> classLoader
  private val classLoaders = mutable.HashMap.empty[JarId, PluginClassLoader]

  // ( jarName, tempId ) -> nbUses
  private val uses = mutable.HashMap.empty[JarId, Int]

  // ( jarName, depTreeId ) -> [depJarName -> depTempId]
  //  private val depTrees = mutable.HashMap.empty[Id, Map[String, Long]]

  private val system = ActorSystem("pluginManager")
  private var refreshSceduler: Cancellable = null
  private var _refreshPolicy = RefreshPolicy.Dynamic
  private var _updatePolicy = UpdatePolicy.OnUse
  private var _timeout: FiniteDuration = 1.minute
  private val dirWatcher = new DirWatcher("plugins/", extensionFilter("jar"), not(prefixFilter("~")))
  private var lastRefresh = 0L
  private var _pluginDirVersion = -1L -> Vector.empty[JarId]
  private val added = mutable.Set.empty[JarId]
  private val removed = mutable.Set.empty[JarId]
  refresh()
  update()


  def stop() {
    stopRefreshScheduler()
    system.shutdown()
  }

  def refreshPolicy = _refreshPolicy

  def refreshPolicy_=(policy: RefreshPolicy) {
    import RefreshPolicy._
    if (_refreshPolicy == TimeoutSchedule && policy != TimeoutSchedule) {
      stopRefreshScheduler()
    } else if (policy == TimeoutSchedule) {
      startRefreshScheduler()
    }
    _refreshPolicy = policy
  }

  def updatePolicy = _updatePolicy

  def updatePolicy_=(policy: UpdatePolicy) {
    _updatePolicy = policy
  }

  def timeout = _timeout

  def timeout_=(value: FiniteDuration) {
    import RefreshPolicy._
    _timeout = value
    if (_refreshPolicy == TimeoutSchedule) {
      startRefreshScheduler()
    }
  }

  def startRefreshScheduler() {
    stopRefreshScheduler()
    refreshSceduler = system.scheduler.schedule(timeout, timeout)(refresh())
  }

  def stopRefreshScheduler() {
    if(refreshSceduler != null && !refreshSceduler.isCancelled) {
      refreshSceduler.cancel()
    }
  }

  def pluginDirVersion: Long = {
    if (needRefresh) refresh()
    _pluginDirVersion._1
  }

  def needRefresh: Boolean = {
    import RefreshPolicy._
    (_refreshPolicy == Dynamic && dirWatcher.changes != null) ||
    (_refreshPolicy == Timeout && (System.currentTimeMillis - lastRefresh > _timeout.toMillis))
  }

  def refresh(): Long = {
    logger.debug("Refreshing")
    lastRefresh = System.currentTimeMillis
    var (version, lastIds) = _pluginDirVersion
    val ids = pluginIds()
    if (lastIds != ids) {
      val newIds = ids.toSet -- lastIds
      val remIds = lastIds.toSet -- ids
      newIds.foreach(createTemp)
      added ++= newIds
      removed ++= remIds
      added --= removed
      version = System.currentTimeMillis
      _pluginDirVersion = (version, ids)
      logger.debug(s"  - ${newIds.size} (${added.size}) added")
      logger.debug(s"  - ${remIds.size} (${removed.size}) removed")

      if(_updatePolicy == UpdatePolicy.OnRefresh) {
        update()
      } else {
        logger.debug("update to apply")
      }
    }
    logger.debug("no changes")

    version
  }

  def update() {
    for (JarId(jarName, jarDate) <- added) {
      val newIds = jarIds.get(jarName).fold(SortedSet(jarDate))(_ + jarDate)
      jarIds(jarName) = newIds
      Cache.get(jarName, jarDate).orElse {
        val pluginDefOpt = scanJarForPlugins(jarName, jarDate)
        pluginDefOpt.fold(
          Cache.save(new JObject(Nil), jarName, jarDate)
        ) {
          Cache.save(_, jarName, jarDate)
        }
        pluginDefOpt
      }.foreach { pluginDef =>
        pluginDefMap += JarId(jarName, jarDate) -> pluginDef
      }
    }
    added.clear()

    for (JarId(jarName, id) <- removed) {
      jarIds.get(jarName).foreach { ids =>
        val newIds = ids - id
        if (newIds.isEmpty) {
          jarIds -= jarName
        } else {
          jarIds(jarName) = newIds
        }
      }
    }
    removed.clear()
  }

  def check() {
    if(needRefresh) refresh()
    if(_updatePolicy == UpdatePolicy.OnUse) update()
  }

  def isLast(id: JarId): Boolean = {
    isLast(id.jarName, id.jarDate)
  }

  def isLast(jarName: String, jarDate: Long): Boolean = {
    jarIds.get(jarName).fold(false)(_.last == jarDate)
  }
  
  def lastId(jarName: String) = JarId(jarName, lastDate(jarName))
  
  def lastIdOpt(jarName: String) = lastDateOpt(jarName).map(JarId(jarName, _))

  def lastDate(jarName: String): Long = {
    jarIds.get(jarName).fold(-1L)(_.last)
  }

  def lastDateOpt(jarName: String): Option[Long] = {
    jarIds.get(jarName).map(_.last)
  }

  def exists(id: JarId): Boolean = {
    exists(id.jarName, id.jarDate)
  }

  def exists(jarName: String, jarDate: Long = -1L): Boolean = {
    jarIds.get(jarName).fold(false) {
      ids => if (jarDate < 0) ids.nonEmpty else ids.contains(jarDate)
    }
  }

  def pluginDirectory: File = {
    val pluginDir = new File("plugins")
    if (!(pluginDir.exists && pluginDir.isDirectory)) {
      pluginDir.mkdir()
    }

    pluginDir
  }

  def scanJarForPlugins(jarName: String, jarDate: Long): Option[PluginDef] = {
    scanJarForPlugins(new File(path(jarName, jarDate)))
  }

  // pluginName -> PluginDef(className, ..dependencies)
  def scanJarForPlugins(file: File): Option[PluginDef] = {
    val jar = new JarFile(file)
    val entries = jar.entries().asScala.toVector
    val pluginDefOpt = entries.collectFirst{
      case entry if entry.getName == "META-INF/plugin.pdef" => entry
    }

    val result = pluginDefOpt.map { entry =>
      logger.debug("plugin definition found")

      val is = jar.getInputStream(entry)
      val b = new Array[Byte](is.available)
      is.read(b)

      val json = parse {
        ConfigFactory
          .parseString(new String(b)).root
          .render(ConfigRenderOptions.concise)
      }

      println(pretty(render(json)))

      json.extract[PluginDef]
    }

    jar.close()

    result
  }

  def load(jarName: String): Boolean = {
    check()
    createClassLoader(jarName).nonEmpty
  }

  def createClassLoader(jarName: String): Option[PluginClassLoader] = {
    val jarDate = lastDate(jarName)
    val id = JarId(jarName, jarDate)

    buildDependencyTree(jarName) map {
      dependencies =>
        val classLoader = PluginClassLoader(jarName, jarDate)
        classLoader ++= dependencies.map(JarId.unapply(_).get)

        classLoaders += JarId(jarName, newDepTreeDate(jarName, jarDate)) -> classLoader
        uses += id -> uses.get(id).fold(1)(_+1)
        dependencies.foreach(id => uses(id) = uses.get(id).fold(1)(_+1))
        classLoader
    }
  }

  def newDepTreeDate(jarName: String, tempId: Long): Long = {
    val depTreeDate = System.currentTimeMillis
    val id = JarId(jarName, tempId)
    depTreeIds += id -> depTreeIds.get(id).fold(SortedSet(depTreeDate))(_ + depTreeDate)
    depTreeDate
  }

  def buildDependencyTree(jarName: String): Option[Set[JarId]] = {
    val depNames = mutable.Set(jarName)
    buildDependencyTree(jarName, depNames)
  }

  def buildDependencyTree(jarName: String, depNames: mutable.Set[String]): Option[Set[JarId]] = {
    this.dependencies(jarName).map {
      dependencies =>
        (for {
          PluginDependency(name, _) <- dependencies
          if !(depNames contains name)
        } yield {
          if (!exists(name)) {
            return None
          }
          depNames += name
          buildDependencyTree(name, depNames).getOrElse(Set.empty[JarId]) + lastId(name)
        }).flatten
    }
  }

  def dependencies(jarName: String, pluginName: String = null, jarDate: Long = -1): Option[Set[PluginDependency]] = {
    jarIdOpt(jarName, jarDate).map { id =>
      pluginDefMap.get(id).map { pluginDef =>
        pluginDef.dependencies.toSet
      }
    }.flatten
  }

  private def jarIdOpt(jarName: String, jarDate: Long = -1L): Option[JarId] = {
    if (exists(jarName, jarDate)) {
      if (jarDate < 0) lastIdOpt(jarName) else Some(JarId(jarName, jarDate))
    } else {
      None
    }
  }

  private def jarDateOpt(jarName: String, jarDate: Long = -1L): Option[Long] = {
    if (exists(jarName, jarDate)) {
      if (jarDate < 0) lastDateOpt(jarName) else Option(jarDate)
    } else {
      None
    }
  }

  def getPlugin(jarName: String, pluginName: String, jarDate: Long = -1): Option[Plugin] = {
    check()

    jarDateOpt(jarName, jarDate).map { date =>
      getClass(jarName, pluginName, date) map { clazz =>
        Plugin(clazz, Some("pluginMain"))
//        new PluginWrapper(clazz, (jarName, depTreeIds(jarName, id).last), system)
      }
    }.flatten
  }

  def getClass(jarName: String, entryName: String, jarDate: Long = -1): Option[Class[_]] = {
    jarIdOpt(jarName, jarDate).map { id =>
      depTreeIds.get(id).map { depTreeIds =>
        classLoaders.get(JarId(jarName, depTreeIds.last)).map { classLoader =>
          val className = pluginDefMap.get(id).fold[String](return None) {
            _.entries.get(entryName).fold[String](return None)(_.`class`)
          }
          classLoader.loadClass(className)
        }
      }.flatten
    }.flatten
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

  def pluginIds(showTemp: Boolean = false): Vector[JarId] = {
    pluginFiles(showTemp).map(f => JarId(f.getName.stripSuffix(".jar"), f.lastModified))
  }

  def availablePlugins(showTemp: Boolean = false): Vector[String] = {
    pluginFiles(showTemp).map(_.getName.stripSuffix(".jar"))
  }

  // jarName -> [tempId -> ..pluginNames]
  def plugins: Map[String, Map[Long, Vector[String]]] = {
    jarIds.map {
      case (jarName, jarDates) =>
        jarName -> jarDates.map { jarDate =>
            jarDate -> pluginDefMap.get(JarId(jarName, jarDate)).fold(Vector.empty[String])(_.entries.keys.toVector)
        }.toMap[Long, Vector[String]]
    }.toMap
  }


// ####################################################################################################################
// #                                              temp files management                                               #
// ####################################################################################################################

  def createTemp(id: JarId): File = {
    createTemp(id.jarName, id.jarDate)
  }

  def createTemp(jarName: String, tempId: Long): File = {
    val jarPath = new File(path(jarName)).toPath
    val tmpPath = new File(path(jarName, tempId)).toPath
    Files.copy(jarPath, tmpPath, REPLACE_EXISTING).toFile
  }

  def deleteTemp(id: JarId): Boolean = {
    deleteTemp(id.jarName, id.jarDate)
  }

  def deleteTemp(jarName: String, tempId: Long = -1L): Boolean = {
    if (tempId < 0 && exists(jarName)) {
      return jarIds(jarName) forall (deleteTemp(jarName, _))
    }

    val file = new File(path(jarName, tempId))
    /*if( exists(jarName, tempId) ) {
      println(s"$jarName is loaded, please unload it before deleting its temp file.")
      false
    } else */ if (file.delete()) {
      logger.info(s"${file.getName} deleted")
      true
    } else {
      logger.error(s"failed to delete ${file.getName}")
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
