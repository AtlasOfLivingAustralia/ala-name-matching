package au.org.ala.biocache.vocab

/**
 * Vocabulary matcher for basis of record values.
 */
object BasisOfRecord extends Vocab {
  val all = loadVocabFromVerticalFile("/basisOfRecord.txt")
}
