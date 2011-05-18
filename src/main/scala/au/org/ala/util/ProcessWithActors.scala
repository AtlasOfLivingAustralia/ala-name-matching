package au.org.ala.util

import scala.actors.Actor
import scala.collection.mutable.ArrayBuffer
import au.org.ala.biocache._

/**
 * A simple threaded implementation of the processing.
 */
object ProcessWithActors {

  val occurrenceDAO = Config.occurrenceDAO
  val persistenceManager = Config.persistenceManager

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
    val startUuid =if(args.length>1) args(1) else ""

    println("Processing from " +startUuid + " with " + threads + "actors")

    val pool = Array.fill(threads){ val p = new Consumer(Actor.self,ids); ids+=1; p.start }

    val start = System.currentTimeMillis
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis

    println("Initialised actors...")

    var count = 0

    //use this variable to evenly distribute the actors work load
    var batches = 0

    var buff = new ArrayBuffer[(FullRecord,FullRecord)]

    //occurrenceDAO.pageOverAll(Raw, fullRecord => {
    occurrenceDAO.pageOverRawProcessed(rawAndProcessed => {

      count += 1
      //we want to add the record to the buffer whether or not we send them to the actor
      buff + rawAndProcessed.get
      if(buff.size>=50){
        val actor = pool(batches % threads).asInstanceOf[Consumer]
        batches += 1
        //find a ready actor...
        while(!actor.ready){ Thread.sleep(50) }

        actor ! buff.toArray
        buff.clear
      }

      //debug counter
      if (count % 1000 == 0) {
        finishTime = System.currentTimeMillis
        println(count
            + " >> Last key : " + rawAndProcessed.get._1.uuid
            + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f)
            + ", time taken for "+1000+" records: " + (finishTime - startTime).toFloat / 1000f
            + ", total time: "+ (finishTime - start).toFloat / 60000f +" minutes"
        )
        startTime = System.currentTimeMillis
      }
      true //indicate to continue
    },startUuid)
    //add the remaining records from the buff
    if(buff.size>0){
      pool(0).asInstanceOf[Consumer] ! buff.toArray
      batches+=1
    }
    println("Finished.")
    //kill the actors 
    pool.foreach(actor => actor ! "exit")
    
    //We can't shutdown the persistence manager until all of the Actors have completed their work
    while(batches > getProcessedTotal(pool)){
      println(batches + " : " + getProcessedTotal(pool))
      Thread.sleep(50)
    }
    
    persistenceManager.shutdown
  }
  def getProcessedTotal(pool:Array[Actor]):Int ={
    var size =0;
    for(i<-0 to pool.length-1){
      size += pool(i).asInstanceOf[Consumer].processedRecords
    }
    size
  }
}


class Consumer (master:Actor,val id:Int)  extends Actor  {

  println("Initialising thread: "+id)
  val processor = new RecordProcessor

  var received, processedRecords = 0

  def ready = processedRecords == received

  def act {
    println("In (Actor.act) thread: "+id)
    loop{
      react {
        case rawAndProcessed :(FullRecord,FullRecord) => {
          val (raw, processed) = rawAndProcessed
          received += 1
          processor.processRecord(raw, processed)
          processedRecords += 1
        }
        case batch:Array[(FullRecord,FullRecord)] => {
          received += 1
          for((raw,processed) <- batch) { processor.processRecord(raw, processed) }
          processedRecords += 1
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
