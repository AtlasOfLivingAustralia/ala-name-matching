package au.org.ala.biocache

/**
 * This trait should be implemented for Cassandra,
 * but could also be implemented for GAE or another backend.
 * 
 * @author Dave Martin (David.Martin@csiro.au)
 */
trait PersistenceManager {

  /**
   * Get an object
   *
   * @param uuid the uuid of the record
   * @param entityType "Occurrence"
   * @param version "Raw, Processed, Consensus"
   * @return
   */
  def get(uuid:String, entityType:String, version:Version) : Option[AnyRef]

  def get(uuid:String, property:String) : Option[String]

  def getArray(uuid:String, property:String) : Option[Array[String]]

  def put(uuid:String, entity:String, anObject:AnyRef, version:Version)

  def put(uuid:String, property:String, propertyValue:String)

  def putArray(uuid:String, property:String, propertyValue: Array[String])

}