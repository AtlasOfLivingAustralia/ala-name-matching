package au.org.ala.biocache.model

import scala.beans.BeanProperty
import org.codehaus.jackson.annotate.JsonIgnore
import au.org.ala.biocache.poso.POSO

/**
 * Represents an occurrence record. These fields map directly on to
 * the latest darwin core terms, with a few additional fields.
 */
class Occurrence extends Cloneable with POSO {
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
  @BeanProperty var bibliographicCitation:String = _
  @BeanProperty var catalogNumber:String = _
  @BeanProperty var collectionCode:String = _
  @BeanProperty var collectionID:String = _
  @BeanProperty var dataGeneralizations:String = _		//used for sensitive data information
  @BeanProperty var datasetID:String = _
  @BeanProperty var datasetName:String = _
  @BeanProperty var disposition:String = _
  @BeanProperty var dynamicProperties:String = _
  @BeanProperty var establishmentMeans:String = _
  @BeanProperty var fieldNotes:String = _
  @BeanProperty var fieldNumber:String = _
  @BeanProperty var identifier:String = _
  @BeanProperty var individualCount:String = _
  @BeanProperty var individualID:String = _
  @BeanProperty var informationWithheld:String = _   //used for sensitive data information
  @BeanProperty var institutionCode:String = _
  @BeanProperty var institutionID:String = _
  @BeanProperty var language:String = _
  @BeanProperty var lifeStage:String = _
  @BeanProperty var modified:String = _
  @BeanProperty var occurrenceAttributes:String = _
  @BeanProperty var occurrenceDetails:String = _
  @BeanProperty var occurrenceRemarks:String = _
  @BeanProperty var occurrenceStatus:String = _
  @BeanProperty var otherCatalogNumbers:String = _
  @BeanProperty var ownerInstitutionCode:String = _
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
  @BeanProperty var samplingEffort:String = _
  @BeanProperty var sex:String = _
  @BeanProperty var source:String = _
  @BeanProperty var userId:String = _  //this is the ALA ID for the user
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
  //stores either U,R or D.  U - a unique record, R - a representative record in a group of duplicates, D - a tool record in a group
  // when null a value of "U" is assumed
  /*
    D has been split into categories:
    D1- tool belongs to the same data resource as the representative record.
    D2- tool belongs to a different data resource as the representative record
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

  @BeanProperty var photographer:String =_

   @JsonIgnore
  def getOriginalSensitiveValues():Map[String,String] = originalSensitiveValues
  def setOriginalSensitiveValues(originalSensitiveValues:Map[String,String])=this.originalSensitiveValues = originalSensitiveValues
}
