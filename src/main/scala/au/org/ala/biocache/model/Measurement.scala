package au.org.ala.biocache.model

import scala.beans.BeanProperty
import au.org.ala.biocache.poso.POSO

/**
 * POSO for holding measurement information for an occurrence.
 */
class Measurement extends Cloneable with POSO {
  override def clone : Measurement = super.clone.asInstanceOf[Measurement]
  @BeanProperty var measurementAccuracy:String = _
  @BeanProperty var measurementDeterminedBy:String = _
  @BeanProperty var measurementDeterminedDate:String = _
  @BeanProperty var measurementID:String = _
  @BeanProperty var measurementMethod:String = _
  @BeanProperty var measurementRemarks:String = _
  @BeanProperty var measurementType:String = _
  @BeanProperty var measurementUnit:String = _
  @BeanProperty var measurementValue:String = _
}
