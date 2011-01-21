package au.org.ala.biocache

import au.org.ala.checklist.lucene.CBIndexSearch
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
import au.org.ala.util.ReflectBean
import org.wyki.cassandra.pelops.{Mutator,Pelops,Selector}
import scala.collection.mutable.{LinkedList,ListBuffer}
import org.apache.cassandra.thrift.{Column,ConsistencyLevel,ColumnPath,SlicePredicate,SliceRange}
import java.util.ArrayList
import org.wyki.cassandra.pelops.Policy

object DAO {

  val hosts = Array{"localhost"}
  val keyspace = "occ"
  val poolName = "occ-pool"
  val nameIndex= new CBIndexSearch("/data/lucene/namematching")

  Pelops.addPool(poolName, hosts, 9160, false, keyspace, new Policy)
  //read in the ORM mappings
  val attributionDefn = fileToArray("/Attribution.txt")
  val occurrenceDefn = fileToArray("/Occurrence.txt")
  val locationDefn = fileToArray("/Location.txt")
  val eventDefn = fileToArray("/Event.txt")
  val classificationDefn = fileToArray("/Classification.txt")
  val identificationDefn = fileToArray("/Identification.txt")
  
  def fileToArray(filePath:String) : Array[String] = {
	  scala.io.Source.fromURL(getClass.getResource(filePath), "utf-8").getLines.toList.map(_.trim).toArray
  }
}

/**
 * A trait to implement by java classes to process occurrence records.
 * 
 * @author Dave Martin (David.Martin@csiro.au)
 */
trait OccurrenceConsumer {
	def consume(record:FullRecord)
}


/**
 * A DAO for accessing occurrences.
 * 
 * @author Dave Martin (David.Martin@csiro.au)
 */
object OccurrenceDAO {

  import Version._
  import ReflectBean._

  val columnFamily = "occ"
	  
  /**
   * Get an occurrence with UUID
   *
   * @param uuid
   * @return
   */
  def getByUuid(uuid:String) : Option[FullRecord] = {
    getByUuid(uuid, Version.Raw)
  }

  def getByUuidJ(uuid:String) : FullRecord = {
    val record = getByUuid(uuid, Version.Raw)
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
  def getByUuid(uuid:String, occurrenceType:Version.Value) : Option[FullRecord] = {

    val selector = Pelops.createSelector(DAO.poolName, DAO.keyspace)
    val slicePredicate = Selector.newColumnsPredicateAll(true, 10000)
    val occurrence = new Occurrence
    val columnList = selector.getColumnsFromRow(uuid, columnFamily, slicePredicate, ConsistencyLevel.ONE)
    createOccurrence(uuid, columnList, occurrenceType)
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
  protected def setProperty(o:Occurrence, c:Classification, l:Location, e:Event, fieldName:String, fieldValue:String){
    if(DAO.occurrenceDefn.contains(fieldName)){
      o.setter(fieldName,fieldValue)
    } else if(DAO.classificationDefn.contains(fieldName)){
      c.setter(fieldName,fieldValue)
    } else if(DAO.eventDefn.contains(fieldName)){
      e.setter(fieldName,fieldValue)
    } else if(DAO.locationDefn.contains(fieldName)){
      l.setter(fieldName,fieldValue)
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
  protected def createOccurrence(uuid:String, columnList:java.util.List[Column], occurrenceType:Version.Value)
    : Option[FullRecord] = {

    val occurrence = new Occurrence
    val classification = new Classification
    val location = new Location
    val event = new Event

    occurrence.uuid = uuid
    val columns = List(columnList.toArray : _*)
    for(column<-columns){

      //ascertain which term should be associated with which object
      var fieldName = new String(column.asInstanceOf[Column].name)
      val fieldValue = new String(column.asInstanceOf[Column].value)

      if(fieldName.endsWith(".p") && occurrenceType == Version.Processed){
        fieldName = fieldName.substring(0, fieldName.length - 2)
        setProperty(occurrence, classification, location, event, fieldName, fieldValue)
      } else if(fieldName.endsWith(".c") && occurrenceType == Version.Consensus){
        fieldName = fieldName.substring(0, fieldName.length - 2)
        setProperty(occurrence, classification, location, event, fieldName, fieldValue)
      } else {
        setProperty(occurrence, classification, location, event, fieldName, fieldValue)
      }
    }
    Some(new FullRecord(occurrence, classification, location, event))
  }

  /**
   * Iterate over records, passing the records to the supplied consumer.
   */
  def pageOverAll(occurrenceType:Version.Value, consumer:OccurrenceConsumer) {
	  pageOverAll(occurrenceType, fullRecord => consumer.consume(fullRecord.get))
  }
  
  /**
   * Iterate over all occurrences, passing the objects to a function.
   *
   * @param occurrenceType
   * @param proc
   */
  def pageOverAll(occurrenceType:Version.Value, proc:((Option[FullRecord])=>Unit) ) {

    val selector = Pelops.createSelector(DAO.poolName, columnFamily);
    val slicePredicate = Selector.newColumnsPredicateAll(true, 10000);
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
   *  
   */
  def updateOccurrence(guid:String, fullRecord:FullRecord, version:Version.Value) {
	OccurrenceDAO.updateOccurrence(guid, fullRecord.o, version)
	OccurrenceDAO.updateOccurrence(guid, fullRecord.c, version)
	OccurrenceDAO.updateOccurrence(guid, fullRecord.l, version)
	OccurrenceDAO.updateOccurrence(guid, fullRecord.e, version)
  }
  
  /**
   * Update an occurrence
   *
   * @param uuid
   * @param anObject
   * @param occurrenceType
   */
  def updateOccurrence(uuid:String, anObject:AnyRef, occurrenceType:Version.Value) {

    //select the correct definition file
    val defn = { anObject match {
    	case l:Location => DAO.locationDefn
    	case o:Occurrence => DAO.occurrenceDefn
    	case e:Event => DAO.eventDefn
    	case c:Classification => DAO.classificationDefn
    	case a:Attribution => DAO.attributionDefn
      }
    }
    
    //additional functionality to support adding Quality Assertions and Field corrections.
    val mutator = Pelops.createMutator(DAO.poolName, columnFamily);
    for(field <- defn){
      val fieldValue = anObject.getClass.getMethods.find(_.getName == field).get.invoke(anObject).asInstanceOf[String]
      if(fieldValue!=null && !fieldValue.isEmpty){
        var fieldName = field
        if(occurrenceType == Version.Processed){
          fieldName = fieldName +".p"
        }
        if(occurrenceType == Version.Consensus){
          fieldName = fieldName +".c"
        }
        mutator.writeColumn(uuid, columnFamily, mutator.newColumn(fieldName, fieldValue))
      }
    }
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
       Some(selector.getColumnFromRow(uuid, columnFamily, "qualityAssertion".getBytes, ConsistencyLevel.ONE))
      } catch {
        case _ => None
      }
    }
    val gson = new Gson

    if(column.isEmpty){
      //parse it
      val json = gson.toJson(Array(qualityAssertion))
      mutator.writeColumn(uuid, columnFamily, mutator.newColumn("qualityAssertion", json))
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
      mutator.writeColumn(uuid, columnFamily, mutator.newColumn("qualityAssertion", json))
      mutator.writeColumn(uuid, columnFamily, mutator.newColumn(errorCode.name, "true"))
    }
    mutator.execute(ConsistencyLevel.ONE)
  }

  def getQualityAssertions(uuid:String): List[QualityAssertion] = {
//	new ArrayList[QualityAssertion]
	  List()
  }


  def addAnnotation(){

    //

  }
}
