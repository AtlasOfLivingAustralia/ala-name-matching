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

  Pelops.addPool(poolName, hosts, 9160, false, keyspace, new Policy)
  //read in the ORM mappings
  val attributionDefn = fileToArray("/Attribution.txt")
  val occurrenceDefn = fileToArray("/Occurrence.txt")
  val locationDefn = fileToArray("/Location.txt")
  val eventDefn = fileToArray("/Event.txt")
  val classificationDefn = fileToArray("/Classification.txt")
  val identificationDefn = fileToArray("/Identification.txt")

  def fileToArray(filePath:String) : List[String] =
    scala.io.Source.fromURL(getClass.getResource(filePath), "utf-8").getLines.toList.map(_.trim)
}

/**
 * A trait to implement by java classes to process occurrence records.
 */
trait OccurrenceConsumer { def consume(record:FullRecord) }

/**
 * A DAO for accessing occurrences.
 */
object OccurrenceDAO {

  import ReflectBean._
  import JavaConversions._

  private val columnFamily = "occ"
  private val qualityAssertionColumn = "qualityAssertion"

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
   *
   * @param uuid
   * @param occurrenceType
   * @return
   */
  def getByUuid(uuid:String, recordVersion:Version) : Option[FullRecord] = {

    val selector = Pelops.createSelector(DAO.poolName, DAO.keyspace)
    val slicePredicate = Selector.newColumnsPredicateAll(true, 10000)
    val occurrence = new Occurrence
    val columnList = selector.getColumnsFromRow(uuid, columnFamily, slicePredicate, ConsistencyLevel.ONE)
    createOccurrence(uuid, columnList, recordVersion)
  }

  /**
   * Set the property on the correct model object
   * @param o the occurrence
   * @param c the classification
   * @param l the location
   * @param e the event
   * @param fieldName the field to set
   * @param fieldValue the value to set
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
    } else if(fieldName startsWith "qa"){
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
   * @return
   */
  protected def createOccurrence(uuid:String, columnList:java.util.List[Column], version:Version)
    : Option[FullRecord] = {

    val tuples = {
      for(column <- columnList)
        yield (new String(column.name), new String(column.value))
    }
    //convert the list
    val map = Map(tuples map {s => (s._1, s._2)} : _*)

    createOccurrence(uuid, map, version)
  }

  /**
   * Create a record from a array of tuple properties
   */
  def createOccurrence(uuid:String, fieldTuples:Array[(String,String)], version:Version)
    : Option[FullRecord] = {
    val fieldMap = Map(fieldTuples map {s => (s._1, s._2)} : _*)
    createOccurrence(uuid, fieldMap, version)
  }

  /**
   * Creates an FullRecord from the map of properties
   */
  def createOccurrence(uuid:String, fields:Map[String,String], version:Version)
    : Option[FullRecord] = {

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
        } else {
          setProperty(occurrence, classification, location, event, assertions, fieldName, fieldValue.get)
        }
      }
    }
    //TODO retrieve assertions
    Some(new FullRecord(occurrence, classification, location, event, assertions.toArray))

  }

  private def removeSuffix(name:String) : String = name.substring(0, name.length - 2)
  private def isProcessedValue(name:String) : Boolean = name endsWith ".p"
  private def isConsensusValue(name:String) : Boolean = name endsWith ".c"
  private def markAsProcessed(name:String) : String = name + ".p"
  private def markAsConsensus(name:String) : String = name + ".c"

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
   * Write to stream...
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
   * Select fields from rows...
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
   * Iterate over all occurrences, passing the objects to a function.
   *
   * @param occurrenceType
   * @param proc
   */
  def pageOverAll(occurrenceType:Version, proc:((Option[FullRecord])=>Unit) ) {

    val selector = Pelops.createSelector(DAO.poolName, columnFamily)
    val slicePredicate = Selector.newColumnsPredicateAll(true, 10000)
    var startKey = ""
    var keyRange = Selector.newKeyRange(startKey, "", 1001)
    var hasMore = true
    var counter = 0
    var columnMap = selector.getColumnsFromRows(keyRange, columnFamily, slicePredicate, ConsistencyLevel.ONE)
      while (columnMap.size>0) {
      val columnsObj = List(columnMap.keySet.toArray : _*)
      //convert to scala List
      val keys = columnsObj.asInstanceOf[List[String]]
      startKey = keys.last
      for(key<-keys){
        val columnsList = columnMap.get(key)
        proc(createOccurrence(key, columnsList, occurrenceType))
      }
      counter += keys.size
      keyRange = Selector.newKeyRange(startKey, "", 1001)
      columnMap = selector.getColumnsFromRows(keyRange, columnFamily, slicePredicate, ConsistencyLevel.ONE)
      columnMap.remove(startKey)
    }
    println("Finished paging. Total count: "+counter)
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
   * Update the occurrence record.
   */
  def updateOccurrence(guid:String, fullRecord:FullRecord, version:Version) {
    updateOccurrence(guid,fullRecord,Array(),version)
  }

  /**
   * Update the occurrence with the supplied record, setting the correct version
   */
  def updateOccurrence(guid:String, fullRecord:FullRecord, assertions:Array[QualityAssertion], version:Version) {

    val mutator = Pelops.createMutator(DAO.poolName, columnFamily)

    //for each field in the definition, check if there is a value to write
    for(anObject <- Array(fullRecord.o,fullRecord.c,fullRecord.e,fullRecord.l)){
      val defn = getDefn(anObject)
      for(field <- defn){
        val fieldValue = anObject.getClass.getMethods.find(_.getName == field).get.invoke(anObject).asInstanceOf[String]
        if(fieldValue!=null && !fieldValue.isEmpty){
          var fieldName = field
          if(version == Processed){
            fieldName = markAsProcessed(fieldName)
          }
          if(version == Consensus){
            fieldName = markAsConsensus(fieldName)
          }
          mutator.writeColumn(guid, columnFamily, mutator.newColumn(fieldName, fieldValue))
        }
      }
    }

    //set the quality assertions flags
    for(qa <- fullRecord.assertions){
      mutator.writeColumn(guid, columnFamily, mutator.newColumn(qa, "true"))
    }

    if(!assertions.isEmpty){
      //serialise the assertion list to JSON and DB
      val gson = new Gson
      val json = gson.toJson(assertions)
      mutator.writeColumn(guid, columnFamily, mutator.newColumn(qualityAssertionColumn, json))
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
  def addQualityAssertion(uuid:String, qualityAssertion:QualityAssertion, errorCode:ErrorCode){

    //set field qualityAssertion
    val selector = Pelops.createSelector(DAO.poolName, columnFamily);
    val mutator = Pelops.createMutator(DAO.poolName, columnFamily);
    val column = {
      try {
       Some(selector.getColumnFromRow(uuid, columnFamily, qualityAssertionColumn.getBytes, ConsistencyLevel.ONE))
      } catch {
        case _ => None //expected behaviour when row doesnt exist
      }
    }
    val gson = new Gson

    if(column.isEmpty){
      //parse it
      val json = gson.toJson(Array(qualityAssertion))
      mutator.writeColumn(uuid, columnFamily, mutator.newColumn(qualityAssertionColumn, json))
    } else {
      var json = new String(column.get.getValue)
      val listType = new TypeToken[ArrayList[QualityAssertion]]() {}.getType()
      var qaList = gson.fromJson(json,listType).asInstanceOf[java.util.List[QualityAssertion]]

      var written = false
      for(i<- 0 until qaList.size){
        val qa = qaList.get(i)
        if(qa equals qualityAssertion){
          //overwrite
          written = true
          qaList.remove(qa)
          qaList.add(i, qualityAssertion)
        }
      }
      if(!written){
        qaList.add(qualityAssertion)
      }

      // check equals
      json = gson.toJson(qaList)
      mutator.writeColumn(uuid, columnFamily, mutator.newColumn(qualityAssertionColumn, json))
      mutator.writeColumn(uuid, columnFamily, mutator.newColumn(errorCode.name, "true"))
    }
    mutator.execute(ConsistencyLevel.ONE)
  }

  /**
   * Retrieve annotations for the supplied UUID.
   */
  def getQualityAssertions(uuid:String): Array[QualityAssertion] = {

    val selector = Pelops.createSelector(DAO.poolName, columnFamily);
    //retrieve and parse JSON in QualityAssertion column
    val column = {
      try {
       Some(selector.getColumnFromRow(uuid, columnFamily, qualityAssertionColumn.getBytes, ConsistencyLevel.ONE))
      } catch {
        case _ => None //expected behaviour when row doesnt exist
      }
    }
    val gson = new Gson

    if(column.isEmpty){
      Array()
    } else {
      //parse it and return list
      val json = new String(column.get.value)
      val listType = new TypeToken[ArrayList[QualityAssertion]]() {}.getType()
      val list = gson.fromJson(json,listType).asInstanceOf[java.util.List[QualityAssertion]]
      list.toArray.asInstanceOf[Array[QualityAssertion]]
    }
  }
}
