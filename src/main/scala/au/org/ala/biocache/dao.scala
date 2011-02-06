package au.org.ala.biocache

import au.org.ala.checklist.lucene.CBIndexSearch
import au.org.ala.util.ReflectBean
import java.io.OutputStream
import scala.collection.JavaConversions
import scala.collection.mutable.ArrayBuffer
import java.util.UUID
import collection.immutable.HashSet

/**
 * DAO configuration. Should be refactored to use a DI framework
 * or make use of Cake pattern.
 */
object DAO {

  import ReflectBean._
  val persistentManager = CassandraPersistenceManager
  val nameIndex = new CBIndexSearch("/data/lucene/namematching")

  //read in the object mappings using reflection
  val attributionDefn = loadDefn(classOf[Attribution])
  val occurrenceDefn = loadDefn(classOf[Occurrence])
  val classificationDefn = loadDefn(classOf[Classification])
  val locationDefn = loadDefn(classOf[Location])
  val eventDefn = loadDefn(classOf[Event])
  val identificationDefn = loadDefn(classOf[Identification])
  val measurementDefn = loadDefn(classOf[Measurement])

  //index definitions
  val occurrenceIndexDefn = fileToSet("/OccurrenceIndex.txt") //PROBABLY NOT THE BEST PLACE FOR THIS

  /** Retrieve the set of fields for the supplied class */
  protected def loadDefn(theClass:java.lang.Class[_]) : Set[String] = {
    HashSet() ++ theClass.getDeclaredFields.map(_.getName).toList
  }

  protected def fileToSet(filePath:String) : Set[String] =
    scala.io.Source.fromURL(getClass.getResource(filePath), "utf-8").getLines.toList.map(_.trim).toSet

  /**
   * for each field in the definition, check if there is a value to write
   */
  def mapObjectToProperties(anObject:AnyRef): Map[String,String] = {
    val defn = getDefn(anObject)
    var properties = scala.collection.mutable.Map[String,String]()
    for (field <- defn) {
        val fieldValue = anObject.getter(field).asInstanceOf[String]
        if (fieldValue != null && !fieldValue.isEmpty) {
             properties.put(field, fieldValue)
        }
    }
    properties.toMap
  }

  /**
   * Set the property on the correct model object
   */
  def mapPropertiesToObject(anObject:AnyRef, map:Map[String,String]){
    //TODO supplied properties will be less that properties in object this could be an optimisation
    val defn = getDefn(anObject)
    for(fieldName<-defn){
      val fieldValue = map.get(fieldName)
      if(!fieldValue.isEmpty && fieldValue.get.length>0){
        anObject.setter(fieldName,fieldValue.get)
      }
    }
  }

  /**
   * Retrieve a object definition (simple ORM mapping)
   */
  def getDefn(anObject:Any) : Set[String] = {
     anObject match {
      case l:Location => DAO.locationDefn
      case o:Occurrence => DAO.occurrenceDefn
      case e:Event => DAO.eventDefn
      case c:Classification => DAO.classificationDefn
      case a:Attribution => DAO.attributionDefn
      case i:Identification => DAO.identificationDefn
      case m:Measurement => DAO.measurementDefn
      case oi:OccurrenceIndex => DAO.occurrenceIndexDefn
      case _ => throw new RuntimeException("Unrecognised entity. No definition registered for: "+anObject)
     }
  }
}

/**
 * A DAO for accessing occurrences.
 */
object OccurrenceDAO {

  import ReflectBean._
  import JavaConversions._
  import scalaj.collection.Imports._

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
  protected def setProperty(fullRecord:FullRecord, fieldName:String, fieldValue:String){
    if(DAO.occurrenceDefn.contains(fieldName)){
      fullRecord.occurrence.setter(fieldName,fieldValue)
    } else if(DAO.classificationDefn.contains(fieldName)){
      fullRecord.classification.setter(fieldName,fieldValue)
    } else if(DAO.locationDefn.contains(fieldName)){
      fullRecord.location.setter(fieldName,fieldValue)
    } else if(DAO.eventDefn.contains(fieldName)){
      fullRecord.event.setter(fieldName,fieldValue)
    } else if(DAO.attributionDefn.contains(fieldName)){
      fullRecord.attribution.setter(fieldName,fieldValue)
    } else if(DAO.identificationDefn.contains(fieldName)){
      fullRecord.identification.setter(fieldName,fieldValue)
    } else if(DAO.measurementDefn.contains(fieldName)){
      fullRecord.measurement.setter(fieldName,fieldValue)
    } else if(isQualityAssertion(fieldName)){
      fullRecord.assertions = fullRecord.assertions ++ Array(fieldName)
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

    var fullRecord = new FullRecord
    fullRecord.occurrence.uuid = uuid
    var assertions = new ArrayBuffer[String]
    val columns = fields.keySet
    for(fieldName<-columns){

      //ascertain which term should be associated with which object
      val fieldValue = fields.get(fieldName)
      if(!fieldValue.isEmpty){
        if(isProcessedValue(fieldName) && version == Processed){
          setProperty(fullRecord, removeSuffix(fieldName), fieldValue.get)
        } else if(isConsensusValue(fieldName) && version == Consensus){
          setProperty(fullRecord, removeSuffix(fieldName), fieldValue.get)
        } else if(version == Raw){
          setProperty(fullRecord, fieldName, fieldValue.get)
        } else if(isQualityAssertion(fieldName)){
          setProperty(fullRecord, fieldName, fieldValue.get)
        }
      }
    }
    fullRecord
  }

  /** Remove the suffix indicating the version of the field */
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

    for(anObject <- fullRecord.objectArray){
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
        setSystemAssertions(uuid, assertions.toList)
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
    DAO.persistentManager.putList(uuid,entityName, qualityAssertionColumn,List(qualityAssertion),false)
    DAO.persistentManager.put(uuid, entityName, qualityAssertion.assertionName, qualityAssertion.positive.toString)
  }

  /**
   * Set the system assertions for a record, overwriting existing assertions
   */
  def setSystemAssertions(uuid:String, qualityAssertions:List[QualityAssertion]){
    DAO.persistentManager.putList(uuid,entityName, qualityAssertionColumn,qualityAssertions,true)
  }

  /**
   * Retrieve annotations for the supplied UUID.
   */
  def getQualityAssertions(uuid:String): List[QualityAssertion] = {
    //val theClass = (Array(new QualityAssertion())).getClass.asInstanceOf[java.lang.Class[Array[AnyRef]]]
    val theClass = classOf[QualityAssertion].asInstanceOf[java.lang.Class[AnyRef]]
    DAO.persistentManager.getList(uuid,entityName, qualityAssertionColumn,theClass).asInstanceOf[List[QualityAssertion]]
  }

  def getQualityAssertionsJ(uuid:String): java.util.List[QualityAssertion] = {

    getQualityAssertions(uuid).asJava
  }

  /**
   * Add a user supplied assertion - updating the status on the record.
   */
  def addUserQualityAssertion(uuid:String, qualityAssertion:QualityAssertion){

    val userAssertions = getUserQualityAssertions(uuid) ++ List(qualityAssertion)
    val systemAssertions = getQualityAssertions(uuid)

    //store the new assertions
    DAO.persistentManager.putList(uuid,entityName,userQualityAssertionColumn,userAssertions,true)

    //update the overall status
    updateAssertionStatus(uuid,qualityAssertion.assertionName,systemAssertions,userAssertions)
  }

  /**
   * Retrieve annotations for the supplied UUID.
   */
  def getUserQualityAssertions(uuid:String): List[QualityAssertion] = {
    val theClass = classOf[QualityAssertion].asInstanceOf[java.lang.Class[AnyRef]]
    DAO.persistentManager.getList(uuid,entityName, userQualityAssertionColumn,theClass)
        .asInstanceOf[List[QualityAssertion]]
  }

  /**
   * Delete a user supplied assertion
   */
  def deleteUserQualityAssertion(uuid:String, assertionUuid:String) : Boolean = {

    //retrieve existing assertions
    val assertions = getUserQualityAssertions(uuid)

    //get the assertion that is to be deleted
    val deletedAssertion = assertions.find(assertion => {println("ASSERTION UUID: "+assertion.uuid);assertion.uuid equals assertionUuid})

    //if not empty, remove the assertion and write back
    if(!deletedAssertion.isEmpty){

        //delete the assertion with the supplied UUID
        val updateAssertions = assertions.filter(qa => {!(qa.uuid equals assertionUuid)})

        //put the assertions back - overwriting existing assertions
        DAO.persistentManager.putList(uuid,entityName,userQualityAssertionColumn,updateAssertions,true)

        val assertionName = deletedAssertion.get.assertionName
        //are there any matching assertions for other users????
        val systemAssertions = getQualityAssertions(uuid)

        //update the assertion status
        updateAssertionStatus(uuid,assertionName,systemAssertions,updateAssertions)
        true
    } else {
        println("########## Unable to find assertion with UUID: "+ assertionUuid)
        false
    }
  }

  /**
   * Update the assertion status using system and user assertions.
   */
  def updateAssertionStatus(uuid:String, assertionName:String, systemAssertions:List[QualityAssertion], userAssertions:List[QualityAssertion])  {

    val assertions = userAssertions.filter(qa => {qa.assertionName equals assertionName})
    //update the status flag on the record, using the system quality assertions
    if(assertions.size>0) {
        //if anyone asserts the negative, the answer is negative
        val positive = userAssertions.foldLeft(true)( (isPositive,qualityAssertion) => { isPositive && qualityAssertion.positive } )
        DAO.persistentManager.put(uuid,entityName,assertionName,positive.toString)
    } else if(systemAssertions.size>0){
        //check system assertions for an answer
        val matchingAssertion = systemAssertions.find(assertion => {assertion.assertionName equals assertionName})
        if(!matchingAssertion.isEmpty){
            val assertion = matchingAssertion.get
            DAO.persistentManager.put(uuid,entityName,assertion.assertionName,assertion.positive.toString)
        }
    } else {
        DAO.persistentManager.put(uuid,entityName,assertionName,true.toString)
    }
  }
}
