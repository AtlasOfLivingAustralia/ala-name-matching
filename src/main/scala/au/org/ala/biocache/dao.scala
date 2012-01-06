package au.org.ala.biocache

import com.google.inject.Inject
import java.io.{File, OutputStream}

//import au.org.ala.sds.SensitiveSpeciesFinderFactory

import au.org.ala.util.ReflectBean
import scala.collection.JavaConversions
import java.lang.reflect.Method
import java.util.UUID
import org.slf4j.LoggerFactory
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

trait OccurrenceDAO {

    val entityName = "occ"
     
    val qaEntityName ="qa"

    def setDeleted(rowKey: String, del: Boolean): Unit

    def getRowKeyFromUuid(uuid:String):Option[String]

    def getByUuid(uuid: String): Option[FullRecord]

    def getByRowKey(rowKey: String) :Option[FullRecord]

    def getAllVersionsByRowKey(rowKey:String) : Option[Array[FullRecord]]
    
    def getRawProcessedByRowKey(rowKey:String) :Option[Array[FullRecord]]

    def getAllVersionsByUuid(uuid: String): Option[Array[FullRecord]]

    def getByUuid(uuid: String, version: Version): Option[FullRecord]

    def getByRowKey(rowKey: String, version:Version): Option[FullRecord]

    def getUUIDForUniqueID(uniqueID: String) : Option[String]

    def createOrRetrieveUuid(uniqueID: String): String
    
    def createUuid = UUID.randomUUID.toString

    def writeToStream(outputStream: OutputStream, fieldDelimiter: String, recordDelimiter: String, rowKeys: Array[String], fields: Array[String], qaFields:Array[String]): Unit

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

    def addSystemAssertion(rowKey: String, qualityAssertion: QualityAssertion): Unit

    def updateSystemAssertions(rowKey: String, qualityAssertions: Map[String,Array[QualityAssertion]]): Unit

    def getSystemAssertions(rowKey: String): List[QualityAssertion]

    def addUserAssertion(rowKey: String, qualityAssertion: QualityAssertion): Unit

    def getUserAssertions(rowKey: String): List[QualityAssertion]

    def deleteUserAssertion(rowKey: String, assertionUuid: String): Boolean
    
    def updateAssertionStatus(rowKey: String, assertion: QualityAssertion, systemAssertions: List[QualityAssertion], userAssertions: List[QualityAssertion])

    //def markAsOutlier(uuid:String, outlierResult:OutlierResult)

    def reIndex(rowKey: String)
    
    def delete(rowKey: String)
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
     * Mark the supplied records as 
     */
    def markAsOutlier(uuid:String, testUuid:String, noOfLayers:Int){
      getRowKeyFromUuid(uuid) match {
       case None => throw new IllegalArgumentException("Invalid UUID supplied: " + uuid)
       case Some(rowKey) => {
         //persistenceManager.put(rowKey, "occ", )
       }
      }
    }
    
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
    def getByUuid(uuid: String): Option[FullRecord] = {
        getByUuid(uuid, Raw)
    }
    /**
     * Get an occurrence with rowKey
     */
    def getByRowKey(rowKey:String): Option[FullRecord] ={
      getByRowKey(rowKey, Raw)
    }

    /**
     * Get all versions of the occurrence with UUID
     *
     * @param uuid
     * @return
     */
    def getAllVersionsByUuid(uuid: String): Option[Array[FullRecord]] = {
        val map = getMapFromIndex(uuid)//persistenceManager.getByIndex(uuid, entityName, "uuid")
        if (map.isEmpty) {
            None
        } else {
            // the versions of the record
            val rowKey = map.get.get("rowKey").get
            val raw = FullRecordMapper.createFullRecord(rowKey, map.get, Raw)
            val processed = FullRecordMapper.createFullRecord(rowKey, map.get, Processed)
            val consensus = FullRecordMapper.createFullRecord(rowKey, map.get, Consensus)
            //pass all version to the procedure, wrapped in the Option
            Some(Array(raw, processed, consensus))
        }
    }

   /**
    * Get all the versions based on a row key
    */
    def getAllVersionsByRowKey(rowKey:String): Option[Array[FullRecord]] ={
      val map = persistenceManager.get(rowKey, entityName)
      if(map.isEmpty){
        None
      }
      else{
        // the versions of the record
            val raw = FullRecordMapper.createFullRecord(rowKey, map.get, Raw)
            val processed = FullRecordMapper.createFullRecord(rowKey, map.get, Processed)
            val consensus = FullRecordMapper.createFullRecord(rowKey, map.get, Consensus)
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
    def getByRowKey(rowKey:String, version:Version) :Option[FullRecord] ={
      val propertyMap = persistenceManager.get(rowKey, entityName)
          if (propertyMap.isEmpty) {
              None
          } else {
            Some(FullRecordMapper.createFullRecord(rowKey, propertyMap.get, version))
          }
    }

    /**
     * Get an occurrence, specifying the version of the occurrence.
     */
    def getByUuid(uuid: String, version: Version): Option[FullRecord] = {
        val propertyMap = getMapFromIndex(uuid)//persistenceManager.getByIndex(uuid, entityName, "uuid")
        if (propertyMap.isEmpty) {
            None
        } else {
          Some(FullRecordMapper.createFullRecord(propertyMap.get.get("rowKey").get, propertyMap.get, version))
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
     */
    def createOrRetrieveUuid(uniqueID: String): String = {
        //look up by index
        val recordUUID = getUUIDForUniqueID(uniqueID)
        if (recordUUID.isEmpty) {
            val newUuid = createUuid
            //The uuid will be added when the record is inserted
            //persistenceManager.put(uniqueID, "dr", "uuid", newUuid)
            newUuid
        } else {
            recordUUID.get
        }
    }

    def getUUIDForUniqueID(uniqueID: String) = persistenceManager.get(uniqueID, "occ", "uuid")

    /**
     * Write to stream in a delimited format (CSV).
     */
    def writeToStream(outputStream: OutputStream, fieldDelimiter: String, recordDelimiter: String,
                      rowKeys: Array[String], fields: Array[String], qaFields:Array[String]) {
        //get the codes for the qa fields that need to be included in the download
        val codes = qaFields.map(value=>AssertionCodes.getByName(value).get.getCode)
        persistenceManager.selectRows(rowKeys, entityName, fields ++ FullRecordMapper.qaFields , {
            fieldMap =>
                for (field <- fields) {
                    val fieldValue = fieldMap.get(field)
                    //Create a MS Excel compliant CSV file thus field with delimiters are quoted and embedded quotes are escaped
                    val svalue = fieldValue.getOrElse("")
                    if (svalue.contains(fieldDelimiter) || svalue.contains(recordDelimiter) || svalue.contains("\""))
                        outputStream.write(("\"" + svalue.replaceAll("\"", "\"\"") + "\"").getBytes)
                    else
                        outputStream.write(svalue.getBytes)
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
                 }else {
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
    
    def doesListContainCode(list:List[QualityAssertion], code:Int) = list.filter(ua => ua.code ==code).size>0

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
          val ua2Add =trueUserAssertions.filter(a=>
              name match{
                  case "loc"=> a.code >=AssertionCodes.geospatialBounds._1 && a.code < AssertionCodes.geospatialBounds._2
                  case "class" => a.code >= AssertionCodes.taxonomicBounds._1 && a.code < AssertionCodes.taxonomicBounds._2
                  case "event" => a.code >= AssertionCodes.temporalBounds._1 && a.code < AssertionCodes.temporalBounds._2
                  case _ => false

          })
          val extraAssertions = ListBuffer[QualityAssertion]()
          ua2Add.foreach(qa =>if(!failedass.contains(qa.code)){
              failedass.add(qa.code)
              extraAssertions += qa
          })


          properties+=(FullRecordMapper.markAsQualityAssertion(name) -> Json.toJSONWithGeneric(failedass.toList))
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
    def addSystemAssertion(rowKey: String, qualityAssertion: QualityAssertion) {
        throw new Exception("Unable to add a single System assertions. Please change method if necessary")
        //persistenceManager.putList(rowKey, entityName, FullRecordMapper.qualityAssertionColumn, List(qualityAssertion), classOf[QualityAssertion], false)
        //persistenceManager.put(rowKey, entityName, qualityAssertion.name, qualityAssertion.problemAsserted.toString)
    }

    /**
     * Set the system systemAssertions for a record, overwriting existing systemAssertions
     * TODO change this so that it is updating the contents not replacing - will need this functionality when particular processing phases can be run seperately
     * 
     * Please NOTE a verified record will still have a list of SystemAssertions that failed. But there will be no corresponding qa codes.
     * 
     */
    def updateSystemAssertions(rowKey: String, qualityAssertions: Map[String,Array[QualityAssertion]]) {
        var assertions = new ListBuffer[QualityAssertion] //getSystemAssertions(uuid)
        for(qas <- qualityAssertions.values){
          assertions ++= qas
        }
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

        logger.info("Updating the assertion status for : " + rowKey)        
        //get the phase based on the error type
        val phase =Processors.getProcessorForError(assertion.code)
        logger.debug("Phase " + phase)
        //get existing values for the phase
        var listErrorCodes: Set[Int] = getListOfCodes(rowKey, phase).toSet
        logger.debug("Original: " + listErrorCodes)
        val assertionName = assertion.name
        val assertions = userAssertions.filter(qa => {
            qa.name equals assertionName
        })
        
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
                	listErrorCodes =listErrorCodes + assertion.code
                }
        }
        //check to see if a system assertion exists
        else if (!systemAssertions.isEmpty) {
        	val matchingAssertion = systemAssertions.find(assertion => {
                    assertion.name equals assertionName
                })
                if (!matchingAssertion.isEmpty) {
                	//this assertion has been set by the system
                	val sysassertion = matchingAssertion.get
                    listErrorCodes =listErrorCodes + sysassertion.code
                }
                else{
                	//code needs to be removed
                	listErrorCodes =listErrorCodes - assertion.code
                }
        }
        else{
        	//there are no matching assertions in user or system thus remove this error code
        	listErrorCodes =listErrorCodes - assertion.code
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
                logger.info("Updating the assertion status for : " + rowKey
                + properties)
                 persistenceManager.put(rowKey, entityName, properties.toMap)
            }
    }

    /**
     * Set this record to deleted.
     */
    def setDeleted(rowKey: String, del: Boolean) = {
        persistenceManager.put(rowKey, entityName, FullRecordMapper.deletedColumn, del.toString)
    }
    /**
     * Returns the rowKey based on the supplied uuid
     */
   def getRowKeyFromUuid(uuid:String):Option[String]={
     persistenceManager.getByIndex(uuid, entityName, "uuid", "rowKey")
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
        }
        else{
          indexDAO.indexFromMap(rowKey, map.get, false)
        }
    }

    def delete(rowKey: String)={
        //delete from the data store
        persistenceManager.delete(rowKey, entityName)
        //delete from the index
        indexDAO.removeFromIndex("row_key", rowKey)
    }
}
