import akka.duke.taskmanager.plugin.{Plugin, JarId, Cache, PluginManager}
import ch.qos.logback.classic.{Logger, Level}
import com.typesafe.scalalogging
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.slf4j.LoggerFactory


object Test extends App with LazyLogging {
  LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger].setLevel(Level.TRACE)

  Cache.directory = "plugins/.cache"


  implicit class DebugLogger(logger: scalalogging.slf4j.Logger) {
    def doInDebug(f: DebugLogger => Unit) {
      if(logger.underlying.isDebugEnabled) f(this)
    }

    def apply(message: String) {
      logger.underlying.debug(message)
    }
  }

  implicit class InfoLogger(logger: scalalogging.slf4j.Logger) {
    def doInInfo(f: InfoLogger => Unit) {
      if(logger.underlying.isInfoEnabled) f(this)
    }

    def apply(message: String) {
      logger.underlying.info(message)
    }
  }


  logger.doInDebug { log =>
    PluginManager.plugins.foreach {
      case (jarName, versions) =>
        log(jarName)
        versions.foreach {
          case (version, plugins) =>
            log(f"  #$version%019d")
            plugins.foreach(plugin => log(s"    - $plugin"))
        }
    }
    logger.debug("")
  }

//  val dw = new DirWatcher(".", extensionFilter("txt"), not(containsFilter("Nouveau")))
//  val f = new File("text.txt")
//  f.createNewFile()
//  println(dw.waitChanges())
//  val f2 = new File("text2.txt")
//  Thread.sleep(500)
//  println(dw.hasChanges)
//  Files.move(f.toPath, f2.toPath)
//  Thread.sleep(500)
//  println(dw.hasChanges)
//  println(dw.waitChanges())
//  f2.delete()
//  println(dw.waitChanges())


  println(PluginManager.pluginNames)

  PluginManager.load("plugin1")
  PluginManager.getPlugin("plugin 1").foreach { plugin =>
    println(plugin.listEntries.mkString("entries {\n  ", "\n  ", "\n}"))
    println(plugin.listTasks.mkString("tasks {\n  ", "\n  ", "\n}"))
    println(plugin.listRunnables.mkString("runnables {\n  ", "\n  ", "\n}"))

    val main = plugin.runnable("main")
    println(main.instance.getClass.getClassLoader.getResource("plugin/PluginMain.class"))
    main.run("wru", "exit")

//    val main2 = plugin.newRunnableEntry("main")
//    main2.run("wru", "exit")
  }


//  PluginManager.getPlugin("decount")

//  while(true) {
//    println(dw.waitChanges())
//  }

  PluginManager.stop()

}