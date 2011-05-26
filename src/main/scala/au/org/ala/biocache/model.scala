package au.org.ala.biocache
import scala.reflect.BeanProperty
import java.util.UUID
import org.apache.commons.lang.time.DateFormatUtils
import org.apache.solr.client.solrj.beans.Field
import org.codehaus.jackson.annotate.JsonIgnore
import collection.mutable.HashMap
import org.apache.commons.lang.builder.{ToStringBuilder, EqualsBuilder}

/**
 * Represents an occurrence record. These fields map directly on to
 * the latest darwin core terms, with a few additional fields.
 */
class Occurrence extends Cloneable with Mappable {
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
  //custom fields
  @BeanProperty var images:Array[String] = _

  @JsonIgnore
  def getMap():Map[String,String]={
    val map =Map[String,String]("occurrenceID"->occurrenceID, "accessrights"->accessrights, "associatedMedia"->associatedMedia,
                                "associatedOccurrences"->associatedOccurrences, "associatedReferences"->associatedReferences,
                                "associatedSequences"->associatedSequences, "associatedTaxa"->associatedTaxa, "basisOfRecord"->basisOfRecord,
                                "behavior"->behavior, "catalogNumber"->catalogNumber, "collectionCode" -> collectionCode,
                                "collectionID"->collectionID, "dataGeneralizations"->dataGeneralizations, "datasetID"->datasetID,
                                "disposition"->disposition, "establishmentMeans"->establishmentMeans, "fieldNotes"->fieldNotes,
                                "fieldNumber"->fieldNumber, "identifier"->identifier, "individualCount"->individualCount,
                                "individualID"->individualID, "informationWithheld"->informationWithheld, "institutionCode"->institutionCode,
                                "language"->language, "lifeStage"->lifeStage, "modified"->modified, "occurrenceAttributes"->occurrenceAttributes,
                                "occurrenceDetails"->occurrenceDetails, "occurrenceRemarks"->occurrenceRemarks, "otherCatalogNumbers"->otherCatalogNumbers,
                                "preparations"->preparations, "previousIdentifications"->previousIdentifications, "recordedBy"->recordedBy,
                                "recordNumber"->recordNumber, "relatedResourceID"->relatedResourceID, "relationshipAccordingTo"->relationshipAccordingTo,
                                "relationshipEstablishedDate"->relationshipEstablishedDate, "relationshipOfResource"->relationshipOfResource,
                                "relationshipRemarks"->relationshipRemarks, "reproductiveCondition"->reproductiveCondition, "resourceID"->resourceID,
                                "resourceRelationshipID"->resourceRelationshipID, "rights"->rights ,"rightsholder"->rightsholder,
                                "samplingProtocol"->samplingProtocol, "sex"->sex, "source"->source, "typeStatus"->typeStatus, "images"-> images)

    map.filter(i => i._2!= null)
  }

}
/**
 *  Classes that need to have their fields put into a map should use this trait
 */
trait Mappable{
  def getMap():Map[String,String]
  implicit def arrToString(in:Array[String]):String={
    if(in == null)
      null
    else{
      Json.toJSON(in.asInstanceOf[Array[AnyRef]])
    }
  }
 
}
/**
 * POSO for handling details of a classification associated with an occurrence.
 */
class Classification extends Cloneable with Mappable {
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
  @BeanProperty var speciesGroups:Array[String] =_

  @JsonIgnore
  def getMap():Map[String,String]={
    val map =Map[String,String]("scientificName"->scientificName, "scientificNameAuthorship"->scientificNameAuthorship,
                       "scientificNameID"->scientificNameID, "taxonConceptID"->taxonConceptID, "taxonID"->taxonID,
                       "kingdom"->kingdom,"phylum"->phylum,"classs"->classs,"order"->order,"family"->family,
                       "genus"->genus,"subgenus"->subgenus,"species"->species,"specificEpithet"->specificEpithet,
                       "subspecies"->subspecies, "infraspecificEpithet"->infraspecificEpithet, "infraspecificMarker"->infraspecificMarker,
                       "higherClassification"->higherClassification, "parentNameUsage"->parentNameUsage, "parentNameUsageID"->parentNameUsageID,
                       "acceptedNameUsage"->acceptedNameUsage, "acceptedNameUsageID"->acceptedNameUsageID, "originalNameUsage"->originalNameUsage,
                       "originalNameUsageID"->originalNameUsageID, "taxonRank"->taxonRank, "taxonomicStatus"->taxonomicStatus,
                       "taxonRemarks"->taxonRemarks, "verbatimTaxonRank"->verbatimTaxonRank, "vernacularName"->vernacularName,
                       "nameAccordingTo"->nameAccordingTo, "nameAccordingToID"->nameAccordingToID,"namePublishedIn"->namePublishedIn,
                       "namePublishedInID"->namePublishedInID,"nomenclaturalCode"->nomenclaturalCode,"nomenclaturalStatus"->nomenclaturalStatus,
                       "taxonRankID"->taxonRankID, "kingdomID"->kingdomID, "phylumID"->phylumID, "classID"->classID, "orderID"->orderID,
                       "familyID"->familyID, "genusID"->genusID, "subgenusID"->subgenusID, "speciesID"->speciesID, "subspeciesID"->subspeciesID,
                       "left"->left, "right"->right, "speciesGroups"->speciesGroups)

    map.filter(i => i._2!= null)
  }
}

/**
 * POSO for holding measurement information for an occurrence.
 */
class Measurement extends Cloneable with Mappable {
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

  @JsonIgnore
  def getMap():Map[String,String]={
    val map =Map[String,String]("measurementAccuracy"->measurementAccuracy, "measurementDeterminedBy"->measurementDeterminedBy,
                                "measurementDeterminedDate"->measurementDeterminedDate, "measurementID"->measurementID,
                                "measurementMethod"->measurementMethod, "measurementRemarks"->measurementRemarks,
                                "measurementType"->measurementType, "measurementUnit"->measurementUnit, "measurementValue"->measurementValue)

    map.filter(i => i._2!= null)
  }
}

/**
 * POSO for handling identification information for an occurrence.
 */
class Identification extends Cloneable with Mappable {
  override def clone : Identification = super.clone.asInstanceOf[Identification]
  @BeanProperty var dateIdentified:String = _
  @BeanProperty var identificationAttributes:String = _
  @BeanProperty var identificationID:String = _
  @BeanProperty var identificationQualifier:String = _
  @BeanProperty var identificationReferences:String = _
  @BeanProperty var identificationRemarks:String = _
  @BeanProperty var identifiedBy:String = _
  @BeanProperty var typeStatus:String = _

  @JsonIgnore
  def getMap():Map[String,String]={
    val map =Map[String,String]("dateIdentified"->dateIdentified, "identificationAttributes"->identificationAttributes,
                                "identificationID"->identificationID, "identificationQualifier"->identificationQualifier,
                                "identificationReferences"->identificationReferences, "identificationRemarks"->identificationRemarks,
                                "identifiedBy"->identifiedBy, "typeStatus"->typeStatus)

    map.filter(i => i._2!= null)
  }
}

/**
 * POSO for holding event data for an occurrence
 */
class Event extends Cloneable with Mappable{
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

  @JsonIgnore
  def getMap():Map[String,String]={
    val map =Map[String,String]("day"->day,"endDayOfYear"->endDayOfYear,"eventAttributes"->eventAttributes,
                                "eventDate"->eventDate,"eventID"->eventID,"eventRemarks"->eventRemarks,"eventTime"->eventTime,
                                "verbatimEventDate"->verbatimEventDate,"year"->year,"month"->month,"startDayOfYear"->startDayOfYear,
                                "startYear"->startYear,"endYear"->endYear)

    map.filter(i => i._2!= null)
  }
}

class ContextualLayers extends Cloneable{//} with Mappable{
  override def clone : ContextualLayers = super.clone.asInstanceOf[ContextualLayers]
  //@BeanProperty var ibra_merged:String = _
  //@BeanProperty var imcra4_pb:String = _
  //@BeanProperty var aus1:String = _
  //@BeanProperty var aus2:String = _
  @BeanProperty var fal_dom_no_01:String = _
  @BeanProperty var til_dom_ha_01:String = _
  @BeanProperty var st_dom_no_01:String = _
  @BeanProperty var til_dom_no_01:String = _
  @BeanProperty var fal_dom_ha_01:String = _
  @BeanProperty var irr_dom_no_01:String = _
  @BeanProperty var st_dom_ha_01:String = _
  @BeanProperty var native_veg:String = _
  @BeanProperty var landcover:String = _
  @BeanProperty var vast:String = _
  @BeanProperty var present_veg:String = _
  @BeanProperty var sodicity:String = _
  @BeanProperty var landuse:String = _
  @BeanProperty var capad08_ext:String = _
  @BeanProperty var diwa_type_criteria:String = _
  @BeanProperty var ger_analysis_boundary_v01:String = _
  @BeanProperty var ger_border_ranges:String = _
  @BeanProperty var ger_geri_boundary_v102_australia:String = _
  @BeanProperty var ger_hunter:String = _
  @BeanProperty var ger_hunter_analysis_mask:String = _
  @BeanProperty var ger_hunter_areas_of_interest:String = _
  @BeanProperty var ger_k2c_management_regions_oct2009:String = _
  @BeanProperty var ger_kosciuszko_to_coast:String = _
  @BeanProperty var ger_s2s_priority_area_billabong_creek_v01:String = _
  @BeanProperty var ger_s2s_priority_areas_v05:String = _
  @BeanProperty var ger_slopes_to_summit:String = _
  @BeanProperty var ger_upper_hunter_focus_area_v2:String = _
  @BeanProperty var ibra_sub_merged:String = _
  @BeanProperty var ramsar:String = _
  @BeanProperty var nrm_regions_2010:String = _
  @BeanProperty var australian_coral_ecoregions:String = _
}

/**
 * POSO for the Environmental Layer values
 */
class EnvironmentalLayers extends Cloneable{//} with Mappable{
  override def clone : EnvironmentalLayers = super.clone.asInstanceOf[EnvironmentalLayers]
  @BeanProperty var nrm_exp:String = _
  @BeanProperty var rel_spr:String = _
  @BeanProperty var rain_aut:String = _
  @BeanProperty var rel_sum:String = _
  @BeanProperty var rain_win:String = _
  @BeanProperty var erosivity:String = _
  @BeanProperty var rain_ann:String = _
  @BeanProperty var rel_win:String = _
  @BeanProperty var rel_aut:String = _
  @BeanProperty var rain_sum:String = _
  @BeanProperty var rain_spr:String = _
  @BeanProperty var aria:String = _
  @BeanProperty var dairy_dse:String = _
  @BeanProperty var sheep_dse:String = _
  @BeanProperty var beef_dse:String = _
  @BeanProperty var erodibility:String = _
  @BeanProperty var pawhc:String = _
  @BeanProperty var drain_cv:String = _
  @BeanProperty var soilm_cv:String = _
  @BeanProperty var evap_mean:String = _
  @BeanProperty var soilm_mean:String = _
  @BeanProperty var drain_mean:String = _
  @BeanProperty var evap_cv:String = _
  @BeanProperty var runoff_mean:String = _
  @BeanProperty var ndvi_mean:String = _
  @BeanProperty var elevation:String = _
  @BeanProperty var slope_length:String = _
  @BeanProperty var npp_mean:String = _
  @BeanProperty var aspect:String = _
  @BeanProperty var threatnd_sp:String = _
  @BeanProperty var migratry_sp:String = _
  @BeanProperty var wdef_mean:String = _
  @BeanProperty var mintx:String = _
  @BeanProperty var pwat_max:String = _
  @BeanProperty var spls_mean:String = _
  @BeanProperty var wpot_max:String = _
  @BeanProperty var rainx:String = _
  @BeanProperty var adefi:String = _
  @BeanProperty var rainm:String = _
  @BeanProperty var wdef_max:String = _
  @BeanProperty var arid_mean:String = _
  @BeanProperty var rtimin:String = _
  @BeanProperty var wpot_mean:String = _
  @BeanProperty var srain2mp:String = _
  @BeanProperty var arid_min:String = _
  @BeanProperty var evapi:String = _
  @BeanProperty var pwat_mean:String = _
  @BeanProperty var megagi:String = _
  @BeanProperty var wpot_min:String = _
  @BeanProperty var vpd2min:String = _
  @BeanProperty var trnga:String = _
  @BeanProperty var raini:String = _
  @BeanProperty var wdef_min:String = _
  @BeanProperty var rh2mean:String = _
  @BeanProperty var maxtx:String = _
  @BeanProperty var minti:String = _
  @BeanProperty var mesogi:String = _
  @BeanProperty var vpd29_x:String = _
  @BeanProperty var vpd2max:String = _
  @BeanProperty var slrain0:String = _
  @BeanProperty var vpd29_i:String = _
  @BeanProperty var eaeo_max:String = _
  @BeanProperty var mintm:String = _
  @BeanProperty var vpd2mean:String = _
  @BeanProperty var trngx:String = _
  @BeanProperty var adefm:String = _
  @BeanProperty var tminabsi:String = _
  @BeanProperty var rhu215_x:String = _
  @BeanProperty var rh2max:String = _
  @BeanProperty var rhu215_i:String = _
  @BeanProperty var srain0mp:String = _
  @BeanProperty var rhu215_m:String = _
  @BeanProperty var evapm:String = _
  @BeanProperty var pwat_min:String = _
  @BeanProperty var rh2min:String = _
  @BeanProperty var srain1mp:String = _
  @BeanProperty var c4gi:String = _
  @BeanProperty var tmaxabsx:String = _
  @BeanProperty var rtxmin:String = _
  @BeanProperty var rprecmin:String = _
  @BeanProperty var eaeo_mean:String = _
  @BeanProperty var rtxmax:String = _
  @BeanProperty var vpd29_m:String = _
  @BeanProperty var slrain2:String = _
  @BeanProperty var rtimax:String = _
  @BeanProperty var tmaxabsm:String = _
  @BeanProperty var trngm:String = _
  @BeanProperty var trngi:String = _
  @BeanProperty var adefx:String = _
  @BeanProperty var slrain1:String = _
  @BeanProperty var arid_max:String = _
  @BeanProperty var srain2:String = _
  @BeanProperty var evapx:String = _
  @BeanProperty var radnx:String = _
  @BeanProperty var maxti:String = _
  @BeanProperty var radni:String = _
  @BeanProperty var eaeo_min:String = _
  @BeanProperty var srain0:String = _
  @BeanProperty var spls_max:String = _
  @BeanProperty var maxtm:String = _
  @BeanProperty var radnm:String = _
  @BeanProperty var tminabsm:String = _
  @BeanProperty var spls_min:String = _
  @BeanProperty var srain1:String = _
  @BeanProperty var rprecmax:String = _
  @BeanProperty var worldclim_bio_6:String = _
  @BeanProperty var worldclim_bio_17:String = _
  @BeanProperty var worldclim_bio_4:String = _
  @BeanProperty var worldclim_bio_8:String = _
  @BeanProperty var worldclim_bio_2:String = _
  @BeanProperty var worldclim_bio_19:String = _
  @BeanProperty var worldclim_bio_7:String = _
  @BeanProperty var worldclim_bio_12:String = _
  @BeanProperty var worldclim_bio_5:String = _
  @BeanProperty var worldclim_bio_3:String = _
  @BeanProperty var worldclim_bio_14:String = _
  @BeanProperty var worldclim_bio_10:String = _
  @BeanProperty var worldclim_bio_1:String = _
  @BeanProperty var worldclim_bio_15:String = _
  @BeanProperty var worldclim_bio_11:String = _
  @BeanProperty var worldclim_bio_16:String = _
  @BeanProperty var worldclim_bio_18:String = _
  @BeanProperty var worldclim_bio_13:String = _
  @BeanProperty var worldclim_bio_9:String = _
  @BeanProperty var mean_oxygen_cars2006:String = _
  @BeanProperty var mean_silicate_cars2006:String = _
  @BeanProperty var mean_nitrate_cars2006:String = _
  @BeanProperty var mean_phosphate_cars2006:String = _
  @BeanProperty var mean_salinity_cars2009a:String = _
  @BeanProperty var mean_temperature_cars2009a:String = _
  @BeanProperty var substrate_bdensity:String = _
  @BeanProperty var substrate_geollmeanage:String = _
  @BeanProperty var substrate_geolmeanage:String = _
  @BeanProperty var substrate_geollrngeage:String = _
  @BeanProperty var substrate_distanywater:String = _
  @BeanProperty var substrate_pmnln0:String = _
  @BeanProperty var substrate_solpawhc:String = _
  @BeanProperty var substrate_datasupt:String = _
  @BeanProperty var substrate_clay:String = _
  @BeanProperty var substrate_valleybottom:String = _
  @BeanProperty var substrate_soldepth:String = _
  @BeanProperty var substrate_pmnlconcn0:String = _
  @BeanProperty var substrate_wr_unr:String = _
  @BeanProperty var substrate_distcoast:String = _
  @BeanProperty var substrate_distnonpermw:String = _
  @BeanProperty var substrate_nmnlconcn0:String = _
  @BeanProperty var substrate_nmnln0:String = _
  @BeanProperty var substrate_erosional:String = _
  @BeanProperty var substrate_hpedality:String = _
  @BeanProperty var substrate_twi:String = _
  @BeanProperty var substrate_relief:String = _
  @BeanProperty var substrate_ptotn0:String = _
  @BeanProperty var substrate_calcrete:String = _
  @BeanProperty var substrate_distpermwat:String = _
  @BeanProperty var substrate_ntotn0:String = _
  @BeanProperty var substrate_gravity:String = _
  @BeanProperty var substrate_ksat:String = _
  @BeanProperty var substrate_mrrtf:String = _
  @BeanProperty var substrate_geolrngeage:String = _
  @BeanProperty var substrate_slope:String = _
  @BeanProperty var substrate_corg0:String = _
  @BeanProperty var substrate_fert:String = _
  @BeanProperty var substrate_ridgetopflat:String = _
  @BeanProperty var substrate_mrvbf:String = _
  @BeanProperty var substrate_pedality:String = _
  @BeanProperty var substrate_ks_err:String = _
  @BeanProperty var substrate_nutrients:String = _
  @BeanProperty var substrate_roughness:String = _
  @BeanProperty var substrate_coarse:String = _
  @BeanProperty var bath_topo_ausbath_09_v4:String = _
  @BeanProperty var wind_windsp9m:String = _
  @BeanProperty var wind_windspmean:String = _
  @BeanProperty var wind_windspmax:String = _
  @BeanProperty var wind_windsp9x:String = _
  @BeanProperty var wind_windsp15x:String = _
  @BeanProperty var wind_windrx:String = _
  @BeanProperty var wind_windspmin:String = _
  @BeanProperty var wind_windsp15i:String = _
  @BeanProperty var wind_windri:String = _
  @BeanProperty var wind_windsp9i:String = _
  @BeanProperty var wind_windsp15m:String = _
  @BeanProperty var wind_windrm:String = _
  @BeanProperty var bioclim_bio25:String = _
  @BeanProperty var bioclim_bio7:String = _
  @BeanProperty var bioclim_bio19:String = _
  @BeanProperty var bioclim_bio33:String = _
  @BeanProperty var bioclim_bio32:String = _
  @BeanProperty var bioclim_bio13:String = _
  @BeanProperty var bioclim_bio6:String = _
  @BeanProperty var bioclim_bio34:String = _
  @BeanProperty var bioclim_bio27:String = _
  @BeanProperty var bioclim_bio8:String = _
  @BeanProperty var bioclim_bio22:String = _
  @BeanProperty var bioclim_bio14:String = _
  @BeanProperty var bioclim_bio35:String = _
  @BeanProperty var bioclim_bio1:String = _
  @BeanProperty var bioclim_bio9:String = _
  @BeanProperty var bioclim_bio11:String = _
  @BeanProperty var bioclim_bio24:String = _
  @BeanProperty var bioclim_bio18:String = _
  @BeanProperty var bioclim_bio5:String = _
  @BeanProperty var bioclim_bio21:String = _
  @BeanProperty var bioclim_bio20:String = _
  @BeanProperty var bioclim_bio15:String = _
  @BeanProperty var bioclim_bio3:String = _
  @BeanProperty var bioclim_bio29:String = _
  @BeanProperty var bioclim_bio31:String = _
  @BeanProperty var bioclim_bio16:String = _
  @BeanProperty var bioclim_bio23:String = _
  @BeanProperty var bioclim_bio2:String = _
  @BeanProperty var bioclim_bio17:String = _
  @BeanProperty var bioclim_bio10:String = _
  @BeanProperty var bioclim_bio28:String = _
  @BeanProperty var bioclim_bio4:String = _
  @BeanProperty var bioclim_bio12:String = _
  @BeanProperty var bioclim_bio26:String = _
  @BeanProperty var bioclim_bio30:String = _
  @BeanProperty var ALA_SPATIAL_layer_occurrence_av_4:String = _
  @BeanProperty var ALA_SPATIAL_layer_species_av_4:String = _
  @BeanProperty var east_afa_final:String = _
//@BeanProperty var _DLCMv1_Class:String = _
//@BeanProperty var _trend_evi_min:String = _
//@BeanProperty var _trend_evi_mean:String = _
//@BeanProperty var _trend_evi_max:String = _
}

/**
 * POSO for holding location information for an occurrence.
 */
class Location extends Cloneable with Mappable{
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
  //custom additional fields for environment layers
  //TODO Should we put this in an environment model?
  @BeanProperty var mean_temperature_cars2009a_band1:String =_
  @BeanProperty var mean_oxygen_cars2006_band1:String =_
  @BeanProperty var bioclim_bio34:String =_
  @BeanProperty var bioclim_bio12:String =_
  @BeanProperty var bioclim_bio11:String =_
  @BeanProperty var environmentalLayers:EnvironmentalLayers =new EnvironmentalLayers
  @BeanProperty var contextualLayers:ContextualLayers = new ContextualLayers
  //fields that need be hidden from all public API
  @BeanProperty @JsonIgnore var originalDecimalLatitude:String =_
  @BeanProperty @JsonIgnore var originalDecimlaLongitude:String =_
  @BeanProperty @JsonIgnore var originalLocality:String =_

  override def toString = ToStringBuilder.reflectionToString(this)

  @JsonIgnore
  def getMap():Map[String,String]={
    val map =Map[String,String]("uuid "->uuid, "continent"->continent, "coordinatePrecision"->coordinatePrecision,
                                "coordinateUncertaintyInMeters"->coordinateUncertaintyInMeters, "country"->country, "countryCode"->countryCode,
                                "county"->county, "decimalLatitude"->decimalLatitude, "decimalLongitude"->decimalLongitude,
                                "footprintSpatialFit"->footprintSpatialFit, "footprintWKT"->footprintWKT, "geodeticDatum"->geodeticDatum,
                                "georeferencedBy"->georeferencedBy, "georeferenceProtocol"->georeferenceProtocol, "georeferenceRemarks"->georeferenceRemarks,
                                "georeferenceSources"->georeferenceSources, "georeferenceVerificationStatus"->georeferenceVerificationStatus,
                                "habitat"->habitat, "higherGeography"->higherGeography, "higherGeographyID"->higherGeographyID, "island"->island,
                                "islandGroup"->islandGroup, "locality"->locality, "locationAttributes"->locationAttributes, "locationID"->locationID,
                                "locationRemarks"->locationRemarks, "maximumDepthInMeters"->maximumDepthInMeters,
                                "maximumDistanceAboveSurfaceInMeters"->maximumDistanceAboveSurfaceInMeters, "maximumElevationInMeters"->maximumElevationInMeters,
                                "minimumDepthInMeters"->minimumDepthInMeters, "minimumDistanceAboveSurfaceInMeters"->minimumDistanceAboveSurfaceInMeters,
                                "minimumElevationInMeters"->minimumElevationInMeters, "pointRadiusSpatialFit"->pointRadiusSpatialFit,
                                "stateProvince"->stateProvince, "verbatimCoordinates"->verbatimCoordinates, "verbatimCoordinateSystem"->verbatimCoordinateSystem,
                                "verbatimDepth"->verbatimDepth, "verbatimElevation"->verbatimElevation, "verbatimLatitude"->verbatimLatitude,
                                "verbatimLocality"->verbatimLocality, "verbatimLongitude"->verbatimLongitude, "waterBody"->waterBody, 
                                "ibra"->ibra, "imcra"->imcra, "lga"->lga, "mean_temperature_cars2009a_band1"->mean_temperature_cars2009a_band1,
                                "mean_oxygen_cars2006_band1"->mean_oxygen_cars2006_band1, "bioclim_bio34"->bioclim_bio34,
                                "bioclim_bio12"->bioclim_bio12,"bioclim_bio11"->bioclim_bio11 )

    map.filter(i => i._2!= null)
  }

}
/**
 * An Occurrence Model that can be used to create an Index entry
 * The @Field annotations are used for the SOLR implementation
 * But I am assuming that the will not get in the way if we decide to use a 
 * different indexing process.
 */
class OccurrenceIndex extends Cloneable with Mappable {
  override def clone : OccurrenceIndex = super.clone.asInstanceOf[OccurrenceIndex]
  @BeanProperty @Field("id") var uuid:String =_
  @BeanProperty @Field("occurrence_id") var occurrenceID:String =_
  //processed values
  @BeanProperty @Field("data_hub_uid") var dataHubUid:String =_
  @BeanProperty @Field("data_hub") var dataHub:String =_
  @BeanProperty @Field("institution_uid") var institutionUid:String =_
  @BeanProperty @Field("institution_code") var raw_institutionCode:String =_
  @BeanProperty @Field("institution_name") var institutionName:String =_
  @BeanProperty @Field("collection_uid") var collectionUid:String =_
  @BeanProperty @Field("collection_code") var raw_collectionCode:String =_
  @BeanProperty @Field("collection_name") var collectionName:String =_
  @BeanProperty @Field("catalogue_number") var raw_catalogNumber:String =_
  @BeanProperty @Field("taxon_concept_lsid") var taxonConceptID:String =_
  @BeanProperty @Field("occurrence_date") var eventDate:java.util.Date =_
  @BeanProperty @Field("occurrence_year") var occurrenceYear:java.util.Date =_
  @BeanProperty @Field("taxon_name") var scientificName:String =_
  @BeanProperty @Field("common_name") var vernacularName:String =_
  @BeanProperty @Field("rank") var taxonRank:String =_
  @BeanProperty @Field("rank_id") var taxonRankID:java.lang.Integer =_
  @BeanProperty @Field("country_code") var raw_countryCode:String =_
  @BeanProperty @Field("kingdom") var kingdom:String =_
  @BeanProperty @Field("phylum") var phylum:String =_
  @BeanProperty @Field("class") var classs:String =_
  @BeanProperty @Field("order") var order:String =_
  @BeanProperty @Field("family") var family:String =_
  @BeanProperty @Field("genus") var genus:String =_
  @BeanProperty @Field("species") var species:String =_
  @BeanProperty @Field("state") var stateProvince:String =_
  @BeanProperty @Field("latitude") var decimalLatitude:java.lang.Double =_
  @BeanProperty @Field("longitude") var decimalLongitude:java.lang.Double =_
  @BeanProperty @Field("year") var year:String =_
  @BeanProperty @Field("month") var month:String =_
  @BeanProperty @Field("basis_of_record") var basisOfRecord:String =_
  @BeanProperty @Field("type_status") var typeStatus:String =_
  @BeanProperty @Field("location_remarks") var raw_locationRemarks:String =_
  @BeanProperty @Field("occurrence_remarks") var raw_occurrenceRemarks:String =_
  @BeanProperty @Field("lft") var left:java.lang.Integer =_
  @BeanProperty @Field("rgt") var right:java.lang.Integer =_
  @BeanProperty @Field("ibra") var ibra:String = _
  @BeanProperty @Field("imcra") var imcra:String = _
  @BeanProperty @Field("places") var lga:String = _
  @BeanProperty @Field("data_provider_uid") var dataProviderUid:String =_
  @BeanProperty @Field("data_provider") var dataProviderName:String =_
  @BeanProperty @Field("data_resource_uid") var dataResourceUid:String =_
  @BeanProperty @Field("data_resource") var dataResourceName:String =_
  @BeanProperty @Field("assertions") var assertions:Array[String] =_
  @BeanProperty @Field("user_assertions") var hasUserAssertions:String =_
  @BeanProperty @Field("species_group") var speciesGroups:Array[String] =_
  @BeanProperty @Field("image_url") var image:String = _
  @BeanProperty @Field("geospatial_kosher") var geospatialKosher:String =_
  @BeanProperty @Field("taxonomic_kosher") var taxonomicKosher:String =_
  @BeanProperty @Field("collector") var raw_recordedBy:String = _

  //environment
  @BeanProperty @Field("mean_temperature_cars2009a_band1_env") var mean_temperature_cars2009a_band1:java.lang.Double =_
  @BeanProperty @Field("mean_oxygen_cars2006_band1_env") var mean_oxygen_cars2006_band1:java.lang.Double =_
  @BeanProperty @Field("bioclim_bio34_env") var bioclim_bio34:java.lang.Double =_
  @BeanProperty @Field("bioclim_bio12_env") var bioclim_bio12:java.lang.Double =_
  @BeanProperty @Field("bioclim_bio11_env") var bioclim_bio11:java.lang.Double =_
  
  //extra raw record fields
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
  @BeanProperty @Field("names_and_lsid") var namesLsid:String =_
  @BeanProperty @Field("multimedia") var multimedia:String =_

  @JsonIgnore
  def getMap():Map[String,String]={
    
    val sdate = if(eventDate == null) null else DateFormatUtils.format(eventDate, "yyyy-MM-dd")

    val map =Map[String,String]("id"-> uuid, "occurrence_id"-> occurrenceID, "data_hub_uid"-> dataHubUid, "data_hub" -> dataHub,
                                "institution_uid"-> institutionUid, "institution_code"-> raw_institutionCode,
                                "institution_name"-> institutionName, "collection_uid"-> collectionUid,
                                "collection_code"-> raw_collectionCode, "collection_name"-> collectionName,
                                "catalogue_number"-> raw_catalogNumber, "taxon_concept_lsid"-> taxonConceptID,
                                "occurrence_date"-> sdate, "taxon_name"-> scientificName, "common_name"-> vernacularName,
                                "rank"-> taxonRank, "rank_id"-> taxonRankID, "country_code"-> raw_countryCode,
                                "kingdom"-> kingdom, "phylum"-> phylum, "class"-> classs, "order"-> order, "family"-> family,
                                "genus"-> genus, "species"-> species, "state"-> stateProvince, "latitude"-> decimalLatitude,
                                "longitude"-> decimalLongitude, "year"-> year, "month"-> month, "basis_of_record"-> basisOfRecord,
                                "type_status"-> typeStatus, "location_remarks"-> raw_locationRemarks, "occurrence_remarks"-> raw_occurrenceRemarks,
                                "lft"-> left, "rgt"-> right, "ibra"-> ibra, "imcra"-> imcra,
                                "places"-> lga, "data_provider_uid"-> dataProviderUid, "data_provider"-> dataProviderName,
                                "data_resource_uid"-> dataResourceUid, "data_resource"-> dataResourceName, "assertions"-> assertions,
                                "user_assertions"-> hasUserAssertions, "species_group"-> speciesGroups,
                                "image_url"-> image, "geospatial_kosher"-> geospatialKosher, "taxonomic_kosher"-> taxonomicKosher,
                                "raw_taxon_name"-> raw_scientificName, "raw_basis_of_record"-> raw_basisOfRecord,
                                "raw_type_status"-> raw_typeStatus, "raw_common_name"-> raw_vernacularName, "lat_long"-> latLong,
                                "point-1"-> point1, "point-0.1"-> point01, "point-0.01"-> point001, "point-0.001"-> point0001,
                                "point-0.0001"-> point00001, "names_and_lsid"-> namesLsid, "multimedia"-> multimedia, "collector"->raw_recordedBy,
                                "mean_temperature_cars2009a_band1_env"-> mean_temperature_cars2009a_band1,
                                "mean_oxygen_cars2006_band1_env"-> mean_oxygen_cars2006_band1, "bioclim_bio34_env"-> bioclim_bio34,
                                "bioclim_bio12_env"-> bioclim_bio12, "bioclim_bio11_env"->bioclim_bio11 )

    map.filter(i => i._2!= null)
  }
  implicit def int2String(in:java.lang.Integer):String={
    if(in == null) null else in.toString
  }
  implicit def double2String(in:java.lang.Double):String ={
    if(in == null) null else in.toString
  }
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
  @BeanProperty var sensitive:Array[SensitiveSpecies])
  extends Cloneable {
  def this() = this(null,null,null,null,null,null,null, null)
  override def clone : TaxonProfile = super.clone.asInstanceOf[TaxonProfile]
}

/**
 * Represents the full attribution for a record.
 */
class Attribution  (
  @BeanProperty var dataProviderUid:String,
  @BeanProperty var dataProviderName:String,
  @BeanProperty var dataResourceUid:String,
  @BeanProperty var dataResourceName:String,
  @BeanProperty var collectionUid:String,
  @BeanProperty var institutionUid:String,
  @BeanProperty var dataHubUid:String,
  @BeanProperty var dataHubName:String,
  @BeanProperty var institutionName:String,
  @BeanProperty var collectionName:String,
  @BeanProperty var taxonomicHints:Array[String])
  extends Cloneable with Mappable {
  def this() = this(null,null,null,null,null,null,null,null,null,null, null)
  override def clone : Attribution = super.clone.asInstanceOf[Attribution]
  override def toString = ToStringBuilder.reflectionToString(this)

    @JsonIgnore
  private var parsedHints:Map[String,Set[String]] = null
  /**
   * Parse the hints into a usable map with rank -> Set.
   */
  @JsonIgnore
  def retrieveParseHints : Map[String,Set[String]] = {
      if(parsedHints==null){
          if(taxonomicHints!=null){
            val rankSciNames = new HashMap[String,Set[String]]
            val pairs = taxonomicHints.toList.map(x=> x.split(":"))
            for(pair <- pairs){
              val values = rankSciNames.getOrElse(pair(0),Set())
              rankSciNames.put(pair(0), values + pair(1).trim.toLowerCase)
            }
            parsedHints = rankSciNames.toMap
          } else {
            parsedHints = Map[String,Set[String]]()
          }
      }
      parsedHints
  }

  @JsonIgnore
  def getMap():Map[String,String]={
    val map = Map[String,String](
        "dataProviderUid"->dataProviderUid, "dataProviderName"->dataProviderName,
            "dataResourceUid"->dataResourceUid, "dataResourceName"->dataResourceName,
            "collectionUid"->collectionUid, "institutionUid"->institutionUid, "dataHubUid"->dataHubUid, "dataHubName"->dataHubName,
            "institutionName"->institutionName , "collectionName"->collectionName, "taxonomicHints"->taxonomicHints)
    map.filter(i => i._2!= null)
  }
}

/**
 * Encapsulates a complete specimen or occurrence record.
 */
class FullRecord (
  @BeanProperty var uuid:String,
  @BeanProperty var occurrence:Occurrence,
  @BeanProperty var classification:Classification,
  @BeanProperty var location:Location,
  @BeanProperty var event:Event,
  @BeanProperty var attribution:Attribution,
  @BeanProperty var identification:Identification,
  @BeanProperty var measurement:Measurement,
  @BeanProperty var assertions:Array[String],
  @BeanProperty var geospatiallyKosher:Boolean = true,
  @BeanProperty var taxonomicallyKosher:Boolean = true,
  @BeanProperty var deleted:Boolean = false)
  extends Cloneable {
    
  var objectArray = Array(occurrence,classification,location,event,attribution,identification,measurement, location.environmentalLayers, location.contextualLayers)

  def reinitObjectArray()={
    objectArray = Array(occurrence,classification,location,event,attribution,identification,measurement, location.environmentalLayers, location.contextualLayers)
  }

  def this(uuid:String) = this(uuid,new Occurrence,new Classification,new Location,new Event,new Attribution,new Identification,
      new Measurement, Array())

  def this() = this(null,new Occurrence,new Classification,new Location,new Event,new Attribution,new Identification,
      new Measurement, Array())

  override def clone : FullRecord = new FullRecord(this.uuid,
      occurrence.clone,classification.clone,location.clone,event.clone,attribution.clone,
      identification.clone,measurement.clone,assertions.clone)

  /**
   * Equals implementation that compares the contents of all the contained POSOs
   */
  override def equals(that: Any) = that match {
        case other: FullRecord => {
            if(this.uuid != other.uuid) false
            else if(!EqualsBuilder.reflectionEquals(this.occurrence, other.occurrence)) false
            else if(!EqualsBuilder.reflectionEquals(this.classification, other.classification)) false
            else if(!EqualsBuilder.reflectionEquals(this.location, other.location)) false
            else if(!EqualsBuilder.reflectionEquals(this.event, other.event)) false
            else if(!EqualsBuilder.reflectionEquals(this.attribution, other.attribution, Array("taxonomicHints","parsedHints"))) {
                false
            }
            else if(!EqualsBuilder.reflectionEquals(this.measurement, other.measurement)) false
            else if(!EqualsBuilder.reflectionEquals(this.identification, other.identification)) false
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
  extends Cloneable with Comparable[AnyRef] {

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

  def apply(code:Int) = {
    val uuid = UUID.randomUUID.toString
    val errorCode = AssertionCodes.getByCode(code)
    if(errorCode.isEmpty){
        throw new Exception("Unrecognised code: "+ code)
    }
    new QualityAssertion(uuid,errorCode.get.name,errorCode.get.code,true,null,null,null,null,null)
  }

  def apply(errorCode:ErrorCode) = {
    val uuid = UUID.randomUUID.toString
    new QualityAssertion(uuid,errorCode.name,errorCode.code,true,null,null,null,null,null)
  }
  def apply(errorCode:ErrorCode,problemAsserted:Boolean) = {
    val uuid = UUID.randomUUID.toString
    new QualityAssertion(uuid,errorCode.name,errorCode.code,problemAsserted,null,null,null,null,null)
  }
  def apply(errorCode:ErrorCode,problemAsserted:Boolean,comment:String) = {
    val uuid = UUID.randomUUID.toString
    new QualityAssertion(uuid,errorCode.name,errorCode.code,problemAsserted,comment,null,null,null,null)
  }
  def apply(errorCode:ErrorCode,comment:String) = {
    val uuid = UUID.randomUUID.toString
    new QualityAssertion(uuid,errorCode.name,errorCode.code,true,comment,null,null,null,null)
  }
  def apply(assertionCode:Int,problemAsserted:Boolean,comment:String) = {
    val uuid = UUID.randomUUID.toString
    new QualityAssertion(uuid,null,assertionCode,problemAsserted,comment,null,null,null,null)
  }
}