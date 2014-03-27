package au.org.ala.biocache.model

import scala.beans.BeanProperty

/**
 * Represents a cached profile within system.
 */
class TaxonProfile (
  @BeanProperty var guid:String,
  @BeanProperty var scientificName:String,
  @BeanProperty var commonName:String,
  @BeanProperty var rankString:String,
  @BeanProperty var habitats:Array[String],
  @BeanProperty var left:String,
  @BeanProperty var right:String,
  @BeanProperty var sensitive:Array[SensitiveSpecies],
  @BeanProperty var conservation:Array[ConservationStatus])
  extends Cloneable {
  def this() = this(null,null,null,null,null,null,null, null,null)
  override def clone : TaxonProfile = super.clone.asInstanceOf[TaxonProfile]
  private var conservationMap:Map[String,String] = null

  def retrieveConservationStatus(loc: String): Option[String] = {
    if (conservation != null) {
      if (conservationMap == null) {
        val map: scala.collection.mutable.Map[String, String] = new scala.collection.mutable.HashMap[String, String]
        for (cs <- conservation) {
          //Only add the state if it is missing or replaces "null" state information
          if (map.getOrElse(cs.region, "null").contains("null"))
            map += cs.region -> (cs.status + "," + cs.rawStatus)
        }
        conservationMap = map.toMap
      }
      return conservationMap.get(loc)
    }
    return None
  }
}
