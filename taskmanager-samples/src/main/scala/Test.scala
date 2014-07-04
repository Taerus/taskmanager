import akka.duke.taskmanager.plugin.{Cache, PluginManager}
import ch.qos.logback.classic.{Logger, Level}
import com.typesafe.scalalogging
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.slf4j.LoggerFactory


object Test extends App with LazyLogging {
  LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger].setLevel(Level.TRACE)

  Cache.directory = "plugins/.cache"
//  Cache.clear()

//  val conf = ConfigFactory.parseString(
//    """a {
//      |  b : 4
//      |  c = [1,2,3]
//      |}
//    """.stripMargin)
//  println(pretty(render(parse(conf.root.render(ConfigRenderOptions.concise())))))
//
//
//  val json = parse("""
//    {
//      "dependencies": ["plugin2"],
//      "plugins": [{
//         "class": "plugin.PluginMain",
//         "run": "pluginMain"
//      }]
//    }
//                   """)
//
//  println(pretty(render(json)))
//
//  PluginDefParser("""d> pluginZ
//                      |c> cZ
//                      |r> rZ
//                      |
//                      |plugin1
//                      |  C> plugin.PluginMain
//                      |  R> pluginMain par
//                      |  D> plugin2
//                      |
//                      |drop1
//                      |  r> run
//                      |
//                      |drop2
//                      |  c> c1
//                      |  c> c2
//                      |
//                      |plugin1-2
//                      |  c> plugin.Lol
//                      |  d> plugin2
//                      |  d> plugin3
//                      |
//                      |plugin1-3
//                      """.stripMargin)
//  .foreach { case (pn, PluginDefO(c, d, r)) =>
//    println(s"$pn(${c.getOrElse("n/a")} (${r.getOrElse("n/a")}) {${d.mkString(", ")}})")
//  }

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

//  logger.doInInfo { log =>
//    println("println info")
//    log("log info")
//  }
//
//  logger.doInDebug { log =>
//    println("println debug")
//    log("log debug")
//  }

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

  PluginManager.load("plugin1")
  PluginManager.getPlugin("plugin1", "main").foreach { plugin =>
    plugin.init()
    plugin.run("wru", "exit")
  }

//  PluginManager.getPlugin("decount")

//  while(true) {
//    println(dw.waitChanges())
//  }

  PluginManager.stop()

}