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
      expect(true){processed.occurrence.dataGeneralizations != null && processed.occurrence.dataGeneralizations.length>0}
      
  }
  
  test("Not Sensitive"){
      val raw = new FullRecord
      val processed = new FullRecord
      processed.classification.setScientificName("Cataxia maculata")
      processed.classification.setTaxonConceptID("urn:lsid:biodiversity.org.au:afd.taxon:41cc4a69-06d4-4591-9afe-7af431b7153c")
      processed.classification.setTaxonRankID("7000")
      raw.location.decimalLatitude = "-27.56"
      raw.location.decimalLongitude = "152.28"
      LocationProcessor.process("test", raw, processed)
      expect("-27.56"){processed.location.decimalLatitude}
      expect("152.28"){processed.location.decimalLongitude}
      expect(true){processed.occurrence.dataGeneralizations == null}
  }
  
  test("Already Generalised"){
      val raw = new FullRecord
      var processed = new FullRecord
      processed.classification.setScientificName("Crex crex")
      processed.classification.setTaxonConceptID("urn:lsid:biodiversity.org.au:afd.taxon:2ef4ac9c-7dfb-4447-8431-e337355ac1ca")
      processed.classification.setTaxonRankID("7000")
      raw.location.decimalLatitude = "-35.2"
      raw.location.decimalLongitude = "144.8"
      LocationProcessor.process("test", raw, processed)
      expect("-35.2"){processed.location.decimalLatitude}
      expect("144.8"){processed.location.decimalLongitude}
      expect("Location is already generalised"){processed.occurrence.dataGeneralizations}
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
  
  test("Conservation Status in Victoria"){
      val raw = new FullRecord
      val processed = new FullRecord
      raw.location.decimalLatitude = "-38.5"
      raw.location.decimalLongitude = "146.2"
      processed.classification.scientificName = "Victaphanta compacta"
      processed.classification.taxonConceptID = "urn:lsid:biodiversity.org.au:afd.taxon:3809b1ca-8b60-4fcb-acf5-ca4f1dc0e263"
      LocationProcessor.process("test", raw, processed)
      println(processed.occurrence.stateConservation)
      expect("Endangered,Endangered"){processed.occurrence.stateConservation}
  }
  
}
