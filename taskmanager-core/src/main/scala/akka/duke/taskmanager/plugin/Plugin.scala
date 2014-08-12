package akka.duke.taskmanager.plugin


class Plugin(classLoader: ClassLoader, pluginDef: PluginDef) {
  import Plugin._

  lazy val entries = pluginDef.entries.map {
    case (name, entryDef) => name -> Entry(classLoader, entryDef)
  }

  def taskEntries: Map[String, TaskEntry] = entries.collect { case (n, e: TaskEntry) => n -> e }

  def runnableEntries: Map[String, RunnableEntry] = entries.collect { case (n, e: RunnableEntry) => n -> e }


  def listEntries: Vector[String] = entries.keys.toVector

  def listTasks: Vector[String] = taskEntries.keys.toVector

  def listRunnables: Vector[String] = runnableEntries.keys.toVector


  def apply(entryName: String): Entry = {
    entries(entryName)
  }

  def task(entryName: String) = taskEntries(entryName)

  def runnable(entryName: String) = runnableEntries(entryName)


  def getTask(entryName: String) = taskEntries.get(entryName)

  def getRunnable(entryName: String) = runnableEntries.get(entryName)


  def newEntry(entryName: String) = Entry(classLoader, pluginDef.entries(entryName))

  def newTaskEntry(entryName: String) = newEntry(entryName).asInstanceOf[TaskEntry]

  def newRunnableEntry(entryName: String) = newEntry(entryName).asInstanceOf[RunnableEntry]

}


object Plugin {

  object Entry{
    def apply(classLoader: ClassLoader, entryDef: PluginEntry): Entry = {
      val clazz = classLoader.loadClass(entryDef.`class`)
      entryDef match {
        case PluginEntry(_, _, true) =>
          new TaskEntry(clazz)

        case PluginEntry(_, Some(runMethod), _) =>
          new RunnableEntry(clazz, runMethod)

        case _ =>
          new Entry(clazz)
      }
    }
  }

  class Entry(clazz: Class[_]) {
    lazy val instance = clazz.newInstance()

    def is(cls: Class[_]): Boolean = {
      clazz.isAssignableFrom(cls)
    }
  }
  
  class TaskEntry(clazz: Class[_]) extends Entry(clazz) {
    def task = clazz

    def factory = ???
  }
  
  class RunnableEntry(clazz: Class[_], runMethod: String) extends Entry(clazz) {
    private lazy val _runMethod = clazz.getDeclaredMethod(runMethod, classOf[Seq[_]])
    def run(args: String*) {
      _runMethod.invoke(instance, args)
    }
  }
  
}
