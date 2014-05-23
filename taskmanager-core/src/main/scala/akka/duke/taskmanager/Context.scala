package akka.duke.taskmanager

import scala.collection.mutable


object Context {

  private val map = new mutable.HashMap[String, AnyRef].par

  def update(key: String, value: AnyRef) {
    map += key -> value
  }

  def apply[T <: AnyRef](key: String): T = {
    map(key).asInstanceOf[T]
  }

//  def apply[T <: AnyRef](kd: (String, String)) = {
//    map(kd._1) = map(kd._2)
//  }

  def apply[T <: AnyRef](kds: (String, TraversableOnce[String])) = {
    kds._2.foreach( dest => map(kds._1) = map(dest) )
  }

  def copy[T <: AnyRef](select: String, dest: String*) = {
    this.apply(select -> dest)
  }

  def get[T <: AnyRef](key: String): Option[T] = {
    map.get(key).asInstanceOf[Option[T]]
  }

  def +=(kv: (String, AnyRef)) {
    map += kv
  }

  def ++=(xs: TraversableOnce[(String, AnyRef)]) {
    map ++= xs
  }

  def -=(key: String) {
    map -= key
  }

  def --=(xs: TraversableOnce[String]) {
    map --= xs
  }

  def contains(key: String): Boolean = {
    map.contains(key)
  }

  def keys = map.keys

}
