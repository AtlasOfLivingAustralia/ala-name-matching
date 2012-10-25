package au.org.ala.biocache
import scala.reflect.BeanProperty
import java.util.UUID
import org.apache.solr.client.solrj.beans.Field
import collection.mutable.HashMap
import collection.JavaConversions
import org.apache.commons.lang.builder.{ToStringBuilder, EqualsBuilder}
import java.util.Date
import org.apache.commons.lang.time.DateFormatUtils
import org.codehaus.jackson.annotate.{JsonIgnoreProperties, JsonIgnore}

/**
 * Represents an occurrence record. These fields map directly on to
 * the latest darwin core terms, with a few additional fields.
 */
class Occurrence extends Cloneable /*with Mappable*/ with POSO {
  import JavaConversions._  
  override def clone : Occurrence = super.clone.asInstanceOf[Occurrence]
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
  @BeanProperty var occurrenceStatus:String = _
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
  //Additional fields for HISPID support
  @BeanProperty var collectorFieldNumber:String = _  //This value now maps to the correct DWC field http://rs.tdwg.org/dwc/terms/fieldNumber
  @BeanProperty var cultivated:String = _ //http://www.chah.org.au/hispid/terms/cultivatedOccurrence
  @BeanProperty var duplicates:String = _ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0711
  @BeanProperty var duplicatesOriginalInstitutionID:String = _ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0580
  @BeanProperty var duplicatesOriginalUnitID:String = _ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0579
  @BeanProperty var loanIdentifier:String = _ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0712
  @BeanProperty var loanSequenceNumber:String = _  //this one would be http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0713 but not in current archive
  @BeanProperty var loanDestination:String = _ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0714
  @BeanProperty var loanForBotanist:String = _ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0715
  @BeanProperty var loanDate:String = _ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0717
  @BeanProperty var loanReturnDate:String = _ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0718
  @BeanProperty var phenology:String =_ //http://www.chah.org.au/hispid/terms/phenology
  @BeanProperty var preferredFlag:String =_
  @BeanProperty var secondaryCollectors:String =_ //http://www.chah.org.au/hispid/terms/secondaryCollectors
  @BeanProperty var naturalOccurrence:String = _ //http://www.chah.org.au/hispid/terms/naturalOccurrence
  //this property is in use in flickr tagging - currently no equivalent in DwC
  @BeanProperty var validDistribution:String = _
  //custom fields
  @BeanProperty var images:Array[String] = _
  //custom fields
  @BeanProperty var sounds:Array[String] = _
  //custom fields
  @BeanProperty var videos:Array[String] = _
  @BeanProperty var interactions:Array[String] = _
  //stores either U,R or D.  U - a unique record, R - a representative record in a group of duplicates, D - a duplicate record in a group
  // when null a value of "U" is assumed
  /*
    D has been split into categories: 
    D1- duplicate belongs to the same data resource as the representative record. 
    D2- duplicate belongs to a different data resource as the representative record
  */
  @BeanProperty var duplicationStatus:String =_
  @BeanProperty var duplicationType:Array[String] =_
  //Store the conservation status
  //FIXME these should actually be on the classification object
  @BeanProperty var austConservation:String = _
  @BeanProperty var stateConservation:String = _
  //Store the original values before the SDS changes
  var originalSensitiveValues:Map[String,String] =_
  @BeanProperty var outlierForLayers:Array[String] = _

   @JsonIgnore
  def getOriginalSensitiveValues():Map[String,String] = originalSensitiveValues
  def setOriginalSensitiveValues(originalSensitiveValues:Map[String,String])=this.originalSensitiveValues = originalSensitiveValues
}

/**
 * POSO for handling details of a classification associated with an occurrence.
 */
class Classification extends Cloneable /*with Mappable*/ with POSO {
  import JavaConversions._
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
  @BeanProperty var superfamily:String = _	//an addition to darwin core
  @BeanProperty var family:String = _
  @BeanProperty var subfamily:String = _ //an addition to darwin core
  @BeanProperty var genus:String = _  
  @BeanProperty var subgenus:String = _
  @BeanProperty var species:String = _
  @BeanProperty var specificEpithet:String = _
  @BeanProperty var subspecies:String = _
  @BeanProperty var infraspecificEpithet:String = _
  @BeanProperty var infraspecificMarker:String = _
  @BeanProperty var cultivarName:String = _ //an addition to darwin core for http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0315
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
  //additional fields for HISPID support
  @BeanProperty var scientificNameWithoutAuthor:String = _
  @BeanProperty var scientificNameAddendum:String = _ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0334
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
  @BeanProperty var speciesHabitats:Array[String] = _
  @BeanProperty var speciesGroups:Array[String] =_
  @BeanProperty var nameMatchMetric:String =_ //stores the type of name match that was performed
  @BeanProperty var taxonomicIssue:String = _  //stores if no issue, questionableSpecies, conferSpecies or affinitySpecies
}

/**
 * POSO for holding measurement information for an occurrence.
 */
class Measurement extends Cloneable /*with Mappable*/ with POSO {
  import JavaConversions._
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
class Identification extends Cloneable /*with Mappable*/ with POSO {
  import JavaConversions._
  override def clone : Identification = super.clone.asInstanceOf[Identification]
  @BeanProperty var dateIdentified:String = _
  @BeanProperty var identificationAttributes:String = _
  @BeanProperty var identificationID:String = _
  @BeanProperty var identificationQualifier:String = _
  @BeanProperty var identificationReferences:String = _
  @BeanProperty var identificationRemarks:String = _
  @BeanProperty var identifiedBy:String = _
  @BeanProperty var identifierRole:String = _ //HISPID addition http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0376
  @BeanProperty var typeStatus:String = _
  /* AVH addition */
  @BeanProperty var abcdTypeStatus:String = _ //ABCD addition for AVH http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0645
  @BeanProperty var typeStatusQualifier:String = _ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0647
  @BeanProperty var typifiedName:String = _ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0604
  @BeanProperty var verbatimDateIdentified:String = _ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0383
  @BeanProperty var verifier:String = _ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0649
  @BeanProperty var verificationDate:String =_ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0657
  @BeanProperty var verificationNotes:String = _ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0658
  @BeanProperty var abcdIdentificationQualifier:String =_ //ABCD addition for AVH http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0332
  @BeanProperty var abcdIdentificationQualifierInsertionPoint:String =_ //ABCD addition for AVH http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0333
}

/**
 * POSO for holding event data for an occurrence
 */
class Event extends Cloneable/*with Mappable*/with POSO {
  import JavaConversions._
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

  override def toString = ToStringBuilder.reflectionToString(this)
}

/**
 * POSO for holding location information for an occurrence.
 */
class Location extends Cloneable /*with Mappable*/ with POSO {
  import JavaConversions._
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
  def getOriginalDecimalLatitude():String = originalDecimalLatitude
  def setOriginalDecimalLatitude(decimalLatitude:String)=this.originalDecimalLatitude = decimalLatitude
  
  @JsonIgnore
  def getOriginalDecimalLongitude():String = originalDecimalLongitude
  def setOriginalDecimalLongitude(decimalLongitude:String)= this.originalDecimalLongitude = decimalLongitude
  
  @JsonIgnore
  def getOriginalVerbatimLatitude():String = originalVerbatimLatitude
  def setOriginalVerbatimLatitude(latitude:String)=this.originalVerbatimLatitude = latitude
  
  @JsonIgnore
  def getOriginalVerbatimLongitude():String = originalVerbatimLongitude
  def setOriginalVerbatimLongitude(longitude:String)= this.originalVerbatimLongitude = longitude
  
  @JsonIgnore
  def getOriginalLocality():String = originalLocality
  def setOrginalLocality(locality:String) = this.originalLocality = locality
  @JsonIgnore
  def getOriginalLocationRemarks():String = originalLocationRemarks
  def setOriginalLocationRemarks(remarks:String) = this.originalLocationRemarks = remarks
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
  @BeanProperty var right:String,
  @BeanProperty var sensitive:Array[SensitiveSpecies],
  @BeanProperty var conservation:Array[ConservationSpecies])
  extends Cloneable {
  def this() = this(null,null,null,null,null,null,null, null,null)
  override def clone : TaxonProfile = super.clone.asInstanceOf[TaxonProfile]
  private var conservationMap:Map[String,String]= null
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
        //println("conservation " + conservationMap)
      }
      return conservationMap.get(loc)
    }
    return None
  }
}

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
  import JavaConversions._
  def this() = this(null,null,null,null,null,null,null,null,null,null,null, null, null, null)
  override def clone : Attribution = super.clone.asInstanceOf[Attribution]
  override def toString = ToStringBuilder.reflectionToString(this)
  // stores whether or not the data resource has collections associated with it
  @JsonIgnore var hasMappedCollections:Boolean=false
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

/**
 * Encapsulates a complete specimen or occurrence record.
 */
@JsonIgnoreProperties class FullRecord (
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

/**
 * Stores the information about a sensitive species
 * 
 */
class SensitiveSpecies(
  @BeanProperty var zone:String,
  @BeanProperty var category:String){

  def this() = this(null, null)

  override def toString():String = {
    "zone:"+zone+" category:" + category
  }
}

class ConservationSpecies(
        @BeanProperty var region:String,
        @BeanProperty var regionId:String,
        @BeanProperty var status:String,
        @BeanProperty var rawStatus:String
        ){
    def this() = this(null, null, null,null)
}

/**
 * Quality Assertions are made by man or machine.
 * Man - provided through a UI, giving a positive or negative assertion
 * Machine - provided through backend processing
 */
class QualityAssertion (
  @BeanProperty var uuid:String,
  @BeanProperty var name:String,
  @BeanProperty var code:Int,
  @BeanProperty var problemAsserted:Boolean,
  @BeanProperty var comment:String,
  @BeanProperty var value:String,
  @BeanProperty var userId:String,
  @BeanProperty var userDisplayName:String,
  @BeanProperty var created:String)
  extends Cloneable with Comparable[AnyRef] with POSO {

  def this() = this(null,null,-1,false,null,null,null,null,null)
  override def clone : QualityAssertion = super.clone.asInstanceOf[QualityAssertion]
  override def equals(that: Any) = that match {
    case other: QualityAssertion => {
      (other.code == code) && (other.problemAsserted == problemAsserted) && (other.userId == userId)
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
  import BiocacheConversions._  
  def apply(code:Int) = {
    val uuid = UUID.randomUUID.toString
    val errorCode = AssertionCodes.getByCode(code)
    if(errorCode.isEmpty){
        throw new Exception("Unrecognised code: "+ code)
    }
    new QualityAssertion(uuid,errorCode.get.name,errorCode.get.code,true,null,null,null,null,new Date())
  }

  def apply(errorCode:ErrorCode) = {
    val uuid = UUID.randomUUID.toString
    new QualityAssertion(uuid,errorCode.name,errorCode.code,true,null,null,null,null,new Date())
  }
  def apply(errorCode:ErrorCode,problemAsserted:Boolean) = {
    val uuid = UUID.randomUUID.toString
    new QualityAssertion(uuid,errorCode.name,errorCode.code,problemAsserted,null,null,null,null,new Date())
  }
  def apply(errorCode:ErrorCode,problemAsserted:Boolean,comment:String) = {
    val uuid = UUID.randomUUID.toString
    new QualityAssertion(uuid,errorCode.name,errorCode.code,problemAsserted,comment,null,null,null,new Date())
  }
  def apply(errorCode:ErrorCode,comment:String) = {
    val uuid = UUID.randomUUID.toString
    new QualityAssertion(uuid,errorCode.name,errorCode.code,true,comment,null,null,null,new Date())
  }
  def apply(assertionCode:Int,problemAsserted:Boolean,comment:String) = {
    val uuid = UUID.randomUUID.toString
    new QualityAssertion(uuid,null,assertionCode,problemAsserted,comment,null,null,null,new Date())
  }
}
/**
 * A class that represents a JCU assertion query. 
 */
@JsonIgnoreProperties(ignoreUnknown=true) 
class JCUAssertion(@BeanProperty var id:java.lang.Integer,
      @BeanProperty var apiKey:String,
      @BeanProperty var status:String,
      @BeanProperty var lastModified:java.util.Date,
      @BeanProperty var comment:String,
      @BeanProperty var classification:String,
      @BeanProperty var species:String,
      @BeanProperty var user:JCUUser,
      @BeanProperty var area:String,
      @BeanProperty var ignored:java.lang.Boolean
    ) {
  def this() = this(null,null,null,null,null,null,null,null,null,null)
}

@JsonIgnoreProperties(ignoreUnknown=true)     
class JCUUser(@BeanProperty var email:String, @BeanProperty var authority:java.lang.Integer, @BeanProperty var isAdmin:Boolean){
  def this() = this(null,null,false)
}


/**
 * A type of Quality Assertion that needs to be applied based on a query 
 */
@JsonIgnoreProperties(Array("id","rawAssertion", "rawQuery","records"))
class AssertionQuery(
  @BeanProperty var id:String,
  @BeanProperty var uuid:String,
  @BeanProperty var rawAssertion:String,
  @BeanProperty var createdDate:java.util.Date,
  @BeanProperty var modifiedDate:java.util.Date,  
  @BeanProperty var rawQuery:String,
  @BeanProperty var qidQuery:String,
  @BeanProperty var deletedDate:java.util.Date,
  @BeanProperty var userName:String,
  @BeanProperty var authority:String,
  @BeanProperty var assertionType:String,
  @BeanProperty var comment: String,
  @BeanProperty var includeNew:Boolean,
  @BeanProperty var disabled:Boolean,
  @BeanProperty var lastApplied:java.util.Date,
  @BeanProperty var records:Array[String] = Array()) extends POSO{
  
  def this()= this(null,null, null,null, null, null,null,null,null,null,null,null,true, false,null)
    
  def this(jcu:JCUAssertion) = this(jcu.apiKey + "|" + jcu.id, Config.assertionQueryDAO.createOrRetrieveUuid(jcu.apiKey + "|" + jcu.id),null,null,jcu.lastModified,null,null,null,jcu.user.email, if(jcu.user.authority != null) jcu.user.authority.toString() else null, jcu.classification, jcu.comment, true, if(jcu.ignored == null) false else jcu.ignored,null)
}


class OutlierResult (
  @BeanProperty var testUuid:String,
  @BeanProperty var outlierForLayersCount:Int)

