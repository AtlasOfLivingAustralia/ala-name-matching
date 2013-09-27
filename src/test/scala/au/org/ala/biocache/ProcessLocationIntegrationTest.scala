package au.org.ala.biocache

import org.junit.Ignore

/**
 * Integration test the relies on data present in cassandra.
 */
@Ignore
class ProcessLocationIntegrationTest extends ConfigFunSuite {

  test("Conservation Status in Victoria") {
    val raw = new FullRecord
    val processed = new FullRecord
    raw.location.decimalLatitude = "-38.5"
    raw.location.decimalLongitude = "146.2"
    processed.classification.scientificName = "Victaphanta compacta"
    processed.classification.taxonConceptID = "urn:lsid:biodiversity.org.au:afd.taxon:3809b1ca-8b60-4fcb-acf5-ca4f1dc0e263"
    (new LocationProcessor).process("test", raw, processed)
    println(processed.occurrence.stateConservation)
    expectResult("Endangered,Endangered") {
      processed.occurrence.stateConservation
    }
  }
}