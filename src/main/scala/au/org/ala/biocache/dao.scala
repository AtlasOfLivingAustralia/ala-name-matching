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

    def getByUuid(uuid: String): Option[FullRecord]

    def getAllVersionsByUuid(uuid: String): Option[Array[FullRecord]]

    def getByUuid(uuid: String, version: Version): Option[FullRecord]

    def createOrRetrieveUuid(uniqueID: String): String

    def writeToStream(outputStream: OutputStream, fieldDelimiter: String, recordDelimiter: String, uuids: Array[String], fields: Array[String]): Unit

    def pageOverAllVersions(proc: ((Option[Array[FullRecord]]) => Boolean),startUuid:String="", pageSize: Int = 1000): Unit

    def pageOverAll(version: Version, proc: ((Option[FullRecord]) => Boolean),startUuid:String="", pageSize: Int = 1000): Unit

    def pageOverSelectAll(version: Version, proc: ((Option[FullRecord]) => Boolean),fields: Array[String],startUuid:String="", pageSize: Int = 1000): Unit

    def pageOverRawProcessed(proc: (Option[(FullRecord, FullRecord)] => Boolean),startUuid:String="", pageSize: Int = 1000): Unit

    def addRawOccurrenceBatch(fullRecords: Array[FullRecord]): Unit

    def updateOccurrence(uuid: String, fullRecord: FullRecord, version: Version): Unit

    def updateOccurrence(uuid: String, fullRecord: FullRecord, assertions: Option[Map[String,Array[QualityAssertion]]], version: Version): Unit

    def updateOccurrence(uuid: String, oldRecord: FullRecord, updatedRecord: FullRecord, assertions: Option[Map[String,Array[QualityAssertion]]], version: Version)

    def updateOccurrence(uuid: String, anObject: AnyRef, version: Version): Unit

    def addSystemAssertion(uuid: String, qualityAssertion: QualityAssertion): Unit

    def updateSystemAssertions(uuid: String, qualityAssertions: Map[String,Array[QualityAssertion]]): Unit

    def getSystemAssertions(uuid: String): List[QualityAssertion]

    def addUserAssertion(uuid: String, qualityAssertion: QualityAssertion): Unit

    def getUserAssertions(uuid: String): List[QualityAssertion]

    def deleteUserAssertion(uuid: String, assertionUuid: String): Boolean

    def updateAssertionStatus(uuid: String, assertionName: String, systemAssertions: List[QualityAssertion], userAssertions: List[QualityAssertion])

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
     * Get all versions of the occurrence with UUID
     *
     * @param uuid
     * @return
     */
    def getAllVersionsByUuid(uuid: String): Option[Array[FullRecord]] = {

        val map = persistenceManager.get(uuid, entityName)
        if (map.isEmpty) {
            None
        } else {
            // the versions of the record
            val raw = FullRecordMapper.createFullRecord(uuid, map.get, Raw)
            val processed = FullRecordMapper.createFullRecord(uuid, map.get, Processed)
            val consensus = FullRecordMapper.createFullRecord(uuid, map.get, Consensus)
            //pass all version to the procedure, wrapped in the Option
            Some(Array(raw, processed, consensus))
        }
    }

    /**
     * Get an occurrence, specifying the version of the occurrence.
     */
    def getByUuid(uuid: String, version: Version): Option[FullRecord] = {
        val propertyMap = persistenceManager.get(uuid, entityName)
        if (propertyMap.isEmpty) {
            None
        } else {
            Some(FullRecordMapper.createFullRecord(uuid, propertyMap.get, version))
        }
    }

    /**
     * Create or retrieve the UUID for this record. The uniqueID should be a
     * has of properties that provides a unique ID for the record within
     * the dataset.
     */
    def createOrRetrieveUuid(uniqueID: String): String = {

        val recordUUID = persistenceManager.get(uniqueID, "dr", "uuid")
        if (recordUUID.isEmpty) {
            val newUuid = UUID.randomUUID.toString
            persistenceManager.put(uniqueID, "dr", "uuid", newUuid)
            newUuid
        } else {
            recordUUID.get
        }
    }

    /**
     * Write to stream in a delimited format (CSV).
     */
    def writeToStream(outputStream: OutputStream, fieldDelimiter: String, recordDelimiter: String, uuids: Array[String], fields: Array[String]) {
        persistenceManager.selectRows(uuids, entityName, fields, {
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
                outputStream.write(recordDelimiter.getBytes)
        })
    }

    /**
     * Iterate over all occurrences, passing all versions of FullRecord
     * to the supplied function.
     * Function returns a boolean indicating if the paging should continue.
     *
     * @param occurrenceType
     * @param proc, the function to execute.
     * @param startUuid, The uuid of the occurrence at which to start the paging
     */
    def pageOverAllVersions(proc: ((Option[Array[FullRecord]]) => Boolean),startUuid:String="", pageSize: Int = 1000) {
        persistenceManager.pageOverAll(entityName, (guid, map) => {
            //retrieve all versions
            val raw = FullRecordMapper.createFullRecord(guid, map, Raw)
            val processed = FullRecordMapper.createFullRecord(guid, map, Processed)
            val consensus = FullRecordMapper.createFullRecord(guid, map, Consensus)
            //pass all version to the procedure, wrapped in the Option
            proc(Some(Array(raw, processed, consensus)))
        },startUuid, pageSize)
    }

    /**
     * Iterate over all occurrences, passing the objects to a function.
     * Function returns a boolean indicating if the paging should continue.
     *
     * @param occurrenceType
     * @param proc, the function to execute.
     * @param startUuid, The uuid of the occurrence at which to start the paging
     */
    def pageOverAll(version: Version, proc: ((Option[FullRecord]) => Boolean),startUuid:String="", pageSize: Int = 1000) {
        persistenceManager.pageOverAll(entityName, (guid, map) => {
            //retrieve all versions
            val fullRecord = FullRecordMapper.createFullRecord(guid, map, version)
            //pass all version to the procedure, wrapped in the Option
            proc(Some(fullRecord))
        },startUuid, pageSize)
    }

    /**
     * Iterates over the sepcified version of all the occurrence records. The values retrieved
     * from the persistence manager is limited to the supplied fields
     */
    def pageOverSelectAll(version: Version, proc: ((Option[FullRecord]) => Boolean),fields: Array[String],startUuid:String="", pageSize: Int = 1000){

    }

    /**
     * Iterate over all occurrences, passing the objects to a function.
     * Function returns a boolean indicating if the paging should continue.
     *
     * @param occurrenceType
     * @param proc, the function to execute.
     * @param startUuid, The uuid of the occurrence at which to start the paging
     */
    def pageOverRawProcessed(proc: (Option[(FullRecord, FullRecord)] => Boolean),startUuid:String="", pageSize: Int = 1000) {
        persistenceManager.pageOverAll(entityName, (guid, map) => {
            //retrieve all versions
            val raw = FullRecordMapper.createFullRecord(guid, map, Versions.RAW)
            val processed = FullRecordMapper.createFullRecord(guid, map, Versions.PROCESSED)
            //pass all version to the procedure, wrapped in the Option
            proc(Some(raw, processed))
        },startUuid, pageSize)
    }

    /**
     * Update the version of the occurrence record.
     */
    def addRawOccurrenceBatch(fullRecords: Array[FullRecord]) {

        var batch = scala.collection.mutable.Map[String, Map[String, String]]()
        for (fullRecord <- fullRecords) {
            var properties = fullRecord2Map(fullRecord, Versions.RAW)
            batch.put(fullRecord.uuid, properties.toMap)
        }
        //commit
        persistenceManager.putBatch(entityName, batch.toMap)
    }

    /**
     * if the objects is Mappable return the map of the properties otherwise returns an empty map
     */
    protected def mapObjectToProperties(anObject: AnyRef, version: Version): Map[String, String] = {
        var properties = scala.collection.mutable.Map[String, String]()
        if (anObject.isInstanceOf[Mappable]) {
            val map = anObject.asInstanceOf[Mappable].getMap


            map foreach {
                case (key, value) => {
                    version match {
                        case Processed => properties.put(FullRecordMapper.markAsProcessed(key), value)
                        case Consensus => properties.put(FullRecordMapper.markAsConsensus(key), value)
                        case Raw => properties.put(key, value)
                    }
                }
            }
        }
        else{
          val defn = FullRecordMapper.getDefn(anObject)
            for (field <- defn.keySet) {
                //val fieldValue = anObject.getter(field).asInstanceOf[String]
                //Use the cached version of the getter method
                val getter = defn.get(field).get.asInstanceOf[(Method,Method)]._1
                val fieldValue = getter.invoke(anObject)
                if (fieldValue != null) {
                    version match {
                      case Processed => properties.put(FullRecordMapper.markAsProcessed(field.toString), fieldValue.toString)
                      case Consensus => properties.put(FullRecordMapper.markAsConsensus(field.toString), fieldValue.toString)
                      case Raw => properties.put(field.toString, fieldValue.toString)

                    
                    }
                }
            }
        }
        properties.toMap

    }

    /**
     * Update the version of the occurrence record.
     */
    def updateOccurrence(uuid: String, fullRecord: FullRecord, version: Version) {
        updateOccurrence(uuid, fullRecord, None, version)
    }

    /**
     * Convert a full record to a map of properties
     */
    def fullRecord2Map(fullRecord: FullRecord, version: Version): scala.collection.mutable.Map[String, String] = {
        var properties = scala.collection.mutable.Map[String, String]()
        for (anObject <- fullRecord.objectArray) {
            val map = mapObjectToProperties(anObject, version)
            //add all to map
            properties.put("uuid", fullRecord.uuid)
            properties.putAll(map)
        }
        properties
    }

    /**
     * Update the occurrence with the supplied record, setting the correct version
     */
    def updateOccurrence(uuid: String, fullRecord: FullRecord, assertions: Option[Map[String,Array[QualityAssertion]]], version: Version) {

        //construct a map of properties to write
        val properties = fullRecord2Map(fullRecord, version)

        if (!assertions.isEmpty) {
            properties ++= convertAssertionsToMap(assertions.get)
            updateSystemAssertions(uuid, assertions.get)
        }

        //commit to cassandra
        persistenceManager.put(uuid, entityName, properties.toMap)
    }

    /**
     * Update the occurrence with the supplied record, setting the correct version
     */
    def updateOccurrence(uuid: String, oldRecord: FullRecord, newRecord: FullRecord, assertions: Option[Map[String,Array[QualityAssertion]]], version: Version) {

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

        if (!assertions.isEmpty){//} && !propertiesToPersist.isEmpty) {
            propertiesToPersist ++= convertAssertionsToMap(assertions.get)
            updateSystemAssertions(uuid, assertions.get)
        }
       

        //commit to cassandra if changes exist
        if(!propertiesToPersist.isEmpty)
          persistenceManager.put(uuid, entityName, propertiesToPersist.toMap)
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
     * @param uuid
     * @param anObject
     * @param occurrenceType
     */
    def updateOccurrence(uuid: String, anObject: AnyRef, version: Version) {
        val map = FullRecordMapper.mapObjectToProperties(anObject, version)
        persistenceManager.put(uuid, entityName, map)
    }

    /**
     * Adds a quality assertion to the row with the supplied UUID.
     *
     * @param uuid
     * @param qualityAssertion
     */
    def addSystemAssertion(uuid: String, qualityAssertion: QualityAssertion) {
        persistenceManager.putList(uuid, entityName, FullRecordMapper.qualityAssertionColumn, List(qualityAssertion), classOf[QualityAssertion], false)
        persistenceManager.put(uuid, entityName, qualityAssertion.name, qualityAssertion.problemAsserted.toString)
    }

    /**
     * Set the system systemAssertions for a record, overwriting existing systemAssertions
     * TODO change this so that it is updating the contents not replacing - will need this functionality when particular processing phases can be run seperately
     */
    def updateSystemAssertions(uuid: String, qualityAssertions: Map[String,Array[QualityAssertion]]) {
        var assertions = new ListBuffer[QualityAssertion] //getSystemAssertions(uuid)
        for(qas <- qualityAssertions.values){
          assertions ++= qas
        }
        persistenceManager.putList(uuid, entityName, FullRecordMapper.qualityAssertionColumn,assertions.toList, classOf[QualityAssertion], true)
    }

    /**
     * Retrieve annotations for the supplied UUID.
     */
    def getSystemAssertions(uuid: String): List[QualityAssertion] = {
        persistenceManager.getList(uuid, entityName, FullRecordMapper.qualityAssertionColumn, classOf[QualityAssertion])
    }

    /**
     * Add a user supplied assertion - updating the status on the record.
     */
    def addUserAssertion(uuid: String, qualityAssertion: QualityAssertion) {

        val userAssertions = getUserAssertions(uuid)

        if (!userAssertions.contains(qualityAssertion)) {
            val updatedUserAssertions = userAssertions :+ qualityAssertion
            val systemAssertions = getSystemAssertions(uuid)
            //store the new systemAssertions
            persistenceManager.putList(uuid, entityName, FullRecordMapper.userQualityAssertionColumn, updatedUserAssertions, classOf[QualityAssertion], true)
            //update the overall status
            updateAssertionStatus(uuid, qualityAssertion.name, systemAssertions, updatedUserAssertions)
        }
    }

    /**
     * Retrieve annotations for the supplied UUID.
     */
    def getUserAssertions(uuid: String): List[QualityAssertion] = {
        val theClass = classOf[QualityAssertion].asInstanceOf[java.lang.Class[AnyRef]]
        persistenceManager.getList(uuid, entityName, FullRecordMapper.userQualityAssertionColumn, theClass)
            .asInstanceOf[List[QualityAssertion]]
    }

    /**
     * Delete a user supplied assertion
     */
    def deleteUserAssertion(uuid: String, assertionUuid: String): Boolean = {

        logger.debug("Deleting assertion for : " + uuid + " with assertion uuid : " + uuid)

        //retrieve existing systemAssertions
        val assertions = getUserAssertions(uuid)

        //get the assertion that is to be deleted
        val deletedAssertion = assertions.find(assertion => {
            assertion.uuid equals assertionUuid
        })

        //if not empty, remove the assertion and write back
        if (!deletedAssertion.isEmpty) {

            //delete the assertion with the supplied UUID
            val updateAssertions = assertions.filter(qa => {
                !(qa.uuid equals assertionUuid)
            })

            //put the systemAssertions back - overwriting existing systemAssertions
            persistenceManager.putList(uuid, entityName, FullRecordMapper.userQualityAssertionColumn, updateAssertions, classOf[QualityAssertion], true)

            val assertionName = deletedAssertion.get.name
            //are there any matching systemAssertions for other users????
            val systemAssertions = getSystemAssertions(uuid)

            //update the assertion status
            updateAssertionStatus(uuid, assertionName, systemAssertions, updateAssertions)
            true
        } else {
            logger.warn("Unable to find assertion with UUID: " + assertionUuid)
            false
        }
    }

    /**
     * Update the assertion status using system and user systemAssertions.
     */
    def updateAssertionStatus(uuid: String, assertionName: String, systemAssertions: List[QualityAssertion], userAssertions: List[QualityAssertion]) {

        logger.info("Updating the assertion status for : " + uuid)

        val assertions = userAssertions.filter(qa => {
            qa.name equals assertionName
        })
        //update the status flag on the record, using the system quality systemAssertions
        if (!assertions.isEmpty) {

            //if anyone asserts the negative, the answer is negative
            val negativeAssertion = userAssertions.find(qa => qa.problemAsserted)
            if (!negativeAssertion.isEmpty) {
                val qualityAssertion = negativeAssertion.get
                persistenceManager.put(uuid,
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
                persistenceManager.put(uuid, entityName, assertion.name, assertion.problemAsserted.toString)
            }
        } else {
            persistenceManager.put(uuid, entityName, assertionName, true.toString)
        }

        //set the overall decision
        var properties = scala.collection.mutable.Map[String, String]()
        val geospatiallyKosher = AssertionCodes.isGeospatiallyKosher((userAssertions ++ systemAssertions).toArray)
        val taxonomicallyKosher = AssertionCodes.isTaxonomicallyKosher((userAssertions ++ systemAssertions).toArray)
        properties.put(FullRecordMapper.geospatialDecisionColumn, geospatiallyKosher.toString)
        properties.put(FullRecordMapper.taxonomicDecisionColumn, taxonomicallyKosher.toString)

        logger.info("Updating the assertion status for : " + uuid
            + ", geospatiallyKosher:" + geospatiallyKosher
            + ", taxonomicallyKosher:" + taxonomicallyKosher)

        persistenceManager.put(uuid, entityName, properties.toMap)
    }

    /**
     * Set this record to deleted.
     */
    def setUuidDeleted(uuid: String, del: Boolean) = {
        persistenceManager.put(uuid, entityName, FullRecordMapper.deletedColumn, del.toString)
    }

    /**
     * Should be possible to factor this out
     */
    def reIndex(uuid: String) {
        logger.debug("Reindexing UUID: " + uuid)
        val map = persistenceManager.get(uuid, entityName)        
        //index from the map - this should be more efficient
        if(map.isEmpty){
          logger.debug("Unable to reindex UUID: " + uuid)
        }
        else{
          indexDAO.indexFromMap(uuid, map.get, false)
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
