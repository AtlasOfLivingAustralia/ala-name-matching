package au.org.ala.biocache

import collection.JavaConversions
import java.io.OutputStream

/**
 * This is the interface to use for java applications.
 * This will allow apps to:
 *
 * 1) Retrieve single record, three versions
 * 2) Page over records
 * 3) Add user supplied or system systemAssertions for records
 * 4) Add user supplied corrections to records
 * 5) Record downloads
 */
object Store {

  import JavaConversions._
  import scalaj.collection.Imports._

  /**
   * A java API friendly version of the getByUuid that doesnt require knowledge of a scala type.
   */
  def getByUuid(uuid:java.lang.String, version:Version) : FullRecord = {
    OccurrenceDAO.getByUuid(uuid, version).getOrElse(null)
  }

  /**
   * A java API friendly version of the getByUuid that doesnt require knowledge of a scala type.
   */
  def getByUuid(uuid:java.lang.String) : FullRecord = {
    OccurrenceDAO.getByUuid(uuid, Raw).getOrElse(null)
  }

  /**
   * Retrieve all versions of the record with the supplied UUID.
   */
  def getAllVersionsByUuid(uuid:java.lang.String) : Array[FullRecord] = {
    OccurrenceDAO.getAllVersionsByUuid(uuid).getOrElse(null)
  }

  /**
   * Iterate over records, passing the records to the supplied consumer.
   */
  def pageOverAll(version:Version, consumer:OccurrenceConsumer, pageSize:Int) {
    OccurrenceDAO.pageOverAll(version, fullRecord => consumer.consume(fullRecord.get), pageSize)
  }

  /**
   * Page over all versions of the record, handing off to the OccurrenceVersionConsumer.
   */
  def pageOverAllVersions(consumer:OccurrenceVersionConsumer, pageSize:Int) {
      OccurrenceDAO.pageOverAllVersions(fullRecordVersion => {
          if(!fullRecordVersion.isEmpty){
            consumer.consume(fullRecordVersion.get)
          } else {
            true
          }
      }, pageSize)
  }

  /**
   * Retrieve the system supplied systemAssertions.
   */
  def getSystemAssertions(uuid:java.lang.String) : java.util.List[QualityAssertion] = {
    OccurrenceDAO.getSystemAssertions(uuid).asJava[QualityAssertion]
  }

  /**
   * Retrieve the user supplied systemAssertions.
   */
  def getUserAssertion(uuid:java.lang.String, assertionUuid:java.lang.String) : QualityAssertion = {
    OccurrenceDAO.getUserAssertions(uuid).find(ass => {ass.uuid == assertionUuid}).getOrElse(null)
  }

  /**
   * Retrieve the user supplied systemAssertions.
   */
  def getUserAssertions(uuid:java.lang.String) : java.util.List[QualityAssertion] = {
    OccurrenceDAO.getUserAssertions(uuid).asJava[QualityAssertion]
  }

  /**
   * Add a user assertion
   *
   * Requires a re-index
   */
  def addUserAssertion(uuid:java.lang.String, qualityAssertion:QualityAssertion){
    OccurrenceDAO.addUserAssertion(uuid, qualityAssertion)
    OccurrenceDAO.reIndex(uuid)
  }

  /**
   * Delete an assertion
   *
   * Requires a re-index
   */
  def deleteUserAssertion(uuid:java.lang.String, assertionUuid:java.lang.String){
    OccurrenceDAO.deleteUserAssertion(uuid,assertionUuid)
    OccurrenceDAO.reIndex(uuid)
  }

  /**
   * Writes the select records to the stream.
   */
  def writeToStream(outputStream:OutputStream,fieldDelimiter:java.lang.String,
        recordDelimiter:java.lang.String,uuids:Array[String],fields:Array[java.lang.String]) {
    OccurrenceDAO.writeToStream(outputStream,fieldDelimiter,recordDelimiter,uuids,fields)
  }

  /**
   * Retrieve the assertion codes
   */
  def retrieveAssertionCodes : Array[ErrorCode] = AssertionCodes.all.toArray

  /**
   * Retrieve the geospatial codes.
   */
  def retrieveGeospatialCodes : Array[ErrorCode] = AssertionCodes.geospatialCodes.toArray

  /**
   * Retrieve the taxonomic codes.
   */
  def retrieveTaxonomicCodes : Array[ErrorCode] = AssertionCodes.taxonomicCodes.toArray

  /**
   * Retrieve temporal codes
   */
  def retrieveTemporalCodes : Array[ErrorCode] = AssertionCodes.temporalCodes.toArray

  /**
   * Retrieve miscellaneous codes
   */
  def retrieveMiscellaneousCodes : Array[ErrorCode] = AssertionCodes.miscellaneousCodes.toArray
}

/**
 *    A trait to implement by java classes to process occurrence records.
 */
trait OccurrenceConsumer {
  /** Consume the supplied record */
  def consume(record:FullRecord) : Boolean
}

/**
 * A trait to implement by java classes to process occurrence records.
 */
trait OccurrenceVersionConsumer {
  /** Passes an array of versions. Raw, Process and consensus versions */
  def consume(record:Array[FullRecord]) : Boolean
}