package au.org.ala.biocache.model

import scala.beans.BeanProperty

/**
 * POSO representing a conservation status.
 */
class ConservationStatus(
  @BeanProperty var region:String,
  @BeanProperty var regionId:String,
  @BeanProperty var status:String,
  @BeanProperty var rawStatus:String
  ){
  def this() = this(null, null, null,null)
}
