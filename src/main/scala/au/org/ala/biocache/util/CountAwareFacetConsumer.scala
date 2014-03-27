package au.org.ala.biocache.util

import au.org.ala.biocache.tool.DuplicationDetection
import scala.collection.mutable.ArrayBuffer
import java.util.concurrent.BlockingQueue

class CountAwareFacetConsumer(q: BlockingQueue[String], id: Int, proc: Array[String] => Unit, countSize: Int = 0, minSize: Int = 1) extends Thread {

  var shouldStop = false

  override def run() {
    val buf = new ArrayBuffer[String]()
    var counter = 0
    var batchSize = 0
    while (!shouldStop || q.size() > 0) {
      try {
        //wait 1 second before assuming that the queue is empty
        val value = q.poll(1, java.util.concurrent.TimeUnit.SECONDS)
        if (value != null) {
          DuplicationDetection.logger.debug("Count Aware Consumer " + id + " is handling " + value)
          val values = value.split("\t")
          val count = Integer.parseInt(values(1))
          if (count >= minSize) {
            counter += count
            batchSize += 1
            buf += values(0)
            if (counter >= countSize || batchSize == 200) {
              val array = buf.toArray
              buf.clear()
              counter = 0
              batchSize = 0
              proc(array)
            }
          }
        }
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
    println("Stopping " + id)
  }
}