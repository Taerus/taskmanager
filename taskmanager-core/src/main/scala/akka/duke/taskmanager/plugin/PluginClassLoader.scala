package akka.duke.taskmanager.plugin

import scala.collection.mutable
import java.net.URL
import scala.tools.nsc.util.ScalaClassLoader.URLClassLoader
import java.io.File


class PluginClassLoader(val jarPath: String,
                        parent: ClassLoader = ClassLoader.getSystemClassLoader)
  extends ClassLoader(parent) {

  protected val _dependencies: mutable.HashMap[String, Long] = mutable.HashMap.empty[String, Long]
  protected var built = false
  protected lazy val ucl = {
    built = true
    new URLClassLoader(
      Seq(new File(jarPath).toURI.toURL) ++ dependencies.map( d => new File(path(d._1, d._2)).toURI.toURL),
      parent)
  }


  def dependencies = _dependencies.toMap

  def +=(jarName: String, tempId: Long) {
    if(!built) {
      _dependencies += jarName -> tempId
    }
  }

  def ++=(dependencies: TraversableOnce[(String, Long)]) {
    if(!built) {
      this._dependencies ++= dependencies
    }
  }

  override def loadClass(name: String): Class[_] = {
    ucl.loadClass(name)
  }

  override def findResource(name: String): URL = {
    ucl.getResource(name)
  }

  override def equals(that: Any): Boolean = {
    if(!that.isInstanceOf[PluginClassLoader]) {
      return false
    }

    val cl = that.asInstanceOf[PluginClassLoader]

    cl.jarPath == jarPath && cl._dependencies == _dependencies
  }

}


object PluginClassLoader {

  def apply(jarName: String, tempId: Long = -1L, parent: ClassLoader = ClassLoader.getSystemClassLoader) = {
    new PluginClassLoader(path(jarName, tempId), parent)
  }

}

