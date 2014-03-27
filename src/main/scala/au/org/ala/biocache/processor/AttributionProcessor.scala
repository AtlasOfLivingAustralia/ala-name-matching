package au.org.ala.biocache.processor

import org.slf4j.LoggerFactory
import scala.collection.mutable.ArrayBuffer
import au.org.ala.biocache.caches.AttributionDAO
import au.org.ala.biocache.model.{QualityAssertion, FullRecord}
import au.org.ala.biocache.vocab.AssertionCodes

/**
 * A processor of attribution information.
 */
class AttributionProcessor extends Processor {

  val logger = LoggerFactory.getLogger("AttributionProcessor")

  /**
   * Retrieve attribution infromation from collectory and tag the occurrence record.
   */
  def process(guid: String, raw: FullRecord, processed: FullRecord, lastProcessed: Option[FullRecord]=None): Array[QualityAssertion] = {
    var assertions = new ArrayBuffer[QualityAssertion]

    //get the data resource information to check if it has mapped collections
    if (raw.attribution.dataResourceUid != null) {
      val dataResource = AttributionDAO.getDataResourceByUid(raw.attribution.dataResourceUid)
      if (!dataResource.isEmpty) {
        //the processed collection code is catering for the situation where the collection code is provided as a default in the collectory
        if (dataResource.get.hasMappedCollections && (raw.occurrence.collectionCode != null || processed.occurrence.collectionCode != null)) {
          val collCode = if (raw.occurrence.collectionCode != null) raw.occurrence.collectionCode else processed.occurrence.collectionCode
          //use the collection code as the institution code when one does not exist
          val instCode = if (raw.occurrence.institutionCode != null) raw.occurrence.institutionCode else if (processed.occurrence.institutionCode != null) processed.occurrence.institutionCode else collCode
          val attribution = AttributionDAO.getByCodes(instCode, collCode)
          if (!attribution.isEmpty) {
            processed.attribution = attribution.get
            assertions ++= Array(QualityAssertion(AssertionCodes.UNRECOGNISED_COLLECTIONCODE,1))
            assertions ++= Array(QualityAssertion(AssertionCodes.UNRECOGNISED_INSTITUTIONCODE,1))
            //need to reinitialise the object array - DM switched to def, that
            //way objectArray created each time its accessed
            //processed.reinitObjectArray
          } else {
            assertions ++= Array(QualityAssertion(AssertionCodes.UNRECOGNISED_COLLECTIONCODE, "Unrecognised collection code institution code combination"))
            assertions ++= Array(QualityAssertion(AssertionCodes.UNRECOGNISED_INSTITUTIONCODE, "Unrecognised collection code institution code combination"))
          }
        }
        //update the details that come from the data resource
        processed.attribution.dataResourceName = dataResource.get.dataResourceName
        processed.attribution.dataProviderUid = dataResource.get.dataProviderUid
        processed.attribution.dataProviderName = dataResource.get.dataProviderName
        processed.attribution.dataHubUid = dataResource.get.dataHubUid
        processed.attribution.dataResourceUid = dataResource.get.dataResourceUid
        processed.attribution.provenance = dataResource.get.provenance
        //only add the taxonomic hints if they were not populated by the collection
        if (processed.attribution.taxonomicHints == null)
          processed.attribution.taxonomicHints = dataResource.get.taxonomicHints
      }
    }

    assertions.toArray
  }

  def getName = "attr"
}