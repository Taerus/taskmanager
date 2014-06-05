package akka.duke.taskmanager

import akka.actor.ActorLogging
import akka.duke.taskmanager.event.Publisher
import akka.duke.taskmanager.event.Events._
import akka.duke.taskmanager.Message._


/** A task has three different behaviors : `stopped`, `running` and `paused` (`paused` is only available if the task
  * implementation class inherit [[Pausable]]).
  * It has five states (`Stopped`, `Running`, `Paused`, `Restarting` and `Terminated`) associated to those behaviours :
  *   - `Stopped` : the task is `stopped`
  *   - `Running` : the task is `running`
  *   - `Paused` : the task is `paused`
  *   - `Restarting` : the task is restarting (while performing stop follow by start, associated with `running`)
  *   - `Terminated` : the task job is complete (equivalent to `Stopped`)
  *
  *
  * Commands handled by `Task` :
  *   - `Start`     : change state from `Stopped` | `Terminated` | `Paused` to `Running`
  *   - `Stop`      : change state from `Running` | `Paused` to `Stopped`
  *   - (*) `Pause`     : change state from `Running` to `Paused`
  *   - `Restart`   : change state from `Running` | `Paused` to `Restarting`
  *   - `Terminate` : change state from `Running` to `Terminated`
  *   - + commands from [[akka.duke.taskmanager.event.Publisher Publisher]]
  *   - + commands from [[Configurable]]
  *
  *   * : if the task inherit [[Pausable]]
  *
  * Requests handled by `Task` :
  *   - `GetState` : Respond by the current state
  *   - + resquests from [[akka.duke.taskmanager.event.Publisher Publisher]]
  *
  * Events :
  *   - StateChanged(newState: State)
  *
  * @note A task must return to its initial state when stop() is invoke in order to be restarted without problem
  */
trait Task extends ComposableActor with Publisher with Configurable with ActorLogging {
  import Task._, State._

  listeners += context.parent

  var state: State = _
  changeState(Stopped)


  override def onConfigChanged() {
    log.debug("configuration changed")
    state match {
      case Running | Restarting | Paused =>
        val msg = s"${self.path.name} must be restarted to apply the new configuration."
        log.warning(msg)
        publish(InfoEvent(msg))
      case _ =>
    }
  }

  override def onApplyConfig() {
    log.info("config applied")
  }

  /** The method called when the task goes into running state. If the task delegate its work to some actors, they have to
    * be created here.
    */
  def start(): Unit

  /** The method called to stop the process. Kills all children actors by default. After this method have been executed
    * the task data should have been reinitialized in order to be restarted later.
    */
  def stop(): Unit = {
    context.children foreach context.stop
  }

  def restart(): Unit = {
    stop()
    start()
  }

  /** This define the behavior of the task in Running state. */
  def runReceive: Receive = PartialFunction.empty[Any, Unit]

  /** This define the behavior of the task in Paused state. */
  def pauseReceive: Receive = runReceive

  /** This define the behavior of the task in Stopped or Terminated state. */
  def stopReceive: Receive = PartialFunction.empty[Any, Unit]

  /** This define the global behavior of the task. Make sure the state behavior does not catch a message that should be
    * processed in this one unless it is expected.
    */
  def commonReceive: Receive = PartialFunction.empty[Any, Unit]


  final def stopped: Receive = {
    case Start =>
      applyConfig()
      changeState(Running)
      start()
  }

  final def running: Receive = {
    case Stop =>
      stop()
      changeState(Stopped)
    case Terminate =>
      stop()
      changeState(Terminated)
    case Restart =>
      changeState(Restarting)
      restart()
      changeState(Running)
    case Pause =>
      if(isInstanceOf[Pausable]) {
        asInstanceOf[Pausable].pause()
        changeState(Paused)
      }
  }

  final def paused: Receive = {
    case Start =>
      changeState(Running)
      asInstanceOf[Pausable].resume()
    case Stop =>
      stop()
      changeState(Stopped)
    case Restart =>
      changeState(Restarting)
      restart()
      changeState(Running)
  }

  final def common: Receive = {
    case GetState =>
      sender ! state
  }

  private def changeState(newState: State = Stopped) {
    newState match {
      case Running | Restarting =>
        become(running    orElse common    orElse runReceive     orElse commonReceive)
      case Paused =>
        become(paused     orElse common    orElse pauseReceive   orElse commonReceive)
      case Stopped | Terminated =>
        become(stopped    orElse common    orElse stopReceive    orElse commonReceive)
    }
    log.info(s"State: $state => $newState")
    state = newState
    publish(StateChanged(newState))
  }

}

object Task {

  object State extends Enumeration {
    type State = Value
    val Stopped,
        Running,
        Paused,
        Restarting,
        Terminated = Value
  }
  import State._

  sealed trait TaskCommand extends Command
  sealed trait TaskRequest extends Request with TaskCommand
  sealed trait TaskEvent   extends Event


  case object Start                         extends TaskCommand
  case object Pause                         extends TaskCommand
  case object Stop                          extends TaskCommand
  case object Restart                       extends TaskCommand
  case object Terminate                     extends TaskCommand

  case object GetState                      extends TaskRequest

  case class  StateChanged(newState: State) extends TaskEvent

}
