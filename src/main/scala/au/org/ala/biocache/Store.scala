package au.org.ala.biocache

import java.io.OutputStream
import collection.JavaConversions
import java.util.Date
import outliers.JackKnifeStats
import au.org.ala.biocache.qa.ValidationRuleRunner
import org.ala.layers.dao.IntersectCallback
import collection.mutable.ArrayBuffer
import au.org.ala.biocache.util._
import au.org.ala.biocache.dao.{OutlierStatsDAO, OccurrenceDAO}
import au.org.ala.biocache.load.{MediaStore, SimpleLoader, MapDataLoader}
import au.org.ala.biocache.model._
import au.org.ala.biocache.index.{IndexRecords, IndexFields}
import au.org.ala.biocache.vocab._
import au.org.ala.biocache.outliers.JackKnifeStats
import scala.Some
import au.org.ala.biocache.outliers.RecordJackKnifeStats
import au.org.ala.biocache.outliers.SampledRecord
import au.org.ala.biocache.vocab.ErrorCode
import au.org.ala.biocache.parser.ProcessedValue
import au.org.ala.biocache.outliers.JackKnifeStats
import au.org.ala.biocache.vocab.SpeciesGroup
import scala.Some
import au.org.ala.biocache.parser.ProcessedValue
import au.org.ala.biocache.outliers.RecordJackKnifeStats
import au.org.ala.biocache.outliers.SampledRecord
import au.org.ala.biocache.vocab.ErrorCode
import au.org.ala.biocache.outliers.JackKnifeStats
import au.org.ala.biocache.vocab.SpeciesGroup
import scala.Some
import au.org.ala.biocache.parser.ProcessedValue
import au.org.ala.biocache.outliers.RecordJackKnifeStats
import au.org.ala.biocache.outliers.SampledRecord
import au.org.ala.biocache.vocab.ErrorCode
import au.org.ala.biocache.tool.{DataResourceDelete, RecordProcessor, ProcessWithActors, Sampling}

/**
 * This is the interface to use for java applications or any application using this as a library.
 *
 * This will allow apps to:
 * <ul>
 * <li> Retrieve single record, three versions </li>
 * <li> Page over records </li>
 * <li> Add user supplied or system systemAssertions for records </li>
 * <li> Add user supplied corrections to records </li>
 * <li> Record downloads </li>
 * <li> Add records in a temporary space </li>
 * </ul>
 *
 * ...and lots more.
 */
object Store {

  private val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
  private val outlierStatsDAO = Config.getInstance(classOf[OutlierStatsDAO]).asInstanceOf[OutlierStatsDAO]
  private val deletedRecordDAO = Config.deletedRecordDAO
  private val duplicateDAO = Config.duplicateDAO
  private val validationRuleDAO = Config.validationRuleDAO
  //TODO need a better mechanism for doing this....
  private val propertiesToHide = Set("originalSensitiveValues","originalDecimalLatitude","originalDecimalLongitude", "orginalLocationRemarks", "originalVerbatimLatitude", "originalVerbatimLongitude")
  private var readOnly = false

  import JavaConversions._
  import scala.collection.JavaConverters._
  import BiocacheConversions._
  /**
   * A java API friendly version of the getByUuid that doesnt require knowledge of a scala type.
   */
  def getByUuid(uuid: java.lang.String, version: Version): FullRecord =
    occurrenceDAO.getByUuid(uuid, version).getOrElse(null)

  def getSensitiveByUuid(uuid:java.lang.String, version:Version):FullRecord =
    occurrenceDAO.getByUuid(uuid, version, true).getOrElse(null)
  
  def getByRowKey(rowKey: java.lang.String, version: Version): FullRecord =
    occurrenceDAO.getByRowKey(rowKey, version).getOrElse(null)
  
  def getSensitiveByRowKey(rowKey: java.lang.String, version: Version): FullRecord =
    occurrenceDAO.getByRowKey(rowKey, version, true).getOrElse(null)

  /**
   * A java API friendly version of the getByUuid that doesnt require knowledge of a scala type.
   */
  def getByUuid(uuid: java.lang.String): FullRecord = occurrenceDAO.getByUuid(uuid, Raw).getOrElse(null)

  /**
   * Retrieve all versions of the record with the supplied UUID.
   */
  def getAllVersionsByUuid(uuid: java.lang.String, includeSensitive:java.lang.Boolean): Array[FullRecord] =
    occurrenceDAO.getAllVersionsByUuid(uuid, includeSensitive).getOrElse(null)

  def getAllVersionsByRowKey(rowKey: java.lang.String, includeSensitive:java.lang.Boolean) : Array[FullRecord] =
    occurrenceDAO.getAllVersionsByRowKey(rowKey, includeSensitive).getOrElse(null)

  /**
   * Get the raw processed comparison based on the uuid for the occurrence.
   */
  def getComparisonByUuid(uuid: java.lang.String): java.util.Map[String,java.util.List[ProcessedValue]] =
    getComparison(occurrenceDAO.getAllVersionsByUuid(uuid).getOrElse(null))

  /**
   * Get the raw processed comparison based on the rowKey for the occurrence
   */
  def getComparisonByRowKey(rowKey: java.lang.String) : java.util.Map[String,java.util.List[ProcessedValue]] =
    getComparison(occurrenceDAO.getAllVersionsByRowKey(rowKey).getOrElse(null))


  private def getComparison(recordVersions:Array[FullRecord]) = {
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
   * Adds or updates a raw full record with values that are in the FullRecord
   * relies on a rowKey being set. This method only loads the record.
   *
   * Record is processed and indexed if should index is true
   */
  def loadRecord(dataResourceIdentifer:String, properties:java.util.Map[String,String], shouldIndex:Boolean){
    (new RecordProcessor).addRecord(dataResourceIdentifer, properties.toMap[String,String])
  }

  /**
   * Load the record, download any media associated with the record but avoid processing the record.
   */
  def loadRecordOnly(dataResourceUid:String, fr:FullRecord, identifyingTerms:java.util.List[String]){
    fr.lastModifiedTime = new Date()
    (new SimpleLoader).load(dataResourceUid, fr, identifyingTerms.toList, true, true)
  }

  /**
   * Load the record, download any media associated with the record.
   */
  def loadRecord(dataResourceUid:String, fr:FullRecord, identifyingTerms:java.util.List[String], shouldIndex:Boolean = true){
    fr.lastModifiedTime = new Date()
    (new SimpleLoader).load(dataResourceUid, fr, identifyingTerms.toList, true, true)
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
   if(!rowKeys.isEmpty){
     val processor = new RecordProcessor
     processor.processRecords(rowKeys)
     if(shouldIndex){
       IndexRecords.indexList(rowKeys)
     }
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
      record.lastModifiedTime = new Date
      if(isNew){
        record.firstLoaded = record.lastModifiedTime
      }
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
  def insertRecord(dataResourceIdentifier:String, properties:java.util.Map[String,String], shouldIndex:Boolean){
    (new RecordProcessor).addRecordAndProcess(dataResourceIdentifier, properties.toMap[String,String])
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
  
  def deleteRecords(dataResource:java.lang.String,query:java.lang.String, fromPersistent:Boolean, fromIndex:Boolean){              
    val deletor:RecordDeletor = if(dataResource != null) new DataResourceDelete(dataResource)  else new QueryDelete(query)
    logger.debug("Delete from storage using the query: " + query)
    if(fromPersistent){
      deletor.deleteFromPersistent
    }
    logger.debug("Delete from index")
    if(fromIndex){
      deletor.deleteFromIndex
    }
    logger.debug("Delete complete.")
  }

  /**
   * Get system assertions that have failed.
   *
   * @param uuid
   * @return
   */
  def getSystemAssertions(uuid: java.lang.String): java.util.List[QualityAssertion] = {
    val rowKey = occurrenceDAO.getRowKeyFromUuid(uuid).getOrElse(uuid);
    occurrenceDAO.getSystemAssertions(rowKey).filter(_.qaStatus == 0).asJava
  }

  /**
   * Retrieve the system supplied systemAssertions.
   * 
   * A user can supply either a uuid or rowKey
   */
  def getAllSystemAssertions(uuid: java.lang.String): java.util.Map[String,java.util.List[QualityAssertion]] = {
    //system assertions are handled using row keys - this is unlike user assertions.
    val rowKey = occurrenceDAO.getRowKeyFromUuid(uuid).getOrElse(uuid);
    val list = occurrenceDAO.getSystemAssertions(rowKey)
    val unchecked = AssertionCodes.getMissingCodes((list.map(it=>AssertionCodes.getByCode(it.code).getOrElse(null))).toSet)

    ((list ++ unchecked.map(it => QualityAssertion(it, 3))).groupBy {
      case i if (i.qaStatus == 0)=> {
        AssertionCodes.getByCode(i.code).getOrElse(AssertionCodes.GEOSPATIAL_ISSUE).category match{
          case ErrorCodeCategory.Error => "failed"
          case code:String => code.toString.toLowerCase
        }
      }
      case i if i.qaStatus == 1 => "passed"
      case _ => "unchecked"
    }).mapValues(_.asJava).asJava
  }

  /**
   * Retrieve the user supplied systemAssertions.
   */
  def getUserAssertion(uuid: java.lang.String, assertionUuid: java.lang.String): QualityAssertion = {
    val rowKey = occurrenceDAO.getRowKeyFromUuid(uuid).getOrElse(uuid)
    occurrenceDAO.getUserAssertions(rowKey).find(ass => { ass.uuid == assertionUuid }).getOrElse(null)
  }

  /**
   * Retrieve the user supplied systemAssertions.
   */
  def getUserAssertions(uuid: java.lang.String): java.util.List[QualityAssertion] = {
    val rowKey = occurrenceDAO.getRowKeyFromUuid(uuid).getOrElse(uuid);
    occurrenceDAO.getUserAssertions(rowKey).asJava
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
      throw new Exception("In read only model. Please try again later")
    }
  }

  /**
   * Adds the supplied list of qa's to the data store and reindexes to allow changes to be available in the search
   * @param assertionMap
   */
  def addUserAssertions(assertionMap:java.util.Map[String, QualityAssertion]) {
    if (!readOnly){
        val arrayBuffer = new ArrayBuffer[String]()
        var count=0
         assertionMap.foreach({
           case (uuid, qa) => {
             count+=1
             //apply the assertion and add to the reindex list
             val rowKey = occurrenceDAO.getRowKeyFromUuid(uuid).getOrElse(uuid)
             occurrenceDAO.addUserAssertion(rowKey, qa)
             arrayBuffer += rowKey
           }})
        // now reindex
        IndexRecords.indexList(arrayBuffer.toList)
      logger.debug("Added " + count + " user assertions in bulk")
    } else {
      throw new Exception("In read only model. Please try again later")
    }
  }
  
  def addValidationRule(validationRule:ValidationRule) = validationRuleDAO.upsert(validationRule)
  
  def getValidationRule(uuid:java.lang.String):ValidationRule =
    validationRuleDAO.get(uuid).getOrElse(null)
  
  def getValidationRules(ids:Array[String]):Array[ValidationRule] =
    validationRuleDAO.get(ids.toList).toArray[ValidationRule]

  def getValidationRules():Array[ValidationRule] =
    validationRuleDAO.list.toArray[ValidationRule]

  /**
   * Applies the supplied query assertion to the records.
   */
  def applyValidationRule(uuid:java.lang.String) = new ValidationRuleRunner().applySingle(uuid)
  
  def deleteValidationRule(id:java.lang.String, date:java.util.Date) = validationRuleDAO.delete(id, date)

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
      throw new Exception("In read only model. Please try again later")
    }
  }
  /**
   * Puts biocache store into readonly model.
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
    } catch {
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

  def reindexRange(startKey:java.lang.String, endKey:java.lang.String){
    if(startKey!=null && endKey !=null)
      IndexRecords.index(Some(startKey), Some(endKey), None, false, false, None)
    else
      throw new Exception("Start and end key must be supplied")
  }

  /**
   * Indexes a dataResource from a specific date
   *
   * @param dataResource the resource to index
   */
  def index(dataResource:java.lang.String) =
    IndexRecords.index(None, None, Some(dataResource), false, false, None)

  /**
   * Index a resource, indexing custom fields
   *
   * @param dataResource the resource to index
   * @param customIndexFields the additional fields to index on top of the default set of fields
   * @param callback a callback used for monitoring the process
   */
  def index(dataResource:java.lang.String, customIndexFields:Array[String], callback:ObserverCallback = null) = {
    IndexRecords.index(None, None, Some(dataResource), false, false, None,
      miscIndexProperties = customIndexFields, callback = callback)
    storeCustomIndexFields(dataResource,customIndexFields)
  }

  /**
   * Run the sampling for this dataset
   * @param dataResourceUid
   */
  def sample(dataResourceUid:java.lang.String) = Sampling.sampleDataResource(dataResourceUid)

  /**
   * Run the sampling for this dataset
   * @param dataResourceUid
   */
  def sample(dataResourceUid:java.lang.String, callback:IntersectCallback) =
    Sampling.sampleDataResource(dataResourceUid, callback)

  /**
   * Process records for the supplied resource
   * @param dataResourceUid
   * @param threads
   */
  def process(dataResourceUid:java.lang.String, threads:Int = 1, callback:ObserverCallback = null) = {
    ProcessWithActors.processRecords(1, None , Some(dataResourceUid), callback=callback)
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
  def retrieveSpeciesGroups: java.util.List[SpeciesGroup] = SpeciesGroups.groups.asJava
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

  def getJackKnifeOutliersFor(guid:String) : java.util.List[(String,java.util.List[SampledRecord])] = outlierStatsDAO.getJackKnifeOutliersFor(guid)

  def getJackKnifeRecordDetailsFor(uuid:String) : java.util.List[RecordJackKnifeStats] = outlierStatsDAO.getJackKnifeRecordDetailsFor(uuid)

  def getDuplicateDetails(uuid:String):DuplicateRecordDetails = duplicateDAO.getDuplicateInfo(uuid).getOrElse(new DuplicateRecordDetails())
  
  /**
   * Returns a list of record uuids that have been deleted since the supplied date inclusive
   */
  def getDeletedRecords(date:java.util.Date):Array[String] = deletedRecordDAO.getUuidsForDeletedRecords(org.apache.commons.lang.time.DateFormatUtils.format(date, "yyyy-MM-dd"))

  def storeCustomIndexFields(tempUid:String, customIndexFields:Array[String]){
    Config.persistenceManager.put(tempUid, "upload", "customIndexFields", Json.toJSON(customIndexFields.map(v=> if(v.endsWith("_i") || v.endsWith("_d")) v else v + "_s")))
  }

  def retrieveCustomIndexFields(tempUid:String) : Array[String] = {
    try {
      val s = Config.persistenceManager.get(tempUid, "upload", "customIndexFields")
      Json.toStringArray(s.getOrElse("[]"))
    } catch {
      case _:Exception => Array()
    }
  }
}

/**
 * A trait to implement by java classes to process occurrence records.
 */
trait ObserverCallback {
  def progressMessage(recordCount: Int)
}

/**
 * A trait to implement by java classes to process occurrence records.
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