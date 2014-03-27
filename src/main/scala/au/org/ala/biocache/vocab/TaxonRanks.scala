package au.org.ala.biocache.vocab

/**
 * Vocab of taxon ranks.
 */
object TaxonRanks extends Vocab {
  val all = loadVocabFromFile("/taxonRanks.txt")
}
