package au.org.ala.biocache

import com.google.inject.Inject

//import au.org.ala.sds.SensitiveSpeciesFinderFactory

import au.org.ala.util.ReflectBean
import java.io.OutputStream
import scala.collection.JavaConversions
import java.lang.reflect.Method
import java.util.UUID
import org.slf4j.LoggerFactory
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

trait OccurrenceDAO {

    val entityName = "occ"

    def setUuidDeleted(uuid: String, del: Boolean): Unit

    def getRowKeyFromUuid(uuid:String):Option[String]

    def getByUuid(uuid: String): Option[FullRecord]

    def getByRowKey(rowKey: String) :Option[FullRecord]

    def getAllVersionsByRowKey(rowKey:String) : Option[Array[FullRecord]]
    
    def getRawProcessedByRowKey(rowKey:String) :Option[Array[FullRecord]]

    def getAllVersionsByUuid(uuid: String): Option[Array[FullRecord]]

    def getByUuid(uuid: String, version: Version): Option[FullRecord]

    def getByRowKey(rowKey: String, version:Version): Option[FullRecord]

    def createOrRetrieveUuid(uniqueID: String): String
    
    def createUuid = UUID.randomUUID.toString

    def writeToStream(outputStream: OutputStream, fieldDelimiter: String, recordDelimiter: String, rowKeys: Array[String], fields: Array[String], qaFields:Array[String]): Unit

    def pageOverAllVersions(proc: ((Option[Array[FullRecord]]) => Boolean),startKey:String="", endKey:String="", pageSize: Int = 1000): Unit

    def pageOverAll(version: Version, proc: ((Option[FullRecord]) => Boolean),startKey:String="", endKey:String="", pageSize: Int = 1000): Unit

    def pageOverSelectAll(version: Version, proc: ((Option[FullRecord]) => Boolean),fields: Array[String],startKey:String="", pageSize: Int = 1000): Unit

    def pageOverRawProcessed(proc: (Option[(FullRecord, FullRecord)] => Boolean),startKey:String="", endKey:String="", pageSize: Int = 1000): Unit

    def addRawOccurrenceBatch(fullRecords: Array[FullRecord]): Unit

    def updateOccurrence(rowKey: String, fullRecord: FullRecord, version: Version): Unit

    def updateOccurrence(rowKey: String, fullRecord: FullRecord, assertions: Option[Map[String,Array[QualityAssertion]]], version: Version): Unit

    def updateOccurrence(rowKey: String, oldRecord: FullRecord, updatedRecord: FullRecord, assertions: Option[Map[String,Array[QualityAssertion]]], version: Version)

    def updateOccurrence(rowKey: String, anObject: AnyRef, version: Version): Unit

    def addSystemAssertion(rowKey: String, qualityAssertion: QualityAssertion): Unit

    def updateSystemAssertions(rowKey: String, qualityAssertions: Map[String,Array[QualityAssertion]]): Unit

    def getSystemAssertions(uuid: String): List[QualityAssertion]

    def addUserAssertion(uuid: String, qualityAssertion: QualityAssertion): Unit

    def getUserAssertions(uuid: String): List[QualityAssertion]

    def deleteUserAssertion(uuid: String, assertionUuid: String): Boolean

    def updateAssertionStatus(rowKey: String, assertionName: String, systemAssertions: List[QualityAssertion], userAssertions: List[QualityAssertion])

    def reIndex(uuid: String)
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

        val map = persistenceManager.getByIndex(uuid, entityName, "uuid")
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
        val propertyMap = persistenceManager.getByIndex(uuid, entityName, "uuid")
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
        

        val recordUUID = persistenceManager.get(uniqueID, "occ", "uuid")
        if (recordUUID.isEmpty) {
            val newUuid = createUuid
            //The uuid will be added when the record is inserted
            //persistenceManager.put(uniqueID, "dr", "uuid", newUuid)
            newUuid
        } else {
            recordUUID.get
        }
    }

    /**
     * Write to stream in a delimited format (CSV).
     */
    def writeToStream(outputStream: OutputStream, fieldDelimiter: String, recordDelimiter: String, rowKeys: Array[String], fields: Array[String], qaFields:Array[String]) {
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
     * @param occurrenceType
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
     * @param occurrenceType
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
     * Iterates over the sepcified version of all the occurrence records. The values retrieved
     * from the persistence manager is limited to the supplied fields
     */
    def pageOverSelectAll(version: Version, proc: ((Option[FullRecord]) => Boolean),fields: Array[String],startKey:String="", pageSize: Int = 1000){

    }

    /**
     * Iterate over all occurrences, passing the objects to a function.
     * Function returns a boolean indicating if the paging should continue.
     *
     * @param occurrenceType
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
     * Update the version of the occurrence record.
     *
     * /TODO: This does not follow the new occ row design
     */
    def addRawOccurrenceBatch(fullRecords: Array[FullRecord]) {

        var batch = scala.collection.mutable.Map[String, Map[String, String]]()
        for (fullRecord <- fullRecords) {
            var properties = fullRecord2Map(fullRecord, Versions.RAW)
            batch.put(fullRecord.rowKey, properties.toMap)
        }
        //commit
        persistenceManager.putBatch(entityName, batch.toMap)
    }

//    /**
//     * if the objects is Mappable return the map of the properties otherwise returns an empty map
//     */
//    protected def mapObjectToProperties(anObject: AnyRef, version: Version): Map[String, String] = {
//        var properties = scala.collection.mutable.Map[String, String]()
//        if (anObject.isInstanceOf[Mappable]) {
//            val map = anObject.asInstanceOf[Mappable].getMap
//
//
//            map foreach {
//                case (key, value) => {
//                    version match {
//                        case Processed => properties.put(FullRecordMapper.markAsProcessed(key), value)
//                        case Consensus => properties.put(FullRecordMapper.markAsConsensus(key), value)
//                        case Raw => properties.put(key, value)
//                    }
//                }
//            }
//        }
//        else{
//          val defn = FullRecordMapper.getDefn(anObject)
//            for (field <- defn.keySet) {
//                //val fieldValue = anObject.getter(field).asInstanceOf[String]
//                //Use the cached version of the getter method
//                val getter = defn.get(field).get.asInstanceOf[(Method,Method)]._1
//                val fieldValue = getter.invoke(anObject)
//                if (fieldValue != null) {
//                    version match {
//                      case Processed => properties.put(FullRecordMapper.markAsProcessed(field.toString), fieldValue.toString)
//                      case Consensus => properties.put(FullRecordMapper.markAsConsensus(field.toString), fieldValue.toString)
//                      case Raw => properties.put(field.toString, fieldValue.toString)
//
//                    
//                    }
//                }
//            }
//        }
//        properties.toMap
//
//    }

    /**
     * Update the version of the occurrence record.
     */
    def updateOccurrence(rowKey: String, fullRecord: FullRecord, version: Version) {
        updateOccurrence(rowKey, fullRecord, None, version)
    }

    /**
     * Convert a full record to a map of properties
     */
    def fullRecord2Map(fullRecord: FullRecord, version: Version): scala.collection.mutable.Map[String, String] = {
        var properties = scala.collection.mutable.Map[String, String]()
        fullRecord.objectArray.foreach(poso => {
            val map = FullRecordMapper.mapObjectToProperties(poso, version)
            //poso.
            //add all to map           
            properties.putAll(map)
        })
        //add the special cases to the map
        properties.put("uuid", fullRecord.uuid)
        properties.put("rowKey", fullRecord.rowKey)
        if(fullRecord.lastModifiedTime != "")
            properties.put(FullRecordMapper.markNameBasedOnVersion(FullRecordMapper.alaModifiedColumn, version), fullRecord.lastModifiedTime)
        properties
    }

    /**
     * Update the occurrence with the supplied record, setting the correct version
     */
    def updateOccurrence(rowKey: String, fullRecord: FullRecord, assertions: Option[Map[String,Array[QualityAssertion]]], version: Version) {

        //construct a map of properties to write
        val properties = fullRecord2Map(fullRecord, version)

        if (!assertions.isEmpty) {
            properties ++= convertAssertionsToMap(assertions.get)
            updateSystemAssertions(rowKey, assertions.get)
        }

        //commit to cassandra
        persistenceManager.put(rowKey, entityName, properties.toMap)
    }

    /**
     * Update the occurrence with the supplied record, setting the correct version
     */
    def updateOccurrence(rowKey: String, oldRecord: FullRecord, newRecord: FullRecord, assertions: Option[Map[String,Array[QualityAssertion]]], version: Version) {

        //construct a map of properties to write
        val oldproperties = fullRecord2Map(oldRecord, version)
        val properties = fullRecord2Map(newRecord, version)

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

        //TODO check for deleted properties
        val deletedProperties = oldproperties.filter({
            case (key, value) => !properties.contains(key)
        })
        
        propertiesToPersist ++= deletedProperties.map({
            case (key, value) => key -> ""
        })

//        if (!assertions.isEmpty){//} && !propertiesToPersist.isEmpty) {
//            propertiesToPersist ++= convertAssertionsToMap(assertions.get)
//            updateSystemAssertions(rowKey, assertions.get)
//        }
        val timeCol = FullRecordMapper.markNameBasedOnVersion(FullRecordMapper.alaModifiedColumn, version)
        
        if(!assertions.isEmpty){
        	initAssertions(newRecord, assertions.get)
        	//only add  the assertions if they are different OR the properties to persist contain more than the last modified time stamp
        	if((oldRecord.assertions.toSet != newRecord.assertions.toSet) || !(propertiesToPersist.size == 1 && propertiesToPersist.getOrElse(timeCol, "") != "")){
        	    //only add the assertions if they have changed since the last time or the number of records to persist >1
        	    propertiesToPersist ++= convertAssertionsToMap(assertions.get)
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

    /**
     * Convert the assertions to a map
     */
    def convertAssertionsToMap(systemAssertions: Map[String,Array[QualityAssertion]]): Map[String, String] = {
        //if supplied, update the assertions
        val properties = new collection.mutable.ListMap[String, String]


        //for each qa type get the list of QA's that failed
        for(name <- systemAssertions.keySet){
          val assertions = systemAssertions.get(name).get
          val failedass = new ArrayBuffer[java.lang.Integer]
          for(qa <- assertions){
            failedass.add(qa.code)
          }
          
            properties+=(FullRecordMapper.markAsQualityAssertion(name) -> Json.toJSONWithGeneric(failedass.toList))
            if(name == FullRecordMapper.geospatialQa){
              properties += (FullRecordMapper.geospatialDecisionColumn -> AssertionCodes.isGeospatiallyKosher(assertions).toString)
            }
            else if(name == FullRecordMapper.taxonomicalQa){
              properties += (FullRecordMapper.taxonomicDecisionColumn -> AssertionCodes.isTaxonomicallyKosher(assertions).toString)
            }
          
        }

       /* //    //set the systemAssertions on the full record
        //    fullRecord.assertions = systemAssertions.toArray.map(_.name)

        //set the quality systemAssertions flags for all error codes - following the principle writes are fast
        for (qa <- systemAssertions) {
            properties += (FullRecordMapper.markAsQualityAssertion(qa.name) -> qa.problemAsserted.toString)
        }

        //for the uncatered codes and false values
        val cateredForCodes = systemAssertions.toArray.map(_.code).toSet
        val uncateredForCodes = AssertionCodes.all.filter(errorCode => {
            !cateredForCodes.contains(errorCode.code)
        })
        for (errorCode <- uncateredForCodes) {
            properties += (FullRecordMapper.markAsQualityAssertion(errorCode.name) -> "false")
        }

        //set the overall decision
        val geospatiallyKosher = AssertionCodes.isGeospatiallyKosher(systemAssertions)
        val taxonomicallyKosher = AssertionCodes.isTaxonomicallyKosher(systemAssertions)

        properties += (FullRecordMapper.geospatialDecisionColumn -> geospatiallyKosher.toString)
        properties += (FullRecordMapper.taxonomicDecisionColumn -> taxonomicallyKosher.toString)*/
       
        properties.toMap
    }

    /**
     * Update an occurrence entity. E.g. Occurrence, Classification, Taxon
     *
     *  IS this being used?
     *
     * @param uuid
     * @param anObject
     * @param occurrenceType
     */
    def updateOccurrence(rowKey: String, anObject: AnyRef, version: Version) {
        val map = FullRecordMapper.mapObjectToProperties(anObject, version)
        persistenceManager.put(rowKey, entityName, map)
    }

    /**
     * Adds a quality assertion to the row with the supplied UUID.
     *
     * @param uuid
     * @param qualityAssertion
     */
    def addSystemAssertion(rowKey: String, qualityAssertion: QualityAssertion) {
        persistenceManager.putList(rowKey, entityName, FullRecordMapper.qualityAssertionColumn, List(qualityAssertion), classOf[QualityAssertion], false)
        persistenceManager.put(rowKey, entityName, qualityAssertion.name, qualityAssertion.problemAsserted.toString)
    }

    /**
     * Set the system systemAssertions for a record, overwriting existing systemAssertions
     * TODO change this so that it is updating the contents not replacing - will need this functionality when particular processing phases can be run seperately
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
    def addUserAssertion(uuid: String, qualityAssertion: QualityAssertion) {

        val (rowKey,userAssertions) = getUserAssertionsRK(uuid)

        if (!userAssertions.isEmpty && !userAssertions.get.contains(qualityAssertion)) {
            val updatedUserAssertions = userAssertions.get :+ qualityAssertion
            val systemAssertions = getSystemAssertions(rowKey.get)
            //store the new systemAssertions
            persistenceManager.putList(rowKey.get, entityName, FullRecordMapper.userQualityAssertionColumn, updatedUserAssertions, classOf[QualityAssertion], true)
            //update the overall status
            updateAssertionStatus(rowKey.get, qualityAssertion.name, systemAssertions, updatedUserAssertions)
        }
    }

    /**
     * Retrieve the row key and annotations for the supplied UUID.
     */
    def getUserAssertionsRK(uuid: String): (Option[String], Option[List[QualityAssertion]]) = {
        val theClass = classOf[QualityAssertion].asInstanceOf[java.lang.Class[AnyRef]]

        //get the rowKey
        val rowKey = getRowKeyFromUuid(uuid)
        if(rowKey.isEmpty){
          (None,None)
        }
        else{
          (rowKey,Some(persistenceManager.getList(rowKey.get, entityName, FullRecordMapper.userQualityAssertionColumn, theClass)
              .asInstanceOf[List[QualityAssertion]]))
        }
    }
    /**
     * Retrieve annotations for the supplied UUID.
     */
    def getUserAssertions(uuid:String): List[QualityAssertion] ={
        val (rowKey, assertions) = getUserAssertionsRK(uuid)
        if(!assertions.isEmpty)
          assertions.get
        else
          List()
    }

    /**
     * Delete a user supplied assertion
     */
    def deleteUserAssertion(uuid: String, assertionUuid: String): Boolean = {

        logger.debug("Deleting assertion for : " + uuid + " with assertion uuid : " + uuid)

        //retrieve existing systemAssertions
        val (rowKey,assertions) = getUserAssertionsRK(uuid)
        if(assertions.isEmpty){
          logger.warn("Unable to locate in index uuid: " + uuid)
          false
        }
        else{
            //get the assertion that is to be deleted
            val deletedAssertion = assertions.get.find(assertion => {
                assertion.uuid equals assertionUuid
            })

            //if not empty, remove the assertion and write back
            if (!deletedAssertion.isEmpty) {

                //delete the assertion with the supplied UUID
                val updateAssertions = assertions.get.filter(qa => {
                    !(qa.uuid equals assertionUuid)
                })

                //put the systemAssertions back - overwriting existing systemAssertions
                persistenceManager.putList(rowKey.get, entityName, FullRecordMapper.userQualityAssertionColumn, updateAssertions, classOf[QualityAssertion], true)

                val assertionName = deletedAssertion.get.name
                //are there any matching systemAssertions for other users????
                val systemAssertions = getSystemAssertions(rowKey.get)

                //update the assertion status
                updateAssertionStatus(rowKey.get, assertionName, systemAssertions, updateAssertions)
                true
            } else {
                logger.warn("Unable to find assertion with UUID: " + assertionUuid)
                false
            }
        }
    }

    /**
     * Update the assertion status using system and user systemAssertions.
     */
    def updateAssertionStatus(rowKey: String, assertionName: String, systemAssertions: List[QualityAssertion], userAssertions: List[QualityAssertion]) {

        logger.info("Updating the assertion status for : " + rowKey)

        val assertions = userAssertions.filter(qa => {
            qa.name equals assertionName
        })
            //update the status flag on the record, using the system quality systemAssertions
            if (!assertions.isEmpty) {

                //if anyone asserts the negative, the answer is negative
                val negativeAssertion = userAssertions.find(qa => qa.problemAsserted)
                if (!negativeAssertion.isEmpty) {
                    val qualityAssertion = negativeAssertion.get
                    persistenceManager.put(rowKey,
                        entityName,
                        FullRecordMapper.markAsQualityAssertion(qualityAssertion.name),
                        qualityAssertion.problemAsserted.toString)
                }
            } else if (!systemAssertions.isEmpty) {
                //check system systemAssertions for an answer
                val matchingAssertion = systemAssertions.find(assertion => {
                    assertion.name equals assertionName
                })
                if (!matchingAssertion.isEmpty) {
                    val assertion = matchingAssertion.get
                    persistenceManager.put(rowKey, entityName, assertion.name, assertion.problemAsserted.toString)
                }
            } else {
                persistenceManager.put(rowKey, entityName, assertionName, true.toString)
            }

            //set the overall decision
            var properties = scala.collection.mutable.Map[String, String]()
            val geospatiallyKosher = AssertionCodes.isGeospatiallyKosher((userAssertions ++ systemAssertions).toArray)
            val taxonomicallyKosher = AssertionCodes.isTaxonomicallyKosher((userAssertions ++ systemAssertions).toArray)
            properties.put(FullRecordMapper.geospatialDecisionColumn, geospatiallyKosher.toString)
            properties.put(FullRecordMapper.taxonomicDecisionColumn, taxonomicallyKosher.toString)

            logger.info("Updating the assertion status for : " + rowKey
                + ", geospatiallyKosher:" + geospatiallyKosher
                + ", taxonomicallyKosher:" + taxonomicallyKosher)

            persistenceManager.put(rowKey, entityName, properties.toMap)
        
    }

    /**
     * Set this record to deleted.
     */
    def setUuidDeleted(uuid: String, del: Boolean) = {
        persistenceManager.put(uuid, entityName, FullRecordMapper.deletedColumn, del.toString)
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
    def reIndex(uuid: String) {
        logger.debug("Reindexing UUID: " + uuid)
        val map = persistenceManager.getByIndex(uuid, entityName, "uuid")
        //index from the map - this should be more efficient
        if(map.isEmpty){
          logger.debug("Unable to reindex UUID: " + uuid)
        }
        else{
          indexDAO.indexFromMap(map.get.getOrElse("rowKey", ""), map.get, false)
        }
//        val recordVersions = getAllVersionsByUuid(uuid)
//        if (recordVersions.isEmpty) {
//            logger.debug("Unable to reindex UUID: " + uuid)
//        } else {
//            val occurrenceIndex = indexDAO.getOccIndexModel(recordVersions.get)
//            if (!occurrenceIndex.isEmpty) {
//                indexDAO.index(occurrenceIndex.get)
//                logger.debug("Reindexed UUID: " + uuid)
//            }
//        }
    }
}
