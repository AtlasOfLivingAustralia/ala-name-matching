package au.org.ala.biocache.model

import scala.beans.BeanProperty
import au.org.ala.biocache.poso.POSO

/**
 * POSO for handling details of a classification associated with an occurrence.
 */
class Classification extends Cloneable with POSO {
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
  @BeanProperty var namePublishedInYear:String = _
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
  @BeanProperty var taxonomicIssue:Array[String] = _ //stores if no issue, questionableSpecies, conferSpecies or affinitySpecies
  @BeanProperty var nameParseType:String =_
}
