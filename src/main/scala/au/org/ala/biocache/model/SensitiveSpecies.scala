package au.org.ala.biocache.model

import scala.beans.BeanProperty

/**
 * Stores the information about a sensitive species
 */
class SensitiveSpecies(
  @BeanProperty var zone:String,
  @BeanProperty var category:String){

  def this() = this(null, null)

  override def toString():String = "zone:" + zone + " category:" + category
}
