package au.org.ala.biocache.vocab

/**
 * Vocabulary matcher for type status values.
 */
object TypeStatus extends Vocab {
  val all = loadVocabFromFile("/typeStatus.txt")
}
