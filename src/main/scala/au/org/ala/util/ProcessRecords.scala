package au.org.ala.util

import org.slf4j.LoggerFactory
import au.org.ala.biocache._
import org.apache.commons.lang.StringUtils

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
    logger.info("Starting processing records....")
    val p = new RecordProcessor
    p.processAll
    //p.processSelect("BOR", "ATTR")
    logger.info("Finished. Shutting down.")
    Config.persistenceManager.shutdown
  }
}

class RecordProcessor {

  val logger = LoggerFactory.getLogger(classOf[RecordProcessor])
  //The time that the processing started - used to populate lastProcessed
  val processTime = org.apache.commons.lang.time.DateFormatUtils.format(new java.util.Date, "yyyy-MM-dd'T'HH:mm:ss'Z'")

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
    var processed = new FullRecord(raw.rowKey, raw.uuid)
    //var assertions = new ArrayBuffer[QualityAssertion]
    var assertions = new scala.collection.mutable.HashMap[String, Array[QualityAssertion]]

    //run each processor in the specified order
    Processors.foreach(processor => {
      assertions += ( processor.getName -> processor.process(guid, raw, processed))
    })
    
    val systemAssertions = Some(assertions.toMap)
    //mark the processed time
    processed.lastModifiedTime = processTime
    //store the occurrence
    occurrenceDAO.updateOccurrence(guid, currentProcessed, processed, systemAssertions, Processed)
    //update raw if necessary
    updateRawIfSensitised(raw,processed, guid)
  }

  def updateRawIfSensitised(raw: FullRecord, processed: FullRecord, guid: String) {
    val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
    if (raw.location.originalDecimalLatitude != null && processed.occurrence.dataGeneralizations == null
      && processed.occurrence.informationWithheld == null) {
      val location = new Location
      location.decimalLatitude = raw.location.decimalLatitude
      location.decimalLongitude = raw.location.decimalLongitude
      location.locationRemarks = raw.location.originalLocationRemarks
      occurrenceDAO.updateOccurrence(guid, location, Versions.RAW)
      //remove the existing originalDecimal coordinates
      Config.persistenceManager.deleteColumns(guid, "occ", "originalDecimalLatitude", "originalDecimalLongitude");
    } else if (raw.location.originalDecimalLatitude != null && raw.location.originalDecimalLongitude != null) {
      val location = new Location
      location.originalDecimalLatitude = raw.location.originalDecimalLatitude
      location.originalDecimalLongitude = raw.location.originalDecimalLongitude
      location.decimalLatitude = raw.location.decimalLatitude
      location.decimalLongitude = raw.location.decimalLongitude
      location.originalLocationRemarks = raw.location.originalLocationRemarks
      occurrenceDAO.updateOccurrence(guid, location, Versions.RAW)
      //remove the location remarks all the time
      Config.persistenceManager.deleteColumns(guid,"occ", "locationRemarks")
      //remove the decimal coordinates if there are no processed coordinates (indicates informationWithheld)
      if (StringUtils.isEmpty(processed.location.decimalLatitude) && StringUtils.isEmpty(processed.location.decimalLongitude)) {
        Config.persistenceManager.deleteColumns(guid, "occ", "decimalLatitude", "decimalLongitude")
      }
    }
  }

  /**
   * Process a record, adding metadata and records quality systemAssertions
   */
  def processRecord(raw:FullRecord) : (FullRecord, Map[String, Array[QualityAssertion]]) = {

    //NC: Changed so that a processed record only contains values that have been processed.
    var processed = new FullRecord(raw.rowKey, raw.uuid)//raw.clone
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
    updateRawIfSensitised(raw, processed, raw.rowKey)
  }
}
