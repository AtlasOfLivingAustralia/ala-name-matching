package au.org.ala.biocache.processor

import au.org.ala.biocache.model.{QualityAssertion, FullRecord}
import au.org.ala.biocache.vocab.{TypeStatus, AssertionCodes}

/**
 * Process type status information
 */
class TypeStatusProcessor extends Processor {
  /**
   * Process the type status
   */
  def process(guid: String, raw: FullRecord, processed: FullRecord,lastProcessed: Option[FullRecord]=None): Array[QualityAssertion] = {

    if (raw.identification.typeStatus != null && !raw.identification.typeStatus.isEmpty) {
      val term = TypeStatus.matchTerm(raw.identification.typeStatus)
      if (term.isEmpty) {
        //add a quality assertion
        Array(QualityAssertion(AssertionCodes.UNRECOGNISED_TYPESTATUS, "Unrecognised type status"))
      } else {
        processed.identification.typeStatus = term.get.canonical
        Array(QualityAssertion(AssertionCodes.UNRECOGNISED_TYPESTATUS,1))
      }
    } else {
      Array()
    }
  }

  def getName = "type"
}
