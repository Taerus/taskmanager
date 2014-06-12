package akka.duke.taskmanager.plugin

import java.util.jar.JarFile
import scala.collection.mutable


class PluginClassLoader(val jarPath: String,
                        parent: ClassLoader = ClassLoader.getSystemClassLoader)
  extends ClassLoader(parent) {

  protected val _dependencies: mutable.HashMap[String, Long] = mutable.HashMap.empty[String, Long]
  protected var built = false


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

  override protected def findClass(name: String): Class[_] = {
    if(!built) {
      built = true
    }

    try {
      val b = loadClassData(name)
      return defineClass(name, b, 0, b.length)
    } catch { case e: Exception => () }

    for( (jarName, tempId) <- _dependencies ) {
      try {
        val b = loadClassData(name, path(jarName, tempId))
        return defineClass(name, b, 0, b.length)
      } catch { case e: Exception => () }
    }

    throw new ClassNotFoundException(name)
  }

  override def equals(that: Any): Boolean = {
    if(!that.isInstanceOf[PluginClassLoader]) {
      return false
    }

    val cl = that.asInstanceOf[PluginClassLoader]

    cl.jarPath == jarPath && cl._dependencies == _dependencies
  }

  protected def loadClassData(name: String, path: String = jarPath): Array[Byte] = {
    PluginClassLoader.loadClassData(name, path)
  }

}


object PluginClassLoader {

  def apply(jarName: String, tempId: Long = -1L, parent: ClassLoader = ClassLoader.getSystemClassLoader) = {
    new PluginClassLoader(path(jarName, tempId), parent)
  }

  def loadClassData(name: String, jarPath: String): Array[Byte] = {
    val jar = new JarFile(jarPath)
    val entry = jar.getEntry(name.replace(".", "/") + ".class")
    val is = jar.getInputStream(entry)
    val b = new Array[Byte](is.available())
    is.read(b)
    jar.close()

    b
  }

}

