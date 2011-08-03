package au.org.ala.util

import scala.actors.Actor
import scala.collection.mutable.ArrayBuffer
import au.org.ala.biocache._
import java.io.File

/**
 * A simple threaded implementation of the processing.
 */
object ProcessWithActors {
  import FileHelper._
  val occurrenceDAO = Config.occurrenceDAO
  val persistenceManager = Config.persistenceManager

  def main(args : Array[String]) : Unit = {

    println("Starting...")
    var threads:Int = 4
    var startUuid:Option[String] = None
    
    var dr:Option[String] = None
    val parser = new OptionParser("process records options") {
        intOpt("t", "thread", "The number of threads to use", {v:Int => threads = v } )
        opt("s", "start","The record to start with", {v:String => startUuid = Some(v)})
        opt("dr", "resource", "The data resource to process", {v:String =>dr = Some(v)})
    }
    
    if(parser.parse(args)){
      println("Processing "+dr.getOrElse("")+" from " +startUuid + " with " + threads + "actors")
      processRecords(threads, startUuid, dr)
    }
    
    //shutdown the persistence
    persistenceManager.shutdown
  }
  
  def getProcessedTotal(pool:Array[Actor]):Int ={
    var size =0;
    for(i<-0 to pool.length-1){
      size += pool(i).asInstanceOf[Consumer].processedRecords
    }
    size
  }
  /**
   * Processes the supplied row keys in a Thread
   */
  def processRecords(threads: Int, file:File) : Unit ={
    var ids = 0
    val pool = Array.fill(threads){ val p = new Consumer(Actor.self,ids); ids +=1; p.start }
    println("Starting to process a list of records...");
    val start = System.currentTimeMillis
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis
    var buff = new ArrayBuffer[String]
    //use this variable to evenly distribute the actors work load
    var batches = 0
    var count =0 
val processor = new RecordProcessor
    println("Initialised actors...")
    file.foreachLine(line=>{
        count+=1
        val rec = occurrenceDAO.getRawProcessedByRowKey(line)
        val lstart = System.currentTimeMillis
//        processor.processRecord(rec.get(0), rec.get(1))
//        println("total time " + count + ": " + (System.currentTimeMillis - lstart))
        buff + line
        if(buff.size >= 50){
            val actor = pool(batches % threads).asInstanceOf[Consumer]
        batches += 1
        count+=1
        //find a ready actor...
        while(!actor.ready){ Thread.sleep(50) }

        actor ! buff.toArray
        buff.clear
        
        
      }
        if (count % 1000 == 0) {
        finishTime = System.currentTimeMillis
        println(count
            + " >> Last key : " + line
            + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f)
            + ", time taken for "+1000+" records: " + (finishTime - startTime).toFloat / 1000f
            + ", total time: "+ (finishTime - start).toFloat / 60000f +" minutes"
        )
        }
    })
    //add the remaining records from the buff
    if(buff.size>0){
      pool(0).asInstanceOf[Consumer] ! buff.toArray
      batches+=1
    }
     println(count
            
            + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f)
            + ", time taken for "+1000+" records: " + (finishTime - startTime).toFloat / 1000f
            + ", total time: "+ (finishTime - start).toFloat / 60000f +" minutes"
        )
    println("Finished.")
    //kill the actors
    pool.foreach(actor => actor ! "exit")

    //We can't shutdown the persistence manager until all of the Actors have completed their work
    while(batches > getProcessedTotal(pool)){
      println(batches + " : " + getProcessedTotal(pool))
      Thread.sleep(50)
    }
    
  }
  
  /**
   * Process the records using the supplied number of threads
   */
  def processRecords(threads: Int, firstKey:Option[String], dr: Option[String]): Unit = {
    
    val endUuid = if(dr.isEmpty) "" else dr.get +"|~"
    
    val startUuid = {
	    if(firstKey.isEmpty && !dr.isEmpty) {
	        dr.get +"|"
	    } else {
	       firstKey.get
	    }
    }
    var ids = 0
    val pool = Array.fill(threads){ val p = new Consumer(Actor.self,ids); ids +=1; p.start }
    
    println("Starting with " + startUuid +" endingwith " + endUuid)
    val start = System.currentTimeMillis
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis

    println("Initialised actors...")

    var count = 0
    var guid = "";
    //use this variable to evenly distribute the actors work load
    var batches = 0

    var buff = new ArrayBuffer[(FullRecord,FullRecord)]

    //occurrenceDAO.pageOverAll(Raw, fullRecord => {
    occurrenceDAO.pageOverRawProcessed(rawAndProcessed => {
    if(guid == "")
      println("First rowKey processed: " + rawAndProcessed.get._1.rowKey)
      guid = rawAndProcessed.get._1.rowKey
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
            + " >> Last key : " + rawAndProcessed.get._1.rowKey
            + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f)
            + ", time taken for "+1000+" records: " + (finishTime - startTime).toFloat / 1000f
            + ", total time: "+ (finishTime - start).toFloat / 60000f +" minutes"
        )
        startTime = System.currentTimeMillis
      }
      true //indicate to continue
    }, startUuid, endUuid)
    
    println("Last row key processed: " + guid)
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
  }
}

/**
 * A consumer actor asks for new work.
 */
class Consumer (master:Actor,val id:Int)  extends Actor  {

  println("Initialising thread: "+id)
  val processor = new RecordProcessor
  val occurrenceDAO = Config.occurrenceDAO
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
        case keys:Array[String]=>{
            //get the raw and Processed records for the row key
            received +=1
            val start = System.currentTimeMillis  
            var counter:Float=0
            for(key <- keys){
                counter +=1
                val records = occurrenceDAO.getRawProcessedByRowKey(key)
                if(!records.isEmpty){
                    processor.processRecord(records.get(0), records.get(1))
                }
            }
            val finished = System.currentTimeMillis
            println("Actor "+id +">>> Last Key: "+keys.last+", records per sec: " + counter / (((finished - start).toFloat) / 1000f))
            processedRecords+=1
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