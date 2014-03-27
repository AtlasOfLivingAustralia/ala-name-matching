package au.org.ala.biocache.dao

import scala.collection.JavaConversions
import org.slf4j.LoggerFactory
import com.google.inject.Inject
import au.org.ala.biocache._
import java.io.OutputStream
import scala.collection.mutable.{ListBuffer, ArrayBuffer}
import scala.Some
import au.org.ala.biocache.model._
import au.org.ala.biocache.index.{IndexFields, IndexDAO}
import au.org.ala.biocache.load.{MediaStore, FullRecordMapper}
import au.org.ala.biocache.persistence.PersistenceManager
import au.org.ala.biocache.vocab.{AssertionCodes, ErrorCode}
import scala.Some
import au.org.ala.biocache.vocab.ErrorCode
import au.org.ala.biocache.util.Json
import au.org.ala.biocache.processor.Processors

/**
 * A DAO for accessing occurrences.
 */
class OccurrenceDAOImpl extends OccurrenceDAO {

  import JavaConversions._

  protected val logger = LoggerFactory.getLogger("OccurrenceDAO")
  @Inject
  var persistenceManager: PersistenceManager = _
  @Inject
  var indexDAO: IndexDAO = _

  val elpattern = """el[0-9]+""".r
  val clpattern = """cl[0-9]+""".r

  /**
   * Gets the map for a record based on searching the index for new and old ids
   */
  def getMapFromIndex(value:String):Option[Map[String,String]]={
    persistenceManager.getByIndex(value, entityName, "uuid") match {
     case None => persistenceManager.getByIndex(value, entityName, "portalId")  //legacy record ID
     case Some(map) => Some(map)
    }
  }

  /**
   * Get an occurrence with UUID
   *
   * @param uuid
   * @return
   */
  def getByUuid(uuid: String, includeSensitive:Boolean): Option[FullRecord] = {
    getByUuid(uuid, Raw, includeSensitive)
  }
  /**
   * Get an occurrence with rowKey
   */
  def getByRowKey(rowKey:String, includeSensitive:Boolean): Option[FullRecord] ={
    getByRowKey(rowKey, Raw, includeSensitive)
  }

  /**
   * Get all versions of the occurrence with UUID
   *
   * @param uuid
   * @return
   */
  def getAllVersionsByUuid(uuid: String, includeSensitive:Boolean=false): Option[Array[FullRecord]] = {
    //get the rowKey for the uuid
    val rowKey = getRowKeyFromUuid(uuid)
    if(rowKey.isDefined){
      getAllVersionsByRowKey(rowKey.get, includeSensitive)
    } else {
      None
    }
  }

 /**
  * Get all the versions based on a row key
  */
  def getAllVersionsByRowKey(rowKey:String, includeSensitive:Boolean=false): Option[Array[FullRecord]] ={
    val map = persistenceManager.get(rowKey, entityName)
    if(map.isEmpty){
      None
    } else {
      // the versions of the record
      val raw = FullRecordMapper.createFullRecord(rowKey, map.get, Raw)
      val processed = FullRecordMapper.createFullRecord(rowKey, map.get, Processed)
      val consensus = FullRecordMapper.createFullRecord(rowKey, map.get, Consensus)
      if(includeSensitive && raw.occurrence.originalSensitiveValues != null){
        FullRecordMapper.mapPropertiesToObject(raw, raw.occurrence.originalSensitiveValues)
      }
      //pass all version to the procedure, wrapped in the Option
      Some(Array(raw, processed, consensus))
    }
  }

  def getRawProcessedByRowKey(rowKey:String) :Option[Array[FullRecord]] ={
    val map = persistenceManager.get(rowKey, entityName)
    if(map.isEmpty){
      None
    } else {
      // the versions of the record
      val raw = FullRecordMapper.createFullRecord(rowKey, map.get, Raw)
      val processed = FullRecordMapper.createFullRecord(rowKey, map.get, Processed)
      Some(Array(raw, processed))
    }
  }

  /**
   * Get the supplied version based on a rowKey
   */
  def getByRowKey(rowKey:String, version:Version, includeSensitive:Boolean=false) :Option[FullRecord] ={
    val propertyMap = persistenceManager.get(rowKey, entityName)
    if (propertyMap.isEmpty) {
      None
    } else {
      val record = FullRecordMapper.createFullRecord(rowKey, propertyMap.get, version)
      if(includeSensitive && record.occurrence.originalSensitiveValues != null && version == Versions.RAW)
        FullRecordMapper.mapPropertiesToObject(record, record.occurrence.originalSensitiveValues)
      Some(record)
    }
  }

  /**
   * Get an occurrence, specifying the version of the occurrence.
   */
  def getByUuid(uuid: String, version: Version, includeSensitive:Boolean=false): Option[FullRecord] = {
    //get the row key from the supplied uuid
    val rowKey = getRowKeyFromUuid(uuid)
    if(rowKey.isDefined){
      getByRowKey(rowKey.get, version,includeSensitive)
    } else {
      None
    }
  }

  /**
   * Create or retrieve the UUID for this record. The uniqueID should be a
   * has of properties that provides a unique ID for the record within
   * the dataset.
   *
   * This method has been changed so that it queries the existing occ record
   * to see if it exists.  We wish for uuids to be persistent between loads.
   *
   * Returns uuid and true when a new uid was created
   */
  def createOrRetrieveUuid(uniqueID: String): (String,Boolean) = {
    //look up by index
    val recordUUID = getUUIDForUniqueID(uniqueID)
    if (recordUUID.isEmpty) {
      val newUuid = createUuid
      //The uuid will be added when the record is inserted
      //persistenceManager.put(uniqueID, "dr", "uuid", newUuid)
      (newUuid, true)
    } else {
      (recordUUID.get,false)
    }
  }

  def getUUIDForUniqueID(uniqueID: String) = persistenceManager.get(uniqueID, "occ", "uuid")

  /**
   * Writes the supplied field values to the writer.  The Writer specifies the format in which the record is
   * written.
   */
  def writeToRecordWriter(writer:RecordWriter, rowKeys: Array[String], fields: Array[String], qaFields:Array[String], includeSensitive:Boolean=false){
    //get the codes for the qa fields that need to be included in the download
    //TODO fix this in case the value can't be found
    val mfields = fields.toBuffer
    val codes = qaFields.map(value=>AssertionCodes.getByName(value).get.getCode)
    val firstEL = fields.find(value => {elpattern.findFirstIn(value).nonEmpty})
    val firstCL = fields.find(value => {clpattern.findFirstIn(value).nonEmpty})
    val firstMisc = fields.find(value =>{IndexFields.storeMiscFields.contains(value)})
    if(firstEL.isDefined)
      mfields += "el.p"
    if(firstCL.isDefined)
      mfields += "cl.p"
    if(includeSensitive)
      mfields += "originalSensitiveValues"
    if(firstMisc.isDefined)
      mfields += FullRecordMapper.miscPropertiesColumn
    mfields ++=  FullRecordMapper.qaFields

    persistenceManager.selectRows(rowKeys, entityName, mfields , { fieldMap =>
      val array = scala.collection.mutable.ArrayBuffer[String]()
      val sensitiveMap:scala.collection.Map[String,String] = if(includeSensitive) Json.toStringMap(fieldMap.getOrElse("originalSensitiveValues", "{}")) else Map()
      val elMap = if(firstEL.isDefined) Json.toStringMap(fieldMap.getOrElse("el.p", "{}")) else Map[String,String]()
      val clMap = if(firstCL.isDefined) Json.toStringMap(fieldMap.getOrElse("cl.p", "{}")) else Map[String,String]()
      val miscMap = if(firstMisc.isDefined)Json.toStringMap(fieldMap.getOrElse(FullRecordMapper.miscPropertiesColumn, "{}")) else Map[String,String]()
      fields.foreach(field => {
        val fieldValue = field match{
          case a if elpattern.findFirstIn(a).nonEmpty => elMap.getOrElse(a, "")
          case a if clpattern.findFirstIn(a).nonEmpty => clMap.getOrElse(a, "")
          case a if firstMisc.isDefined && IndexFields.storeMiscFields.contains(a) => miscMap.getOrElse(a, "")
          case _ => if(includeSensitive) sensitiveMap.getOrElse(field, getHackValue(field,fieldMap)) else getHackValue(field,fieldMap)
        }
        // if(includeSensitive) sensitiveMap.getOrElse(field, getHackValue(field,fieldMap))else getHackValue(field,fieldMap)
        //Create a MS Excel compliant CSV file thus field with delimiters are quoted and embedded quotes are escaped
        array += fieldValue
      })
      //now handle the QA fields
      val failedCodes = getErrorCodes(fieldMap);
      //work way through the codes and add to output
      codes.foreach(code => {
          array += (failedCodes.contains(code)).toString
      })
      writer.write(array.toArray)
    })
  }

  /**
   * Write to stream in a delimited format (CSV).
   */
  def writeToStream(outputStream: OutputStream, fieldDelimiter: String, recordDelimiter: String,
                    rowKeys: Array[String], fields: Array[String], qaFields:Array[String], includeSensitive:Boolean=false) {
    //get the codes for the qa fields that need to be included in the download
    //TODO fix this in case the value can't be found
    val mfields = scala.collection.mutable.ArrayBuffer[String]()
    mfields ++= fields
    val codes = qaFields.map(value=>AssertionCodes.getByName(value).get.getCode)
    val firstEL = fields.find(value => {elpattern.findFirstIn(value).nonEmpty})
    val firstCL = fields.find(value => {clpattern.findFirstIn(value).nonEmpty})
    var extraFields = Array[String]()
    if(firstEL.isDefined)
      mfields += "el.p"
    if(firstCL.isDefined)
      mfields += "cl.p"
    if(includeSensitive)
      mfields += "originalSensitiveValues"
    mfields ++=  FullRecordMapper.qaFields

    //val fieldsToQuery = if(includeSensitive) fields ++ FullRecordMapper.qaFields ++ Array("originalSensitiveValues") else fields ++ FullRecordMapper.qaFields
    persistenceManager.selectRows(rowKeys, entityName, mfields, { fieldMap =>
      val sensitiveMap:scala.collection.Map[String,String] = if(includeSensitive) Json.toStringMap(fieldMap.getOrElse("originalSensitiveValues", "{}")) else Map()
      val elMap = if(firstEL.isDefined) Json.toStringMap(fieldMap.getOrElse("el.p", "{}")) else Map[String,String]()
      val clMap = if(firstCL.isDefined)Json.toStringMap(fieldMap.getOrElse("cl.p", "{}")) else Map[String,String]()
      fields.foreach (field => {
        val fieldValue = field match {
          case a if elpattern.findFirstIn(a).nonEmpty => elMap.getOrElse(a, "")
          case a if clpattern.findFirstIn(a).nonEmpty => clMap.getOrElse(a, "")
          case _ => if(includeSensitive) sensitiveMap.getOrElse(field, getHackValue(field,fieldMap))else getHackValue(field,fieldMap)
        }
         // if(includeSensitive) sensitiveMap.getOrElse(field, getHackValue(field,fieldMap))else getHackValue(field,fieldMap)
        //Create a MS Excel compliant CSV file thus field with delimiters are quoted and embedded quotes are escaped

        if (fieldValue.contains(fieldDelimiter) || fieldValue.contains(recordDelimiter) || fieldValue.contains("\""))
          outputStream.write(("\"" + fieldValue.replaceAll("\"", "\"\"") + "\"").getBytes)
        else
          outputStream.write(fieldValue.getBytes)
        outputStream.write(fieldDelimiter.getBytes)
      })
      //now handle the QA fields
      val failedCodes = getErrorCodes(fieldMap)
      //work way through the codes and add to output
      codes.foreach(code => {
          outputStream.write((failedCodes.contains(code)).toString.getBytes)
          outputStream.write(fieldDelimiter.getBytes)
      })
      outputStream.write(recordDelimiter.getBytes)
    })
  }
  /**
   * A temporary HACK to get some of the values for the download that are NOT stored directly
   * TODO REMOVE this Hack
   */
  def getHackValue(field:String, map:Map[String,String]):String ={
    if(FullRecordMapper.geospatialDecisionColumn == field){
      if("false" == map.getOrElse(field,""))
        "Spatially suspect"
      else
        "Spatially valid"
    }
    else if("outlierForLayers.p" == field){
      val out = map.getOrElse("outlierForLayers.p", "[]")
      Json.toStringArray(out).length.toString
    }
    else
      map.getOrElse(field,"")
  }

  def getErrorCodes(map:Map[String, String]):Array[Integer]={
    val array:Array[List[Integer]] = FullRecordMapper.qaFields.filter(field => map.get(field).getOrElse("[]") != "[]").toArray.map(field => {
      Json.toListWithGeneric(map.get(field).get,classOf[java.lang.Integer])
    }).asInstanceOf[Array[List[Integer]]]
    if(!array.isEmpty)
      return array.reduceLeft(_++_).toArray
    return Array()
  }

  /**
   * Iterate over all occurrences, passing all versions of FullRecord
   * to the supplied function.
   * Function returns a boolean indicating if the paging should continue.
   *
   * @param proc, the function to execute.
   * @param startKey, The row key of the occurrence at which to start the paging
   * @param endKey, The row key of the occurrence at which to end the paging
   */
  def pageOverAllVersions(proc: ((Option[Array[FullRecord]]) => Boolean),startKey:String="", endKey:String="", pageSize: Int = 1000) {
    persistenceManager.pageOverAll(entityName, (guid, map) => {
      //retrieve all versions
      val raw = FullRecordMapper.createFullRecord(guid, map, Raw)
      val processed = FullRecordMapper.createFullRecord(guid, map, Processed)
      val consensus = FullRecordMapper.createFullRecord(guid, map, Consensus)
      //pass all version to the procedure, wrapped in the Option
      proc(Some(Array(raw, processed, consensus)))
    },startKey, endKey, pageSize)
  }

  /**
   * Iterate over all occurrences, passing the objects to a function.
   * Function returns a boolean indicating if the paging should continue.
   *
   * @param proc, the function to execute.
   * @param startKey, The row key of the occurrence at which to start the paging
   * @param endKey, The row key of the occurrence at which to end the paging
   */
  def pageOverAll(version: Version, proc: ((Option[FullRecord]) => Boolean),startKey:String="", endKey:String="", pageSize: Int = 1000) {
    persistenceManager.pageOverAll(entityName, (guid, map) => {
      //retrieve all versions
      val fullRecord = FullRecordMapper.createFullRecord(guid, map, version)
      //pass all version to the procedure, wrapped in the Option
      proc(Some(fullRecord))
    },startKey, endKey, pageSize)
  }

  /**
   * Iterate over all occurrences, passing the objects to a function.
   * Function returns a boolean indicating if the paging should continue.
   *
   * @param proc, the function to execute.
   * @param startKey, The row key of the occurrence at which to start the paging
   * @param endKey, The row key of the occurrence at which to end the paging
   */
  def pageOverRawProcessed(proc: (Option[(FullRecord, FullRecord)] => Boolean),startKey:String="", endKey:String="", pageSize: Int = 1000) {
    persistenceManager.pageOverAll(entityName, (guid, map) => {
      //retrieve all versions
      val raw = FullRecordMapper.createFullRecord(guid, map, Versions.RAW)
      val processed = FullRecordMapper.createFullRecord(guid, map, Versions.PROCESSED)
      //pass all version to the procedure, wrapped in the Option
      proc(Some(raw, processed))
    },startKey,endKey, pageSize)
  }

  /**
   * Iterate over the undeleted occurrences. Prevents overhead of processing records that are deleted. Also it is quicker to get a smaller
   * number of columns.  Thus only get all the columns for record that need to be processed.
   *
   * The shouldProcess function should take the map and determine based on conditions whether or not to retrieve the complete record
   *
   */
  def conditionalPageOverRawProcessed(proc: (Option[(FullRecord, FullRecord)] => Boolean),
                                      condition:(Map[String,String]=>Boolean),columnsToRetrieve:Array[String],
                                      startKey:String="", endKey:String="", pageSize: Int = 1000){
    val columns = columnsToRetrieve ++ Array("uuid","rowKey")
    persistenceManager.pageOverSelect(entityName, (guid, map)=>{
      //val deleted = map.getOrElse(FullRecordMapper.deletedColumn,"false")
      //if(deleted.equals("false")){
      if(condition(map)){
        if(map.contains("rowKey")){
          val recordmap = persistenceManager.get(map.get("rowKey").get,entityName)
          if(!recordmap.isEmpty){
            val raw = FullRecordMapper.createFullRecord(guid, recordmap.get, Versions.RAW)
            val processed = FullRecordMapper.createFullRecord(guid, recordmap.get, Versions.PROCESSED)
            //pass all version to the procedure, wrapped in the Option
            proc(Some(raw, processed))
          }
        } else {
          logger.info("Unable to page over records : " +guid)
        }
      }
      true
    }, startKey, endKey,pageSize, columns: _*)
  }

  /**
   * Update the version of the occurrence record.
   */
  def addRawOccurrence(fr:FullRecord) {
    //process the record
    val properties = FullRecordMapper.fullRecord2Map(fr, Versions.RAW)
    //commit
    persistenceManager.put(fr.rowKey, entityName, properties.toMap)
  }

  /**
   * Update the version of the occurrence record.
   */
  def addRawOccurrenceBatch(fullRecords: Array[FullRecord]) {
    var batch = scala.collection.mutable.Map[String, Map[String, String]]()
    fullRecords.foreach(fr  => {
      //download the media in associatedMedia?????
      downloadMedia(fr)
      //process the record
      var properties = FullRecordMapper.fullRecord2Map(fr, Versions.RAW)
      batch.put(fr.rowKey, properties.toMap)
    })
    //commit
    persistenceManager.putBatch(entityName, batch.toMap)
  }

  /**
   * Download the associated media and update the references in the FR.
   * Returns true if media has been downloaded.
   *
   * @param fr
   */
  def downloadMedia(fr:FullRecord) : Boolean = {
    if (fr.occurrence.associatedMedia != null){
      val filesToImport = fr.occurrence.associatedMedia.split(";")
      val associatedMediaBuffer = new ArrayBuffer[String]
      filesToImport.foreach(fileToStore => {
        val filePath = MediaStore.save(fr.uuid, fr.attribution.dataResourceUid, fileToStore)
        if(!filePath.isEmpty) associatedMediaBuffer += filePath.get
      })
      fr.occurrence.associatedMedia = associatedMediaBuffer.toArray.mkString(";")
      true
    } else {
      false
    }
  }

  /**
   * Update the version of the occurrence record.
   */
  def updateOccurrence(rowKey: String, fullRecord: FullRecord, version: Version) {
    updateOccurrence(rowKey, fullRecord, None, version)
  }

  /**
   * Update the occurrence with the supplied record, setting the correct version
   */
  def updateOccurrence(rowKey: String, fullRecord: FullRecord, assertions: Option[Map[String,Array[QualityAssertion]]], version: Version) {

    //construct a map of properties to write
    val properties = FullRecordMapper.fullRecord2Map(fullRecord, version)

    if (!assertions.isEmpty) {
      properties ++= convertAssertionsToMap(rowKey,assertions.get, fullRecord.userVerified)
      updateSystemAssertions(rowKey, assertions.get)
    }

    //commit to cassandra
    persistenceManager.put(rowKey, entityName, properties.toMap)
  }

  /**
   * Update the occurrence with the supplied record, setting the correct version
   */
  def updateOccurrence(rowKey: String, oldRecord: FullRecord, newRecord: FullRecord,
                       assertions: Option[Map[String,Array[QualityAssertion]]], version: Version) {

    //construct a map of properties to write
    val oldproperties = FullRecordMapper.fullRecord2Map(oldRecord, version)
    val properties = FullRecordMapper.fullRecord2Map(newRecord, version)

    //only write changes.........
    var propertiesToPersist = properties.filter({
      case (key, value) => {
        if (oldproperties.contains(key)) {
          val oldValue = oldproperties.get(key).get
          oldValue != value
        } else {
          true
        }
      }
    })

    //check for deleted properties
    val deletedProperties = oldproperties.filter({
      case (key, value) => !properties.contains(key)
    })

    propertiesToPersist ++= deletedProperties.map({
      case (key, value) => key -> ""
    })

    val timeCol = FullRecordMapper.markNameBasedOnVersion(FullRecordMapper.alaModifiedColumn, version)

    if(!assertions.isEmpty){
      initAssertions(newRecord, assertions.get)
      //only add  the assertions if they are different OR the properties to persist contain more than the last modified time stamp
      if((oldRecord.assertions.toSet != newRecord.assertions.toSet) || !(propertiesToPersist.size == 1 && propertiesToPersist.getOrElse(timeCol, "") != "")){
        //only add the assertions if they have changed since the last time or the number of records to persist >1
        propertiesToPersist ++= convertAssertionsToMap(rowKey,assertions.get,newRecord.userVerified)
        updateSystemAssertions(rowKey, assertions.get)
      }
    }

    //commit to cassandra if changes exist - changes exist if the properties to persist contain more info than the lastModifedTime
    if(!propertiesToPersist.isEmpty && !(propertiesToPersist.size == 1 && propertiesToPersist.getOrElse(timeCol, "") != "")){
      persistenceManager.put(rowKey, entityName, propertiesToPersist.toMap)
    }
  }

  private def initAssertions(processed:FullRecord, assertions:Map[String, Array[QualityAssertion]]){
    assertions.values.foreach(array => {
      val failedQas = array.filter(_.qaStatus==0).map(_.getName)
      processed.assertions = processed.assertions  ++ failedQas
    })
  }

  def doesListContainCode(list:List[QualityAssertion], code:Int) = !list.filter(ua => ua.code ==code).isEmpty

  /**
   * Convert the assertions to a map
   */
  def convertAssertionsToMap(rowKey:String, systemAssertions: Map[String,Array[QualityAssertion]], verified:Boolean): Map[String, String] = {
    //if supplied, update the assertions
    val properties = new collection.mutable.ListMap[String, String]

    if(verified){
        //kosher fields are always set to true for verified BUT we still want to store and report the QA's that failed
        properties += (FullRecordMapper.geospatialDecisionColumn -> "true")
        properties += (FullRecordMapper.taxonomicDecisionColumn -> "true")
    }

    val userAssertions = getUserAssertions(rowKey)
    val falseUserAssertions = userAssertions.filter(qa => qa.qaStatus == 1)//userAssertions.filter(a => !a.problemAsserted)
    //true user assertions are assertions that have not been proven false by another user
    val trueUserAssertions = userAssertions.filter(a => a.qaStatus == 0 && !doesListContainCode(falseUserAssertions,a.code))

    //for each qa type get the list of QA's that failed
    val assertionsDeleted = ListBuffer[QualityAssertion]() // stores the assertions that should not be considered for the kosher fields
    for(name <- systemAssertions.keySet){
      val assertions = systemAssertions.get(name).getOrElse(Array[QualityAssertion]())
      val failedass = new ArrayBuffer[Int]
      assertions.foreach(qa => {
        //only add if it has failed
        if(qa.getQaStatus == 0){
          //check to see if a user assertion counteracts this code
          if(!doesListContainCode(falseUserAssertions,qa.code))
            failedass.add(qa.code)
          else
            assertionsDeleted += qa
        }
      })
      //add the "true" user assertions to the arrays
      //filter the list based on the name of the phase
      //TODO fix the phase based range stuff
      val ua2Add = trueUserAssertions.filter(a =>
        name match {
          case "loc"   => a.code >= AssertionCodes.geospatialBounds._1 && a.code < AssertionCodes.geospatialBounds._2
          case "class" => a.code >= AssertionCodes.taxonomicBounds._1 && a.code < AssertionCodes.taxonomicBounds._2
          case "event" => a.code >= AssertionCodes.temporalBounds._1 && a.code < AssertionCodes.temporalBounds._2
          case _       => false
      })
      val extraAssertions = ListBuffer[QualityAssertion]()
      ua2Add.foreach(qa =>if(!failedass.contains(qa.code)){
        failedass.add(qa.code)
        extraAssertions += qa
      })

      properties += (FullRecordMapper.markAsQualityAssertion(name) -> Json.toJSONWithGeneric(failedass.toList))
      if(!verified){
        if(name == FullRecordMapper.geospatialQa){
          properties += (FullRecordMapper.geospatialDecisionColumn -> AssertionCodes.isGeospatiallyKosher(failedass.toArray).toString)
        } else if(name == FullRecordMapper.taxonomicalQa){
          properties += (FullRecordMapper.taxonomicDecisionColumn -> AssertionCodes.isTaxonomicallyKosher(failedass.toArray).toString)
        }
      }
    }
    properties.toMap
  }

  /**
   * Update an occurrence entity. E.g. Occurrence, Classification, Taxon
   *
   *  IS this being used?
   *
   * @param anObject
   */
  def updateOccurrence(rowKey: String, anObject: AnyRef, version: Version) {
    val map = FullRecordMapper.mapObjectToProperties(anObject, version)
    persistenceManager.put(rowKey, entityName, map)
  }

  /**
   * Adds a quality assertion to the row with the supplied UUID.
   *
   * @param qualityAssertion
   */
  def addSystemAssertion(rowKey: String, qualityAssertion: QualityAssertion,replaceExistCode:Boolean=false,checkExisting:Boolean=true) {
    val baseAssertions = if(replaceExistCode) (getSystemAssertions(rowKey).filterNot(_.code == qualityAssertion.code) :+ qualityAssertion) else (getSystemAssertions(rowKey) :+ qualityAssertion)
    val systemAssertions =baseAssertions.groupBy(x => x.code).values.map( _.head).toList
    if(checkExisting){
      val userAssertions = getUserAssertions(rowKey)
      updateAssertionStatus(rowKey, qualityAssertion, systemAssertions, userAssertions)
    }
    persistenceManager.putList(rowKey, entityName, FullRecordMapper.qualityAssertionColumn, systemAssertions.toList, classOf[QualityAssertion], true)
  }

  /**
   * Remove system assertion, and update status.
   *
   * @param rowKey
   * @param assertionCode
   */
  def removeSystemAssertion(rowKey: String, assertionCode:ErrorCode){
    val systemAssertions = getSystemAssertions(rowKey)
    val newSystemAssertions = systemAssertions.filter(_.code != assertionCode.code)
    val userAssertions = getUserAssertions(rowKey)
    updateAssertionStatus(rowKey, QualityAssertion(assertionCode), systemAssertions, userAssertions)
    persistenceManager.putList(rowKey, entityName, FullRecordMapper.qualityAssertionColumn, newSystemAssertions.toList, classOf[QualityAssertion], true)
  }

  /**
   * Set the system systemAssertions for a record, overwriting existing systemAssertions
   * TODO change this so that it is updating the contents not replacing - will need this functionality when
   * particular processing phases can be run separately
   *
   * Please NOTE a verified record will still have a list of SystemAssertions that failed. But there will be no corresponding qa codes.
   */
  def updateSystemAssertions(rowKey: String, qualityAssertions: Map[String,Array[QualityAssertion]]) {
    var assertions = new ListBuffer[QualityAssertion] //getSystemAssertions(uuid)
    qualityAssertions.values.foreach(x => { assertions ++= x })
    persistenceManager.putList(rowKey, entityName, FullRecordMapper.qualityAssertionColumn,assertions.toList, classOf[QualityAssertion], true)
  }

  /**
   * Retrieve annotations for the supplied UUID.
   */
  def getSystemAssertions(rowKey: String): List[QualityAssertion] = {
    persistenceManager.getList(rowKey, entityName, FullRecordMapper.qualityAssertionColumn, classOf[QualityAssertion])
  }

  /**
   * Add a user supplied assertion - updating the status on the record.
   */
  def addUserAssertion(rowKey: String, qualityAssertion: QualityAssertion) {
    val qaRowKey = rowKey+ "|" +qualityAssertion.getUserId + "|" + qualityAssertion.getCode

    //TODO add the serialised record to the quality assertion for later stage processing
    val qualityAssertionProperties = FullRecordMapper.mapObjectToProperties(qualityAssertion)
    val record = this.getRawProcessedByRowKey(rowKey)

    if(!record.isEmpty){
      //preserve the raw record
      val qaMap = qualityAssertionProperties ++ Map("snapshot" -> Json.toJSON(record.get))
      persistenceManager.put(qaRowKey, qaEntityName, qaMap)
      val systemAssertions = getSystemAssertions(rowKey)
      val userAssertions = getUserAssertions(rowKey)
      updateAssertionStatus(rowKey, qualityAssertion, systemAssertions, userAssertions)
      //set the last user assertion date
      persistenceManager.put(rowKey, entityName, FullRecordMapper.lastUserAssertionDateColumn, qualityAssertion.created)
      //when the user assertion is verified need to add extra value
      if(AssertionCodes.isVerified(qualityAssertion)){
        persistenceManager.put(rowKey, entityName, FullRecordMapper.userVerifiedColumn, "true")
      }
    }
  }

  /**
   * Retrieve annotations for the supplied UUID.
   */
  def getUserAssertions(rowKey:String): List[QualityAssertion] ={
    val startKey = rowKey + "|"
    val endKey = startKey + "~"
    val userAssertions = new ArrayBuffer[QualityAssertion]
    //page over all the qa's that are for this record
    persistenceManager.pageOverAll(qaEntityName,(guid, map)=>{
      val qa = new QualityAssertion()
      FullRecordMapper.mapPropertiesToObject(qa, map)
      userAssertions += qa
      true
    },startKey, endKey, 1000)

    userAssertions.toList
  }

  /**
   * Retrieves a distinct list of user ids for the assertions
   */
  def getUserIdsForAssertions(rowKey: String):Set[String] ={
    val startKey = rowKey + "|"
    val endKey = startKey +"~"
    val userIds =  new ArrayBuffer[String]
    persistenceManager.pageOverSelect(qaEntityName, (guid, map) =>{
      val userId = map.get("userId")
      if(userId.isDefined)
        userIds += userId.get
      true
    }, startKey, endKey, 1000, "userId")
    userIds.toSet
  }

  /**
   * Delete a user supplied assertion
   */
  def deleteUserAssertion(rowKey: String, assertionUuid: String): Boolean = {

    logger.debug("Deleting assertion for : " + rowKey + " with assertion uuid : " + assertionUuid)

    val assertions = getUserAssertions(rowKey)
    if(assertions.isEmpty){
      //logger.warn("Unable to locate in index uuid: " + uuid)
      false
    }
    else {
      //get the assertion that is to be deleted
      val deletedAssertion = assertions.find(assertion => {
        assertion.uuid equals assertionUuid
      })

      if (!deletedAssertion.isEmpty) {

        //delete the assertion with the supplied UUID
        val updateAssertions = assertions.filter(qa => {
            !(qa.uuid equals assertionUuid)
        })

        //put the systemAssertions back - overwriting existing systemAssertions
        //persistenceManager.putList(rowKey, entityName, FullRecordMapper.userQualityAssertionColumn, updateAssertions, classOf[QualityAssertion], true)

        val assertionName = deletedAssertion.get.name
        //are there any matching systemAssertions for other users????
        val systemAssertions = getSystemAssertions(rowKey)
        //also delete it from the QA column family eventually we will not add it as a List to the occ column family
        val qaRowKey = rowKey + "|" + deletedAssertion.get.getUserId +"|" + deletedAssertion.get.getCode
        persistenceManager.delete(qaRowKey, qaEntityName)
        //update the assertion status
        updateAssertionStatus(rowKey, deletedAssertion.get, systemAssertions, updateAssertions)
        true
      } else {
        logger.warn("Unable to find assertion with UUID: " + assertionUuid)
        false
      }
    }
    false
  }

  private def getListOfCodes(rowKey:String,phase:String):List[Int]={
    //persistenceManager.getList(rowKey, entityName, FullRecordMapper.qualityAssertionColumn, classOf[QualityAssertion])
    persistenceManager.getList(rowKey, entityName, FullRecordMapper.markAsQualityAssertion(phase), classOf[Int])
  }

  /**
   * Update the assertion status using system and user systemAssertions.
   */
  def updateAssertionStatus(rowKey: String, assertion:QualityAssertion, systemAssertions: List[QualityAssertion], userAssertions: List[QualityAssertion]) {

    logger.debug("Updating the assertion status for : " + rowKey)

    //get the phase based on the error type
    val phase = Processors.getProcessorForError(assertion.code)
    logger.debug("Phase " + phase)

    //get existing values for the phase
    var listErrorCodes: Set[Int] = getListOfCodes(rowKey, phase).toSet
    logger.debug("Original: " + listErrorCodes)

    val assertionName = assertion.name
    val assertions = userAssertions.filter { _.name equals assertionName }
    val userVerified = userAssertions.filter( qa => qa.code == AssertionCodes.VERIFIED.code).size > 0

    //if the a user assertion has been set for the supplied QA we will set the status bases on user assertions
    if (!assertions.isEmpty) {
        //if a single user has decided that there is NO QA issue this takes precidence
      //val negativeAssertion = assertions.find(qa => !qa.problemAsserted)
      val negativeAssertion = assertions.find(qa => qa.qaStatus == 1)
      if (!negativeAssertion.isEmpty) {
        //need to remove this assertion from the error codes if it exists
        listErrorCodes = listErrorCodes - assertion.code
      } else {
        //at least one user has flagged this assertion so we need to add it
        listErrorCodes = listErrorCodes + assertion.code
      }
    } else if (!systemAssertions.isEmpty) {
      //check to see if a system assertion exists
      val matchingAssertion = systemAssertions.find { _.name equals assertionName }
      if (!matchingAssertion.isEmpty) {
        //this assertion has been set by the system
        val sysassertion = matchingAssertion.get
        listErrorCodes = listErrorCodes + sysassertion.code
      } else {
        //code needs to be removed
        listErrorCodes = listErrorCodes - assertion.code
      }
    } else {
      //there are no matching assertions in user or system thus remove this error code
      listErrorCodes = listErrorCodes - assertion.code
    }

    logger.debug("Final " + listErrorCodes)
    //update the list
    //persistenceManager.putList(rowKey, entityName, FullRecordMapper.qualityAssertionColumn,assertions.toList, classOf[QualityAssertion], true)
    persistenceManager.putList(rowKey, entityName, FullRecordMapper.markAsQualityAssertion(phase), listErrorCodes.toList, classOf[Int], true)

    //set the overall decision if necessary
    var properties = scala.collection.mutable.Map[String, String]()
    //need to update the user assertion flag in the occurrence record
    properties += (FullRecordMapper.userQualityAssertionColumn -> (userAssertions.size>0).toString)
    if(userVerified){
      properties += (FullRecordMapper.geospatialDecisionColumn -> "true")
      properties += (FullRecordMapper.taxonomicDecisionColumn -> "true")
    } else if(phase == FullRecordMapper.geospatialQa){
      properties += (FullRecordMapper.geospatialDecisionColumn -> AssertionCodes.isGeospatiallyKosher(listErrorCodes.toArray).toString)
    } else if(phase == FullRecordMapper.taxonomicalQa){
      properties += (FullRecordMapper.taxonomicDecisionColumn -> AssertionCodes.isTaxonomicallyKosher(listErrorCodes.toArray).toString)
    }
    if(!properties.isEmpty){
      logger.debug("Updating the assertion status for : " + rowKey + properties)
      persistenceManager.put(rowKey, entityName, properties.toMap)
    }
  }

  /**
   * Set this record to deleted.
   */
  def setDeleted(rowKey: String, del: Boolean,dateTime:Option[String]=None) = {
    if(dateTime.isDefined){
      val values = Map(FullRecordMapper.deletedColumn -> del.toString, FullRecordMapper.dateDeletedColumn -> dateTime.get)
      persistenceManager.put(rowKey, entityName, values)
    } else {
      persistenceManager.put(rowKey, entityName, FullRecordMapper.deletedColumn, del.toString)
    }
    //remove the datedeleted column if the records becomes undeleted...
    if(!del)
      persistenceManager.deleteColumns(rowKey,entityName, FullRecordMapper.dateDeletedColumn)
  }

  /**
   * Returns the rowKey based on the supplied uuid
   */
  def getRowKeyFromUuid(uuid:String):Option[String] = {
    def rk = getRowKeyFromUuidDB(uuid)
    if(rk.isDefined){
      rk
    } else {
      //work around so that index is searched if it can't be found in the cassandra secondary index.
      getRowKeyFromUuidIndex(uuid)
    }
  }

  def getRowKeyFromUuidDB(uuid:String):Option[String] = persistenceManager.getByIndex(uuid, entityName, "uuid", "rowKey")

  def getRowKeyFromUuidIndex(uuid:String):Option[String] = {
    if(uuid.startsWith("dr")){
      Some(uuid)
    } else {
      val list = Config.indexDAO.getRowKeysForQuery("id:"+uuid,1)
      if(list.isDefined){
        list.get.headOption
      } else {
        None
      }
    }
  }

  /**
   * Should be possible to factor this out
   */
  def reIndex(rowKey: String) {
    logger.debug("Reindexing rowKey: " + rowKey)
    //val map = persistenceManager.getByIndex(uuid, entityName, "uuid")
    val map = persistenceManager.get(rowKey, entityName)
    //index from the map - this should be more efficient
    if(map.isEmpty){
      logger.debug("Unable to reindex : " + rowKey)
    } else {
      indexDAO.indexFromMap(rowKey, map.get, batch=false)
    }
  }
  /**
   * Deletes a record from the data store optionally removing from the index and logging it.
   *
   * @param rowKey The id of the record to be deleted
   * @param removeFromIndex true when the recored should be removed from the index
   * @param logDeleted true when the record should be inserted into the dellog table before removal.
   */
  def delete(rowKey: String, removeFromIndex:Boolean=true, logDeleted:Boolean=false)={
    if(logDeleted){
      //log the deleted record to history
      //get the map version of the record
      val map = persistenceManager.get(rowKey, entityName)
      if(map.isDefined){
        val stringValue = Json.toJSON(map.get)
        val uuid = map.get.getOrElse("uuid","")
        val values = Map(rowKey -> uuid, "value|"+rowKey -> stringValue)
        val deletedKey = org.apache.commons.lang.time.DateFormatUtils.format(new java.util.Date, "yyyy-MM-dd")
        persistenceManager.put(deletedKey,"dellog",values)
      }
    }
    //delete from the data store
    persistenceManager.delete(rowKey, entityName)
    //delete from the index
    if(removeFromIndex)
      indexDAO.removeFromIndex("row_key", rowKey)
  }
}
