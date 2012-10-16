package au.org.ala.biocache

import com.google.inject.Inject
import java.io.{File, OutputStream}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.codehaus.jackson.map.ObjectMapper
import au.org.ala.util.ReflectBean
import scala.collection.JavaConversions
import java.util.UUID
import org.slf4j.LoggerFactory
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import au.org.ala.biocache.outliers.JackKnifeStats
import au.org.ala.biocache.outliers.RecordJackKnifeStats
import au.org.ala.util.DuplicateRecordDetails

trait OccurrenceDAO {

    val entityName = "occ"
     
    val qaEntityName ="qa"

    def setDeleted(rowKey: String, del: Boolean, dateTime:Option[String]=None): Unit

    def getRowKeyFromUuid(uuid:String):Option[String]

    def getByUuid(uuid: String): Option[FullRecord] = getByUuid(uuid, false)
    
    def getByUuid(uuid: String, includeSensitive:Boolean): Option[FullRecord]

    def getByRowKey(rowKey: String) :Option[FullRecord] = getByRowKey(rowKey, false)
    
    def getByRowKey(rowKey: String, includeSensitive:Boolean) :Option[FullRecord]

    def getAllVersionsByRowKey(rowKey:String, includeSensitive:Boolean=false) : Option[Array[FullRecord]]
    
    def getRawProcessedByRowKey(rowKey:String) :Option[Array[FullRecord]]

    def getAllVersionsByUuid(uuid: String, includeSenstive:Boolean=false): Option[Array[FullRecord]]

    def getByUuid(uuid: String, version: Version, includeSensitive:Boolean=false): Option[FullRecord]

    def getByRowKey(rowKey: String, version:Version, includeSensitive:Boolean=false): Option[FullRecord]

    def getUUIDForUniqueID(uniqueID: String) : Option[String]

    def createOrRetrieveUuid(uniqueID: String): (String, Boolean)
    
    def createUuid = UUID.randomUUID.toString

    def writeToStream(outputStream: OutputStream, fieldDelimiter: String, recordDelimiter: String, rowKeys: Array[String], fields: Array[String], qaFields:Array[String], includeSensitive:Boolean=false): Unit
    
    def writeToRecordWriter(writer:RecordWriter, rowKeys: Array[String], fields: Array[String], qaFields:Array[String], includeSensitive:Boolean=false): Unit

    def pageOverAllVersions(proc: ((Option[Array[FullRecord]]) => Boolean),startKey:String="", endKey:String="", pageSize: Int = 1000): Unit

    def pageOverAll(version: Version, proc: ((Option[FullRecord]) => Boolean),startKey:String="", endKey:String="", pageSize: Int = 1000): Unit

    def pageOverRawProcessed(proc: (Option[(FullRecord, FullRecord)] => Boolean),startKey:String="", endKey:String="", pageSize: Int = 1000): Unit
    
    def conditionalPageOverRawProcessed(proc: (Option[(FullRecord, FullRecord)] => Boolean), condition:(Map[String,String]=>Boolean),columnsToRetrieve:Array[String],startKey:String="", endKey:String="", pageSize: Int = 1000): Unit

    def addRawOccurrence(fullRecord: FullRecord): Unit

    def addRawOccurrenceBatch(fullRecords: Array[FullRecord]): Unit

    def updateOccurrence(rowKey: String, fullRecord: FullRecord, version: Version): Unit

    def updateOccurrence(rowKey: String, fullRecord: FullRecord, assertions: Option[Map[String,Array[QualityAssertion]]], version: Version): Unit

    def updateOccurrence(rowKey: String, oldRecord: FullRecord, updatedRecord: FullRecord, assertions: Option[Map[String,Array[QualityAssertion]]], version: Version)

    def updateOccurrence(rowKey: String, anObject: AnyRef, version: Version): Unit

    def addSystemAssertion(rowKey: String, qualityAssertion: QualityAssertion, checkExisting:Boolean=true): Unit

    def removeSystemAssertion(rowKey: String, errorCode:ErrorCode) : Unit

    def updateSystemAssertions(rowKey: String, qualityAssertions: Map[String,Array[QualityAssertion]]): Unit

    def getSystemAssertions(rowKey: String): List[QualityAssertion]

    def addUserAssertion(rowKey: String, qualityAssertion: QualityAssertion): Unit

    def getUserAssertions(rowKey: String): List[QualityAssertion]
    
    def getUserIdsForAssertions(rowKey: String):Set[String]

    def deleteUserAssertion(rowKey: String, assertionUuid: String): Boolean
    
    def updateAssertionStatus(rowKey: String, assertion: QualityAssertion, systemAssertions: List[QualityAssertion], userAssertions: List[QualityAssertion])

    def reIndex(rowKey: String)
    
    def delete(rowKey: String, removeFromIndex:Boolean=true,logDeleted:Boolean=false)
}

/**
 * A DAO for accessing occurrences.
 */
class OccurrenceDAOImpl extends OccurrenceDAO {

    import ReflectBean._
    import JavaConversions._

    protected val logger = LoggerFactory.getLogger("OccurrenceDAO")
    @Inject
    var persistenceManager: PersistenceManager = _
    @Inject
    var indexDAO: IndexDAO = _

    /**
     * Gets the map for a record based on searching the index for new and old ids
     */
    def getMapFromIndex(value:String):Option[Map[String,String]]={
      persistenceManager.getByIndex(value, entityName, "uuid") match {
       case None => persistenceManager.getByIndex(value, entityName, "portalId")
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
        val map = getMapFromIndex(uuid)//persistenceManager.getByIndex(uuid, entityName, "uuid")
        if (map.isEmpty) {
            None
        } else {
            // the versions of the record
            val rowKey = map.get.getOrElse("rowKey",uuid)
            val raw = FullRecordMapper.createFullRecord(rowKey, map.get, Raw)
            val processed = FullRecordMapper.createFullRecord(rowKey, map.get, Processed)
            val consensus = FullRecordMapper.createFullRecord(rowKey, map.get, Consensus)
            if(includeSensitive && raw.occurrence.originalSensitiveValues != null){
              FullRecordMapper.mapPropertiesToObject(raw, raw.occurrence.originalSensitiveValues)
              //Only the RAW values are being changed back to sensitive values. The processed values will reflect the values due to processing.
//              //update lat and lon of processed
//              processed.location.decimalLatitude = raw.location.decimalLatitude
//              processed.location.decimalLongitude = raw.location.decimalLongitude
//              //remove the values in data generalisations and information withheld
//              processed.occurrence.dataGeneralizations = null
//              processed.occurrence.informationWithheld = null
            }
            //pass all version to the procedure, wrapped in the Option
            Some(Array(raw, processed, consensus))
        }
    }

   /**
    * Get all the versions based on a row key
    */
    def getAllVersionsByRowKey(rowKey:String, includeSensitive:Boolean=false): Option[Array[FullRecord]] ={
      val map = persistenceManager.get(rowKey, entityName)
      if(map.isEmpty){
        None
      }
      else{
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
      }
      else{
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
        val propertyMap = getMapFromIndex(uuid)//persistenceManager.getByIndex(uuid, entityName, "uuid")
        if (propertyMap.isEmpty) {
          None
        } else {
          val record =FullRecordMapper.createFullRecord(propertyMap.get.get("rowKey").get, propertyMap.get, version)
          if(includeSensitive && record.occurrence.originalSensitiveValues != null && version == Versions.RAW)
              FullRecordMapper.mapPropertiesToObject(record, record.occurrence.originalSensitiveValues)
          Some(record)
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
     *  Returns uuid and true when a new uid was created
     *
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
    val elpattern = """el[0-9]+""".r
    val clpattern = """cl[0-9]+""".r
    
    /**
     * Writes the supplied field values to the writer.  The Writer specifies the format in which the record is 
     * written.
     */
    def writeToRecordWriter(writer:RecordWriter, rowKeys: Array[String], fields: Array[String], qaFields:Array[String], includeSensitive:Boolean=false){
      //get the codes for the qa fields that need to be included in the download
      //TODO fix this in case the value can't be found
      val mfields = scala.collection.mutable.ArrayBuffer[String]()
      mfields ++= fields
      val codes = qaFields.map(value=>AssertionCodes.getByName(value).get.getCode)
      val firstEL = fields.find(value => {elpattern.findFirstIn(value).nonEmpty})
      val firstCL = fields.find(value => {clpattern.findFirstIn(value).nonEmpty})
      val firstMisc = fields.find(value =>{IndexFields.storeMiscFields.contains(value)})
      if(firstEL.isDefined)
        mfields + "el.p"
      if(firstCL.isDefined)
        mfields + "cl.p"
      if(includeSensitive)
        mfields + "originalSensitiveValues"
      if(firstMisc.isDefined)
        mfields + FullRecordMapper.miscPropertiesColumn
      mfields ++=  FullRecordMapper.qaFields

      persistenceManager.selectRows(rowKeys, entityName, mfields.toArray , { fieldMap =>
        val array = scala.collection.mutable.ArrayBuffer[String]()
        val sensitiveMap:scala.collection.Map[String,String] = if(includeSensitive) Json.toStringMap(fieldMap.getOrElse("originalSensitiveValues", "{}")) else Map()
        val elMap = if(firstEL.isDefined) Json.toStringMap(fieldMap.getOrElse("el.p", "{}")) else Map[String,String]()
        val clMap = if(firstCL.isDefined)Json.toStringMap(fieldMap.getOrElse("cl.p", "{}")) else Map[String,String]()
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
          array + fieldValue
        })
        //now handle the QA fields
        val failedCodes = getErrorCodes(fieldMap);
        //work way through the codes and add to output
        for(code <-codes){
            array + (failedCodes.contains(code)).toString
        }
        writer.write(array.toArray)
      })
    }
    
    /**
     * Write to stream in a delimited format (CSV).
     */
    def writeToStream(outputStream: OutputStream, fieldDelimiter: String, recordDelimiter: String,
                      rowKeys: Array[String], fields: Array[String], qaFields:Array[String], includeSensitive:Boolean=false) {
        //get the codes for the qa fields that need to be included in the download
        //TODO fix thi in case the value can't be found
        val mfields = scala.collection.mutable.ArrayBuffer[String]()
        mfields ++= fields
        val codes = qaFields.map(value=>AssertionCodes.getByName(value).get.getCode)
        val firstEL = fields.find(value => {elpattern.findFirstIn(value).nonEmpty})
        val firstCL = fields.find(value => {clpattern.findFirstIn(value).nonEmpty})
        var extraFields = Array[String]()
        if(firstEL.isDefined)
          mfields + "el.p"
        if(firstCL.isDefined)
          mfields + "cl.p"
        if(includeSensitive)
          mfields + "originalSensitiveValues"
        mfields ++=  FullRecordMapper.qaFields
          
        //val fieldsToQuery = if(includeSensitive) fields ++ FullRecordMapper.qaFields ++ Array("originalSensitiveValues") else fields ++ FullRecordMapper.qaFields
        persistenceManager.selectRows(rowKeys, entityName, mfields.toArray , { fieldMap =>
          val sensitiveMap:scala.collection.Map[String,String] = if(includeSensitive) Json.toStringMap(fieldMap.getOrElse("originalSensitiveValues", "{}")) else Map()
          val elMap = if(firstEL.isDefined) Json.toStringMap(fieldMap.getOrElse("el.p", "{}")) else Map[String,String]()
          val clMap = if(firstCL.isDefined)Json.toStringMap(fieldMap.getOrElse("cl.p", "{}")) else Map[String,String]()
          for (field <- fields) {
              val fieldValue = field match{
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
          }
          //now handle the QA fields
          val failedCodes = getErrorCodes(fieldMap);
          //work way through the codes and add to output
          for(code <-codes){
              outputStream.write((failedCodes.contains(code)).toString.getBytes)
              outputStream.write(fieldDelimiter.getBytes)
          }
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
        map.getOrElse(field,""); 
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
      persistenceManager. put(fr.rowKey, entityName, properties.toMap)
    }

    /**
     * Update the version of the occurrence record.
     */
    def addRawOccurrenceBatch(fullRecords: Array[FullRecord]) {
      var batch = scala.collection.mutable.Map[String, Map[String, String]]()
      fullRecords.foreach(fr  => {
        //download the media in associatedMedia?????
        if (fr.occurrence.associatedMedia != null){
          val filesToImport = fr.occurrence.associatedMedia.split(";")
          val associatedMediaBuffer = new ArrayBuffer[String]
          filesToImport.foreach(fileToStore => {
            val filePath = MediaStore.save(fr.uuid, fr.attribution.dataResourceUid, fileToStore)
            if(!filePath.isEmpty) associatedMediaBuffer += filePath.get
          })
          fr.occurrence.associatedMedia = associatedMediaBuffer.toArray.mkString(";")
        }
        //process the record
        var properties = FullRecordMapper.fullRecord2Map(fr, Versions.RAW)
        batch.put(fr.rowKey, properties.toMap)
      })
      //commit
      persistenceManager.putBatch(entityName, batch.toMap)
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
        for(array <- assertions.values){
            for(i <- 0 to array.size-1){
            	processed.assertions = processed.assertions :+ array(i).getName
            }
        }
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
        val falseUserAssertions = userAssertions.filter(a => !a.problemAsserted)
        //true user assertions are assertions that have not been proven false by another user
        val trueUserAssertions = userAssertions.filter(a => a.problemAsserted && !doesListContainCode(falseUserAssertions,a.code))

        //for each qa type get the list of QA's that failed
        val assertionsDeleted = ListBuffer[QualityAssertion]() // stores the assertions that should not be considered for the kosher fields
        for(name <- systemAssertions.keySet){
          val assertions = systemAssertions.get(name).get
          val failedass = new ArrayBuffer[Int]
          for(qa <- assertions){
              //check to see if a user assertion counteracts this code
              if(!doesListContainCode(falseUserAssertions,qa.code))
                  failedass.add(qa.code)
              else
                  assertionsDeleted += qa
          }
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
              }
              else if(name == FullRecordMapper.taxonomicalQa){
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
    def addSystemAssertion(rowKey: String, qualityAssertion: QualityAssertion,checkExisting:Boolean=true) {
      val systemAssertions = (getSystemAssertions(rowKey) :+ qualityAssertion).groupBy(x => x.code).values.map( _.head).toList
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
              persistenceManager.put(rowKey, entityName, FullRecordMapper.userVerifiedColumn,"true")
          }
        }
    }

    /**
     * Retrieve annotations for the supplied UUID.
     */
    def getUserAssertions(rowKey:String): List[QualityAssertion] ={
      val startKey = rowKey + "|"
      val endKey = startKey +"~"
      val system = new ArrayBuffer[QualityAssertion]
      val userAssertions = new ArrayBuffer[QualityAssertion]
      //page over all the qa's that are for this record
      persistenceManager.pageOverAll(qaEntityName,(guid, map)=>{
          val qa = new QualityAssertion()
          FullRecordMapper.mapPropertiesToObject(qa, map)
          userAssertions + qa
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
          userIds + userId.get
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
        else{
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
        	val negativeAssertion = assertions.find(qa => !qa.problemAsserted)
          if (!negativeAssertion.isEmpty) {
            //need to remove this assertion from the error codes if it exists
            listErrorCodes = listErrorCodes - assertion.code
          }
          else {
            //at least one user has flagged this assertion so we need to add it
            listErrorCodes = listErrorCodes + assertion.code
          }
        }
        //check to see if a system assertion exists
        else if (!systemAssertions.isEmpty) {
        	val matchingAssertion = systemAssertions.find { _.name equals assertionName }
          if (!matchingAssertion.isEmpty) {
            //this assertion has been set by the system
            val sysassertion = matchingAssertion.get
            listErrorCodes = listErrorCodes + sysassertion.code
          }
          else {
            //code needs to be removed
            listErrorCodes = listErrorCodes - assertion.code
          }
        }
        else {
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
        properties += (FullRecordMapper.userQualityAssertionColumn-> (userAssertions.size>0).toString)        
        if(userVerified){
          properties += (FullRecordMapper.geospatialDecisionColumn -> "true")
          properties += (FullRecordMapper.taxonomicDecisionColumn -> "true")
        }
        else if(phase == FullRecordMapper.geospatialQa){
          properties += (FullRecordMapper.geospatialDecisionColumn -> AssertionCodes.isGeospatiallyKosher(listErrorCodes.toArray).toString)
        }
        else if(phase == FullRecordMapper.taxonomicalQa){
          properties += (FullRecordMapper.taxonomicDecisionColumn -> AssertionCodes.isTaxonomicallyKosher(listErrorCodes.toArray).toString)
        }
        if(properties.size >0){
          logger.debug("Updating the assertion status for : " + rowKey + properties)
          persistenceManager.put(rowKey, entityName, properties.toMap)
        }
    }

  /**
   * Set this record to deleted.
   */
  def setDeleted(rowKey: String, del: Boolean,dateTime:Option[String]=None) = {
      if(dateTime.isDefined){
        val values = Map(FullRecordMapper.deletedColumn->del.toString, FullRecordMapper.dateDeletedColumn -> dateTime.get)
        persistenceManager.put(rowKey, entityName, values)
      }
      else
        persistenceManager.put(rowKey, entityName, FullRecordMapper.deletedColumn, del.toString)
      //remove the datedeleted column if the records becomes undeleted...
      if(!del)
        persistenceManager.deleteColumns(rowKey,entityName, FullRecordMapper.dateDeletedColumn)
  }

  /**
   * Returns the rowKey based on the supplied uuid
   */
  def getRowKeyFromUuid(uuid:String):Option[String] = persistenceManager.getByIndex(uuid, entityName, "uuid", "rowKey")

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
      }
      else{
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
          val values = Map(rowKey->uuid,"value|"+rowKey->stringValue)
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

trait DeletedRecordDAO {
  //startDate must be in the form yyyy-MM-dd
  def getUuidsForDeletedRecords(startDate:String) : Array[String]
}

class DeletedRecordDAOImpl extends DeletedRecordDAO{
  /**
   * returns all the uuids that have been deleted since startDate inclusive.
   */
  override def getUuidsForDeletedRecords(startDate:String) : Array[String] ={
    val recordBuffer = new ArrayBuffer[String]
    Config.persistenceManager.pageOverColumnRange("dellog",(rowKey,map)=>{
      recordBuffer ++= map.values
      true
    },startDate,"",1000,"dr","dr~")

    recordBuffer.toArray
  }
}

trait DuplicateDAO {
  def getDuplicateInfo(uuid:String) : Option[DuplicateRecordDetails]
}

class DuplicateDAOImpl extends DuplicateDAO {
  protected val logger = LoggerFactory.getLogger("DuplicateDAO")
  @Inject
  var persistenceManager: PersistenceManager = _
  override def getDuplicateInfo(uuid:String):Option[DuplicateRecordDetails]={
    
    val mapper = new ObjectMapper
    mapper.registerModule(DefaultScalaModule)
    val stringValue =persistenceManager.get(uuid, "occ_duplicates", "value")
    if(stringValue.isDefined){
      Some(mapper.readValue[DuplicateRecordDetails](stringValue.get, classOf[DuplicateRecordDetails]))
    }
    else
      None
  }
}

trait AssertionQueryDAO{
  def getAssertionQuery(id:String) : Option[AssertionQuery]
  def upsertAssertionQuery(assertionQuery:AssertionQuery)
  def deleteAssertionQuery(id:String, date:java.util.Date=null, physicallyRemove:Boolean=false)
}

class AssertionQueryDAOImpl extends AssertionQueryDAO{
  import BiocacheConversions._
  @Inject
  var persistenceManager: PersistenceManager = _
  
  def getAssertionQuery(id:String):Option[AssertionQuery]={
    val aq = new AssertionQuery()
    
    val map = persistenceManager.get(id, "queryassert")
    if(map.isDefined){
      FullRecordMapper.mapPropertiesToObject(aq, map.get)
      Some(aq)
    }
    else
      None
  }
  
  def upsertAssertionQuery(assertionQuery:AssertionQuery){
    val properties = FullRecordMapper.mapObjectToProperties(assertionQuery)
    persistenceManager.put(assertionQuery.getId(),"queryassert",properties)
  }
  
  def deleteAssertionQuery(id:String, date:java.util.Date=null, physicallyRemove:Boolean=false){
    if(physicallyRemove)
      persistenceManager.delete(id, "queryassert")
    else if(date != null){
      persistenceManager.put(id, "queryassert", "deletedDate",date)
    }
  }
}

trait OutlierStatsDAO {
  def getJackKnifeStatsFor(guid:String) : java.util.Map[String, JackKnifeStats]
  def getJackKnifeOutliersFor(guid:String) : java.util.Map[String, Array[String]]
  def getJackKnifeRecordDetailsFor(uuid:String) : Array[RecordJackKnifeStats]
}

class OutlierStatsDAOImpl extends OutlierStatsDAO {

  protected val logger = LoggerFactory.getLogger("OutlierStatsDAO")
  @Inject
  var persistenceManager: PersistenceManager = _

  def getJackKnifeStatsFor(guid:String) : java.util.Map[String, JackKnifeStats] = {

    logger.debug("Getting outlier stats for: " + guid)
    val mapper = new ObjectMapper
    mapper.registerModule(DefaultScalaModule)
    val stringValue = persistenceManager.get(guid,"outliers", "jackKnifeStats").getOrElse("{}")

    logger.debug("Retrieved outlier stats for: " + stringValue)
    val obj = mapper.readValue(stringValue, classOf[java.util.Map[String, JackKnifeStats]])
    obj.asInstanceOf[java.util.Map[String, JackKnifeStats]]
  }

  def getJackKnifeOutliersFor(guid:String) : java.util.Map[String, Array[String]] = {

    logger.debug("Getting outlier stats for: " + guid)
    val mapper = new ObjectMapper
    mapper.registerModule(DefaultScalaModule)
    val stringValue = persistenceManager.get(guid,"outliers", "jackKnifeOutliers").getOrElse("{}")

    logger.debug("Retrieved outlier stats for: " + stringValue)
    val obj = mapper.readValue(stringValue, classOf[java.util.Map[String, Array[String]]])
    obj.asInstanceOf[java.util.Map[String, Array[String]]]
  }

  def getJackKnifeRecordDetailsFor(uuid:String) : Array[RecordJackKnifeStats] = {

    logger.debug("Getting outlier stats for record: " + uuid)
    val mapper = new ObjectMapper
    mapper.registerModule(DefaultScalaModule)
    val stringValue = persistenceManager.get(uuid,"occ_outliers", "jackKnife").getOrElse("[]")

    logger.debug("Retrieved outlier stats for: " + stringValue)
    val obj = mapper.readValue(stringValue, classOf[Array[RecordJackKnifeStats]])
    obj.asInstanceOf[Array[RecordJackKnifeStats]]
  }
}