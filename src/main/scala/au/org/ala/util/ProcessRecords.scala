package au.org.ala.util

import collection.mutable.ArrayBuffer
import org.slf4j.LoggerFactory
import au.org.ala.biocache._

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

  /**
   * Run record processing.
   */
  def main(args: Array[String]): Unit = {
    //logger.info("Starting processing records....")
    val p = new RecordProcessor
    p.processAll
    //p.processSelect("BOR", "ATTR")
    //logger.info("Finished. Shutting down.")
    Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager].shutdown
  }
}

class RecordProcessor {

  val logger = LoggerFactory.getLogger(classOf[RecordProcessor])

  var workflow = Array(ClassificationProcessor,LocationProcessor,EventProcessor,BasisOfRecordProcessor,
        TypeStatusProcessor,AttributionProcessor,ImageProcessor)

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

//  def processSelect(workflowNames:String*){
//    var counter = 0
//    var startTime = System.currentTimeMillis
//    var finishTime = System.currentTimeMillis
//    //Get the processors that perform the processing of the supplied workflow names
//    workflow = workflowNames.map(( s: String ) => Processors.processorMap.getOrElse(s,null) ).toArray;
//    println("Processing the workflows: " + workflow)
//    Config.occurrenceDAO.pageOverAll(Raw,record => {
//      counter += 1
//      if (!record.isEmpty) {
//        val raw = record.get
//        processRecord(raw)
//
//        //debug counter
//        if (counter % 1000 == 0) {
//          finishTime = System.currentTimeMillis
//          logger.info(counter + " >> Last key : " + raw.uuid + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
//          startTime = System.currentTimeMillis
//        }
//      }
//      true
//    })
//
//  }

  /**
   * Process a record, adding metadata and records quality systemAssertions.
   * This version passes the original to optimise updates.
   */
  def processRecord(raw:FullRecord, currentProcessed:FullRecord){

    val guid = raw.rowKey
    val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
    //NC: Changed so that a processed record only contains values that have been processed.
    var processed = new FullRecord
    //var assertions = new ArrayBuffer[QualityAssertion]
    var assertions = new scala.collection.mutable.HashMap[String, Array[QualityAssertion]]

    //run each processor in the specified order
    workflow.foreach(processor => { 
        assertions += ( processor.getName -> processor.process(guid, raw, processed))
      })

    val systemAssertions = Some(assertions.toMap)
    //store the occurrence
    occurrenceDAO.updateOccurrence(guid, currentProcessed, processed, systemAssertions, Processed)
  }

  /**
   * Process a record, adding metadata and records quality systemAssertions
   */
  def processRecord(raw:FullRecord){

    val guid = raw.rowKey
    val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
    //NC: Changed so that a processed record only contains values that have been processed.
    var processed = new FullRecord//raw.clone
    //var assertions = new ArrayBuffer[QualityAssertion]
    var assertions = new scala.collection.mutable.HashMap[String, Array[QualityAssertion]]

    workflow.foreach(processor => {
        assertions += (processor.getName()->processor.process(guid, raw, processed))
    })

    val systemAssertions = Some(assertions.toMap)
  
    //store the occurrence
    occurrenceDAO.updateOccurrence(guid, processed, systemAssertions, Processed)
  }
}