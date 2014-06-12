package akka.duke.taskmanager.plugin


class PluginIntializationException(message: String = null, cause: Throwable = null)
  extends RuntimeException(message, cause)


class PluginCapabilityException(message: String) extends RuntimeException(message)