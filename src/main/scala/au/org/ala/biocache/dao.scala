package au.org.ala.biocache

import au.org.ala.checklist.lucene.CBIndexSearch
import com.google.gson.Gson
import au.org.ala.util.ReflectBean
import java.io.OutputStream
import scala.collection.JavaConversions
import scala.collection.mutable.ArrayBuffer
import java.util.UUID
import org.wyki.cassandra.pelops.{Pelops, Policy}

/**
 * DAO configuration. Should be refactored to use a DI framework
 * or make use of Cake pattern.
 */
object DAO {

  import ReflectBean._
  val persistentManager = CassandraPersistenceManager
  val nameIndex = new CBIndexSearch("/data/lucene/namematching")

  //read in the ORM mappings
  val attributionDefn = fileToArray("/Attribution.txt")
  val occurrenceDefn = fileToArray("/Occurrence.txt")
  val locationDefn = fileToArray("/Location.txt")
  val eventDefn = fileToArray("/Event.txt")
  val classificationDefn = fileToArray("/Classification.txt")
  val identificationDefn = fileToArray("/Identification.txt")
  val occurrenceIndexDefn = DAO.fileToArray("/OccurrenceIndex.txt") //PROBABLY NOT THE BEST PLACE FOR THIS

  protected def fileToArray(filePath:String) : List[String] =
    scala.io.Source.fromURL(getClass.getResource(filePath), "utf-8").getLines.toList.map(_.trim)

  /**
   * for each field in the definition, check if there is a value to write
   */
  def mapObjectToProperties(anObject:AnyRef): Map[String,String] = {
    val defn = getDefn(anObject)
    var properties = scala.collection.mutable.Map[String,String]()
    for (field <- defn) {
        val fieldValue = anObject.getClass.getMethods.find(_.getName == field).get.invoke(anObject).asInstanceOf[String]
        if (fieldValue != null && !fieldValue.isEmpty) {
             properties.put(field, fieldValue)
        }
    }
    properties.toMap
  }

    /**
   * Set the property on the correct model object
   */
  protected def mapPropertiesToObject(anObject:AnyRef, map:Map[String,String]){
    val defn = getDefn(anObject)
    for(fieldName<-defn){
      val fieldValue = map.get(fieldName)
      if(!fieldValue.isEmpty){
        anObject.setter(fieldName,fieldValue)
      }
    }
  }

  /**
   * Retrieve a object definition (simple ORM mapping)
   */
  def getDefn(anObject:Any) : List[String] = {
     anObject match {
      case l:Location => DAO.locationDefn
      case o:Occurrence => DAO.occurrenceDefn
      case e:Event => DAO.eventDefn
      case c:Classification => DAO.classificationDefn
      case a:Attribution => DAO.attributionDefn
      case _ => throw new RuntimeException("Unrecognised entity. No definition registered for: "+anObject)
     }
  }
}

/**
 * A trait to implement by java classes to process occurrence records.
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

/**
 * A DAO for accessing occurrences.
 */
object OccurrenceDAO {

  import ReflectBean._
  import JavaConversions._

  private val entityName = "occ"
  private val qualityAssertionColumn = "qualityAssertion"
  private val userQualityAssertionColumn = "userQualityAssertion"

  /**
   * Get an occurrence with UUID
   *
   * @param uuid
   * @return
   */
  def getByUuid(uuid:String) : Option[FullRecord] = {
    getByUuid(uuid, Raw)
  }

  /**
   * Get all versions of the occurrence with UUID
   *
   * @param uuid
   * @return
   */
  def getAllVersionsByUuid(uuid:String) : Option[Array[FullRecord]] = {

    val map = DAO.persistentManager.get(uuid, entityName)
    if(map.isEmpty){
      None
    } else {
      //create the versions of the record
      val raw = createOccurrence(uuid, map.get, Raw)
      val processed = createOccurrence(uuid, map.get, Processed)
      val consensus = createOccurrence(uuid, map.get, Consensus)
      //pass all version to the procedure, wrapped in the Option
      Some(Array(raw, processed, consensus))
    }
  }

  /**
   * A java API friendly version of the getByUuid that doesnt require knowledge of a scala type.
   */
  def getByUuidJ(uuid:String) : FullRecord = {
    val record = getByUuid(uuid, Raw)
    if(record.isEmpty){
      null
    } else {
      record.get
    }
  }  
  /**
   * Get an occurrence, specifying the version of the occurrence.
   */
  def getByUuid(uuid:String, version:Version) : Option[FullRecord] = {
    val propertyMap = DAO.persistentManager.get(uuid, entityName)
    if(propertyMap.isEmpty){
      None
    } else {
      Some(createOccurrence(uuid, propertyMap.get, version))
    }
  }

  /**
   * Set the property on the correct model object
   */
  protected def setProperty(o:Occurrence, c:Classification, l:Location, e:Event, assertions:ArrayBuffer[String], 
		  fieldName:String, fieldValue:String){
    if(DAO.occurrenceDefn.contains(fieldName)){
      o.setter(fieldName,fieldValue)
    } else if(DAO.classificationDefn.contains(fieldName)){
      c.setter(fieldName,fieldValue)
    } else if(DAO.eventDefn.contains(fieldName)){
      e.setter(fieldName,fieldValue)
    } else if(DAO.locationDefn.contains(fieldName)){
      l.setter(fieldName,fieldValue)
    } else if(isQualityAssertion(fieldName)){
      assertions.add(fieldName)
    }
  }

  /**
   * Create a record from a array of tuple properties
   */
  def createOccurrence(uuid:String, fieldTuples:Array[(String,String)], version:Version) : FullRecord = {
    val fieldMap = Map(fieldTuples map {s => (s._1, s._2)} : _*)
    createOccurrence(uuid, fieldMap, version)
  }

  /**
   * Creates an FullRecord from the map of properties
   */
  def createOccurrence(uuid:String, fields:Map[String,String], version:Version) : FullRecord = {

    val occurrence = new Occurrence
    val classification = new Classification
    val location = new Location
    val event = new Event
    var assertions = new ArrayBuffer[String]

    occurrence.uuid = uuid
    val columns = fields.keySet
    for(fieldName<-columns){

      //ascertain which term should be associated with which object
      val fieldValue = fields.get(fieldName)
      if(!fieldValue.isEmpty){
        if(isProcessedValue(fieldName) && version == Processed){
          setProperty(occurrence, classification, location, event, assertions, removeSuffix(fieldName), fieldValue.get)
        } else if(isConsensusValue(fieldName) && version == Consensus){
          setProperty(occurrence, classification, location, event, assertions, removeSuffix(fieldName), fieldValue.get)
        } else if(version == Raw){
          setProperty(occurrence, classification, location, event, assertions, fieldName, fieldValue.get)
        } else if(isQualityAssertion(fieldName)){
          setProperty(occurrence, classification, location, event, assertions, fieldName, fieldValue.get)
        }
      }
    }
    //construct the full record
    new FullRecord(occurrence, classification, location, event, assertions.toArray)
  }

  /** Remove the suffix indicating the version of the field*/
  private def removeSuffix(name:String) : String = name.substring(0, name.length - 2)

   /** Is this a "processed" value? */
  private def isProcessedValue(name:String) : Boolean = name endsWith ".p"

  /** Is this a "consensus" value? */
  private def isConsensusValue(name:String) : Boolean = name endsWith ".c"

  /** Is this a "consensus" value? */
  private def isQualityAssertion(name:String) : Boolean = name endsWith ".qa"

  /** Add a suffix to this field name to indicate version type */
  private def markAsProcessed(name:String) : String = name + ".p"

  /** Add a suffix to this field name to indicate version type */
  private def markAsConsensus(name:String) : String = name + ".c"

  /** Add a suffix to this field name to indicate quality assertion field */
  private def markAsQualityAssertion(name:String) : String = name + ".qa"

  /**
   * Iterate over records, passing the records to the supplied consumer.
   */
  def pageOverAll(version:Version, consumer:OccurrenceConsumer) {
    pageOverAll(version, fullRecord => consumer.consume(fullRecord.get))
  }

  /**
   * Create or retrieve the UUID for this record. The uniqueID should be a
   * has of properties that provides a unique ID for the record within
   * the dataset.
   */
  def createOrRetrieveUuid(uniqueID: String): String = {

    val recordUUID = DAO.persistentManager.get(uniqueID, "dr", "uuid")
    if(recordUUID.isEmpty){
      val newUuid = UUID.randomUUID.toString
      DAO.persistentManager.put(uniqueID, "dr", "uuid", newUuid)
      newUuid
    } else {
      recordUUID.get
    }
  }

  /**
   * Write to stream in a delimited format (CSV).
   */
  def writeToStream(outputStream:OutputStream,fieldDelimiter:String,recordDelimiter:String,uuids:Array[String],fields:Array[String]) {
    DAO.persistentManager.selectRows(uuids, entityName, fields, { fieldMap =>
      for(field<-fields){
        val fieldValue = fieldMap.get(field)
        outputStream.write(fieldValue.getOrElse("").getBytes)
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
   */
  def pageOverAllVersions(proc:((Option[Array[FullRecord]])=>Boolean) ) {
     DAO.persistentManager.pageOverAll(entityName, (guid, map) => {
       //retrieve all versions
       val raw = createOccurrence(guid, map, Raw)
       val processed = createOccurrence(guid, map, Processed)
       val consensus = createOccurrence(guid, map, Consensus)
       //pass all version to the procedure, wrapped in the Option
       proc(Some(Array(raw, processed, consensus)))
     })
  }

  /**
   * Page over all versions of the record, handing off to the OccurrenceVersionConsumer.
   */
  def pageOverAllVersions(consumer:OccurrenceVersionConsumer) {
     DAO.persistentManager.pageOverAll(entityName, (guid, map) => {
       //retrieve all versions
       val raw = createOccurrence(guid, map, Raw)
       val processed = createOccurrence(guid, map, Processed)
       val consensus = createOccurrence(guid, map, Consensus)
       //pass all version to the procedure, wrapped in the Option
       consumer.consume(Array(raw, processed, consensus))
     })
  }

  /**
   * Iterate over all occurrences, passing the objects to a function.
   * Function returns a boolean indicating if the paging should continue.
   *
   * @param occurrenceType
   * @param proc, the function to execute.
   */
  def pageOverAll(version:Version, proc:((Option[FullRecord])=>Boolean) ) {
     DAO.persistentManager.pageOverAll(entityName, (guid, map) => {
       //retrieve all versions
       val fullRecord = createOccurrence(guid, map, version)
       //pass all version to the procedure, wrapped in the Option
       proc(Some(fullRecord))
     })
  }

  /**
   * Update the version of the occurrence record.
   */
  def updateOccurrence(uuid:String, fullRecord:FullRecord, version:Version) {
    updateOccurrence(uuid,fullRecord,Array(),version)
  }

  /**
   * for each field in the definition, check if there is a value to write
   */
  protected def mapObjectToProperties(anObject:AnyRef, version:Version): Map[String,String] = {
    var properties = scala.collection.mutable.Map[String,String]()
    val defn = DAO.getDefn(anObject)
    for (field <- defn) {
        val fieldValue = anObject.getClass.getMethods.find(_.getName == field).get.invoke(anObject).asInstanceOf[String]
        if (fieldValue != null && !fieldValue.isEmpty) {
            version match {
                case Processed => properties.put(markAsProcessed(field), fieldValue)
                case Consensus => properties.put(markAsConsensus(field), fieldValue)
                case _ => properties.put(field, fieldValue)
            }
        }
    }
    properties.toMap
  }

  /**
   * Update the occurrence with the supplied record, setting the correct version
   */
  def updateOccurrence(uuid:String, fullRecord:FullRecord, assertions:Array[QualityAssertion], version:Version) {
    //construct a map of properties to write
    var properties = scala.collection.mutable.Map[String,String]()

    for(anObject <- Array(fullRecord.o,fullRecord.c,fullRecord.e,fullRecord.l)){
      val map = mapObjectToProperties(anObject,version)
      //add all to map
      properties.putAll(map)
    }

    //set the assertions on the full record
    fullRecord.assertions = assertions.toArray.map(_.assertionName)
    //set the quality assertions flags
    for(qa <- assertions){
      properties.put(markAsQualityAssertion(qa.assertionName), qa.positive.toString)
    }

    if(!assertions.isEmpty){
      //serialise the assertion list to JSON and DB
      val gson = new Gson
      val json = gson.toJson(assertions)
      properties.put(qualityAssertionColumn, json)
    }

    //commit to cassandra
    DAO.persistentManager.put(uuid,entityName,properties.toMap)
  }

  /**
   * Update an occurrence entity. E.g. Occurrence, Classification, Taxon
   *
   * @param uuid
   * @param anObject
   * @param occurrenceType
   */
  def updateOccurrence(uuid:String, anObject:AnyRef, version:Version) {

    val map = mapObjectToProperties(anObject,version)
    DAO.persistentManager.put(uuid,entityName,map)
  }

  /**
   * Adds a quality assertion to the row with the supplied UUID.
   * 
   * @param uuid
   * @param qualityAssertion
   */
  def addQualityAssertion(uuid:String, qualityAssertion:QualityAssertion){
    DAO.persistentManager.putArray(uuid,entityName, qualityAssertionColumn,Array(qualityAssertion).asInstanceOf[Array[AnyRef]],false)
    DAO.persistentManager.put(uuid, entityName, qualityAssertion.assertionName, qualityAssertion.positive.toString)
  }

  /**
   * Retrieve annotations for the supplied UUID.
   */
  def getQualityAssertions(uuid:String): Array[QualityAssertion] = {
    val theClass = (Array(new QualityAssertion())).getClass.asInstanceOf[Class[AnyRef]]
    DAO.persistentManager.getArray(uuid,entityName, qualityAssertionColumn,theClass).asInstanceOf[Array[QualityAssertion]]
  }

  /**
   * Add a user supplied assertion - updating the status on the record.
   * This will by default,
   *
   */
  def addUserQualityAssertion(uuid:String, qualityAssertion:QualityAssertion){
    DAO.persistentManager.putArray(uuid,entityName, userQualityAssertionColumn,Array(qualityAssertion).asInstanceOf[Array[AnyRef]],false)
    DAO.persistentManager.put(uuid,entityName, qualityAssertion.assertionName,qualityAssertion.positive.toString)
  }


  /**
   * Retrieve annotations for the supplied UUID.
   */
  def getUserQualityAssertions(uuid:String): Array[QualityAssertion] = {
    val theClass = (Array(new QualityAssertion())).getClass.asInstanceOf[Class[AnyRef]]
    DAO.persistentManager.getArray(uuid,entityName, userQualityAssertionColumn,theClass)
        .asInstanceOf[Array[QualityAssertion]]
  }

  /**
  * Delete a user supplied assertion
  */
  def deleteUserQualityAssertion(uuid:String, assertionUuid:String) {

    val assertions = getUserQualityAssertions(uuid)

    //get the assertion that is to be deleted
    val deletedAssertion = assertions.find(qa => {qa.uuid == uuid})

    if(!deletedAssertion.isEmpty){

        val assertionName = deletedAssertion.get.assertionName

        //delete the assertion with the supplied UUID
        val updateAssertions = assertions.filter(qa => {qa.uuid != uuid})

        //put the assertions back - overwriting existing assertions
        //CassandraPersistenceManager.putArray(uuid,userQualityAssertionColumn,updateAssertions.asInstanceOf[Array[Comparable[AnyRef]]],true)
        DAO.persistentManager.putArray(uuid,entityName,userQualityAssertionColumn,updateAssertions.asInstanceOf[Array[AnyRef]],true)

        //update the status flag on the record, using the system quality assertions
        val systemAssertions = getQualityAssertions(uuid)

        //find other assertions for this code
//        updateAssertions.filter()

        //is there another user assertion asserting the property ???

        //default to "positive" if there are no system quality assertions for the property
    }
  }
}
