package au.org.ala.biocache.util

import java.util.concurrent.BlockingQueue
import au.org.ala.biocache.tool.DuplicationDetection
import org.slf4j.LoggerFactory

/**
 * A generic threaded consumer that takes a string and calls the supplied proc
 */
class StringConsumer(q: BlockingQueue[String], id: Int, proc: String => Unit) extends Thread {

  protected val logger = LoggerFactory.getLogger("StringConsumer")

  var shouldStop = false

  override def run() {
    while (!shouldStop || q.size() > 0) {
      try {
        //wait 1 second before assuming that the queue is empty
        val guid = q.poll(1, java.util.concurrent.TimeUnit.SECONDS)
        if (guid != null) {
          logger.debug("Guid Consumer " + id + " is handling " + guid)
          proc(guid)
        }
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
    println("Stopping " + id)
  }
}
