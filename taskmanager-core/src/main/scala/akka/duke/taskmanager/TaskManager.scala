package akka.duke.taskmanager

import akka.actor._
import scala.collection.mutable
import akka.duke.taskmanager.event.Listener
import akka.duke.taskmanager.Configurable.{ConfigurableCommand, AddConfig, SetConfig}
import Message._
import Task._
import akka.duke.taskmanager.Message.BadRequest
import scala.Some
import akka.duke.taskmanager.Configurable.AddConfig
import akka.duke.taskmanager.Configurable.SetConfig

trait TaskManager extends ComposableActor with Configurable with Listener with ActorLogging {
  import TaskManager._

  val taskLoader: TaskLoader

  val taskMap = new mutable.HashMap[String, ActorRef]

  override def preStart() {
    super.preStart()
    log.info(s"${self.path.name} started")
  }

  override def postStop() {
    super.postStop()
    log.info(s"${self.path.name} stopped")
  }

  def handleEvent(event: Event) {}

  private def forwardCommand(taskOpt: Option[ActorRef], command: AnyRef, taskName: String) {
    taskOpt match {
      case Some(task) =>
        log.debug(s"${command.getClass.getSimpleName} forwarded to $taskName")
        task forward command
      case None =>
        if (command.isInstanceOf[TaskRequest]) {
          sender ! BadRequest(s"$taskName does not exist in ${self.path.name}")
        }
    }
  }

  receiveBuilder += {
    case msg @ AddTask(taskId, taskName) =>
      if(taskMap contains taskName) {
        log.error(s"${self.path.name} already have a task named $taskName")
      } else {
        taskLoader.load(taskId, taskName, context) match {
          case Some(task) =>
            taskMap += taskName -> task
            log.info(s"$taskName added to ${self.path.name}")
          case None =>
            sender ! msg
        }
      }

    case RemoveTask(taskName) =>
      taskMap.get(taskName) foreach { task =>
        task ! Stop
        task ! PoisonPill
        taskMap -= taskName
      }

    case ListTasks =>
      sender ! TaskList(taskMap.keys.toList)

    case ListAvailableTasks =>
      sender ! AvailableTaskList(taskLoader.list())

    case ListAvailableTasksFrom(sources) =>
      sender ! AvailableTaskMap(taskLoader.list(sources))

    case TaskCmd(command, taskName) =>
      forwardCommand(taskMap.get(taskName), command, taskName)

    case ConfigTask(command, taskName) =>
      forwardCommand(taskMap.get(taskName), command, taskName)

    case SetTaskConfig(confDef, taskName) =>
      forwardCommand(taskMap.get(taskName), SetConfig(confDef), taskName)

    case AddTaskConfig(confDef, taskName) =>
      forwardCommand(taskMap.get(taskName), AddConfig(confDef), taskName)

  }

}


object TaskManager {

  sealed trait TaskManagerCommand extends Command
  case class  AddTask(taskId: String, taskName: String)             extends TaskManagerCommand
  case class  RemoveTask(taskName: String)                          extends TaskManagerCommand
  case class  TaskCmd(command: TaskCommand, taskName: String)       extends TaskManagerCommand
  case class  SetTaskConfig(confDef: ConfDef, taskName: String)     extends TaskManagerCommand
  case class  AddTaskConfig(confDef: ConfDef, taskName: String)     extends TaskManagerCommand
  case class  ConfigTask(cmd: ConfigurableCommand, taskName: String)extends TaskManagerCommand


  sealed trait TaskManagerRequest extends TaskManagerCommand with Request
  case object ListTasks                                             extends Request
  case object ListAvailableTasks                                    extends Request
  case class  ListAvailableTasksFrom(sources: List[String])         extends Request


  sealed trait TaskManagerResponse                                  extends Responce
  case class  TaskList(tasks: List[String])                         extends TaskManagerResponse
  case class  AvailableTaskList(tasks: List[String])                extends TaskManagerResponse
  case class  AvailableTaskMap(taskMap: Map[String, List[String]])  extends TaskManagerResponse

}
