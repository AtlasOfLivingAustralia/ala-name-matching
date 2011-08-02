package au.org.ala.biocache
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Performs some Location Processing tests
 */
@RunWith(classOf[JUnitRunner])
class ProcessLocationTests extends FunSuite{

  test("Sensitive Species Generalise"){
      val raw = new FullRecord
      var processed = new FullRecord
      processed.classification.setScientificName("Crex crex")
      processed.classification.setTaxonConceptID("urn:lsid:biodiversity.org.au:afd.taxon:2ef4ac9c-7dfb-4447-8431-e337355ac1ca")
      processed.classification.setTaxonRankID("7000")
      raw.location.decimalLatitude = "-35.21667"
      raw.location.decimalLongitude = "144.81060"
      LocationProcessor.process("test", raw, processed)
      expect("-35.2"){processed.location.decimalLatitude}
      expect("144.8"){processed.location.decimalLongitude}
  }
  
  test("Uncertainty in Precision"){
      val raw = new FullRecord
      var processed = new FullRecord
      raw.location.decimalLatitude = "-35.21667"
      raw.location.decimalLongitude = "144.81060"
      raw.location.coordinatePrecision ="100.66";
      val qas = LocationProcessor.process("test", raw, processed)
      println(processed.location.coordinateUncertaintyInMeters)
      println(qas(0))
      expect(25){qas(0).code}
      expect("100"){processed.location.coordinateUncertaintyInMeters}
  }
  
  test("Uncertainty in meter"){
      val raw = new FullRecord
      var processed = new FullRecord
      raw.location.decimalLatitude = "-35.21667"
      raw.location.decimalLongitude = "144.81060"
      raw.location.coordinateUncertaintyInMeters ="100 meters";
      val qas = LocationProcessor.process("test", raw, processed)
      println(processed.location.coordinateUncertaintyInMeters)
      expect(true){qas.isEmpty}
      expect("100"){processed.location.coordinateUncertaintyInMeters}
  }  
  
}
