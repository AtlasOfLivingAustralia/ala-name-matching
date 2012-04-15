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

object IndexRecordMultiThreaded extends Counter {

  def main(args:Array[String]){
    
    var wsBase = "http://biocache.ala.org.au/ws"
    var numThreads = 5
    var ranges:Array[(String,String)] = Array()
    var dirPrefix = "/data"
      
    val parser = new OptionParser("multithread index"){
       intOpt("t","threads", "The number of threads to perform the indexing on",{v: Int => numThreads =v})
       opt("ws","wsBase","The base URL for the biocache ws to query for the ranges",{v: String => wsBase = v})
       opt("p","prefix","The prefix to apply to the solr dirctories",{v: String => dirPrefix =v})       
    }
    if(parser.parse(args)){
    
        ranges = calculateRanges(wsBase, numThreads)
        var counter = 0
        val threads = new ArrayBuffer[Thread]
        val solrDirs = new ArrayBuffer[String]
        solrDirs + (dirPrefix +"/solr/bio-proto-merged/data/index")
        ranges.foreach(r => {
          println("start: " + r._1 +", end key: " + r._2)
    
          val ir = new IndexRunner(this, counter,  r._1,  r._2, dirPrefix+"/solr-template/bio-proto/conf", dirPrefix+"/solr-create/bio-proto-thread-"+counter+"/conf")
          val t = new Thread(ir)
          t.start
          threads + t
          solrDirs + (dirPrefix+"/solr-create/bio-proto-thread-"+counter +"/data/index")
          counter += 1
        })
        //wait for threads to complete and merge all indexes
        threads.foreach(thread =>
          thread.join
          )
          IndexMergeTool.main(solrDirs.toArray)
     }
  }

  /**
   * For a give webservice URL, calculate a partitioning per thread
   */
  def calculateRanges(baseUrl:String, threads:Int) : Array[(String,String)] = {

    //http://biocache.ala.org.au/ws/occurrences/search?q=*:*&facet=off&sort=row_key&dir=asc

    val firstRequest = baseUrl + "/occurrences/search?q=*:*&pageSize=1&facet=off&sort=row_key&dir=asc"
    val json  = JSON.parseFull(Source.fromURL(new URL(firstRequest)).mkString)
    if (!json.isEmpty){
      val totalRecords = json.get.asInstanceOf[Map[String, Object]].getOrElse("totalRecords", 0).asInstanceOf[Double].toInt
      println("Total records: " + totalRecords)

      val pageSize = totalRecords.toInt / threads

      var lastKey = ""
      var buff = Array.fill(threads)(("",""))

      for (i <- 0 until threads){
        val json  = JSON.parseFull(Source.fromURL(new URL(baseUrl + "/occurrences/search?q=*:*&pageSize=1&facet=off&sort=row_key&dir=asc&start=" + (i * pageSize))).mkString)
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

      buff(buff.length - 1) = (lastKey, "")

      buff
    } else {
      Array()
    }
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
    val indexer = new SolrIndexDAO(newIndexDir.getParent)
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

        indexer.indexFromMap(guid, map)
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