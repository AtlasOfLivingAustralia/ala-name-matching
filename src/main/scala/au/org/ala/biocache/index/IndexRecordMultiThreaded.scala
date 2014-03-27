package au.org.ala.biocache.index

import au.org.ala.biocache._
import org.apache.commons.io.FileUtils
import java.io.File
import org.apache.lucene.misc.IndexMergeTool
import scala.io.Source
import java.net.URL
import scala.util.parsing.json.JSON
import java.lang.Thread
import scala.collection.mutable.ArrayBuffer
import java.io.FileWriter
import au.com.bytecode.opencsv.CSVWriter
import scala.collection.mutable.HashSet
import org.apache.commons.lang3.StringUtils
import au.org.ala.biocache.processor.{Processors, LocationProcessor}
import au.org.ala.biocache.load.FullRecordMapper
import au.org.ala.biocache.vocab.{ErrorCode, AssertionCodes}
import au.org.ala.biocache.util.{Json, OptionParser}
import au.org.ala.biocache.model.QualityAssertion
import au.org.ala.biocache.tool.RecordProcessor

object CreateIndexesAndMerge extends Counter {

  def main(args: Array[String]) {
    createIndexesAndMerge()
  }

  def createIndexesAndMerge() {
    IndexMergeTool.main(Array("/data/solr/bio-proto-merged/data/index",
      "/data/solr-create/bio-proto-thread-0/data/index",
      "/data/solr-create/bio-proto-thread-1/data/index",
      "/data/solr-create/bio-proto-thread-2/data/index"))
  }
}

trait Counter {
  var counter = 0

  def addToCounter(amount: Int) = counter += amount

  var startTime = System.currentTimeMillis
  var finishTime = System.currentTimeMillis

  def printOutStatus(threadId: Int, lastKey: String, runnerType: String) = {
    finishTime = System.currentTimeMillis
    println("[" + runnerType + " Thread " + threadId + "] " + counter + " >> Last key : " + lastKey + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
    startTime = System.currentTimeMillis
  }
}

/**
 * A trait that will calculate the ranges to use for a mulit threads
 */
trait RangeCalculator {

  /**
   * For a give webservice URL, calculate a partitioning per thread
   */
  def calculateRanges(threads: Int, query: String = "*:*", start: String = "", end: String = ""): Array[(String, String)] = {

    val firstRequest = Config.biocacheServiceUrl + "/occurrences/search?q=" + query + "&pageSize=1&facet=off&sort=row_key&dir=asc"
    val json = JSON.parseFull(Source.fromURL(new URL(firstRequest)).mkString)
    if (!json.isEmpty) {
      val totalRecords = json.get.asInstanceOf[Map[String, Object]].getOrElse("totalRecords", 0).asInstanceOf[Double].toInt
      println("Total records: " + totalRecords)

      val pageSize = totalRecords.toInt / threads

      var lastKey = start
      var buff = Array.fill(threads)(("", ""))

      for (i <- 0 until threads) {
        val json = JSON.parseFull(Source.fromURL(
          new URL(Config.biocacheServiceUrl + "/occurrences/search?q=" + query + "&facets=row_key&pageSize=0&flimit=1&fsort=index&foffset=" + (i * pageSize))).mkString)
        val facetResults = json.get.asInstanceOf[Map[String, Object]]
          .getOrElse("facetResults", List[Map[String, Object]]())
          .asInstanceOf[List[Map[String, Object]]]

        val rowKey = facetResults.head.get("fieldResult").get.asInstanceOf[List[Map[String, String]]].head.getOrElse("label", "")
        println("Retrieved row key: " + rowKey)

        if (i > 0) {
          buff(i - 1) = (lastKey, rowKey)
        }
        //we want the first key to be ""
        if (i != 0)
          lastKey = rowKey
      }

      buff(buff.length - 1) = (lastKey, end)

      buff
    } else {
      Array()
    }
  }

  def generateRanges(keys: Array[String], start: String, end: String): Array[(String, String)] = {
    val buff = new ArrayBuffer[(String, String)]
    var i = 0
    while (i < keys.size) {
      if (i == 0)
        buff += ((start, keys(i)))
      else if (i == keys.size - 1)
        buff += ((keys(i - 1), end))
      else
        buff += ((keys(i - 1), keys(i)))
      i += 1
    }
    buff.toArray[(String, String)]
  }

}

object RecordActionMultiThreaded extends Counter with RangeCalculator {

  def main(args: Array[String]) {

    var numThreads = 8
    var pageSize = 200
    var ranges: Array[(String, String)] = Array()
    var dirPrefix = "/data"
    var keys: Option[Array[String]] = None
    var columns: Option[Array[String]] = None
    var action = ""
    var start, end = ""
    var dr: Option[String] = None
    var validActions = List("range", "process", "index", "col", "repair", "datum")

    val parser = new OptionParser("multi-thread index") {
      arg("<action>", "The action to perform by the Multithreader; either range, process or index, col", {
        v: String => action = v
      })
      intOpt("t", "threads", "The number of threads to perform the indexing on", {
        v: Int => numThreads = v
      })
      intOpt("ps", "pagesize", "The pagesSize for the records", {
        v: Int => pageSize = v
      })
      opt("p", "prefix", "The prefix to apply to the solr directories", {
        v: String => dirPrefix = v
      })
      opt("k", "keys", "A comma separated list of keys on which to perform the range threads. Prevents the need to query SOLR for the ranges.", {
        v: String => keys = Some(v.split(","))
      })
      opt("s", "start", "The rowKey in which to start the range", {
        v: String => start = v
      })
      opt("e", "end", "The rowKey in which to end the range", {
        v: String => end = v
      })
      opt("dr", "dr", "The data resource over which to obtain the range", {
        v: String => dr = Some(v)
      })
      opt("c", "columns", "The columns to export", {
        v: String => columns = Some(v.split(","))
      })
    }
    if (parser.parse(args)) {
      if (validActions.contains(action)) {
        val (query, start, end) = if (dr.isDefined) ("data_resource_uid:" + dr.get, dr.get + "|", dr.get + "|~") else ("*:*", "", "")
        Config.persistenceManager.get("test", "occ", "blah")
        ranges = if (keys.isEmpty) calculateRanges(numThreads, query, start, end) else generateRanges(keys.get, start, end)
        if (action == "range")
          println(ranges.mkString("\n"))

        else if (action != "range") {
          var counter = 0
          val threads = new ArrayBuffer[Thread]
          val columnRunners = new ArrayBuffer[ColumnReporterRunner]
          val solrDirs = new ArrayBuffer[String]
          solrDirs += (dirPrefix + "/solr/bio-proto-merged/data/index")
          ranges.foreach(r => {
            println("start: " + r._1 + ", end key: " + r._2)

            val ir = {
              if (action == "datum") {
                new DatumRecordsRunner(this, counter, r._1, r._2)
              }
              else if (action == "repair") {
                new RepairRecordsRunner(this, counter, r._1, r._2)
              }
              else if (action == "index") {
                solrDirs += (dirPrefix + "/solr-create/bio-proto-thread-" + counter + "/data/index")
                new IndexRunner(this, counter, r._1, r._2, dirPrefix + "/solr-template/bio-proto/conf", dirPrefix + "/solr-create/bio-proto-thread-" + counter + "/conf", pageSize)
                //new IndexRunner(this, counter,  r._1,  r._2, dirPrefix+"/solr-template/bio-proto/biocache/conf", dirPrefix+"/solr-create/bio-proto-thread-"+counter+"/biocache/conf", pageSize)
              } else if (action == "process") {
                new ProcessRecordsRunner(this, counter, r._1, r._2)
              } else if (action == "col") {
                if (columns.isEmpty)
                  new ColumnReporterRunner(this, counter, r._1, r._2)
                else {
                  new ColumnExporter(this, counter, r._1, r._2, columns.get.toList)
                }
              } else
                new Thread()
            }
            val t = new Thread(ir)
            t.start
            threads += t
            if (ir.isInstanceOf[ColumnReporterRunner]) {
              columnRunners += ir.asInstanceOf[ColumnReporterRunner]
            }
            //solrDirs + (dirPrefix+"/solr-create/bio-proto-thread-"+counter +"/data/index")
            counter += 1
          })

          //wait for threads to complete and merge all indexes
          threads.foreach(thread =>
            thread.join
          )
          if (action == "index") {
            IndexMergeTool.main(solrDirs.toArray)
            Config.persistenceManager.shutdown
            println("Waiting to see if shutdown")
            System.exit(0)
          } else if (action == "col") {
            var allSet: Set[String] = Set()
            columnRunners.foreach(c => allSet ++= c.myset)
            allSet = allSet.filterNot(it => it.endsWith(".p") || it.endsWith(".qa"))
            println(allSet)
          }
        }
      }
    }
  }
}

class ColumnExporter(centralCounter: Counter, threadId: Int, startKey: String, endKey: String, columns: List[String]) extends Runnable {

  def run {

    val outWriter = new FileWriter(new File("/data/tmp/fullexport" + threadId + ".txt"))
    val writer = new CSVWriter(outWriter, '\t', '"', '\\')
    writer.writeNext(Array("rowKey") ++ columns.toArray[String])
    val start = System.currentTimeMillis
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis
    var counter = 0
    val pageSize = 10000
    Config.persistenceManager.pageOverSelect("occ", (key, map) => {
      counter += 1
      exportRecord(writer, columns, key, map)
      if (counter % pageSize == 0 && counter > 0) {
        centralCounter.addToCounter(pageSize)
        finishTime = System.currentTimeMillis
        centralCounter.printOutStatus(threadId, key, "Column Reporter")
        startTime = System.currentTimeMillis
      }
      true
    }, startKey, endKey, 1000, columns: _*)

    val fin = System.currentTimeMillis
    println("[Exporter Thread " + threadId + "] " + counter + " took " + ((fin - start).toFloat) / 1000f + " seconds")
  }

  def exportRecord(writer: CSVWriter, fieldsToExport: List[String], guid: String, map: Map[String, String]) {
    val line = Array(guid) ++ (for (field <- fieldsToExport) yield map.getOrElse(field, ""))
    writer.writeNext(line)
  }
}

class ColumnReporterRunner(centralCounter: Counter, threadId: Int, startKey: String, endKey: String) extends Runnable {

  val myset = new HashSet[String]

  def run {
    println("[THREAD " + threadId + "] " + startKey + " TO " + endKey)
    val start = System.currentTimeMillis
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis
    var counter = 0
    val pageSize = 10000
    Config.persistenceManager.pageOverAll("occ", (guid, map) => {
      myset ++= map.keySet
      counter += 1
      if (counter % pageSize == 0 && counter > 0) {
        centralCounter.addToCounter(pageSize)
        finishTime = System.currentTimeMillis
        centralCounter.printOutStatus(threadId, guid, "Column Reporter")
        startTime = System.currentTimeMillis
      }
      true
    }, startKey, endKey, 1000)
    val fin = System.currentTimeMillis
    println("[Thread " + threadId + "] " + counter + " took " + ((fin - start).toFloat) / 1000f + " seconds")
    println("[THREAD " + threadId + "] " + myset)
  }
}

class RepairRecordsRunner(centralCounter: Counter, threadId: Int, startKey: String, endKey: String) extends Runnable {

  var counter = 0

  def run {
    val pageSize = 1000
    var counter = 0
    val start = System.currentTimeMillis
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis
    println("Starting to repair from " + startKey + " to " + endKey)
    Config.persistenceManager.pageOverSelect("occ", (guid, map) => {
      counter += 1

      val dstatus = map.getOrElse("duplicationStatus.p", "")
      if (dstatus.equals("D")) {
        val qa = Config.occurrenceDAO.getSystemAssertions(guid).find(_.getCode == AssertionCodes.INFERRED_DUPLICATE_RECORD.code)
        if (qa.isEmpty) {
          //need to add the QA
          Config.occurrenceDAO.addSystemAssertion(guid, QualityAssertion(AssertionCodes.INFERRED_DUPLICATE_RECORD, "Record has been inferred as closely related to  " + map.getOrElse("associatedOccurrences.p", "")), false, false)
          println("REINDEX:::" + guid)
        }
      }
      if (counter % pageSize == 0 && counter > 0) {
        centralCounter.addToCounter(pageSize)
        finishTime = System.currentTimeMillis
        centralCounter.printOutStatus(threadId, guid, "Repairer")
        startTime = System.currentTimeMillis
      }
      true
    }, startKey, endKey, pageSize, "qualityAssertion", "rowKey", "uuid", "duplicationStatus.p", "associatedOccurrences.p")
  }

  val qaphases = Array("loc.qa", "offline.qa", "class.qa", "bor.qa", "type.qa", "attr.qa", "image.qa", "event.qa")

  def sortOutQas(guid: String, list: List[QualityAssertion]): (String, String) = {
    val failed: Map[String, List[Int]] = list.filter(_.qaStatus == 0).map(_.code).groupBy(qa => Processors.getProcessorForError(qa) + ".qa")
    val gk = AssertionCodes.isGeospatiallyKosher(failed.getOrElse("loc.qa", List()).toArray).toString
    val tk = AssertionCodes.isTaxonomicallyKosher(failed.getOrElse("class.qa", List()).toArray).toString

    val empty = qaphases.filterNot(p => failed.contains(p)).map(_ -> "[]")
    val map = Map("geospatiallyKosher" -> gk, "taxonomicallyKosher" -> tk) ++ failed.filterNot(_._1 == ".qa").map {
      case (key, value) => {
        (key, Json.toJSON(value.toArray))
      }
    } ++ empty
    //revise the properties in the db
    Config.persistenceManager.put(guid, "occ", map)

    //check to see if there is a tool QA and remove one
    val dupQA = list.filter(_.code == AssertionCodes.INFERRED_DUPLICATE_RECORD.code)
    //dupQA.foreach(qa => println(qa.getComment))
    if (dupQA.size > 1) {
      val newList: List[QualityAssertion] = list.diff(dupQA) ++ List(dupQA(0))
      //println("Original size " + list.length + "  new size =" + newList.length)
      Config.persistenceManager.putList(guid, "occ", FullRecordMapper.qualityAssertionColumn, newList, classOf[QualityAssertion], true)
    }

    //println("FAILED: " + failed)
    //println("The map to add " + map)
    (gk, tk)
  }
}


class DatumRecordsRunner(centralCounter: Counter, threadId: Int, startKey: String, endKey: String) extends Runnable {
  val processor = new RecordProcessor
  var ids = 0
  val threads = 2
  var batches = 0

  def run {
    val pageSize = 1000
    var counter = 0
    var numIssue = 0
    val start = System.currentTimeMillis
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis
    //var buff = new ArrayBuffer[(FullRecord,FullRecord)]
    println("Starting thread " + threadId + " from " + startKey + " to " + endKey)
    def locProcess = new LocationProcessor
    Config.persistenceManager.pageOverSelect("occ", (guid, map) => {
      counter += 1


      if (StringUtils.isNotBlank(map.getOrElse("geodeticDatum", ""))) {
        //check the precision of the lat/lon
        def lat = map.getOrElse("decimalLatitude", "0")
        def lon = map.getOrElse("decimalLongitude", "0")
        def locqa = Json.toIntArray(map.getOrElse("loc.qa", "[]"))
        if (locProcess.getNumberOfDecimalPlacesInDouble(lat) != locProcess.getNumberOfDecimalPlacesInDouble(lon) && locqa.contains(45)) {
          numIssue += 1
          println("FIXME from THREAD " + threadId + "\t" + guid)
        }
      }

      if (counter % pageSize == 0 && counter > 0) {
        centralCounter.addToCounter(pageSize)
        finishTime = System.currentTimeMillis
        centralCounter.printOutStatus(threadId, guid, "Datum")
        startTime = System.currentTimeMillis
      }
      true;
    }, startKey, endKey, 1000, "decimalLatitude", "decimalLongitude", "rowKey", "uuid", "geodeticDatum", "loc.qa")
    val fin = System.currentTimeMillis
    println("[Datum Thread " + threadId + "] " + counter + " took " + ((fin - start).toFloat) / 1000f + " seconds")
    println("Finished.")
  }
}


class ProcessRecordsRunner(centralCounter: Counter, threadId: Int, startKey: String, endKey: String) extends Runnable {

  val processor = new RecordProcessor
  var ids = 0
  val threads = 2
  var batches = 0

  def run {
    val pageSize = 1000
    var counter = 0
    val start = System.currentTimeMillis
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis
    //var buff = new ArrayBuffer[(FullRecord,FullRecord)]
    println("Starting thread " + threadId + " from " + startKey + " to " + endKey)
    Config.occurrenceDAO.pageOverRawProcessed(rawAndProcessed => {
      counter += 1
      if (!rawAndProcessed.get._1.deleted)
        processor.processRecord(rawAndProcessed.get._1, rawAndProcessed.get._2)
      if (counter % pageSize == 0 && counter > 0) {
        centralCounter.addToCounter(pageSize)
        finishTime = System.currentTimeMillis
        centralCounter.printOutStatus(threadId, rawAndProcessed.get._1.rowKey, "Processor")
        startTime = System.currentTimeMillis
      }
      true
    }, startKey, endKey, 1000)
    val fin = System.currentTimeMillis
    println("[Processor Thread " + threadId + "] " + counter + " took " + ((fin - start).toFloat) / 1000f + " seconds")
    println("Finished.")
  }
}

class IndexRunner(centralCounter: Counter, threadId: Int, startKey: String, endKey: String, sourceConfDirPath: String, targetConfDir: String, pageSize: Int = 200) extends Runnable {

  def run {

    val newIndexDir = new File(targetConfDir)
    if (newIndexDir.exists) FileUtils.deleteDirectory(newIndexDir)
    FileUtils.forceMkdir(newIndexDir)

    //create a copy of SOLR home
    val sourceConfDir = new File(sourceConfDirPath)

    FileUtils.copyDirectory(sourceConfDir, newIndexDir)

    FileUtils.copyFileToDirectory(new File(sourceConfDir.getParent + "/solr.xml"), newIndexDir.getParentFile)
    //FileUtils.copyFileToDirectory(new File(sourceConfDir.getParentFile.getParent+"/solr.xml"), newIndexDir.getParentFile.getParentFile)

    //val pageSize = 1000
    println("Set SOLR Home: " + newIndexDir.getParent)
    val indexer = new SolrIndexDAO(newIndexDir.getParent, Config.excludeSensitiveValuesFor, Config.extraMiscFields)
    indexer.solrConfigPath = newIndexDir.getAbsolutePath + "/solrconfig.xml"

    var counter = 0
    val start = System.currentTimeMillis
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis
    var check = true
    //page through and create and index for this range
    Config.persistenceManager.pageOverAll("occ", (guid, map) => {
      counter += 1

      val commit = counter % 10000 == 0
      //ignore the record if it has the guid that is the startKey this is because it will be indexed last by the previous thread.
      if (check) {
        check = false
        if (!guid.equals(startKey)) {
          indexer.indexFromMap(guid, map, commit = commit)
        }
      } else {
        indexer.indexFromMap(guid, map, commit = commit)
      }


      if (counter % pageSize == 0 && counter > 0) {
        centralCounter.addToCounter(pageSize)
        finishTime = System.currentTimeMillis
        centralCounter.printOutStatus(threadId, guid, "Indexer")
        startTime = System.currentTimeMillis
      }

      true
    }, startKey, endKey, pageSize = pageSize)

    indexer.finaliseIndex(true, true)

    finishTime = System.currentTimeMillis
    println("Total indexing time " + ((finishTime - start).toFloat) / 1000f + " seconds")
  }
}
