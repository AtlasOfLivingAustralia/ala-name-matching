package au.org.ala.biocache.persistence

import scala.collection.mutable.ListBuffer
import org.scale7.cassandra.pelops.Cluster
import com.google.inject.name.Named
import com.google.inject.Inject
import java.util.UUID
import scala.slick.jdbc.{StaticQuery => Q}

/**
 * Trait (interface) for persistence storage in the Biocache.
 * 
 * This trait is implemented for Cassandra,
 * but could also be implemented for another backend supporting basic key value pair storage and
 * allowing the selection of a set of key value pairs via a record ID.
 *
 * @author Dave Martin (David.Martin@csiro.au)
 */
trait PersistenceManager {

  /**
   * Get a single property.
   */
  def get(uuid:String, entityName:String, propertyName:String) : Option[String]

  /**
   * Gets the supplied properties for this record
   */
  def getSelected(uuid:String, entityName:String, propertyNames:Seq[String]):Option[Map[String,String]]

  /**
   * Get a key value pair map for this record.
   */
  def get(uuid:String, entityName:String): Option[Map[String, String]]

  /**
   * Get a key value pair map for this column timestamps of this record.
   */
  def getColumnsWithTimestamps(uuid:String, entityName:String): Option[Map[String, Long]]

  /**
   * Gets KVP map for a record based on a value in an index
   */
  def getByIndex(uuid:String, entityName:String, idxColumn:String) : Option[Map[String,String]]

  /**
   * Gets a single property based on an indexed value.  Returns the value of the "first" matched record.
   */
  def getByIndex(uuid:String, entityName:String, idxColumn:String, propertyName:String) :Option[String]

  /**
   * Retrieve an array of objects from a single column.
   */
  def getList[A](uuid: String, entityName: String, propertyName: String, theClass:java.lang.Class[_]) : List[A]

  /**
   * Put a single property.
   */
  def put(uuid:String, entityName:String, propertyName:String, propertyValue:String) : String

  /**
   * Put a set of key value pairs.
   */
  def put(uuid:String, entityName:String, keyValuePairs:Map[String, String]) : String

  /**
   * Add a batch of properties.
   */
  def putBatch(entityName:String, batch:Map[String, Map[String,String]])

  /**
   * Store a list of the supplied object
   * @param overwrite if true, current stored value will be replaced without a read.
   */
  def putList[A](uuid: String, entityName: String, propertyName: String, objectList:Seq[A], theClass:java.lang.Class[_], overwrite: Boolean) : String

  /**
   * Page over all entities, passing the retrieved UUID and property map to the supplied function.
   * Function should return false to exit paging.
   */
  def pageOverAll(entityName:String, proc:((String, Map[String,String])=>Boolean),startUuid:String="",endUuid:String="", pageSize:Int = 1000)

  /**
   * Page over the records, retrieving the supplied columns only.
   */
  def pageOverSelect(entityName:String, proc:((String, Map[String,String])=>Boolean), startUuid:String, endUuid:String, pageSize:Int, columnName:String*)

  /**
   * Page over the records, retrieving the supplied columns range.
   */
  def pageOverColumnRange(entityName:String, proc:((String, Map[String,String])=>Boolean), startUuid:String="", endUuid:String="", pageSize:Int=1000, startColumn:String="", endColumn:String="")

  /**
   * Select the properties for the supplied record UUIDs
   */
  def selectRows(uuids:Seq[String],entityName:String,propertyNames:Seq[String],proc:((Map[String,String])=>Unit))

  /**
   * The column to delete.
   */
  def deleteColumns(uuid:String, entityName:String, columnName:String*)

  /**
   * Delete row
   */
  def delete(uuid:String, entityName:String)

  /**
   * Close db connections etc
   */
  def shutdown

  /**
   * The field delimiter to use
   */
  def fieldDelimiter = '.'
}



