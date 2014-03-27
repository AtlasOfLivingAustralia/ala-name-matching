package au.org.ala.biocache.model

import scala.beans.BeanProperty
import org.codehaus.jackson.annotate.JsonIgnore
import org.apache.commons.lang.builder.ToStringBuilder
import scala.collection.mutable.HashMap
import au.org.ala.biocache.poso.POSO

/**
 * Represents the full attribution for a record.
 */
class Attribution (
  @BeanProperty var dataProviderUid:String,
  @BeanProperty var dataProviderName:String,
  @BeanProperty var dataResourceUid:String,
  @BeanProperty var dataResourceName:String,
  @BeanProperty var collectionUid:String,
  @BeanProperty var institutionUid:String,
  @BeanProperty var dataHubUid:Array[String],
  @BeanProperty var dataHubName:String,
  @BeanProperty var institutionName:String,
  @BeanProperty var collectionName:String,
  @BeanProperty var citation:String,
  @BeanProperty var provenance:String,
  @JsonIgnore var taxonomicHints:Array[String],
  @JsonIgnore var defaultDwcValues:Map[String,String])
  extends Cloneable with POSO {
  def this() = this(null,null,null,null,null,null,null,null,null,null,null, null, null, null)
  override def clone : Attribution = super.clone.asInstanceOf[Attribution]
  override def toString = ToStringBuilder.reflectionToString(this)
  // stores whether or not the data resource has collections associated with it
  @JsonIgnore var hasMappedCollections:Boolean = false
  @JsonIgnore private var parsedHints:Map[String,Set[String]] = null

  /**
   * Parse the hints into a usable map with rank -> Set.
   */
  @JsonIgnore
  def retrieveParseHints: Map[String, Set[String]] = {
    if (parsedHints == null) {
      if (taxonomicHints != null) {
        val rankSciNames = new HashMap[String, Set[String]]
        val pairs = taxonomicHints.toList.map(x => x.split(":"))
        pairs.foreach(pair => {
          val values = rankSciNames.getOrElse(pair(0), Set())
          rankSciNames.put(pair(0), values + pair(1).trim.toLowerCase)
        })
        parsedHints = rankSciNames.toMap
      } else {
        parsedHints = Map[String, Set[String]]()
      }
    }
    parsedHints
  }
}
