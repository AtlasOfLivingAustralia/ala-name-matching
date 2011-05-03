package au.org.ala.util

import scala.actors.Actor
import scala.collection.mutable.ArrayBuffer
import au.org.ala.biocache._

/**
 * A simple threaded implementation of the processing.
 */
object ProcessWithActors {

  val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
  val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]

  def main(args : Array[String]) : Unit = {

    println("Starting...")
    var ids = 0
    val threads = {
        if(args.length>0){
             args(0).toInt
        } else {
            4
        }
    }
    val pool = Array.fill(threads){ val p = new Consumer(Actor.self,ids); ids+=1; p.start }

    val start = System.currentTimeMillis
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis

    println("Initialised actors...")

    var count = 0

    var buff = new ArrayBuffer[FullRecord]

    occurrenceDAO.pageOverAll(Raw, fullRecord => {

      count += 1
      //we want to add the record to the buffer whether or not we send them to the actor
      buff + fullRecord.get
      if(buff.size>=50){
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
        println(count + " >> Last key : " + fullRecord.get.uuid
            + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f)
            + ", time taken for "+1000+" records: " + (finishTime - startTime).toFloat / 1000f
            + ", total time: "+ (finishTime - start).toFloat / 60000f +" minutes"
        )
        startTime = System.currentTimeMillis
      }
      true //indicate to continue
    })
    println("Finished.")
    //kill the actors 
    pool.foreach(actor => actor ! "exit")

    persistenceManager.shutdown
  }
}


class Consumer (master:Actor,val id:Int)  extends Actor  {

  println("Initialising thread: "+id)

  var received = 0
  var processed = 0

  def ready = processed==received

  def act {
    println("In (Actor.act) thread: "+id)
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
        case s:String => {
            if(s == "exit"){
              println("Killing (Actor.act) thread: "+id)
              exit()
            }
        }
      }
    }
  }
}