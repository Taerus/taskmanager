package akka.duke.taskmanager.plugin

import java.lang.reflect.Method
import akka.duke.taskmanager.Task
import concurrent.duration._
import concurrent.ExecutionContext.Implicits.global
import scala.Some
import akka.actor.{ActorRef, ActorSystem}
import akka.duke.taskmanager.plugin.annotations.Run


class PluginWrapper(clazz: Class[_], id: JarId, system: ActorSystem) {

  private val runMethode: Option[Method] = clazz.getDeclaredMethods.find(_.isAnnotationPresent(classOf[Run]))
  private val parallelRun: Boolean = runMethode.exists(_.getAnnotation(classOf[Run]).parallel)

  private var instance: Any = null


  def init() {
    if(instance != null) throw new PluginIntializationException("Plugin already initialized")
    try {
      instance = clazz.newInstance()
    } catch {
      case e: Exception =>
        throw new PluginIntializationException("failed to instantiate the plugin", e.getCause)
    }
  }

  def isRunnable: Boolean = runMethode.nonEmpty
  def nonRunnable: Boolean = runMethode.isEmpty

  def runDefault(args: String*) {
    run(args:_*)(parallelRun)
  }

  def run(args: String*)(parallel: Boolean = parallelRun) {
    runMethode match {
      case Some(runM) =>
        if(instance == null) throw new RuntimeException("plugin not initialized")
        if(parallel) {
          system.scheduler.scheduleOnce(Duration.Zero){
            runM.invoke(instance, args)
          }
        } else {
          runM.invoke(instance, args)
        }

      case None =>
        throw new PluginCapabilityException("try to run a non-runnable plugin")
    }
  }

  def isTask: Boolean = clazz.isAssignableFrom(classOf[Task])

  def newTask: Task = {
    if(isTask) {
      clazz.newInstance().asInstanceOf[Task]
    } else {
      throw new PluginCapabilityException("non task plugin")
    }
  }

  def newTaskOpt: Option[Task] = {
    if(isTask) {
      Some(clazz.newInstance().asInstanceOf[Task])
    } else {
      None
    }
  }

}
