package au.org.ala.util

import org.slf4j.LoggerFactory
import au.org.ala.biocache._
import org.apache.commons.lang.StringUtils
import java.util.UUID
import java.io.FileReader
import au.com.bytecode.opencsv.CSVReader
import java.util.concurrent.ArrayBlockingQueue

/**
 * 1. Classification matching
 * 	- include a flag to indicate record hasnt been matched to NSLs
 * 
 * 2. Parse locality information
 * 	- "Vic" -> Victoria
 * 
 * 3. Point matching
 * 	- parse latitude/longitude
 * 	- retrieve associated point mapping
 * 	- check state supplied to state point lies in
 * 	- marine/non-marine/limnetic (need a webservice from BIE)
 * 
 * 4. Type status normalization
 * 	- use GBIF's vocabulary
 * 
 * 5. Date parsing
 * 	- date validation
 * 	- support for date ranges
 * 
 * 6. Collectory lookups for attribution chain
 * 
 * Tests to conform to: http://bit.ly/eqSiFs
 */
object ProcessRecords {

  val logger = LoggerFactory.getLogger("ProcessRecords")

  /** Run record processing. */
  def main(args: Array[String]): Unit = {

    var fileName: String = ""
    var threads =1
    val parser = new OptionParser("index records options") {
      opt("f", "fileName", "The record to start processing with", { v: String => fileName = v })
      intOpt("t","threads" ," The number of concurrent threads to perform the FILE processing on.",{v:Int => threads=v})
    }

    val p = new RecordProcessor
    if (parser.parse(args)) {
      if (fileName != "") {
        if(threads ==1)
          p.processFileOfRowKeys(fileName)
        else
          p.processFileThreaded(new java.io.File(fileName), threads)
      } else {
        p.processAll
      }
    }
    logger.info("Finished. Shutting down.")
    Config.persistenceManager.shutdown
  }
}

class RecordProcessor {
  import FileHelper._

  val logger = LoggerFactory.getLogger(classOf[RecordProcessor])
  //The time that the processing started - used to populate lastProcessed
  val processTime = org.apache.commons.lang.time.DateFormatUtils.format(new java.util.Date, "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val duplicates = List("D","D1","D2")
  /**
   * Processes a list of records
   */
  def processRecords(rowKeys:List[String]){
    logger.debug("Starting to process all the records in the list: " + rowKeys)
    var counter = 0
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis
    rowKeys.foreach(rowKey =>{
      val rawProcessed = Config.occurrenceDAO.getRawProcessedByRowKey(rowKey)
      if (!rawProcessed.isEmpty){
        val rp = rawProcessed.get
        processRecord(rp(0), rp(1))

        //debug counter
        if (counter % 100 == 0) {
          finishTime = System.currentTimeMillis
          logger.debug(counter + " >> Last key : " + rp(0).uuid + ", records per sec: " + 100f / (((finishTime - startTime).toFloat) / 1000f))
          startTime = System.currentTimeMillis
        }
      }
    })
  }
  
  /**
   * Process all records in the store
   */
  def processFileOfRowKeys(fileName:String) {
    logger.info("Starting processing from file......" + fileName)
    var counter = 0
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis

    val csvReader = new CSVReader(new FileReader(fileName))
    var current = csvReader.readNext()

    //page over all records and process
    //occurrenceDAO.pageOverAll(Raw, record => {
    while(current != null){
      counter += 1

      val rawProcessed = Config.occurrenceDAO.getRawProcessedByRowKey(current(0))
      if (!rawProcessed.isEmpty){
        val rp = rawProcessed.get
        processRecord(rp(0), rp(1))

        //debug counter
        if (counter % 1000 == 0) {
          finishTime = System.currentTimeMillis
          println(counter + " >> Last key : " + rp(0).uuid + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
          startTime = System.currentTimeMillis
        }
      }
      current = csvReader.readNext()
    }
    logger.info("Finished processing from file.")
  }
  
  def processFileThreaded(file:java.io.File, threads:Int){
    val queue = new ArrayBlockingQueue[String](100)
    var ids =0
     val pool:Array[StringConsumer] = Array.fill(threads){
     var counter=0 
     var startTime = System.currentTimeMillis
     var finishTime = System.currentTimeMillis
            
      val p = new StringConsumer(queue,ids,{guid =>
        counter +=1
        val rawProcessed = Config.occurrenceDAO.getRawProcessedByRowKey(guid)
        if (!rawProcessed.isEmpty){
        val rp = rawProcessed.get
        processRecord(rp(0), rp(1))
        
        //debug counter
        if (counter % 1000 == 0) {
          finishTime = System.currentTimeMillis
          logger.info(counter + " >> Last key : " + rp(0).uuid + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
          startTime = System.currentTimeMillis
        }
      }
      });ids +=1;p.start;p }
    
    file.foreachLine(line =>{
      //add to the queue
      queue.put(line.trim)
    }) 
    pool.foreach(t =>t.shouldStop = true)
    pool.foreach(_.join)    
  }

  /**
   * Process all records in the store
   */
  def processAll {
    var counter = 0
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis

    //page over all records and process
    //occurrenceDAO.pageOverAll(Raw, record => {
    Config.occurrenceDAO.pageOverRawProcessed(record => {
      counter += 1
      if (!record.isEmpty) {
        
        val (raw,processed) = record.get
        //println(raw.rowKey)
        processRecord(raw, processed)

        //debug counter
        if (counter % 1000 == 0) {
          finishTime = System.currentTimeMillis
          logger.info(counter + " >> Last key : " + raw.uuid + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
          startTime = System.currentTimeMillis
        }
      }
      true
    })
  }

  /**
   * Process a record, adding metadata and records quality systemAssertions.
   * This version passes the original to optimise updates.
   */
  def processRecord(raw:FullRecord, currentProcessed:FullRecord){

    val guid = raw.rowKey
    val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
    //NC: Changed so that a processed record only contains values that have been processed.
    var processed = raw.createNewProcessedRecord
    //var assertions = new ArrayBuffer[QualityAssertion]
    var assertions = new scala.collection.mutable.HashMap[String, Array[QualityAssertion]]

    //run each processor in the specified order
    Processors.foreach(processor => {
      assertions += ( processor.getName -> processor.process(guid, raw, processed, Some(currentProcessed)))
    })
    
   
    //mark the processed time
    processed.lastModifiedTime = processTime
    //put the properties that are not updated during processing
    processed.occurrence.outlierForLayers = currentProcessed.occurrence.outlierForLayers
    processed.occurrence.duplicationStatus = currentProcessed.occurrence.duplicationStatus
    processed.occurrence.duplicationType = currentProcessed.occurrence.duplicationType
    processed.occurrence.associatedOccurrences = currentProcessed.occurrence.associatedOccurrences
    processed.location.distanceOutsideExpertRange = currentProcessed.location.distanceOutsideExpertRange
    //add the QA for the duplicate record
    if(duplicates.contains(processed.occurrence.duplicationStatus)){
      //need to add the QA
      assertions += ("duplicates" -> Array(QualityAssertion(AssertionCodes.INFERRED_DUPLICATE_RECORD,"Record has been inferred as closely related to  " + processed.occurrence.associatedOccurrences)))
    }
    
    //store the occurrence
    val systemAssertions = Some(assertions.toMap)
    occurrenceDAO.updateOccurrence(guid, currentProcessed, processed, systemAssertions, Processed)
  }

  /**
   * Process a record, adding metadata and records quality systemAssertions
   */
  def processRecord(raw:FullRecord) : (FullRecord, Map[String, Array[QualityAssertion]]) = {

    //NC: Changed so that a processed record only contains values that have been processed.
    var processed = raw.createNewProcessedRecord
    var assertions = new scala.collection.mutable.HashMap[String, Array[QualityAssertion]]

    Processors.foreach(processor => {
      assertions += (processor.getName -> processor.process(raw.rowKey, raw, processed))
    })
  
    //store the occurrence
    (processed, assertions.toMap)
  }
  
  /**
   * Process a record, adding metadata and records quality systemAssertions
   */
  def processRecordAndUpdate(raw:FullRecord){

    val (processed, assertions) = processRecord(raw)
    val systemAssertions = Some(assertions)
    //mark the processed time
    processed.asInstanceOf[FullRecord].lastModifiedTime = processTime
    //store the occurrence
    Config.occurrenceDAO.updateOccurrence(raw.rowKey, processed, systemAssertions, Processed)
    //updateRawIfSensitised(raw, processed, raw.rowKey)
  }

  def addRecordAndProcess(dataResourceUid:String, properties:Map[String,String]) : String = {
    val uuid = properties.getOrElse("uuid", UUID.randomUUID().toString)
    val rowKey = dataResourceUid + "|" + uuid
    val raw = FullRecordMapper.createFullRecord(rowKey, properties,Versions.RAW)
    raw.uuid = uuid
    raw.attribution.dataResourceUid = dataResourceUid
    au.org.ala.biocache.Config.occurrenceDAO.updateOccurrence(raw.rowKey, raw, Versions.RAW)
    val processor = new RecordProcessor
    processor.processRecordAndUpdate(raw)
    uuid
  }

  def addRecord(dataResourceUid:String, properties:Map[String,String]) : String = {
    val uuid = properties.getOrElse("uuid", UUID.randomUUID().toString)
    val rowKey = dataResourceUid + "|" + uuid
    val raw = FullRecordMapper.createFullRecord(rowKey, properties,Versions.RAW)
    raw.uuid = uuid
    raw.attribution.dataResourceUid = dataResourceUid
    au.org.ala.biocache.Config.occurrenceDAO.updateOccurrence(raw.rowKey, raw, Versions.RAW)
    uuid
  }
}
