package au.org.ala.biocache

import au.org.ala.checklist.lucene.CBIndexSearch
//import au.org.ala.sds.SensitiveSpeciesFinderFactory
import au.org.ala.util.ReflectBean
import java.io.OutputStream
import scala.collection.JavaConversions
import scala.collection.mutable.ArrayBuffer
import java.util.Properties
import java.util.UUID
import collection.immutable.HashSet
import org.apache.commons.dbcp.BasicDataSource
import org.slf4j.LoggerFactory
import com.google.inject.Guice
import com.google.inject.Module
import com.google.inject.Binder
import com.google.inject.name.Names

/**
 * DAO configuration. Should be refactored to use a DI framework
 * or make use of Cake pattern.
 */
object DAO {

  import ReflectBean._
  protected val logger = LoggerFactory.getLogger("DAO")

  val persistentManager = {
      try {
          val properties = new Properties()
          properties.load(DAO.getClass.getResourceAsStream("/biocache.properties"))
          logger.info("Properties loaded from biocache.properties on classpath")
          if(properties!=null){
             val hostArray = properties.getProperty("cassandraHosts").split(",")
             val port = properties.getProperty("cassandraPort").toInt
             logger.info("Properties loaded from biocache.properties on classpath. hostArray: "+hostArray(0))
             logger.info("Properties loaded from biocache.properties on classpath. port: "+port)
             new CassandraPersistenceManager(hostArray.toArray[String], port)
          } else {
             logger.warn("Unable to load configuration parameters from biocache.properties. Using default settings.");
             new CassandraPersistenceManager
          }
      } catch {
          case e:Exception => {
             logger.warn("Unable to load configuration from biocache.properties. Using default settings.");
             new CassandraPersistenceManager
          }
      }
  }
  val indexer = SolrOccurrenceDAO

  val nameIndex = new CBIndexSearch("/data/lucene/namematching")

  //Only used during record processing - it will take awhile the first time it is accessed
//  lazy val sensitiveSpeciesFinderFactory = {
//    val dataSource = new BasicDataSource();
//    dataSource.setDriverClassName("com.mysql.jdbc.Driver")
//    dataSource.setUrl("jdbc:mysql://localhost/portal")
//    dataSource.setUsername("root")
//    dataSource.setPassword("password")
//    try{
//      val properties = new Properties()
//          properties.load(DAO.getClass.getResourceAsStream("/sds.properties"))
//          logger.info("Properties loaded from sensitive.properties on classpath")
//          if(properties!=null){
//            val driver = properties.getProperty("dataSource.driver")
//            val url = properties.getProperty("dataSource.url")
//            val username = properties.getProperty("dataSource.username")
//            val password = properties.getProperty("dataSource.password")
//            dataSource.setDriverClassName(driver)
//            dataSource.setUrl(url)
//            dataSource.setUsername(username)
//            dataSource.setPassword(password)
//          }
//    }
//    catch{
//      case e :Exception => logger.warn("Unable to load sensitive data service configuration. Using default settings")
//    }
//    SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder(dataSource, nameIndex);
//  }
  //read in the object mappings using reflection
  val attributionDefn = loadDefn(classOf[Attribution])
  val occurrenceDefn = loadDefn(classOf[Occurrence])
  val classificationDefn = loadDefn(classOf[Classification])
  val locationDefn = loadDefn(classOf[Location])
  val eventDefn = loadDefn(classOf[Event])
  val identificationDefn = loadDefn(classOf[Identification])
  val measurementDefn = loadDefn(classOf[Measurement])

  //index definitions
  val occurrenceIndexDefn = loadDefn(classOf[OccurrenceIndex])//PROBABLY NOT THE BEST PLACE FOR THIS

  /** Retrieve the set of fields for the supplied class */
  protected def loadDefn(theClass:java.lang.Class[_]) : Set[String] = {
    HashSet() ++ theClass.getDeclaredFields.map(_.getName).toList
  }

  protected def fileToSet(filePath:String) : Set[String] =
    scala.io.Source.fromURL(getClass.getResource(filePath), "utf-8").getLines.toList.map(_.trim).toSet

  /**
   * for each field in the definition, check if there is a value to write
   * Change to use the toMap method of a Mappable
   */
  def mapObjectToProperties(anObject:AnyRef): Map[String,String] = {
    var properties = scala.collection.mutable.Map[String,String]()
    if(anObject.isInstanceOf[Mappable]){

      val map = anObject.asInstanceOf[Mappable].getMap
      map foreach {case (key, value)=> {
               properties.put(key, value)
          }}

    }
    else{
      val defn = getDefn(anObject)
      for (field <- defn) {
          val fieldValue = anObject.getter(field).asInstanceOf[String]
          if (fieldValue != null && !fieldValue.isEmpty) {
               properties.put(field, fieldValue)
          }
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
      if(!fieldValue.isEmpty && !fieldValue.get.trim.isEmpty){
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
  protected val logger = LoggerFactory.getLogger("OccurrenceDAO")
  private val entityName = "occ"
  private val qualityAssertionColumn = "qualityAssertion"
  val userQualityAssertionColumn = "userQualityAssertion"
  val geospatialDecisionColumn = "geospatiallyKosher"
  val taxonomicDecisionColumn = "taxonomicallyKosher"

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
      val raw = createFullRecord(uuid, map.get, Raw)
      val processed = createFullRecord(uuid, map.get, Processed)
      val consensus = createFullRecord(uuid, map.get, Consensus)
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
      Some(createFullRecord(uuid, propertyMap.get, version))
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
      if(fieldValue equals "true"){
        fullRecord.assertions = fullRecord.assertions :+ removeQualityAssertionMarker(fieldName)
      }
    }
  }

  /**
   * Create a record from a array of tuple properties
   */
  def createFullRecord(uuid:String, fieldTuples:Array[(String,String)], version:Version) : FullRecord = {
    val fieldMap = Map(fieldTuples map { s => (s._1, s._2) } : _*)
    createFullRecord(uuid, fieldMap, version)
  }

  /**
   * Creates an FullRecord from the map of properties
   */
  def createFullRecord(uuid:String, fields:Map[String,String], version:Version) : FullRecord = {

    var fullRecord = new FullRecord
    fullRecord.uuid = uuid
    var assertions = new ArrayBuffer[String]
    val columns = fields.keySet
    for(fieldName<-columns){

      //ascertain which term should be associated with which object
      val fieldValue = fields.get(fieldName)
      if(!fieldValue.isEmpty){
        if(isQualityAssertion(fieldName)){
          setProperty(fullRecord, fieldName, fieldValue.get)
        } else if(taxonomicDecisionColumn.equals(fieldName)){
          fullRecord.taxonomicallyKosher = "true".equals(fieldValue.get)
        } else if(geospatialDecisionColumn.equals(fieldName)){
          fullRecord.geospatiallyKosher = "true".equals(fieldValue.get)
        } else if(isProcessedValue(fieldName) && version == Processed){
          setProperty(fullRecord, removeSuffix(fieldName), fieldValue.get)
        } else if(isConsensusValue(fieldName) && version == Consensus){
          setProperty(fullRecord, removeSuffix(fieldName), fieldValue.get)
        } else if(version == Raw){
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
  def isQualityAssertion(name:String) : Boolean = name endsWith ".qa"

  /** Add a suffix to this field name to indicate version type */
  private def markAsProcessed(name:String) : String = name + ".p"

  /** Add a suffix to this field name to indicate version type */
  private def markAsConsensus(name:String) : String = name + ".c"

  /** Add a suffix to this field name to indicate quality assertion field */
  private def markAsQualityAssertion(name:String) : String = name + ".qa"

  /** Remove the quality assertion marker */
  def removeQualityAssertionMarker(name:String) : String = name.dropRight(3)

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
  def pageOverAllVersions(proc:((Option[Array[FullRecord]])=>Boolean), pageSize:Int = 1000) {
     DAO.persistentManager.pageOverAll(entityName, (guid, map) => {
       //retrieve all versions
       val raw = createFullRecord(guid, map, Raw)
       val processed = createFullRecord(guid, map, Processed)
       val consensus = createFullRecord(guid, map, Consensus)
       //pass all version to the procedure, wrapped in the Option
       proc(Some(Array(raw, processed, consensus)))
     }, pageSize)
  }

  /**
   * Iterate over all occurrences, passing the objects to a function.
   * Function returns a boolean indicating if the paging should continue.
   *
   * @param occurrenceType
   * @param proc, the function to execute.
   */
  def pageOverAll(version:Version, proc:((Option[FullRecord])=>Boolean),pageSize:Int = 1000) {
     DAO.persistentManager.pageOverAll(entityName, (guid, map) => {
       //retrieve all versions
       val fullRecord = createFullRecord(guid, map, version)
       //pass all version to the procedure, wrapped in the Option
       proc(Some(fullRecord))
     },pageSize)
  }

  /**
   * Update the version of the occurrence record.
   */
  def addRawOccurrenceBatch(fullRecords:Array[FullRecord]) {

    var batch = scala.collection.mutable.Map[String,Map[String,String]]()
    for(fullRecord<-fullRecords){
        var properties = fullRecord2Map(fullRecord, Versions.RAW)
        batch.put(fullRecord.uuid, properties.toMap)
    }
    //commit to cassandra
    DAO.persistentManager.putBatch(entityName,batch.toMap)
  }

  /**
   * if the objects is Mappable return the map of the properties otherwise returns an empty map
   */
  protected def mapObjectToProperties(anObject:AnyRef, version:Version): Map[String,String] = {
    var properties = scala.collection.mutable.Map[String,String]()
    if(anObject.isInstanceOf[Mappable]){
      val map = anObject.asInstanceOf[Mappable].getMap


      map foreach {case (key, value)=> {
            version match{
              case Processed => properties.put(markAsProcessed(key), value)
              case Consensus => properties.put(markAsConsensus(key), value)
              case Raw => properties.put(key, value)
            }
          }}
    }
    properties.toMap

  }

  /**
   * Update the version of the occurrence record.
   */
  def updateOccurrence(uuid:String, fullRecord:FullRecord, version:Version) {
    updateOccurrence(uuid,fullRecord,None,version)
  }

  /**
   * Convert a full record to a map of properties
   */
  def fullRecord2Map(fullRecord:FullRecord, version:Version) : scala.collection.mutable.Map[String,String] = {
    var properties = scala.collection.mutable.Map[String,String]()
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
  def updateOccurrence(uuid:String, fullRecord:FullRecord, assertions:Option[Array[QualityAssertion]], version:Version) {

    //construct a map of properties to write
    val properties = fullRecord2Map(fullRecord,version)

    //if supplied, update the assertions
    if(!assertions.isEmpty){
	    val systemAssertions = assertions.get
	    
	    //set the systemAssertions on the full record
	    fullRecord.assertions = systemAssertions.toArray.map(_.name)
	
	    //set the quality systemAssertions flags for all error codes - following the principle writes are fast
	    for(qa <- systemAssertions){
	      properties.put(markAsQualityAssertion(qa.name), qa.problemAsserted.toString)
	    }
	
	    //for the
	    val cateredForCodes = systemAssertions.toArray.map(_.code).toSet
	    val uncateredForCodes = AssertionCodes.all.filter(errorCode => {!cateredForCodes.contains(errorCode.code)})
	    for(errorCode <- uncateredForCodes){
	        properties.put(markAsQualityAssertion(errorCode.name), "false".toString)
	    }
	
	    updateSystemAssertions(uuid, systemAssertions.toList)
	
	    //set the overall decision
	    val geospatiallyKosher = AssertionCodes.isGeospatiallyKosher(systemAssertions)
	    val taxonomicallyKosher = AssertionCodes.isTaxonomicallyKosher(systemAssertions)
	
	    properties.put(geospatialDecisionColumn, geospatiallyKosher.toString)
	    properties.put(taxonomicDecisionColumn, taxonomicallyKosher.toString)
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
  def addSystemAssertion(uuid:String, qualityAssertion:QualityAssertion){
    DAO.persistentManager.putList(uuid,entityName, qualityAssertionColumn,List(qualityAssertion),false)
    DAO.persistentManager.put(uuid, entityName, qualityAssertion.name, qualityAssertion.problemAsserted.toString)
  }

  /**
   * Set the system systemAssertions for a record, overwriting existing systemAssertions
   */
  def updateSystemAssertions(uuid:String, qualityAssertions:List[QualityAssertion]){
    DAO.persistentManager.putList(uuid,entityName,qualityAssertionColumn,qualityAssertions,true)
  }

  /**
   * Retrieve annotations for the supplied UUID.
   */
  def getSystemAssertions(uuid:String): List[QualityAssertion] = {
    //val theClass = (Array(new QualityAssertion())).getClass.asInstanceOf[java.lang.Class[Array[AnyRef]]]
    val theClass = classOf[QualityAssertion].asInstanceOf[java.lang.Class[AnyRef]]
    DAO.persistentManager.getList(uuid,entityName, qualityAssertionColumn,theClass).asInstanceOf[List[QualityAssertion]]
  }

  /**
   * Add a user supplied assertion - updating the status on the record.
   */
  def addUserAssertion(uuid:String, qualityAssertion:QualityAssertion){

    val userAssertions = getUserAssertions(uuid)

    if(!userAssertions.contains(qualityAssertion)){

        val updatedUserAssertions = userAssertions :+ qualityAssertion

        val systemAssertions = getSystemAssertions(uuid)

        //store the new systemAssertions
        DAO.persistentManager.putList(uuid,entityName,userQualityAssertionColumn,updatedUserAssertions,true)

        //update the overall status
        updateAssertionStatus(uuid,qualityAssertion.name,systemAssertions,updatedUserAssertions)
    }
  }

  /**
   * Retrieve annotations for the supplied UUID.
   */
  def getUserAssertions(uuid:String): List[QualityAssertion] = {
    val theClass = classOf[QualityAssertion].asInstanceOf[java.lang.Class[AnyRef]]
    DAO.persistentManager.getList(uuid,entityName, userQualityAssertionColumn,theClass)
        .asInstanceOf[List[QualityAssertion]]
  }

  /**
   * Delete a user supplied assertion
   */
  def deleteUserAssertion(uuid:String, assertionUuid:String) : Boolean = {

    logger.debug("Deleting assertion for : "+uuid + " with assertion uuid : " + uuid)

    //retrieve existing systemAssertions
    val assertions = getUserAssertions(uuid)

    //get the assertion that is to be deleted
    val deletedAssertion = assertions.find(assertion => { assertion.uuid equals assertionUuid})

    //if not empty, remove the assertion and write back
    if(!deletedAssertion.isEmpty){

        //delete the assertion with the supplied UUID
        val updateAssertions = assertions.filter(qa => {!(qa.uuid equals assertionUuid)})

        //put the systemAssertions back - overwriting existing systemAssertions
        DAO.persistentManager.putList(uuid,entityName,userQualityAssertionColumn,updateAssertions,true)

        val assertionName = deletedAssertion.get.name
        //are there any matching systemAssertions for other users????
        val systemAssertions = getSystemAssertions(uuid)

        //update the assertion status
        updateAssertionStatus(uuid,assertionName,systemAssertions,updateAssertions)
        true
    } else {
        logger.warn("Unable to find assertion with UUID: " + assertionUuid)
        false
    }
  }

  /**
   * Update the assertion status using system and user systemAssertions.
   */
  def updateAssertionStatus(uuid:String, assertionName:String, systemAssertions:List[QualityAssertion], userAssertions:List[QualityAssertion])  {

    println("Updating the assertion status for : " + uuid)

    val assertions = userAssertions.filter(qa => {qa.name equals assertionName})
    //update the status flag on the record, using the system quality systemAssertions
    if(!assertions.isEmpty) {

        //if anyone asserts the negative, the answer is negative
        val negativeAssertion = userAssertions.find(qa => qa.problemAsserted)
        if(!negativeAssertion.isEmpty){
            val qualityAssertion = negativeAssertion.get
            DAO.persistentManager.put(uuid,entityName,
                markAsQualityAssertion(qualityAssertion.name),qualityAssertion.problemAsserted.toString)
        }
    } else if(!systemAssertions.isEmpty) {
        //check system systemAssertions for an answer
        val matchingAssertion = systemAssertions.find(assertion => {assertion.name equals assertionName})
        if(!matchingAssertion.isEmpty){
            val assertion = matchingAssertion.get
            DAO.persistentManager.put(uuid,entityName,assertion.name,assertion.problemAsserted.toString)
        }
    } else {
        DAO.persistentManager.put(uuid,entityName,assertionName,true.toString)
    }

    //set the overall decision
    var properties = scala.collection.mutable.Map[String,String]()
    val geospatiallyKosher = AssertionCodes.isGeospatiallyKosher((userAssertions ++ systemAssertions).toArray)
    val taxonomicallyKosher = AssertionCodes.isTaxonomicallyKosher((userAssertions ++ systemAssertions).toArray)
    properties.put(geospatialDecisionColumn, geospatiallyKosher.toString)
    properties.put(taxonomicDecisionColumn, taxonomicallyKosher.toString)

    println("Updating the assertion status for : " + uuid
        + ", geospatiallyKosher:"+geospatiallyKosher
        + ", taxonomicallyKosher:"+taxonomicallyKosher)

    DAO.persistentManager.put(uuid,entityName,properties.toMap)
  }

  /**
   * Should be possible to factor this out
   */
  def reIndex(uuid:String){
    println("Reindexing UUID: " + uuid)
    val recordVersions = getAllVersionsByUuid(uuid)
    if(recordVersions.isEmpty){
        println("Unable to reindex UUID: " + uuid)
    } else {
        val occurrenceIndex = DAO.indexer.getOccIndexModel(recordVersions.get)
        if(!occurrenceIndex.isEmpty){
            DAO.indexer.index(occurrenceIndex.get)
            println("Reindexed UUID: " + uuid)
        }
    }
  }
}
