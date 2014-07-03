//package akka.duke.taskmanager.plugin
//
//import java.io.{FileNotFoundException, File}
//import scala.io.Source
//import org.slf4j.LoggerFactory
//import com.typesafe.scalalogging.slf4j.LazyLogging
//
//
//object PluginDefParser extends LazyLogging {
//
//  private val GLOBAL = "__GLOBAL__"
//
//  private val pluginNamePatern = """(\S+)(?:\s.*)?""".r
//  private val pluginConfigPatern = """^(.)>\s*(\S.*)$""".r
//
//
//  def apply(str: String): PluginDefMap = {
//    parseString(str)
//  }
//
//  def parseString(str: String): PluginDefMap = {
//
//    val lines = str.split('\n').map(_.trim).filterNot(_.isEmpty)
//
//    var globalDep = Set.empty[String]
//    var pDefs = PluginDefMapUtil.empty()
//    var p: String = GLOBAL
//    var (c, r, d): (String, String, Set[String]) = (null, null, null)
//
//    var drop = false
//
//    def reset() {
//      c = ""
//      r = ""
//      d = Set.empty[String]
//    }
//
//    def add() {
//      if(p == GLOBAL) {
//        globalDep = d
//      } else if(!drop) {
//        if(c.isEmpty && r.nonEmpty) {
//          logger.error(s"run method defined without associated class : $r in $p")
//        } else {
//          pDefs += p -> PluginDefO(Option(c), (d ++ globalDep).toArray, Option(r))
//        }
//      }
//      reset()
//      drop = false
//    }
//
//    reset()
//
//    lines.foreach {
//      case comments if comments.startsWith("#") => ()
//
//      case pluginConfigPatern(t, ps) =>
//        if(!drop) {
//          val params = ps.split("\\s+")
//          t.toLowerCase match {
//            case "c" | "class" =>
//              val cn = params.head
//              if (p == GLOBAL) {
//                logger.warn(s"class tag defined outside a plugin definition is ignored : $cn")
//              } else if (c.nonEmpty) {
//                logger.error(s"class already defined for $p : $cn")
//                drop = true
//              } else {
//                c = cn
//              }
//
//            case "r" | "run" =>
//              val rn = params.head
//              if (p == GLOBAL) {
//                logger.warn(s"run method tag defined outside a plugin definition is ignored : $rn")
//              } else if (r.nonEmpty) {
//                logger.error(s"run method already defined for $p : $rn")
//                drop = true
//              }
//              r = rn
//
//            case "d" | "dependency" =>
//              d += params.head
//
//            case _ => ()
//          }
//        }
//
//      case pluginNamePatern(line) =>
//        add()
//        p = line
//
//      case _ => ()
//    }
//
//    add()
//
//    pDefs
//  }
//
//  def parseFile(path: String): Option[PluginDefMap] = {
//    parseFile(new File(path))
//  }
//
//  def parseFile(file: File): Option[PluginDefMap] = {
//    if(file.exists && file.isFile) {
//      try {
//        val src = Source.fromFile(file)
//        val str = src.getLines().mkString("\n")
//        src.close()
//        Option(parseString(str))
//      } catch {
//        case e: FileNotFoundException =>
//          logger.error(e.getMessage)
//          None
//        case e: Exception =>
//          None
//      }
//    } else {
//      None
//    }
//  }
//
//}
