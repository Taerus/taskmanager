import akka.duke.taskmanager.plugin.{PluginDef, PluginDefParser, PluginManager, DirWatcher}, DirWatcher._
import java.io.File
import java.nio.file.Files

object Test extends App {

  PluginDefParser("""d> pluginZ
                        |c> cZ
                        |r> rZ
                        |
                        |plugin1
                        |  C> plugin.PluginMain
                        |  R> pluginMain par
                        |  D> plugin2
                        |
                        |drop1
                        |  r> run
                        |
                        |drop2
                        |  c> c1
                        |  c> c2
                        |
                        |plugin1-2
                        |  c> plugin.Lol
                        |  d> plugin2
                        |  d> plugin3
                        |
                        |plugin1-3
                      """.stripMargin)
  .foreach { case (pn, PluginDef(c, d, r)) =>
    println(s"$pn(${c.getOrElse("n/a")} (${r.getOrElse("n/a")}) {${d.mkString(", ")}})")
  }

  PluginManager.load("plugin1")
  val plugin = PluginManager.getPlugin("plugin1", "plugin1").get
  plugin.init()
  plugin.run("wru")()

  val dw = new DirWatcher(".", extensionFilter("txt"), not(containsFilter("Nouveau")))
  val f = new File("text.txt")
  f.createNewFile()
  println(dw.waitChanges())
  val f2 = new File("text2.txt")
  Thread.sleep(500)
  println(dw.hasChanges)
  Files.move(f.toPath, f2.toPath)
  Thread.sleep(500)
  println(dw.hasChanges)
  println(dw.waitChanges())
  f2.delete()
  println(dw.waitChanges())
//  while(true) {
//    println(dw.waitChanges())
//  }

  PluginManager.stop()

}