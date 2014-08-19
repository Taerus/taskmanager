package akka.duke.taskmanager

import com.typesafe.config.{ConfigFactory, Config}


object ConfDef {
  def apply(configs: Config*) = new ConfDef(configs.reverse.reduceLeft(_ withFallback _))
  def apply(name: String) = new ConfDef(name)
  def apply(id: String, name: String) = new ConfDef(id, name)
  def parseString(config: String) = ConfDef(ConfigFactory.parseString(config))
  def default = ConfDef("").default
}

class ConfDef {
  private var t: Byte = _
  private var i: String = _
  private var n: String = _
  private var c: Config = _
  private var d: Boolean = _

  private def this(t: Byte, i: String, n: String, c: Config, d: Boolean = false) { this()
    this.t = t; this.d = d; this.i = i; this.n = n; this.c = c
  }

  private def this(config: Config) = this(0, null, null, config)
  private def this(name: String) = this(1, null, name, null)
  private def this(id: String, name: String) = this(2, id, name, null)

  def default = new ConfDef(t, i, n, c, true)

  def isDefault = d
  def nonDefault = !d

  def getConfig = Option(c)
  def getName: Option[String] = if(t == 1) Option(n) else None
  def getIdName: Option[(String, String)] = if(t == 2) Option(i,n) else None

  def get: AnyRef = t match {
    case 0 => c
    case 1 => n
    case 2 => (i,n)
  }

}
