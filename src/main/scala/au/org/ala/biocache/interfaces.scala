package au.org.ala.biocache

import java.io.OutputStream
import collection.JavaConversions
import au.org.ala.util.ProcessedValue
import scala.collection.mutable.ListBuffer
import au.org.ala.util.RecordProcessor

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

  private val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
  private var readOnly = false;

  import JavaConversions._
  import scalaj.collection.Imports._
  /**
   * A java API friendly version of the getByUuid that doesnt require knowledge of a scala type.
   */
  def getByUuid(uuid: java.lang.String, version: Version): FullRecord = {
    occurrenceDAO.getByUuid(uuid, version).getOrElse(null)
  }

  /**
   * A java API friendly version of the getByUuid that doesnt require knowledge of a scala type.
   */
  def getByUuid(uuid: java.lang.String): FullRecord = {
    occurrenceDAO.getByUuid(uuid, Raw).getOrElse(null)
  }

  /**
   * Retrieve all versions of the record with the supplied UUID.
   */
  def getAllVersionsByUuid(uuid: java.lang.String): Array[FullRecord] = {
    occurrenceDAO.getAllVersionsByUuid(uuid).getOrElse(null)
  }
  
  def getAllVersionsByRowKey(rowKey: java.lang.String) : Array[FullRecord]={
    occurrenceDAO.getAllVersionsByRowKey(rowKey).getOrElse(null)
  }

  //TODO need a better mechanism for doing this....
  private val propertiesToHide = Set("originalDecimalLatitude","originalDecimalLongitude")

  /**
   * A java API friendly version of the getByUuid that doesnt require knowledge of a scala type.
   */
  def getComparisonByUuid(uuid: java.lang.String): java.util.Map[String,java.util.List[ProcessedValue]] = {

    val recordVersions = occurrenceDAO.getAllVersionsByUuid(uuid).getOrElse(null)
    if (recordVersions != null && recordVersions.length > 1) {
      val map = new java.util.HashMap[String, java.util.List[ProcessedValue]]

      val raw = recordVersions(0)
      val processed = recordVersions(1)

      val rawAndProcessed = raw.objectArray zip processed.objectArray
      
      for ((rawPoso, procPoso) <- rawAndProcessed) {
        
        val listBuff = new java.util.LinkedList[ProcessedValue]
        
        if (!rawPoso.isInstanceOf[ContextualLayers] && !rawPoso.isInstanceOf[EnvironmentalLayers]) {
          rawPoso.propertyNames.foreach(name => {
            if(!propertiesToHide.contains(name)){
              val rawValue = rawPoso.getProperty(name)
              val procValue = procPoso.getProperty(name)
              if (!rawValue.isEmpty || !procValue.isEmpty) {
                val term = ProcessedValue(name, rawValue.getOrElse(""), procValue.getOrElse(""))
                listBuff.add(term)
              }
            }
          })
        }
        val name = rawPoso.getClass().getName().substring(rawPoso.getClass().getName().lastIndexOf(".")+1)
        map.put(name, listBuff)
      }

      map
    } else {
      new java.util.HashMap[String, java.util.List[ProcessedValue]]()
    }
  }

  /**
   * Iterate over records, passing the records to the supplied consumer.
   */
  def pageOverAll(version: Version, consumer: OccurrenceConsumer, startKey: String, pageSize: Int) {
    val skey = if (startKey == null) "" else startKey
    occurrenceDAO.pageOverAll(version, fullRecord => consumer.consume(fullRecord.get), skey, "", pageSize)
  }

  /**
   * Page over all versions of the record, handing off to the OccurrenceVersionConsumer.
   */
  def pageOverAllVersions(consumer: OccurrenceVersionConsumer, startKey: String, pageSize: Int) {
    occurrenceDAO.pageOverAllVersions(fullRecordVersion => {
      if (!fullRecordVersion.isEmpty) {
        consumer.consume(fullRecordVersion.get)
      } else {
        true
      }
    }, startKey, "", pageSize)
  }
  /**
   * adds or updates a raw full record with values that are in the FullRecord
   * relies on a rowKey being set
   *  
   * Record is processed and indexed if should index is true
   */
  def upsertRecord(record:FullRecord, shouldIndex:Boolean){
    //rowKey = dr|<cxyzsuid>
    if(record.rowKey != null){
        record.uuid = occurrenceDAO.createOrRetrieveUuid(record.rowKey) 
        occurrenceDAO.addRawOccurrenceBatch(Array(record))
        if(shouldIndex){
            val processor = new RecordProcessor
            processor.processRecordAndUpdate(record)
            occurrenceDAO.reIndex(record.uuid)
        }
    }
  }
  /**
   * Deletes the records for the supplied rowKey from the index and data store
   */
  def deleteRecord(rowKey:String){
      if(rowKey != null){
          occurrenceDAO.delete(rowKey)
      }
  }

  /**
   * Retrieve the system supplied systemAssertions.
   */
  def getSystemAssertions(uuid: java.lang.String): java.util.List[QualityAssertion] = {
    //systemassertions are handled using row keys - this is unlike user assertions.
    val rowKey = occurrenceDAO.getRowKeyFromUuid(uuid)
    if (!rowKey.isEmpty)
      occurrenceDAO.getSystemAssertions(rowKey.get).asJava[QualityAssertion]
    else
      List[QualityAssertion]().asJava[QualityAssertion]
  }

  /**
   * Retrieve the user supplied systemAssertions.
   */
  def getUserAssertion(uuid: java.lang.String, assertionUuid: java.lang.String): QualityAssertion = {
    occurrenceDAO.getUserAssertions(uuid).find(ass => { ass.uuid == assertionUuid }).getOrElse(null)
  }

  /**
   * Retrieve the user supplied systemAssertions.
   */
  def getUserAssertions(uuid: java.lang.String): java.util.List[QualityAssertion] = {
    occurrenceDAO.getUserAssertions(uuid).asJava[QualityAssertion]
  }

  /**
   * Add a user assertion
   *
   * Requires a re-index
   */
  def addUserAssertion(uuid: java.lang.String, qualityAssertion: QualityAssertion) {
    if (!readOnly) {
      occurrenceDAO.addUserAssertion(uuid, qualityAssertion)
      occurrenceDAO.reIndex(uuid)
    } else {
      throw new Exception("In read only mode. Please try again later")
    }
  }

  /**
   * Delete an assertion
   *
   * Requires a re-index
   */
  def deleteUserAssertion(uuid: java.lang.String, assertionUuid: java.lang.String) {
    if (!readOnly) {
      occurrenceDAO.deleteUserAssertion(uuid, assertionUuid)
      occurrenceDAO.reIndex(uuid)
    } else {
      throw new Exception("In read only mode. Please try again later")
    }
  }
  /**
   * Puts biocache store into readonly mode.
   * Useful when we don't want services to update the index.
   * This is generally when a optimise is occurring
   */
  def setReadOnly(ro: Boolean) = {
    readOnly = ro

  }

  /**
   * Writes the select records to the stream.
   */
  def writeToStream(outputStream: OutputStream, fieldDelimiter: java.lang.String,
    recordDelimiter: java.lang.String, keys: Array[String], fields: Array[java.lang.String], qaFields: Array[java.lang.String]) {
    occurrenceDAO.writeToStream(outputStream, fieldDelimiter, recordDelimiter, keys, fields, qaFields)
  }

  /**
   * Retrieve the assertion codes
   */
  def retrieveAssertionCodes: Array[ErrorCode] = AssertionCodes.all.toArray

  /**
   * Retrieve the geospatial codes.
   */
  def retrieveGeospatialCodes: Array[ErrorCode] = AssertionCodes.geospatialCodes.toArray

  /**
   * Retrieve the taxonomic codes.
   */
  def retrieveTaxonomicCodes: Array[ErrorCode] = AssertionCodes.taxonomicCodes.toArray

  /**
   * Retrieve temporal codes
   */
  def retrieveTemporalCodes: Array[ErrorCode] = AssertionCodes.temporalCodes.toArray

  /**
   * Retrieve miscellaneous codes
   */
  def retrieveMiscellaneousCodes: Array[ErrorCode] = AssertionCodes.miscellaneousCodes.toArray

  /**
   * A user friendly set of assertion types.
   */
  def retrieveUserAssertionCodes: Array[ErrorCode] = AssertionCodes.userAssertionCodes.toArray

  /**
   * Retrieve an error code by code.
   */
  def getByCode(codeAsString: String): ErrorCode = {
    val code = codeAsString.toInt
    AssertionCodes.all.find(errorCode => errorCode.code == code).getOrElse(null)
  }
  /**
   * Retrieve the list of species groups
   */
  def retrieveSpeciesGroups: java.util.List[SpeciesGroup] = SpeciesGroups.groups.asJava[SpeciesGroup]
}

/**
 *    A trait to implement by java classes to process occurrence records.
 */
trait OccurrenceConsumer {
  /** Consume the supplied record */
  def consume(record: FullRecord): Boolean
}

/**
 * A trait to implement by java classes to process occurrence records.
 */
trait OccurrenceVersionConsumer {
  /** Passes an array of versions. Raw, Process and consensus versions */
  def consume(record: Array[FullRecord]): Boolean
}