package akka.duke.taskmanager

trait Pausable {

  def pause(): Unit

  def resume(): Unit

}
