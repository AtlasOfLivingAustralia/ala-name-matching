package au.org.ala.biocache.processor

import au.org.ala.biocache.vocab.AssertionCodes

/**
 * Singleton that maintains the workflow of processors
 */
object Processors {

  def foreach(proc: Processor => Unit) = processorMap.values.foreach(proc)

  //need to preserve the ordering of the Processors so that the default values are populated first
  //also classification must be executed before location
  val processorMap = scala.collection.mutable.LinkedHashMap(
    "DEFAULT" -> new DefaultValuesProcessor,
    "IMAGE" -> new MiscellaneousProcessor,
    "OFFLINE" -> new OfflineTestProcessor,
    "ATTR" -> new AttributionProcessor,
    "CLASS" -> new ClassificationProcessor,
    "BOR" -> new BasisOfRecordProcessor,
    "EVENT" -> new EventProcessor,
    "LOC" -> new LocationProcessor,
    "TS" -> new TypeStatusProcessor
  )

  //TODO A better way to do this. Maybe need to group QA failures by issue type instead of phase.
  //Can't change until we are able to reprocess the complete set records.
  def getProcessorForError(code: Int): String = code match {
    case c if c == AssertionCodes.INFERRED_DUPLICATE_RECORD.code || c == AssertionCodes.DETECTED_OUTLIER.code || c == AssertionCodes.SPECIES_OUTSIDE_EXPERT_RANGE.code => "offline"
    case c if c >= AssertionCodes.geospatialBounds._1 && c < AssertionCodes.geospatialBounds._2 => "loc"
    case c if c >= AssertionCodes.taxonomicBounds._1 && c < AssertionCodes.taxonomicBounds._2 => "class"
    case c if c == AssertionCodes.MISSING_BASIS_OF_RECORD.code || c == AssertionCodes.BADLY_FORMED_BASIS_OF_RECORD.code => "bor"
    case c if c == AssertionCodes.UNRECOGNISED_TYPESTATUS.code => "type"
    case c if c == AssertionCodes.UNRECOGNISED_COLLECTIONCODE.code || c == AssertionCodes.UNRECOGNISED_INSTITUTIONCODE.code => "attr"
    case c if c == AssertionCodes.INVALID_IMAGE_URL.code => "image"
    case c if c >= AssertionCodes.temporalBounds._1 && c < AssertionCodes.temporalBounds._2 => "event"
    case _ => ""
  }
}
