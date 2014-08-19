package akka.duke.taskmanager.samples.tasks

import akka.actor.Cancellable
import akka.duke.taskmanager.{Task, Pausable}

import scala.concurrent.duration._


class PausableTask extends Task with Pausable {
  import context.dispatcher

  var sch: Cancellable = _

  def start() {
    print("| Started")
    startScheduler()
  }

  override def stop() {
    stopScheduler()
    println("| Stopped")
  }

  def pause() {
    stopScheduler()
    println("| Paused")
  }

  def resume() {
    print("| Resumed")
    startScheduler()
  }


  def startScheduler() {
    print(" ")
    sch = context.system.scheduler.schedule(0.millis, 400.millis) {
      print(".")
    }
  }

  def stopScheduler() {
    sch.cancel()
    println()
  }

}
