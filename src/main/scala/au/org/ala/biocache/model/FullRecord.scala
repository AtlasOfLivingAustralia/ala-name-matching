package au.org.ala.biocache.model

import org.codehaus.jackson.annotate.JsonIgnoreProperties
import scala.beans.BeanProperty
import org.apache.commons.lang.builder.EqualsBuilder
import au.org.ala.biocache.poso.{POSO, CompositePOSO}

/**
 * Encapsulates a complete specimen or occurrence record.
 */
@JsonIgnoreProperties
class FullRecord (
  @BeanProperty var rowKey:String,
  @BeanProperty var uuid:String,
  @BeanProperty var occurrence:Occurrence,
  @BeanProperty var classification:Classification,
  @BeanProperty var location:Location,
  @BeanProperty var event:Event,
  @BeanProperty var attribution:Attribution,
  @BeanProperty var identification:Identification,
  @BeanProperty var measurement:Measurement,
  @BeanProperty var assertions:Array[String] = Array(),
  @BeanProperty var el:java.util.Map[String,String] = new java.util.HashMap[String,String](),        //environmental layers
  @BeanProperty var cl:java.util.Map[String,String] = new java.util.HashMap[String,String](),        //contextual layers
  @BeanProperty var miscProperties:java.util.Map[String,String] = new java.util.HashMap[String,String](),
  @BeanProperty var queryAssertions:java.util.Map[String,String] = new java.util.HashMap[String,String](),
  @BeanProperty var locationDetermined:Boolean = false,
  @BeanProperty var defaultValuesUsed:Boolean = false,
  @BeanProperty var geospatiallyKosher:Boolean = true,
  @BeanProperty var taxonomicallyKosher:Boolean = true,
  @BeanProperty var deleted:Boolean = false,
  @BeanProperty var userVerified:Boolean = false,
  @BeanProperty var firstLoaded:String="",
  @BeanProperty var lastModifiedTime:String = "",
  @BeanProperty var dateDeleted:String = "",
  @BeanProperty var lastUserAssertionDate:String = "")
  extends Cloneable with CompositePOSO {

  def objectArray:Array[POSO] = Array(occurrence,classification,location,event,attribution,identification,measurement)

  def this(rowKey:String, uuid:String) = this(rowKey,uuid,new Occurrence,new Classification,new Location,new Event,new Attribution,new Identification,
      new Measurement)

  def this() = this(null,null,new Occurrence,new Classification,new Location,new Event,new Attribution,new Identification,
      new Measurement)

  /**
   * Creates an empty new Full record based on this one to be used in Processing.
   * Initialises the userVerified and ids for use in processing
   */
  def createNewProcessedRecord : FullRecord = {
      val record = new FullRecord(this.rowKey, this.uuid)
      record.userVerified = this.userVerified
      record
  }

  override def clone : FullRecord = new FullRecord(this.rowKey,this.uuid,
      occurrence.clone,classification.clone,location.clone,event.clone,attribution.clone,
      identification.clone,measurement.clone, assertions.clone)

  /**
   * Equals implementation that compares the contents of all the contained POSOs
   */
  override def equals(that: Any) = that match {
    case other: FullRecord => {
      if (this.uuid != other.uuid) false
      else if (!EqualsBuilder.reflectionEquals(this.occurrence, other.occurrence)) false
      else if (!EqualsBuilder.reflectionEquals(this.classification, other.classification)) false
      else if (!EqualsBuilder.reflectionEquals(this.location, other.location)) false
      else if (!EqualsBuilder.reflectionEquals(this.event, other.event)) false
      else if (!EqualsBuilder.reflectionEquals(this.attribution, other.attribution, Array("taxonomicHints", "parsedHints"))) {
        false
      }
      else if (!EqualsBuilder.reflectionEquals(this.measurement, other.measurement)) false
      else if (!EqualsBuilder.reflectionEquals(this.identification, other.identification)) false
      else true
    }
    case _ => false
  }
}
