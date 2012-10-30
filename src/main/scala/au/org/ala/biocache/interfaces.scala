package au.org.ala.biocache

import java.io.OutputStream
import collection.JavaConversions
import au.org.ala.util.ProcessedValue
import au.org.ala.util.RecordProcessor
import au.org.ala.util.IndexRecords
import java.util.UUID
import java.util.Date
import au.org.ala.util._
import au.org.ala.biocache.outliers.{JackKnifeStats,RecordJackKnifeStats}
import au.org.ala.biocache.qa.QueryAssertion

/**
 * This is the interface to use for java applications.
 * This will allow apps to:
 *
 * 1) Retrieve single record, three versions
 * 2) Page over records
 * 3) Add user supplied or system systemAssertions for records
 * 4) Add user supplied corrections to records
 * 5) Record downloads
 * 6) Add records in a temporary space
 */
object Store {

  private val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
  private val outlierStatsDAO = Config.getInstance(classOf[OutlierStatsDAO]).asInstanceOf[OutlierStatsDAO]
  private val deletedRecordDAO = Config.deletedRecordDAO
  private val duplicateDAO = Config.duplicateDAO
  private val assertionQueryDAO = Config.assertionQueryDAO

  private var readOnly = false;

  import JavaConversions._
  import scalaj.collection.Imports._
  import BiocacheConversions._
  /**
   * A java API friendly version of the getByUuid that doesnt require knowledge of a scala type.
   */
  def getByUuid(uuid: java.lang.String, version: Version): FullRecord = {
    occurrenceDAO.getByUuid(uuid, version).getOrElse(null)
  }
  
  def getSensitiveByUuid(uuid:java.lang.String, version:Version):FullRecord = occurrenceDAO.getByUuid(uuid, version, true).getOrElse(null)
  
  def getByRowKey(rowKey: java.lang.String, version: Version): FullRecord = occurrenceDAO.getByRowKey(rowKey, version).getOrElse(null)
  
  def getSensitiveByRowKey(rowKey: java.lang.String, version: Version): FullRecord = occurrenceDAO.getByRowKey(rowKey, version, true).getOrElse(null)

  /**
   * A java API friendly version of the getByUuid that doesnt require knowledge of a scala type.
   */
  def getByUuid(uuid: java.lang.String): FullRecord = {
    occurrenceDAO.getByUuid(uuid, Raw).getOrElse(null)
  }

  /**
   * Retrieve all versions of the record with the supplied UUID.
   */
  def getAllVersionsByUuid(uuid: java.lang.String, includeSensitive:java.lang.Boolean): Array[FullRecord] = {
    occurrenceDAO.getAllVersionsByUuid(uuid, includeSensitive).getOrElse(null)
  }
  
  def getAllVersionsByRowKey(rowKey: java.lang.String, includeSensitive:java.lang.Boolean) : Array[FullRecord]={
    occurrenceDAO.getAllVersionsByRowKey(rowKey, includeSensitive).getOrElse(null)
  }

  //TODO need a better mechanism for doing this....
  private val propertiesToHide = Set("originalSensitiveValues","originalDecimalLatitude","originalDecimalLongitude", "orginalLocationRemarks", "originalVerbatimLatitude", "originalVerbatimLongitude")

  /**
   * Get the raw processed comparison based on the uuid for the occurrence.
   */
  def getComparisonByUuid(uuid: java.lang.String): java.util.Map[String,java.util.List[ProcessedValue]] = getComparison(occurrenceDAO.getAllVersionsByUuid(uuid).getOrElse(null))

  /**
   * Get the raw processed comparison based on the rowKey for the occurrence
   */
  def getComparisonByRowKey(rowKey: java.lang.String) : java.util.Map[String,java.util.List[ProcessedValue]] = getComparison(occurrenceDAO.getAllVersionsByRowKey(rowKey).getOrElse(null))
  
  private def getComparison(recordVersions:Array[FullRecord]) ={
    if (recordVersions != null && recordVersions.length > 1) {
      val map = new java.util.HashMap[String, java.util.List[ProcessedValue]]

      val raw = recordVersions(0)
      val processed = recordVersions(1)

      val rawAndProcessed = raw.objectArray zip processed.objectArray

      rawAndProcessed.foreach(rawAndProcessed => {

        val (rawPoso, procPoso) = rawAndProcessed
        val listBuff = new java.util.LinkedList[ProcessedValue]
        
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
        
        val name = rawPoso.getClass().getName().substring(rawPoso.getClass().getName().lastIndexOf(".")+1)
        map.put(name, listBuff)
      })

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
   * Load the record, download any media associated with the record.
   */
  def loadRecord(dataResourceUid:String, fr:FullRecord, identifyingTerms:java.util.List[String], shouldIndex:Boolean = true){
    val s = new SimpleLoader
    fr.lastModifiedTime = new Date()
    s.load(dataResourceUid, fr, identifyingTerms.toList, true, true)
    val processor = new RecordProcessor
    processor.processRecordAndUpdate(fr)
    if(shouldIndex){
      occurrenceDAO.reIndex(fr.rowKey)
    }
  }
  
  /**
   * Loads a batch of records based on being supplied in a list of maps.  
   * 
   * It relies on identifyFields supplying a list dwc terms that make up the unique identifier for the data resource
   */
  def loadRecords(dataResourceUid:String, recordsProperties:java.util.List[java.util.Map[String,String]], identifyFields:java.util.List[String], shouldIndex:Boolean=true){
   val loader = new MapDataLoader
   val rowKeys = loader.load(dataResourceUid, recordsProperties.toList,identifyFields.toList)
   if(rowKeys.size>0){
      val processor = new RecordProcessor
      processor.processRecords(rowKeys)
      if(shouldIndex)
        IndexRecords.indexList(rowKeys)
   }
  }
  
  /**
   * Adds or updates a raw full record with values that are in the FullRecord
   * relies on a rowKey being set
   *  
   * Record is processed and indexed if should index is true
   */
  def upsertRecord(record:FullRecord, shouldIndex:Boolean){
    //rowKey = dr|<cxyzsuid>
    if(record.rowKey != null){
        val (recordUuid, isNew)= occurrenceDAO.createOrRetrieveUuid(record.rowKey)
        record.uuid =recordUuid
        //add the last load time
        record.lastModifiedTime = new Date()
        if(isNew)
            record.firstLoaded = record.lastModifiedTime
        occurrenceDAO.addRawOccurrence(record)
        val processor = new RecordProcessor
        processor.processRecordAndUpdate(record)
        if(shouldIndex){
            occurrenceDAO.reIndex(record.rowKey)
        }
    }
  }

  /**
   * Adds or updates a raw full record with values that are in the FullRecord
   * relies on a rowKey being set. It will then process the supplied record.
   *
   * Record is processed and indexed if should index is true
   */
  def insertRecord(dataResourceIdentifer:String, properties:java.util.Map[String,String], shouldIndex:Boolean){
    val processor = new RecordProcessor
    processor.addRecordAndProcess(dataResourceIdentifer, properties.toMap[String,String])
  }

  /**
   * Adds or updates a raw full record with values that are in the FullRecord
   * relies on a rowKey being set. This method only loads the record.
   *
   * Record is processed and indexed if should index is true
   */
  def loadRecord(dataResourceIdentifer:String, properties:java.util.Map[String,String], shouldIndex:Boolean){
    val processor = new RecordProcessor
    processor.addRecord(dataResourceIdentifer, properties.toMap[String,String])
  }

  /**
   * Deletes the records for the supplied rowKey from the index and data store
   */
  def deleteRecord(rowKey:String) = if(rowKey != null) occurrenceDAO.delete(rowKey)
  /**
   * Deletes the supplied list of row keys from the index and data store
   */
  def deleteRecords(rowKeys:java.util.List[String]){
    val deletor = new ListDelete(rowKeys.toList)
    deletor.deleteFromPersistent
    deletor.deleteFromIndex
  }

  /**
   * Retrieve the system supplied systemAssertions.
   * 
   * A user can supply either a uuid or rowKey
   */
  def getSystemAssertions(uuid: java.lang.String): java.util.List[QualityAssertion] = {
    //systemassertions are handled using row keys - this is unlike user assertions.
    val rowKey = occurrenceDAO.getRowKeyFromUuid(uuid).getOrElse(uuid);
    occurrenceDAO.getSystemAssertions(rowKey).asJava[QualityAssertion]
  }

  /**
   * Retrieve the user supplied systemAssertions.
   */
  def getUserAssertion(uuid: java.lang.String, assertionUuid: java.lang.String): QualityAssertion = {
    val rowKey = occurrenceDAO.getRowKeyFromUuid(uuid).getOrElse(uuid);
    occurrenceDAO.getUserAssertions(rowKey).find(ass => { ass.uuid == assertionUuid }).getOrElse(null)
  }

  /**
   * Retrieve the user supplied systemAssertions.
   */
  def getUserAssertions(uuid: java.lang.String): java.util.List[QualityAssertion] = {
    val rowKey = occurrenceDAO.getRowKeyFromUuid(uuid).getOrElse(uuid);
    occurrenceDAO.getUserAssertions(rowKey).asJava[QualityAssertion]
  }

  /**
   * Add a user assertion
   *
   * Requires a re-index
   */
  def addUserAssertion(uuid: java.lang.String, qualityAssertion: QualityAssertion) {
    if (!readOnly) {
      val rowKey = occurrenceDAO.getRowKeyFromUuid(uuid).getOrElse(uuid);
      occurrenceDAO.addUserAssertion(rowKey, qualityAssertion)
      occurrenceDAO.reIndex(rowKey)
    } else {
      throw new Exception("In read only mode. Please try again later")
    }
  }
  
  def addAssertionQuery(assertionQuery:AssertionQuery) = assertionQueryDAO.upsertAssertionQuery(assertionQuery)
  
  def getAssertionQuery(uuid:java.lang.String):AssertionQuery = assertionQueryDAO.getAssertionQuery(uuid).getOrElse(null)
  
  def getAssertionQueries(ids:Array[String]):Array[AssertionQuery] = assertionQueryDAO.getAssertionQueries(ids.toList).toArray[AssertionQuery]
  
  /**
   * Applies the supplied query assertion to the records.
   */
  def applyAssertionQuery(uuid:java.lang.String) = new QueryAssertion().applySingle(uuid)
  
  def deleteAssertionQuery(id:java.lang.String, date:java.util.Date) = assertionQueryDAO.deleteAssertionQuery(id, date)

  /**
   * Delete an assertion
   *
   * Requires a re-index
   */
  def deleteUserAssertion(uuid: java.lang.String, assertionUuid: java.lang.String) {
    if (!readOnly) {
      val rowKey = occurrenceDAO.getRowKeyFromUuid(uuid).getOrElse(uuid);
      occurrenceDAO.deleteUserAssertion(rowKey, assertionUuid)
      occurrenceDAO.reIndex(rowKey)
    } else {
      throw new Exception("In read only mode. Please try again later")
    }
  }
  /**
   * Puts biocache store into readonly mode.
   * Useful when we don't want services to update the index.
   * This is generally when a optimise is occurring
   */
  def setReadOnly(ro: Boolean) : Unit = readOnly = ro

  def isReadOnly = readOnly
  
  def optimiseIndex() :String = {
      val start = System.currentTimeMillis
      readOnly = true
      try {
          val indexString =Config.indexDAO.optimise
          val finished = System.currentTimeMillis
          readOnly = false
          "Optimised in " + (finished -start).toFloat / 60000f + " minutes.\n" +indexString
      }
      catch{
          case e:Exception => {
              //report error message and take out of readOnly
              readOnly = false
              e.getMessage
          }
      }
  }

  /**
   * Reopens the current index to account for external index changes
   */
  def reopenIndex = Config.indexDAO.reload

  /**
   * Indexes a dataResource from a specific date
   */
  def reindex(dataResource:java.lang.String, startDate:java.lang.String){
      if(dataResource != null && startDate != null)
          IndexRecords.index(None, None, Some(dataResource), false, false, Some(startDate))
      else
          throw new Exception("Must supply data resource and start date")
  }

  /**
   * Indexes a dataResource from a specific date
   */
  def index(dataResource:java.lang.String) = IndexRecords.index(None, None, Some(dataResource), false, false, None)

  def index(dataResource:java.lang.String, customIndexFields:Array[String]) = {
    IndexRecords.index(None, None, Some(dataResource), false, false, None, miscIndexProperties = customIndexFields)
    storeCustomIndexFields(dataResource,customIndexFields)
  }

  /**
   * Run the sampling for this dataset
   * @param dataResourceUid
   */
  def sample(dataResourceUid:java.lang.String) = Sampling.sampleDataResource(dataResourceUid)

  /**
   * Process records for the supplied resource
   * @param dataResourceUid
   * @param threads
   */
  def process(dataResourceUid:java.lang.String, threads:Int = 1) = {
    ProcessWithActors.processRecords(1, None , Some(dataResourceUid))
  }

  /**
   * Writes the select records to the stream. Optionally including the sensitive values.
   */
  def writeToStream(outputStream: OutputStream, fieldDelimiter: java.lang.String,
    recordDelimiter: java.lang.String, keys: Array[String], fields: Array[java.lang.String], qaFields: Array[java.lang.String], includeSensitive:Boolean) {
    occurrenceDAO.writeToStream(outputStream, fieldDelimiter, recordDelimiter, keys, fields, qaFields, includeSensitive)
  }

  /**
   * Writes the select records to the stream.
   */
  def writeToStream(outputStream: OutputStream, fieldDelimiter: java.lang.String,
    recordDelimiter: java.lang.String, keys: Array[String], fields: Array[java.lang.String], qaFields: Array[java.lang.String]) {
    writeToStream(outputStream, fieldDelimiter, recordDelimiter, keys, fields, qaFields, false)
  }
  
  def writeToWriter(writer:RecordWriter,keys: Array[String], fields: Array[java.lang.String], qaFields: Array[java.lang.String], includeSensitive:Boolean){
      occurrenceDAO.writeToRecordWriter(writer, keys, fields, qaFields, includeSensitive)      
  }

  def writeToWriter(writer:RecordWriter,keys: Array[String], fields: Array[java.lang.String], qaFields: Array[java.lang.String]){
      writeToWriter(writer, keys, fields, qaFields, false)
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
  /**
   * Retrieves a map of index fields to storage fields
   */
  def getIndexFieldMap:java.util.Map[String,String] = IndexFields.indexFieldMap
  
  def getStorageFieldMap:java.util.Map[String,String] = IndexFields.storeFieldMap

  /**
   * Returns the biocache id for the supplied layername
   */
  def getLayerId(name :String ):String={
    if(name != null) Layers.nameToIdMap.getOrElse(name.toLowerCase, null)
    else null
  }

  /**
   * Returns the spatial name for the supplied biocache layer id
   */
  def getLayerName(id:String):String={
    if(id != null)Layers.idToNameMap.getOrElse(id, null)
    else null
  }

  def getAlternativeFormats(filePath:String): Array[String] = MediaStore.alternativeFormats(filePath)

  def getJackKnifeStatsFor(guid:String) : java.util.Map[String, JackKnifeStats] = outlierStatsDAO.getJackKnifeStatsFor(guid)

  def getJackKnifeOutliersFor(guid:String) : java.util.Map[String, Array[String]] = outlierStatsDAO.getJackKnifeOutliersFor(guid)

  def getJackKnifeRecordDetailsFor(uuid:String) : Array[RecordJackKnifeStats] = outlierStatsDAO.getJackKnifeRecordDetailsFor(uuid)
  
  def getDuplicateDetails(uuid:String):DuplicateRecordDetails = duplicateDAO.getDuplicateInfo(uuid).getOrElse(null)
  
  /**
   * Returns a list of record uuids that have been deleted since the supplied date inclusive
   */
  def getDeletedRecords(date:java.util.Date):Array[String] = deletedRecordDAO.getUuidsForDeletedRecords(org.apache.commons.lang.time.DateFormatUtils.format(date, "yyyy-MM-dd"))


  def storeCustomIndexFields(tempUid:String, customIndexFields:Array[String]){
    Config.persistenceManager.put(tempUid, "upload", "customIndexFields", Json.toJSON(customIndexFields.map(_ + "_s")))
  }

  def retrieveCustomIndexFields(tempUid:String) : Array[String] = {
    try {
      val s = Config.persistenceManager.get(tempUid, "upload", "customIndexFields")
      Json.toStringArray(s.getOrElse("[]"))
    } catch {
      case _ => Array()
    }
  }
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
/**
 * A trait to be implemented by java classes to write records.
 */
trait RecordWriter{
  /** Writes the supplied record.*/
  def write(record:Array[String])
  /** Performs all the finishing tasks in writing the download file. */
  def finalise
}
