package akka.duke.taskmanager.plugin

import java.lang.reflect.Method


class P(pluginDef: PluginDef) {

  val classMap = pluginDef.entries

  def run(entryName: String)(args: String*) {

  }

  def runnables: Vector[String] = {
    (for( (name, entry) <- pluginDef.entries if entry.run.isDefined ) yield name).toVector
  }

}

case class Plugin(clazz: Class[_], runMethod: Option[String]) {

  private val _runMethod = runMethod.map { name =>
    var m: Option[Method] = None
    try {
      m = Option(clazz.getDeclaredMethod(name, classOf[Seq[_]]))
    } catch {
      case e: Exception => println(e.getMessage)
    }
    m
  }.flatten

  private var _instance: Any = null

  val isRunnable = _runMethod.isDefined

  def init() {
    _instance = clazz.newInstance()
  }
  
  def instance = {
    if(_instance == null) {
      throw new RuntimeException("plugin not initialized")
    } else {
      _instance
    }
  }

  def is(cls: Class[_]): Boolean = {
    clazz.isAssignableFrom(cls)
  }

  def run(args: String*) {
    if(isRunnable) {
      _runMethod.get.invoke(instance, args)
    } else {
      throw new PluginCapabilityException("try to run a non-runnable plugin")
    }
  }

}
