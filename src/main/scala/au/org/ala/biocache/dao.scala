package au.org.ala.biocache

import au.org.ala.checklist.lucene.CBIndexSearch
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
import au.org.ala.util.ReflectBean
import org.wyki.cassandra.pelops.{Pelops,Selector}
import org.wyki.cassandra.pelops.Policy
import java.io.OutputStream
import scala.collection.JavaConversions
import scala.collection.mutable.ArrayBuffer
import org.apache.cassandra.thrift._
import java.util.{UUID, ArrayList}

/**
 * DAO configuration. Should be refactored to use a DI framework
 * or make use of Cake pattern.
 */
object DAO {

  val hosts = Array{"localhost"}
  val keyspace = "occ"
  val poolName = "biocache-store-pool"
  val nameIndex= new CBIndexSearch("/data/lucene/namematching")
  val maxColumnLimit = 10000

  Pelops.addPool(poolName, hosts, 9160, false, keyspace, new Policy)
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

  private val columnFamily = "occ"
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

    val selector = Pelops.createSelector(DAO.poolName, DAO.keyspace)
    val slicePredicate = Selector.newColumnsPredicateAll(true, DAO.maxColumnLimit)
    try {
      val columnList = selector.getColumnsFromRow(uuid, columnFamily, slicePredicate, ConsistencyLevel.ONE)
      val map = columnList2Map(columnList)
      //create the versions of the record
      val raw = createOccurrence(uuid, map, Raw)
      val processed = createOccurrence(uuid, map, Processed)
      val consensus = createOccurrence(uuid, map, Consensus)
      //pass all version to the procedure, wrapped in the Option
      Some(Array(raw, processed, consensus))
    } catch {
      case e:NotFoundException => None
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

    val selector = Pelops.createSelector(DAO.poolName, DAO.keyspace)
    val slicePredicate = Selector.newColumnsPredicateAll(true, DAO.maxColumnLimit)
    try {
      val columnList = selector.getColumnsFromRow(uuid, columnFamily, slicePredicate, ConsistencyLevel.ONE)
      val map = columnList2Map(columnList)
      Some(createOccurrence(uuid, map, version))
    } catch {
      case e:NotFoundException => None
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
   * Creates an occurrence from the list of columns.
   * An occurrence consists of several objects which are returned as a tuple.
   *
   * For a java implementation, a DTO containing the objects will need to be returned.
   *
   * @param uuid
   * @param columnList
   * @param occurrenceType raw, processed or consensus version of the record
   */
  protected def createOccurrence(uuid:String, columnList:java.util.List[Column], version:Version) : FullRecord = {
    //convert the list to map
    val map = columnList2Map(columnList)
    createOccurrence(uuid, map, version)
  }

  /**
   * Convert a set of cassandra columns into a key-value pair map.
   */
  protected def columnList2Map(columnList:java.util.List[Column]) : Map[String,String] = {
    val tuples = {
      for(column <- columnList)
        yield (new String(column.name), new String(column.value))
    }
    //convert the list
    Map(tuples map {s => (s._1, s._2)} : _*)
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

  private def markAsProcessed(name:String) : String = name + ".p"
  private def markAsConsensus(name:String) : String = name + ".c"
  private def markAsQualityAssertion(name:String) : String = name + ".qa"

  /**
   * Iterate over records, passing the records to the supplied consumer.
   */
  def pageOverAll(occurrenceType:Version, consumer:OccurrenceConsumer) {
    pageOverAll(occurrenceType, fullRecord => consumer.consume(fullRecord.get))
  }

  /**
   * Create or retrieve the UUID for this record. The uniqueID should be a
   * has of properties that provides a unique ID for the record within
   * the dataset.
   */
  def createOrRetrieveUuid(uniqueID: String): String = {
    try {
      val selector = Pelops.createSelector(DAO.poolName, columnFamily)
      val column = selector.getColumnFromRow(uniqueID, "dr", "uuid".getBytes, ConsistencyLevel.ONE)
      new String(column.value)
    } catch {
      //NotFoundException is expected behaviour with thrift
      case e:NotFoundException =>
        val newUuid = UUID.randomUUID.toString
        val mutator = Pelops.createMutator(DAO.poolName, columnFamily)
        mutator.writeColumn(uniqueID, "dr", mutator.newColumn("uuid".getBytes, newUuid))
        mutator.execute(ConsistencyLevel.ONE)
        newUuid
    }
  }

  /**
   * Write to stream in a delimited format (CSV).
   */
  def writeToStream(outputStream:OutputStream,fieldDelimiter:String,recordDelimiter:String,uuids:Array[String],fields:Array[String],version:Version) {

    selectRows(uuids,fields,version, { fieldMap =>
      for(field<-fields){
        val fieldValue = fieldMap.get(field)
        if(fieldValue.isEmpty){
          outputStream.write("".getBytes)  //pad out empty values
        } else {
          outputStream.write(fieldValue.get.getBytes)
        }
        outputStream.write(fieldDelimiter.getBytes)
      }
      outputStream.write(recordDelimiter.getBytes)
     })
  }

  /**
   * Select fields from rows and pass to the supplied function.
   */
  def selectRows(uuids:Array[String],fields:Array[String],version:Version, proc:((Map[String,String])=>Unit)) {
    val selector = Pelops.createSelector(DAO.poolName, columnFamily)
    var slicePredicate = new SlicePredicate
    slicePredicate.setColumn_names(fields.toList.map(_.getBytes))

    //retrieve the columns
    var columnMap = selector.getColumnsFromRows(uuids.toList, columnFamily, slicePredicate, ConsistencyLevel.ONE)

    //write them out to the output stream
    val keys = List(columnMap.keySet.toArray : _*)

    for(key<-keys){
      val columnsList = columnMap.get(key)
      val fieldValues = columnsList.map(column => (new String(column.name),new String(column.value))).toArray
      val map = scala.collection.mutable.Map.empty[String,String]
      for(fieldValue <-fieldValues){
        map(fieldValue._1) = fieldValue._2
      }
      proc(map.toMap)
    }
  }

  /**
   * Iterate over all occurrences, passing all versions of FullRecord
   * to the supplied function.
   * Function returns a boolean indicating if the paging should continue.
   *
   * @param occurrenceType
   * @param proc
   */
  def pageOverAllVersions(proc:((Option[Array[FullRecord]])=>Boolean) ) {
     pageOverAll((guid, map) => {
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
     pageOverAll((guid, map) => {
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
   * @param proc
   */
  def pageOverAll(proc:((String, Map[String,String])=>Boolean) ) {

    val selector = Pelops.createSelector(DAO.poolName, columnFamily)
    val slicePredicate = Selector.newColumnsPredicateAll(true, DAO.maxColumnLimit)
    var startKey = ""
    var keyRange = Selector.newKeyRange(startKey, "", 1001)
    var hasMore = true
    var counter = 0
    var columnMap = selector.getColumnsFromRows(keyRange, columnFamily, slicePredicate, ConsistencyLevel.ONE)
    var continue = true
    while (columnMap.size>0 && continue) {
      val columnsObj = List(columnMap.keySet.toArray : _*)
      //convert to scala List
      val keys = columnsObj.asInstanceOf[List[String]]
      startKey = keys.last
      for(uuid<-keys){
        val columnList = columnMap.get(uuid)
        //procedure a map of key value pairs
        val map = columnList2Map(columnList)
        //pass the record ID and the key value pair map to the proc
        continue = proc(uuid, map)
      }
      counter += keys.size
      keyRange = Selector.newKeyRange(startKey, "", 1001)
      columnMap = selector.getColumnsFromRows(keyRange, columnFamily, slicePredicate, ConsistencyLevel.ONE)
      columnMap.remove(startKey)
    }
    println("Finished paging. Total count: "+counter)
  }

  /**
   * Iterate over all occurrences, passing the objects to a function.
   * Function returns a boolean indicating if the paging should continue.
   *
   * @param occurrenceType
   * @param proc
   */
  def pageOverAll(version:Version, proc:((Option[FullRecord])=>Boolean) ) {
     pageOverAll((guid, map) => {
       //retrieve all versions
       val fullRecord = createOccurrence(guid, map, version)
       //pass all version to the procedure, wrapped in the Option
       proc(Some(fullRecord))
     })
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
     }
  }

  /**
   * Update the version of the occurrence record.
   */
  def updateOccurrence(uuid:String, fullRecord:FullRecord, version:Version) {
    updateOccurrence(uuid,fullRecord,Array(),version)
  }

  /**
   * Update the occurrence with the supplied record, setting the correct version
   */
  def updateOccurrence(uuid:String, fullRecord:FullRecord, assertions:Array[QualityAssertion], version:Version) {

    val mutator = Pelops.createMutator(DAO.poolName, columnFamily)

    //for each field in the definition, check if there is a value to write
    for(anObject <- Array(fullRecord.o,fullRecord.c,fullRecord.e,fullRecord.l)){
      val defn = getDefn(anObject)
      for(field <- defn){
        val fieldValue = anObject.getClass.getMethods.find(_.getName == field).get.invoke(anObject).asInstanceOf[String]
        if(fieldValue!=null && !fieldValue.isEmpty){
          var fieldName = field
          version match {
              case Processed =>  markAsProcessed(fieldName)
              case Consensus =>  markAsConsensus(fieldName)
              case _ =>
          }
          /*

          if(version == Processed){
            fieldName = markAsProcessed(fieldName)
          }
          if(version == Consensus){
            fieldName = markAsConsensus(fieldName)
          }
          */
          mutator.writeColumn(uuid, columnFamily, mutator.newColumn(fieldName, fieldValue))
        }
      }
    }

    //set the assertions on the full record
    fullRecord.assertions = assertions.toArray.map(_.assertionName)

    //set the quality assertions flags
    for(qa <- assertions){
      mutator.writeColumn(uuid, columnFamily, mutator.newColumn(qa.assertionName+".qa", qa.positive.toString))
    }

    if(!assertions.isEmpty){
      //serialise the assertion list to JSON and DB
      val gson = new Gson
      val json = gson.toJson(assertions)
      mutator.writeColumn(uuid, columnFamily, mutator.newColumn(qualityAssertionColumn, json))
    }

    //commit to cassandra
    mutator.execute(ConsistencyLevel.ONE)
  }

  /**
   * Update an occurrence
   *
   * @param uuid
   * @param anObject
   * @param occurrenceType
   */
  def updateOccurrence(uuid:String, anObject:AnyRef, version:Version) {

    //select the correct definition file
    val defn = getDefn(anObject)

    //additional functionality to support adding Quality Assertions and Field corrections.
    val mutator = Pelops.createMutator(DAO.poolName, columnFamily)
    //for each field in the definition, check if there is a value to write
    for(field <- defn){
      val fieldValue = anObject.getClass.getMethods.find(_.getName == field).get.invoke(anObject).asInstanceOf[String]
      if(fieldValue!=null && !fieldValue.isEmpty){
        val fieldName = {
          if(version == Processed) markAsProcessed(field)
          else if(version == Consensus) markAsConsensus(field)
          else field
        }
        mutator.writeColumn(uuid, columnFamily, mutator.newColumn(fieldName, fieldValue))
      }
    }
    //commit
    mutator.execute(ConsistencyLevel.ONE)
  }

  /**
   * Adds a quality assertion to the row with the supplied UUID.
   * 
   * @param uuid
   * @param qualityAssertion
   */
  def addQualityAssertion(uuid:String, qualityAssertion:QualityAssertion){
    CassandraPersistenceManager.putArray(uuid,qualityAssertionColumn,Array(qualityAssertion),false)
    CassandraPersistenceManager.put(uuid,qualityAssertion.assertionName,qualityAssertion.positive.toString)
  }

  /**
   * Retrieve annotations for the supplied UUID.
   */
  def getQualityAssertions(uuid:String): Array[QualityAssertion] = {
    val theClass = (Array(new QualityAssertion())).getClass.asInstanceOf[Class[AnyRef]]
    CassandraPersistenceManager.getArray(uuid,qualityAssertionColumn,theClass).asInstanceOf[Array[QualityAssertion]]
  }

  /**
   * Add a user supplied assertion - updating the status on the record.
   * This will by default,
   *
   */
  def addUserQualityAssertion(uuid:String, qualityAssertion:QualityAssertion){
    CassandraPersistenceManager.putArray(uuid,userQualityAssertionColumn,Array(qualityAssertion),false)
    CassandraPersistenceManager.put(uuid,qualityAssertion.assertionName,qualityAssertion.positive.toString)
  }


  /**
   * Retrieve annotations for the supplied UUID.
   */
  def getUserQualityAssertions(uuid:String): Array[QualityAssertion] = {
    val theClass = (Array(new QualityAssertion())).getClass.asInstanceOf[Class[AnyRef]]
    CassandraPersistenceManager.getArray(uuid,userQualityAssertionColumn,theClass).asInstanceOf[Array[QualityAssertion]]
  }

    /**
     * Delete a user supplied assertion
     */
  def deleteUserQualityAssertion(uuid:String, assertionUuid:String) {
    val assertions = getQualityAssertions(uuid)

    //delete the assertion with the supplied UUID

    //put the assertions back - overwriting existing assertions
    CassandraPersistenceManager.putArray(uuid,userQualityAssertionColumn,assertions.asInstanceOf[Array[Comparable[AnyRef]]],true)

    //update the status flag on the record, using the system quality assertions

    //default to "positive" if there are no system quality assertions for the property

  }
}
