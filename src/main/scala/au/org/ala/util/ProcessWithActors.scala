package au.org.ala.util

import scala.actors.Actor
import au.org.ala.biocache.FullRecord
import au.org.ala.biocache.OccurrenceDAO
import au.org.ala.biocache.Raw
import scala.collection.mutable.ArrayBuffer

/**
 * A simple threaded implementation of the processing.
 */
object ProcessWithActors {
  def main(args : Array[String]) : Unit = {

    println("Starting...")
    var ids = 0
    val threads = 4
    val pool = Array.fill(threads){ val p = new Consumer(Actor.self,ids); ids+=1; p.start }

    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis

    println("Initialised actors...")

    var count = 0

    var buff = new ArrayBuffer[FullRecord]

    OccurrenceDAO.pageOverAll(Raw, fullRecord => {

      count += 1

      if(buff.size<50){
        buff + fullRecord.get
      } else {
        val actor = pool(count % threads).asInstanceOf[Consumer]

        //find a ready actor...
        while(!actor.ready){
          Thread.sleep(50)
          //println("Backing off with thread: "+actor.id)
        }

        actor ! buff.toArray
        buff.clear
      }

      //debug counter
      if (count % 1000 == 0) {
        finishTime = System.currentTimeMillis
        println(count + " >> Last key : " + fullRecord.get.occurrence.uuid + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
        startTime = System.currentTimeMillis
      }
      true //indicate to continue
    })
    println("Finished.")
  }
}


class Consumer (master:Actor,val id:Int)  extends Actor  {

  println("Initialising thread: "+id)

  var received = 0
  var processed = 0

  def ready = processed==received

  def act {
    println("In (ACT) thread: "+id)
    loop{
      react {
        case raw:FullRecord => {
          received += 1
          //println(id+"] Processing " + raw.o.uuid +", Received: "+ received+", Processed: "+processed)
          ProcessRecords.processRecord(raw)
          processed += 1
          //println(id+"] Processing " + raw.o.uuid +", Received: "+ received+", Processed: "+processed)
//					master ! "done"
        }
        case raws:Array[FullRecord] => {
          received += 1
          //println(id+"] Processing " + raw.o.uuid +", Received: "+ received+", Processed: "+processed)
          for(raw<-raws) { ProcessRecords.processRecord(raw) }
          processed += 1
          //println(id+"] Processing " + raw.o.uuid +", Received: "+ received+", Processed: "+processed)
//					master ! "done"
        }
      }
    }
  }
}