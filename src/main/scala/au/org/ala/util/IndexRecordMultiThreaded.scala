package au.org.ala.util
import scala.collection.mutable.HashMap
import au.org.ala.biocache.{Json, SolrIndexDAO, Config}
import org.apache.commons.io.FileUtils
import java.io.File
import org.apache.lucene.misc.IndexMergeTool
import scala.io.Source
import java.net.URL
import scala.util.parsing.json.JSON
import java.lang.Thread
import scala.collection.mutable.ArrayBuffer
import scala.actors.Actor
import au.org.ala.biocache.FullRecord

object CreateIndexesAndMerge extends Counter {

  def main(args:Array[String]){
     createIndexesAndMerge()
  }

  def createIndexesAndMerge(){
    //create a copy of the /data/solr/bio-proto/conf in each directory
   //val ir1 = new IndexRunner(this, 1, "dr658|AB378537", "dr658|ACUQ01000964", "/data/solr/bio-proto/conf", "/data/solr-create/bio-proto-thread1/conf")
  // ir1.run
    IndexMergeTool.main(Array("/data/solr/bio-proto-merged/data/index","/data/solr-create/bio-proto-thread-0/data/index","/data/solr-create/bio-proto-thread-1/data/index","/data/solr-create/bio-proto-thread-2/data/index"))

//   val ir2 = new IndexRunner(2, "dr658|ACUQ01000964", "dr658|ACUQ01001963", "/data/solr/bio-proto/conf", "/data/solr-create/bio-proto-thread2/conf")
//   ir2.run
//
//   IndexMergeTool.main(Array("/data/solr/bio-proto-merged/data/index",
//      "/data/solr/bio-proto-thread1/data/index","/data/solr/bio-proto-thread1/data/index"))
  }
}

trait Counter {
  var counter = 0
  def addToCounter(amount:Int) = counter += amount
  var startTime = System.currentTimeMillis
  var finishTime = System.currentTimeMillis
  def printOutStatus(threadId:Int, lastKey:String) = {
    finishTime = System.currentTimeMillis
    println("[Indexer Thread "+threadId+"] " +counter + " >> Last key : " + lastKey + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
    startTime = System.currentTimeMillis
  }
}
//A trait that will calculate the ranges to use for a mulit threads 
trait RangeCalculator{
  
  /**
   * For a give webservice URL, calculate a partitioning per thread
   */
  def calculateRanges(baseUrl:String, threads:Int, query:String="*:*",start:String="", end:String="") : Array[(String,String)] = {

    //http://biocache.ala.org.au/ws/occurrences/search?q=*:*&facet=off&sort=row_key&dir=asc
    
    val firstRequest = baseUrl + "/occurrences/search?q="+query+"&pageSize=1&facet=off&sort=row_key&dir=asc"
    println(baseUrl)
    val json  = JSON.parseFull(Source.fromURL(new URL(firstRequest)).mkString)
    if (!json.isEmpty){
      val totalRecords = json.get.asInstanceOf[Map[String, Object]].getOrElse("totalRecords", 0).asInstanceOf[Double].toInt
      println("Total records: " + totalRecords)

      val pageSize = totalRecords.toInt / threads

      var lastKey = start
      var buff = Array.fill(threads)(("",""))

      for (i <- 0 until threads){
        val json  = JSON.parseFull(Source.fromURL(new URL(baseUrl + "/occurrences/search?q="+query+"&pageSize=1&facet=off&sort=row_key&dir=asc&start=" + (i * pageSize))).mkString)
        val occurrences = json.get.asInstanceOf[Map[String, Object]].getOrElse("occurrences", List[Map[String, String]]()).asInstanceOf[List[Map[String, String]]]
        val rowKey:String = occurrences.first.getOrElse("rowKey", "")

        println("Retrieved rowkey: "  + rowKey)

        if (i > 0){
          buff(i-1) = (lastKey, rowKey)
        }
        //we want the first key to be ""
        if(i != 0)
            lastKey = rowKey
      }

      buff(buff.length - 1) = (lastKey, end)

      buff
    } else {
      Array()
    }
  }
  
  def generateRanges(keys:Array[String], start:String, end:String) :Array[(String,String)] = {
    val buff = new ArrayBuffer[(String,String)]
    var i =0
    while(i<keys.size){
      if(i==0)
        buff + ((start,keys(i)))
      else if (i==keys.size-1)
        buff + ((keys(i-1),end))
      else
        buff + ((keys(i-1), keys(i)))
      i+=1
    }
    buff.toArray[(String,String)]
  }
  
}

object RecordActionMultiThreaded extends Counter with RangeCalculator {

  def main(args:Array[String]){
    
    var wsBase = "http://biocache.ala.org.au/ws"
    var numThreads = 5
    var ranges:Array[(String,String)] = Array()
    var dirPrefix = "/data"
    var keys:Option[Array[String]]=None
    var action = ""
    var start,end =""
    var dr:Option[String]=None
    var validActions = List("range","process","index")
      
    val parser = new OptionParser("multithread index"){
       arg("<action>", "The action to perform by the Multithreader; either range, process or index", {v:String => action = v})
       intOpt("t","threads", "The number of threads to perform the indexing on",{v: Int => numThreads =v})
       opt("ws","wsBase","The base URL for the biocache ws to query for the ranges",{v: String => wsBase = v})
       opt("p","prefix","The prefix to apply to the solr dirctories",{v: String => dirPrefix =v})
       opt("k","keys","A comma separated list of keys on which to perform the range threads. Prevents the need to query SOLR for the ranges.",{v:String =>keys = Some(v.split(","))})
       opt("s", "start", "The rowKey in which to start the range",{v:String => start=v})
       opt("e","end", "The rowKey in which to end the range",{v:String => end=v})
       opt("dr","dr","The data resource over which to obtain the range",{v:String => dr = Some(v)})
    }
    if(parser.parse(args)){
      if(validActions.contains(action)){
        val (query, start, end) = if(dr.isDefined)("data_resource_uid:" + dr.get, dr.get+"|", dr.get+"|~") else ("*:*","","")
        
        ranges = if(keys.isEmpty) calculateRanges(wsBase, numThreads,query, start, end) else generateRanges(keys.get,start,end)
        if(action == "range")
          println(ranges.mkString("\n"))
        if(action != "range"){
          var counter = 0
          val threads = new ArrayBuffer[Thread]
          val solrDirs = new ArrayBuffer[String]
          solrDirs + (dirPrefix +"/solr/bio-proto-merged/data/index")
          ranges.foreach(r => {
            println("start: " + r._1 +", end key: " + r._2)
  //          val runner = new ProcessRecordsRunner(this, counter, r._1, r._2)
  //          val t = new Thread(runner)
  //          t.start()
  //          threads + t
      
            val ir ={ 
              if(action == "index"){
                solrDirs + (dirPrefix+"/solr-create/bio-proto-thread-"+counter +"/data/index")
                new IndexRunner(this, counter,  r._1,  r._2, dirPrefix+"/solr-template/bio-proto/conf", dirPrefix+"/solr-create/bio-proto-thread-"+counter+"/conf")
              }else{
                new ProcessRecordsRunner(this, counter, r._1, r._2)
              }
            }
            val t = new Thread(ir)
            t.start
            threads + t
            //solrDirs + (dirPrefix+"/solr-create/bio-proto-thread-"+counter +"/data/index")
            counter += 1
          })
          //wait for threads to complete and merge all indexes
          threads.foreach(thread =>
            thread.join
            )
            if(action == "index")
              IndexMergeTool.main(solrDirs.toArray)
        }
     }
    }
  }

  
}

class ProcessRecordsRunner(centralCounter:Counter, threadId:Int, startKey:String, endKey:String) extends Runnable {
  val processor = new RecordProcessor
  var ids = 0
  val threads = 2
  var batches=0  
  def run{
     val pageSize = 1000
     var counter=0
     val start = System.currentTimeMillis
     var startTime = System.currentTimeMillis
     var finishTime = System.currentTimeMillis
     //var buff = new ArrayBuffer[(FullRecord,FullRecord)]
     println("Starting thread " + threadId + " from " + startKey + " to " + endKey)
     Config.occurrenceDAO.pageOverRawProcessed(rawAndProcessed => {
       counter +=1
       if(!rawAndProcessed.get._1.deleted)  
         processor.processRecord(rawAndProcessed.get._1, rawAndProcessed.get._2)
       if (counter % pageSize == 0 && counter> 0) {
         centralCounter.addToCounter(pageSize)
         finishTime = System.currentTimeMillis
         centralCounter.printOutStatus(threadId, rawAndProcessed.get._1.rowKey)
         startTime = System.currentTimeMillis
       }
       true;
     },startKey, endKey, 1000)
     val fin = System.currentTimeMillis
     //
     println("[Processor Thread "+threadId+"] " +counter + " took " + ((fin-start).toFloat) / 1000f + " seconds" )
     

    println("Finished.")

  }
}

class IndexRunner (centralCounter:Counter, threadId:Int, startKey:String, endKey:String, sourceConfDirPath:String, targetConfDir:String) extends Runnable {

  def run {

    val newIndexDir = new File(targetConfDir)
    if (newIndexDir.exists) FileUtils.deleteDirectory(newIndexDir)
    FileUtils.forceMkdir(newIndexDir)

    //create a copy of SOLR home
    val sourceConfDir = new File(sourceConfDirPath)

    FileUtils.copyDirectory(sourceConfDir, newIndexDir)

    FileUtils.copyFileToDirectory(new File(sourceConfDir.getParent+"/solr.xml"), newIndexDir.getParentFile)

    val pageSize = 1000
    println("Set SOLR Home: " + newIndexDir.getParent)
    val indexer = new SolrIndexDAO(newIndexDir.getParent,Config.excludeSensitiveValuesFor)
    indexer.solrConfigPath = newIndexDir.getAbsolutePath+"/solrconfig.xml"

    var counter = 0
    val start = System.currentTimeMillis
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis

    //page through and create and index for this range
    Config.persistenceManager.pageOverAll("occ", (guid, map) => {
        counter += 1
//        val fullMap = new HashMap[String, String]
//        fullMap ++= map
        ///convert EL and CL properties at this stage
//        fullMap ++= Json.toStringMap(map.getOrElse("el.p", "{}"))
//        fullMap ++= Json.toStringMap(map.getOrElse("cl.p", "{}"))
       // val mapToIndex = fullMap.toMap
        val commit = counter % 200000 == 0        
        indexer.indexFromMap(guid, map, commit=commit)
        if (counter % pageSize == 0 && counter> 0) {
          centralCounter.addToCounter(pageSize)
          finishTime = System.currentTimeMillis
          centralCounter.printOutStatus(threadId, guid)
          startTime = System.currentTimeMillis
        }
        
        true
    }, startKey, endKey, pageSize = pageSize)

    indexer.finaliseIndex(true,true)

    finishTime = System.currentTimeMillis
    println("Total indexing time " + ((finishTime-start).toFloat)/1000f + " seconds")
  }
}

//class IndexRunner (centralCounter:Counter, threadId:Int, startKey:String, endKey:String, sourceConfDirPath:String, targetConfDir:String) extends Runnable {
//
//  def run {
//
//    val newIndexDir = new File(targetConfDir)
//    if (newIndexDir.exists) FileUtils.deleteDirectory(newIndexDir)
//    FileUtils.forceMkdir(newIndexDir)
//
//    //create a copy of SOLR home
//    val sourceConfDir = new File(sourceConfDirPath)
//
//    FileUtils.copyDirectory(sourceConfDir, newIndexDir)
//
//    FileUtils.copyFileToDirectory(new File(sourceConfDir.getParent+"/solr.xml"), newIndexDir.getParentFile)
//
//    val pageSize = 1000
//    println("Set SOLR Home: " + newIndexDir.getParent)
//    val indexer = new SolrIndexDAO(newIndexDir.getParent, newIndexDir.getAbsolutePath+"/solrconfig.xml")
//    var counter = 0
//    val start = System.currentTimeMillis
//    var startTime = System.currentTimeMillis
//    var finishTime = System.currentTimeMillis
//
//    //page through and create and index for this range
//    Config.persistenceManager.pageOverAll("occ", (guid, map) => {
//        counter += 1
//        val fullMap = new HashMap[String, String]
//        fullMap ++= map
//        ///convert EL and CL properties at this stage
//        fullMap ++= Json.toStringMap(map.getOrElse("el.p", "{}"))
//        fullMap ++= Json.toStringMap(map.getOrElse("cl.p", "{}"))
//        val mapToIndex = fullMap.toMap
//
//        indexer.indexFromMap(guid, mapToIndex)
//        if (counter % pageSize == 0 && counter> 0) {
//          centralCounter.addToCounter(pageSize)
//          finishTime = System.currentTimeMillis
//          centralCounter.printOutStatus(threadId, guid)
//          startTime = System.currentTimeMillis
//        }
//        true
//    }, startKey, endKey, pageSize = pageSize)
//
//    indexer.finaliseIndex(true,true)
//
//    finishTime = System.currentTimeMillis
//    println("Total indexing time " + ((finishTime-start).toFloat)/1000f + " seconds")
//  }
//}