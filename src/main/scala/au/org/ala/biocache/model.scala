package au.org.ala.biocache
import scala.reflect.BeanProperty
import java.util.UUID
import org.apache.solr.client.solrj.beans.Field
import java.util.Date

/**
 * Represents an occurrence record. These fields map directly on to
 * the latest darwin core terms, with a few additional fields.
 */
class Occurrence extends Cloneable {
  override def clone : Occurrence = super.clone.asInstanceOf[Occurrence]
  @BeanProperty var uuid:String = _	
  @BeanProperty var occurrenceID:String = _
  @BeanProperty var accessrights:String = _
  @BeanProperty var associatedMedia:String = _
  @BeanProperty var associatedOccurrences:String = _
  @BeanProperty var associatedReferences:String = _
  @BeanProperty var associatedSequences:String = _
  @BeanProperty var associatedTaxa:String = _
  @BeanProperty var basisOfRecord:String = _
  @BeanProperty var behavior:String = _
  @BeanProperty var catalogNumber:String = _
  @BeanProperty var collectionCode:String = _
  @BeanProperty var collectionID:String = _
  @BeanProperty var dataGeneralizations:String = _		//used for sensitive data information
  @BeanProperty var datasetID:String = _
  @BeanProperty var disposition:String = _
  @BeanProperty var establishmentMeans:String = _
  @BeanProperty var fieldNotes:String = _
  @BeanProperty var fieldNumber:String = _
  @BeanProperty var identifier:String = _
  @BeanProperty var individualCount:String = _
  @BeanProperty var individualID:String = _
  @BeanProperty var informationWithheld:String = _   //used for sensitive data information
  @BeanProperty var institutionCode:String = _
  @BeanProperty var language:String = _
  @BeanProperty var lifeStage:String = _
  @BeanProperty var modified:String = _
  @BeanProperty var occurrenceAttributes:String = _
  @BeanProperty var occurrenceDetails:String = _
  @BeanProperty var occurrenceRemarks:String = _
  @BeanProperty var otherCatalogNumbers:String = _
  @BeanProperty var preparations:String = _
  @BeanProperty var previousIdentifications:String = _
  @BeanProperty var recordedBy:String = _
  @BeanProperty var recordNumber:String = _
  @BeanProperty var relatedResourceID:String = _
  @BeanProperty var relationshipAccordingTo:String = _
  @BeanProperty var relationshipEstablishedDate:String = _
  @BeanProperty var relationshipOfResource:String = _
  @BeanProperty var relationshipRemarks:String = _
  @BeanProperty var reproductiveCondition:String = _
  @BeanProperty var resourceID:String = _
  @BeanProperty var resourceRelationshipID:String = _
  @BeanProperty var rights:String = _
  @BeanProperty var rightsholder:String = _
  @BeanProperty var samplingProtocol:String = _
  @BeanProperty var sex:String = _
  @BeanProperty var source:String = _
  //type status perhaps should be in identification
  @BeanProperty var typeStatus:String = _
}

/**
 * POSO for handling details of a classification associated with an occurrence.
 */
class Classification extends Cloneable {
  override def clone : Classification = super.clone.asInstanceOf[Classification]
  @BeanProperty var scientificName:String = _
  @BeanProperty var scientificNameAuthorship:String = _
  @BeanProperty var scientificNameID:String = _
  @BeanProperty var taxonConceptID:String = _
  @BeanProperty var taxonID:String = _
  @BeanProperty var kingdom:String = _
  @BeanProperty var phylum:String = _
  @BeanProperty var classs:String = _
  @BeanProperty var order:String = _
  @BeanProperty var family:String = _
  @BeanProperty var genus:String = _  
  @BeanProperty var subgenus:String = _
  @BeanProperty var species:String = _
  @BeanProperty var specificEpithet:String = _
  @BeanProperty var subspecies:String = _
  @BeanProperty var infraspecificEpithet:String = _
  @BeanProperty var infraspecificMarker:String = _
  @BeanProperty var higherClassification:String = _
  @BeanProperty var parentNameUsage:String = _
  @BeanProperty var parentNameUsageID:String = _
  @BeanProperty var acceptedNameUsage:String = _
  @BeanProperty var acceptedNameUsageID:String = _
  @BeanProperty var originalNameUsage:String = _
  @BeanProperty var originalNameUsageID:String = _
  @BeanProperty var taxonRank:String = _
  @BeanProperty var taxonomicStatus:String = _
  @BeanProperty var taxonRemarks:String = _
  @BeanProperty var verbatimTaxonRank:String = _
  @BeanProperty var vernacularName:String = _
  @BeanProperty var nameAccordingTo:String = _
  @BeanProperty var nameAccordingToID:String = _
  @BeanProperty var namePublishedIn:String = _
  @BeanProperty var namePublishedInID:String = _
  @BeanProperty var nomenclaturalCode:String = _
  @BeanProperty var nomenclaturalStatus:String = _
  //custom additional fields
  @BeanProperty var taxonRankID:String = _
  @BeanProperty var kingdomID:String = _
  @BeanProperty var phylumID:String = _
  @BeanProperty var classID:String = _
  @BeanProperty var orderID:String = _
  @BeanProperty var familyID:String = _
  @BeanProperty var genusID:String = _  
  @BeanProperty var subgenusID:String = _
  @BeanProperty var speciesID:String = _
  @BeanProperty var subspeciesID:String = _
  @BeanProperty var left:String = _
  @BeanProperty var right:String = _
}

/**
 * POSO for holding measurement information for an occurrence.
 */
class Measurement extends Cloneable {
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

/**
 * POSO for handling identification information for an occurrence.
 */
class Identification extends Cloneable {
  override def clone : Identification = super.clone.asInstanceOf[Identification]
  @BeanProperty var dateIdentified:String = _
  @BeanProperty var identificationAttributes:String = _
  @BeanProperty var identificationID:String = _
  @BeanProperty var identificationQualifier:String = _
  @BeanProperty var identificationReferences:String = _
  @BeanProperty var identificationRemarks:String = _
  @BeanProperty var identifiedBy:String = _
  @BeanProperty var typeStatus:String = _
}

/**
 * POSO for holding event data for an occurrence
 */
class Event extends Cloneable {
  override def clone : Event = super.clone.asInstanceOf[Event]
  @BeanProperty var day:String = _
  @BeanProperty var endDayOfYear:String = _
  @BeanProperty var eventAttributes:String = _
  @BeanProperty var eventDate:String = _
  @BeanProperty var eventID:String = _
  @BeanProperty var eventRemarks:String = _
  @BeanProperty var eventTime:String = _
  @BeanProperty var verbatimEventDate:String = _
  @BeanProperty var year:String = _
  @BeanProperty var month:String = _
  @BeanProperty var startDayOfYear:String = _
  //custom date range fields
  @BeanProperty var startYear:String = _
  @BeanProperty var endYear:String = _
}

/**
 * POSO for holding location information for an occurrence.
 */
class Location extends Cloneable {
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
  @BeanProperty var geodeticDatum:String = _
  @BeanProperty var georeferencedBy:String = _
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
  @BeanProperty var locationAttributes:String = _
  @BeanProperty var locationID:String = _
  @BeanProperty var locationRemarks:String = _
  @BeanProperty var maximumDepthInMeters:String = _
  @BeanProperty var maximumDistanceAboveSurfaceInMeters:String = _
  @BeanProperty var maximumElevationInMeters:String = _
  @BeanProperty var minimumDepthInMeters:String = _
  @BeanProperty var minimumDistanceAboveSurfaceInMeters:String = _
  @BeanProperty var minimumElevationInMeters:String = _
  @BeanProperty var pointRadiusSpatialFit:String = _
  @BeanProperty var stateProvince:String = _
  @BeanProperty var verbatimCoordinates:String = _
  @BeanProperty var verbatimCoordinateSystem:String = _
  @BeanProperty var verbatimDepth:String = _
  @BeanProperty var verbatimElevation:String = _
  @BeanProperty var verbatimLatitude:String = _
  @BeanProperty var verbatimLocality:String = _
  @BeanProperty var verbatimLongitude:String = _
  @BeanProperty var waterBody:String = _
  //custom additional fields
  @BeanProperty var ibra:String = _
  @BeanProperty var imcra:String = _
  @BeanProperty var lga:String = _
}
/**
 * An Occurrence Model that can be used to create an Index entry
 * The @Field annotations are used for the SOLR implementation
 * But I am assuming that the will not get in the way if we decide to use a 
 * different indexing process.
 */
class OccurrenceIndex extends Cloneable {
  override def clone : OccurrenceIndex = super.clone.asInstanceOf[OccurrenceIndex]
  @BeanProperty @Field("id") var uuid:String =_
  @BeanProperty var occurrenceID:String =_
  //processed values
  @BeanProperty @Field("hub_uid") var dataHubUid:String =_
  @BeanProperty @Field("institution_code_uid") var institutionUid:String =_
  @BeanProperty @Field("institution_code") var raw_institutionCode:String =_
  @BeanProperty @Field("institution_code_name") var institutionName:String =_
  @BeanProperty @Field("collection_code_uid") var collectionUid:String =_
  @BeanProperty @Field("collection_code") var raw_collectionCode:String =_
  @BeanProperty @Field("collection_name") var collectionName:String =_
  @BeanProperty @Field("catalogue_number") var raw_catalogNumber:String =_
  @BeanProperty @Field("taxon_concept_lsid") var taxonConceptID:String =_
  @BeanProperty @Field("occurrence_date") var eventDate:java.util.Date =_
  @BeanProperty @Field("taxon_name") var scientificName:String =_
  @BeanProperty @Field("common_name") var vernacularName:String =_
  @BeanProperty @Field("rank") var taxonRank:String =_
  @BeanProperty @Field("country_code") var raw_countryCode:String =_
  @BeanProperty @Field("kingdom") var kingdom:String =_
  @BeanProperty @Field("phylum") var phylum:String =_
  @BeanProperty @Field("class") var classs:String =_
  @BeanProperty @Field("order") var order:String =_
  @BeanProperty @Field("family") var family:String =_
  @BeanProperty @Field("genus") var genus:String =_
  @BeanProperty @Field("species") var species:String =_
  @BeanProperty @Field("state") var stateProvince:String =_
  @BeanProperty @Field("latitude") var decimalLatitude:String =_
  @BeanProperty @Field("longitude") var decimalLongitude:String =_
  @BeanProperty @Field("year") var year:String =_
  @BeanProperty @Field("month") var month:String =_
  @BeanProperty @Field("basis_of_record") var basisOfRecord:String =_
  @BeanProperty @Field("type_status") var typeStatus:String =_
  @BeanProperty @Field("location_remarks") var raw_locationRemarks:String =_
  @BeanProperty @Field("occurrence_remarks") var raw_occurrenceRemarks:String =_
  @BeanProperty @Field("lft") var left:String =_
  @BeanProperty @Field("rgt") var right:String =_
  @BeanProperty @Field("ibra") var ibra:String = _
  @BeanProperty @Field("imcra") var imcra:String = _
  @BeanProperty @Field("places") var lga:String = _
  
  //raw record fields
  @BeanProperty @Field("raw_taxon_name") var raw_scientificName:String =_
  @BeanProperty @Field("raw_basis_of_record") var raw_basisOfRecord:String =_
  @BeanProperty @Field("raw_type_status") var raw_typeStatus:String =_
  @BeanProperty @Field("raw_common_name") var raw_vernacularName:String =_
  
  //constructed fields
  @BeanProperty @Field("lat_long") var latLong:String =_
  @BeanProperty @Field("point-1") var point1:String =_
  @BeanProperty @Field("point-0.1") var point01:String =_
  @BeanProperty @Field("point-0.01") var point001:String =_
  @BeanProperty @Field("point-0.001") var point0001:String =_
  @BeanProperty @Field("point-0.0001") var point00001:String =_
}

/**
 * Enumeration of record versions.
 * sealed = cannot be extended unless declared in this source file.
 * 
 * @author Dave Martin (David.Martin@csiro.au)
 */
abstract sealed class Version
case object Raw extends Version
case object Processed extends Version
case object Consensus extends Version

/**
 * Enum of occurrence record versions
 */
object Versions {
  val RAW = Raw
  val PROCESSED = Processed
  val CONSENSUS = Consensus
}

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
  @BeanProperty var right:String)
  extends Cloneable {
  def this() = this(null,null,null,null,null,null,null)
  override def clone : TaxonProfile = super.clone.asInstanceOf[TaxonProfile]
}

/**
 * Represents the full attribution for a record.
 */
class Attribution (
  @BeanProperty var dataProviderUid:String,
  @BeanProperty var dataResourceUid:String,
  @BeanProperty var collectionUid:String,
  @BeanProperty var institutionUid:String,
  @BeanProperty var dataHubUid:String,
  @BeanProperty var institutionName:String,
  @BeanProperty var collectionName:String)
  extends Cloneable {
  def this() = this(null,null,null,null,null,null,null)
  override def clone : Attribution = super.clone.asInstanceOf[Attribution]
}

/**
 * Encapsulates a complete specimen or occurrence record.
 */
class FullRecord (
  @BeanProperty var o:Occurrence,
  @BeanProperty var c:Classification,
  @BeanProperty var l:Location,
  @BeanProperty var e:Event,
  @BeanProperty var assertions:Array[String])
  extends Cloneable {
  def this() = this(new Occurrence,new Classification,new Location,new Event, Array())
  override def clone : FullRecord = new FullRecord(o.clone,c.clone,l.clone,e.clone,assertions.clone)
}

/**
 * Quality Assertions are made by man or machine.
 * Man - provided through a UI, giving a positive or negative assertion
 * Machine - provided through backend processing
 */
class QualityAssertion (
  @BeanProperty var uuid:String,
  @BeanProperty var assertionName:String,
  @BeanProperty var assertionCode:Int,
  @BeanProperty var positive:Boolean,
  @BeanProperty var comment:String,
  @BeanProperty var value:String,
  @BeanProperty var userId:String,
  @BeanProperty var userDisplayName:String)
  extends Cloneable with Comparable[AnyRef] {

  def this() = this(null,null,-1,false,null,null,null,null)
  override def clone : QualityAssertion = super.clone.asInstanceOf[QualityAssertion]
  override def equals(that: Any) = that match {
    case other: QualityAssertion => {
      (other.assertionCode == assertionCode) && (other.positive == positive) && (other.userId == userId)
    }
    case _ => false
  }

  def compareTo(qa:AnyRef) = -1
}

/**
 * A companion object for the QualityAssertion class that provides factory
 * type functionality.
 */
object QualityAssertion {
  def apply(errorCode:ErrorCode) = {
    val uuid = UUID.randomUUID.toString
    new QualityAssertion(uuid,errorCode.name,errorCode.code,false,null,null,null,null)
  }
  def apply(errorCode:ErrorCode,positive:Boolean) = {
    val uuid = UUID.randomUUID.toString
    new QualityAssertion(uuid,errorCode.name,errorCode.code,positive,null,null,null,null)
  }
  def apply(errorCode:ErrorCode,positive:Boolean,comment:String) = {
    val uuid = UUID.randomUUID.toString
    new QualityAssertion(uuid,errorCode.name,errorCode.code,positive,comment,null,null,null)
  }
  def apply(assertionCode:Int,positive:Boolean,comment:String) = {
    val uuid = UUID.randomUUID.toString
    new QualityAssertion(uuid,null,assertionCode,positive,comment,null,null,null)
  }
}
