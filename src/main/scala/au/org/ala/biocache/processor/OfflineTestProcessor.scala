package au.org.ala.biocache.processor

import au.org.ala.biocache.Config
import au.org.ala.biocache.model.{QualityAssertion, FullRecord}
import au.org.ala.biocache.vocab.AssertionCodes

/**
 * A processor to ensure that the offline test that were performed get recorded correctly
 */
class OfflineTestProcessor extends Processor {

  def process(guid: String, raw: FullRecord, processed: FullRecord, lastProcessed: Option[FullRecord]=None): Array[QualityAssertion] = {

     if(lastProcessed.isDefined){
       //get the current system assertions
       val currentProcessed = lastProcessed.get
       val systemAssertions = Config.occurrenceDAO.getSystemAssertions(guid)
       val offlineAssertions = systemAssertions.filter(sa => AssertionCodes.offlineAssertionCodes.contains(AssertionCodes.getByCode(sa.code).getOrElse(AssertionCodes.GEOSPATIAL_ISSUE)) )
       processed.occurrence.outlierForLayers = currentProcessed.occurrence.outlierForLayers
       processed.occurrence.duplicationStatus = currentProcessed.occurrence.duplicationStatus
       processed.occurrence.duplicationType = currentProcessed.occurrence.duplicationType
       processed.occurrence.associatedOccurrences = currentProcessed.occurrence.associatedOccurrences
       processed.location.distanceOutsideExpertRange = currentProcessed.location.distanceOutsideExpertRange
       processed.queryAssertions = currentProcessed.queryAssertions
       offlineAssertions.toArray
     } else {
       //assume that the assertions were not tested
       Array()
     }
  }

  def getName = "offline"
}
