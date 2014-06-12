package akka.duke.taskmanager.plugin


object PluginDefParser {

  private val GLOBAL = "__GLOBAL__"
  
  private val pluginNamePatern = """(\S+)(?:\s.*)?""".r
  private val pluginConfigPatern = """^(.)>\s*(\S.*)$""".r

  
  def apply(str: String): PluginDefMap = {

    val lines = str.split('\n').map(_.trim).filterNot(_.isEmpty)

    var globalDep = Set.empty[String]
    var pDefs = PluginDefMapUtil.empty()
    var p: String = GLOBAL
    var (c, r, d): (String, String, Set[String]) = (null, null, null)

    var drop = false

    def reset() {
      c = ""
      r = ""
      d = Set.empty[String]
    }

    def add() {
      if(p == GLOBAL) {
        globalDep = d
      } else if(!drop) {
        if(c.isEmpty && r.nonEmpty) {
          println(s"error: run method defined without associated class : $r in $p")
        } else {
          pDefs += p -> PluginDef(Option(c), (d ++ globalDep).toArray, Option(r))
        }
      }
      reset()
      drop = false
    }

    reset()

    lines.foreach {
      case pluginConfigPatern(t, ps) =>
        if(!drop) {
          val params = ps.split("\\s+")
          t.toLowerCase match {
            case "c" | "class" =>
              val cn = params.head
              if (p == GLOBAL) {
                println(s"warning: class tag defined outside a plugin definition is ignored : $cn")
              } else if (c.nonEmpty) {
                println(s"error: class already defined for $p : $cn")
                drop = true
              } else {
                c = cn
              }

            case "r" | "run" =>
              val rn = params.head
              if (p == GLOBAL) {
                println(s"warning: run method tag defined outside a plugin definition is ignored : $rn")
              } else if (r.nonEmpty) {
                println(s"error: run method already defined for $p : $rn")
                drop = true
              }
              r = rn

            case "d" | "dependency" =>
              d += params.head

            case _ => ()
          }
        }

      case pluginNamePatern(line) =>
        add()
        p = line

      case _ => ()
    }

    add()

    pDefs
  }

}
