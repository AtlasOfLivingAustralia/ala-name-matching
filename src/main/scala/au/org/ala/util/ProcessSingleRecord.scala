package au.org.ala.util

import au.org.ala.biocache.Config
import java.io.{InputStreamReader, BufferedReader}
import org.codehaus.jackson.map.ObjectMapper
import org.slf4j.LoggerFactory

/**
 * Utility for processing a single record.
 */
object ProcessSingleRecord {
  val logger = LoggerFactory.getLogger("ProcessSingleRecord")

  def processRecord(uuid: String) {
    val processor = new RecordProcessor
    var records = Config.occurrenceDAO.getAllVersionsByRowKey(uuid)
    if (records.isEmpty) {
      records = Config.occurrenceDAO.getAllVersionsByUuid(uuid)
    }
    if (!records.isEmpty) {
      println("Processing record.....")
      processor.processRecord(records.get(0), records.get(1))
      val processedRecord = Config.occurrenceDAO.getByRowKey(records.get(1).rowKey, au.org.ala.biocache.Processed)
      val objectMapper = new ObjectMapper
      if (!processedRecord.isEmpty)
        logger.info(objectMapper.writeValueAsString(processedRecord.get))
      else
        logger.info("Record not found")
    } else {
      logger.info("UUID or row key not stored....")
    }
    print("\n\nSupply a Row Key for a record: ")
  }

  def main(args: Array[String]) {

    logger.info("Supply a UUID or a Row Key for a record: ")
    var uuid = readStdIn
    while (uuid != "q" && uuid != "exit") {
      processRecord(uuid)
      uuid = readStdIn
    }
    println("Exiting...")
    exit(1)
  }

  def readStdIn = (new BufferedReader(new InputStreamReader(System.in))).readLine.trim
}