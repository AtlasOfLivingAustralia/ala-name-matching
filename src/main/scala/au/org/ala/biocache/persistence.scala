package au.org.ala.biocache

import com.google.gson.Gson
import org.wyki.cassandra.pelops.Pelops
import collection.mutable.ArrayBuffer
import org.apache.cassandra.thrift.{Column, ConsistencyLevel}

/**
 * This trait should be implemented for Cassandra,
 * but could also be implemented for GAE or another backend.
 * 
 * @author Dave Martin (David.Martin@csiro.au)
 */
trait PersistenceManager {

  def get(uuid:String, entityType:String, version:Version) : Option[AnyRef]

  def get(uuid:String, propertyName:String) : Option[String]

  def put(uuid:String, entity:String, anObject:AnyRef, version:Version)

  def put(uuid:String, propertyName:String, propertyValue:String)

  /**
   * Retrieve an array of objects
   */
  def getArray(uuid:String, propertyName:String, theClass:java.lang.Class[AnyRef]) : Array[Comparable[AnyRef]]

  /**
   *  @overwrite if true, current stored value will be replaced without being read.
   */
  def putArray(uuid:String, propertyName:String, propertyArray:Array[Comparable[AnyRef]], overwrite:Boolean)
}

/**
 * Cassandra based implementation of a persistence manager.
 *
 */
object CassandraPersistenceManager extends PersistenceManager {

  val columnFamily = "occ"

  def get(uuid: String, entityType:String, version: Version) = None
  def put(uuid: String, propertyName: String, propertyValue: String) = {}
  def get(uuid: String, propertyName: String) = None
  def put(uuid: String, entity: String, anObject: AnyRef, version: Version) = {}

  /**
   * Retrieve an array of objects, parsing the JSON stored.
   */
  def getArray(uuid:String, propertyName:String, theClass:java.lang.Class[AnyRef]) : Array[Comparable[AnyRef]] = {
    val column = getColumn(uuid, columnFamily, propertyName)
    if(column.isEmpty){
      Array()
    } else {
      val gson = new Gson
      val currentJson = new String(column.get.getValue)
      gson.fromJson(currentJson,theClass).asInstanceOf[Array[Comparable[AnyRef]]]
    }
  }

  /**
   * Store arrays in a single column as JSON.
   */
  def putArray(uuid:String, propertyName:String, propertyArray:Array[Comparable[AnyRef]], overwrite:Boolean) = {

    //initialise the serialiser
    val gson = new Gson
    val mutator = Pelops.createMutator(DAO.poolName, columnFamily);

    if(overwrite){

      val json = gson.toJson(propertyArray)
      mutator.writeColumn(uuid, columnFamily, mutator.newColumn(propertyName, json))

    } else {

      //retrieve existing values
      val column = getColumn(uuid, columnFamily, propertyName)

      if(column.isEmpty){
        //write new values
        val json = gson.toJson(propertyArray)
        mutator.writeColumn(uuid, columnFamily, mutator.newColumn(propertyName, json))
      } else {
        //retrieve the existing objects
        val currentJson = new String(column.get.getValue)
        var objectList = gson.fromJson(currentJson,propertyArray.getClass).asInstanceOf[Array[AnyRef]]

        var written = false
        var buffer = new ArrayBuffer[AnyRef]

        for (theObject <- objectList){
          if(!propertyArray.contains(theObject)){
            //add to buffer
            buffer + theObject
          }
        }

        //PRESERVE UNIQUENESS
        buffer ++=  propertyArray

        // check equals
        val newJson = gson.toJson(buffer.toArray)
        println("GENERATED JSON: " + newJson)
        mutator.writeColumn(uuid, columnFamily, mutator.newColumn(propertyName, newJson))
      }
    }
    mutator.execute(ConsistencyLevel.ONE)
  }

  /**
   * Convienience method for accessing values.
   */
  protected def getColumn(uuid:String,columnFamily:String,columnName:String) : Option[Column] = {
    try {
      val selector = Pelops.createSelector(DAO.poolName, columnFamily)
      Some(selector.getColumnFromRow(uuid, columnFamily, columnName.getBytes, ConsistencyLevel.ONE))
    } catch {
      case _ => None //expected behaviour when row doesnt exist
    }
  }
}