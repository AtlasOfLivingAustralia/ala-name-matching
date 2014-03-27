package au.org.ala.biocache.model

import scala.beans.BeanProperty
import org.apache.commons.lang.builder.ToStringBuilder
import org.codehaus.jackson.annotate.JsonIgnore
import au.org.ala.biocache.poso.POSO

/**
 * POSO for holding location information for an occurrence.
 */
class Location extends Cloneable with POSO {
  override def clone : Location = super.clone.asInstanceOf[Location]
  @BeanProperty var uuid:String = _
  //dwc terms
  @BeanProperty var continent:String = _
  @BeanProperty var coordinatePrecision:String = _
  @BeanProperty var coordinateUncertaintyInMeters:String = _
  @BeanProperty var country:String = _
  @BeanProperty var countryCode:String = _
  @BeanProperty var county:String = _
  @BeanProperty var decimalLatitude:String = _
  @BeanProperty var decimalLongitude:String = _
  @BeanProperty var footprintSpatialFit:String = _
  @BeanProperty var footprintWKT:String = _
  @BeanProperty var footprintSRS:String = _
  @BeanProperty var geodeticDatum:String = _
  @BeanProperty var georeferencedBy:String = _
  @BeanProperty var georeferencedDate:String = _
  @BeanProperty var georeferenceProtocol:String = _
  @BeanProperty var georeferenceRemarks:String = _
  @BeanProperty var georeferenceSources:String = _
  @BeanProperty var georeferenceVerificationStatus:String = _
  @BeanProperty var habitat:String = _
  @BeanProperty var higherGeography:String = _
  @BeanProperty var higherGeographyID:String = _
  @BeanProperty var island:String = _
  @BeanProperty var islandGroup:String = _
  @BeanProperty var locality:String = _
  @BeanProperty var locationAccordingTo:String = _
  @BeanProperty var locationAttributes:String = _
  @BeanProperty var locationID:String = _
  @BeanProperty var locationRemarks:String = _
  @BeanProperty var maximumDepthInMeters:String = _
  @BeanProperty var maximumDistanceAboveSurfaceInMeters:String = _
  @BeanProperty var maximumElevationInMeters:String = _
  @BeanProperty var minimumDepthInMeters:String = _
  @BeanProperty var minimumDistanceAboveSurfaceInMeters:String = _
  @BeanProperty var minimumElevationInMeters:String = _
  @BeanProperty var municipality:String = _
  @BeanProperty var pointRadiusSpatialFit:String = _
  @BeanProperty var stateProvince:String = _
  @BeanProperty var verbatimCoordinates:String = _
  @BeanProperty var verbatimCoordinateSystem:String = _
  @BeanProperty var verbatimDepth:String = _
  @BeanProperty var verbatimElevation:String = _
  @BeanProperty var verbatimLatitude:String = _
  @BeanProperty var verbatimLocality:String = _
  @BeanProperty var verbatimLongitude:String = _
  @BeanProperty var verbatimSRS:String = _
  @BeanProperty var waterBody:String = _
  //custom additional fields
  @BeanProperty var ibra:String = _
  @BeanProperty var ibraSubregion:String = _ //http://www.chah.org.au/hispid/terms/ibraSubregion
  @BeanProperty var imcra:String = _
  @BeanProperty var lga:String = _
  //AVH additions
  @BeanProperty var generalisedLocality: String =_ ///http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0977
  @BeanProperty var nearNamedPlaceRelationTo: String = _ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0980
  @BeanProperty var australianHerbariumRegion: String = _ //http://www.chah.org.au/hispid/terms/australianHerbariumRegion
  // For occurrences found to be outside the expert distribution range for the associated speces.
  @BeanProperty var distanceOutsideExpertRange: String = _
  @BeanProperty var easting: String =_
  @BeanProperty var northing: String =_
  @BeanProperty var zone: String =_

  //fields that need be hidden from all public API
  //These fields can NOT be @BeanProperty because we need the getter method to have a @JsonIgnore annotation
  var originalDecimalLatitude:String =_
  var originalDecimalLongitude:String =_
  var originalLocality:String =_
  var originalLocationRemarks:String=_
  var originalVerbatimLatitude:String=_
  var originalVerbatimLongitude:String=_

  override def toString = ToStringBuilder.reflectionToString(this)

  @JsonIgnore
  def getOriginalDecimalLatitude:String = originalDecimalLatitude
  def setOriginalDecimalLatitude(decimalLatitude:String)=this.originalDecimalLatitude = decimalLatitude

  @JsonIgnore
  def getOriginalDecimalLongitude:String = originalDecimalLongitude
  def setOriginalDecimalLongitude(decimalLongitude:String)= this.originalDecimalLongitude = decimalLongitude

  @JsonIgnore
  def getOriginalVerbatimLatitude:String = originalVerbatimLatitude
  def setOriginalVerbatimLatitude(latitude:String)=this.originalVerbatimLatitude = latitude

  @JsonIgnore
  def getOriginalVerbatimLongitude:String = originalVerbatimLongitude
  def setOriginalVerbatimLongitude(longitude:String)= this.originalVerbatimLongitude = longitude

  @JsonIgnore
  def getOriginalLocality:String = originalLocality
  def setOrginalLocality(locality:String) = this.originalLocality = locality

  @JsonIgnore
  def getOriginalLocationRemarks:String = originalLocationRemarks
  def setOriginalLocationRemarks(remarks:String) = this.originalLocationRemarks = remarks
}
