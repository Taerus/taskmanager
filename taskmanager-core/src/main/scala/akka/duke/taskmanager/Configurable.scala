package akka.duke.taskmanager

import com.typesafe.config.{ConfigFactory, Config}
import Message.Command
import scala.collection.mutable


/** A [[akka.duke.taskmanager.ComposableActor ComposableActor]] extending this trait can load a configuration file
  * identified by an id (the class name by default) using a [[akka.duke.taskmanager.ConfigLoader ConfigLoader]]
  */
trait Configurable { this: ComposableActor =>
  import Configurable._

  if(!Context.contains("defaultConfigLoader")) {
    val baseDirOpt = Context.get[String]("baseDir")
    Context("defaultConfigLoader") = baseDirOpt match {
      case Some(path) =>
        new CpConfigLoader(confDir = path)
      case None =>
        new CpConfigLoader
    }
  }

  val id = getClass.getName.replace(".", "_")

  val configLoaderId = "defaultConfigLoader"

  private var _defaultConfDefs = mutable.MutableList.empty[ConfDef]
  private var _confDefs = mutable.MutableList.empty[ConfDef]
  private var _defaultConfig: Config = ConfigFactory.empty()
  private var _config: Config = ConfigFactory.empty()
  private var _configHasChanged = true

  private def configLoader: ConfigLoader = {
    Context.get[ConfigLoader](configLoaderId) match {
      case Some(loader) => loader
      case None => Context[ConfigLoader]("defaultConfigLoader")
    }
  }

  final def loadConfig() {
    def getConf(confDef: ConfDef): Config = {
      confDef.get match {
        case config: Config => config
        case null | "" => configLoader.load(id)
        case name: String => configLoader.load(id, name)
        case (null | "", name: String) => configLoader.load(name)
        case (id: String, name: String) => configLoader.load(id, name)
        case _ => ConfigFactory.empty
      }
    }

    if(_defaultConfDefs.nonEmpty) {
      _defaultConfig = _defaultConfDefs.map(getConf).reduceLeft(_ withFallback _)
    }
    if(_confDefs.nonEmpty) {
      _config = _confDefs.map(getConf).reduceLeft(_ withFallback _)
    }
  }

  final def defaultConfig = _defaultConfig
  final def config: Config = _config withFallback _defaultConfig
  final def configHasChanged = _configHasChanged

  final def applyConfig() = {
    onApplyConfig()
    _configHasChanged = false
  }

  /** Action performed after the config was saved **/
  def postSaveConfig() {}

  /** Action performed after the config was changed **/
  def onConfigChanged() {}

  /** Action performed to apply the config **/
  def onApplyConfig() {}

  receiveBuilder += {
    case cmd: AlterConfig =>
      cmd match {
        case SetConfig(confDef) if confDef.isDefault => _defaultConfDefs = mutable.MutableList(confDef)
        case AddConfig(confDef) if confDef.isDefault => confDef +=: _defaultConfDefs
        case DefaultConfig      => _confDefs.clear()

        case SetConfig(confDef) => _confDefs = mutable.MutableList(confDef)
        case AddConfig(confDef) => confDef +=: _confDefs
        case ReloadConfig       =>
      }
      loadConfig()
      _configHasChanged = true
      onConfigChanged()

    case SaveConfig =>
      configLoader.save(_config, id, self.path.name)
      postSaveConfig()
  }

}


object Configurable {

  sealed trait ConfigurableCommand extends Command

  sealed trait AlterConfig extends ConfigurableCommand
  case class  SetConfig(confDef: ConfDef)   extends AlterConfig
  case class  AddConfig(confDef: ConfDef)   extends AlterConfig
  case object ReloadConfig                  extends AlterConfig
  case object DefaultConfig                 extends AlterConfig

  case object SaveConfig                    extends ConfigurableCommand

}
