package au.org.ala.biocache.vocab

/**
 * Matching of coordinates for centre points for states.
 * This is for detecting auto-generated coordinates at very low accuracy.
 */
object StateProvinceCentrePoints extends CentrePoints {
  val map = loadFromFile("/stateProvinceCentrePoints.txt")
  val vocab = StateProvinces
}
