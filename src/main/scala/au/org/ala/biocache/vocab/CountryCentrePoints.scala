package au.org.ala.biocache.vocab

/**
 * Created by mar759 on 18/02/2014.
 */
object CountryCentrePoints extends CentrePoints {
  val map = loadFromFile("/countryCentrePoints.txt")
  val vocab = Countries
}
