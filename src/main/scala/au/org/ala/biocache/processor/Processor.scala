package au.org.ala.biocache.processor

import au.org.ala.biocache.model.{QualityAssertion, FullRecord}

/**
 * Trait to be implemented by all processors.
 * This is a simple Command Pattern implementation.
 *
 * Processors are changed in order to support a chain of processors.
 */
trait Processor {

  /** Process the raw version of the record, updating the processed and returning an array of assertions */
  def process(uuid: String, raw: FullRecord, processed: FullRecord, lastProcessed: Option[FullRecord]=None): Array[QualityAssertion]

  /** Return the name of this processor (largely for logging purposes) */
  def getName: String
}