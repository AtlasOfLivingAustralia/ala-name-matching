package au.org.ala.biocache.vocab

/**
 * Created by mar759 on 18/02/2014.
 */
object StateProvinceToCountry extends ValueMap {
  map = loadFromFile("/stateProvince2Countries.txt")
}
