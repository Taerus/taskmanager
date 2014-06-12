package akka.duke.taskmanager.plugin

import java.nio.file._
import java.nio.file.StandardWatchEventKinds._
import scala.collection.JavaConverters._
import DirWatcher._
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

class DirWatcher(dirPath: String, filters: Filter*) {

  private val filter: Filter = {
    filename => filters.forall(_(filename))
  }

  private val watcher = FileSystems.getDefault.newWatchService()
  private var temp: Option[(Set[String], Set[String])] = None
  private val path = Paths.get(dirPath)
  path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)

  def changes: Option[(Set[String], Set[String])] = {
    Option(watcher.poll()).map(process).orElse(pullTemp())
  }

  def waitChanges(timeout: Duration = Duration.Inf): Option[(Set[String], Set[String])] = {
    if(temp.isDefined) {
      Option(watcher.poll()).map(process).orElse(pullTemp())
    } else {
      Option {
        if(timeout == Duration.Inf) {
          watcher.take()
        } else {
          watcher.poll(timeout.toMillis, TimeUnit.MILLISECONDS)
        }
      }.map(process).orElse(pullTemp())
    }
  }

  def hasChanges: Boolean = {
    if(temp.isEmpty) temp = Option(watcher.poll()).map(process)
    temp.isDefined
  }
  
  private def pullTemp(): Option[(Set[String], Set[String])] = {
    val ret = temp
    temp = None
    ret
  }

  private def process(key: WatchKey): (Set[String], Set[String]) = {
    var (added, removed) = if(temp.isDefined) pullTemp().get else (Set.empty[String], Set.empty[String])

    for(event <- key.pollEvents.asScala if event.kind != OVERFLOW) {
      val filename = event.asInstanceOf[WatchEvent[Path]].context().getFileName.toString
      if( filter(filename) )
        event.kind match {
          case ENTRY_CREATE =>
            added += filename

          case ENTRY_DELETE =>
            removed += filename

          case ENTRY_MODIFY =>
            removed += filename
            added += filename
        }
    }

    key.reset()

    (added, removed)
  }

}


object DirWatcher {

  type Filter = String => Boolean

  def not(filter: Filter): Filter = {
    filename => !filter(filename)
  }

  def extensionFilter(extensions: String*): Filter = {
    filename: String => extensions.exists(ext => filename.toLowerCase endsWith s".${ext.toLowerCase}" )
  }

  def prefixFilter(prefixes: String*): Filter = {
    filename: String => prefixes.exists(filename startsWith)
  }

  def suffixFilter(suffixes: String*): Filter = {
    filename: String => suffixes.exists(filename.replaceFirst("""^(.+)\.\w+$""", "$1") endsWith)
  }

  def matchFilter(regex: String): Filter = {
    filename: String => filename.matches(regex)
  }

  def containsFilter(subStr: String): Filter = {
    filename: String => filename.contains(subStr)
  }

}
