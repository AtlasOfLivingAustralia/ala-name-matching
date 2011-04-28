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

  val logger = LoggerFactory.getLogger("ProcessRecords")

  val workflow = Array(ClassificationProcessor,LocationProcessor,EventProcessor,BasisOfRecordProcessor,
      TypeStatusProcessor,AttributionProcessor,ImageProcessor)

  /**
   * Run record processing.
   */
  def main(args: Array[String]): Unit = {
    logger.info("Starting processing records....")
    processAll
    logger.info("Finished. Shutting down.")
    Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager].shutdown
  }

  /**
   * Process all records in the store
   */
  def processAll {
    var counter = 0
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis

    val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]

    //page over all records and process
    occurrenceDAO.pageOverAll(Raw, record => {
      counter += 1
      if (!record.isEmpty) {
        val raw = record.get
        processRecord(raw)

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
   * Process a record, adding metadata and records quality systemAssertions
   */
  def processRecord(raw:FullRecord){

    val guid = raw.uuid
    val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
    //NC: Changed so that a processed record only contains values that have been processed.
    var processed = new FullRecord//raw.clone
    var assertions = new ArrayBuffer[QualityAssertion]

    workflow.foreach(processor => {
        assertions ++= processor.process(guid, raw, processed)
    })

    val systemAssertions = Some(assertions.toArray)
  
    //store the occurrence
    occurrenceDAO.updateOccurrence(guid, processed, systemAssertions, Processed)
  }
}