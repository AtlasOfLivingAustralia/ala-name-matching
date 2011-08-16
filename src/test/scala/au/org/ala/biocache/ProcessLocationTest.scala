package au.org.ala.biocache
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Performs some Location Processing tests
 */
@RunWith(classOf[JUnitRunner])
class ProcessLocationTest extends FunSuite{

  test("Sensitive Species Generalise"){
      val raw = new FullRecord
      var processed = new FullRecord
      processed.classification.setScientificName("Crex crex")
      processed.classification.setTaxonConceptID("urn:lsid:biodiversity.org.au:afd.taxon:2ef4ac9c-7dfb-4447-8431-e337355ac1ca")
      processed.classification.setTaxonRankID("7000")
      raw.location.decimalLatitude = "-35.21667"
      raw.location.decimalLongitude = "144.81060"
      (new LocationProcessor).process("test", raw, processed)
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
      (new LocationProcessor).process("test", raw, processed)
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
      (new LocationProcessor).process("test", raw, processed)
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
      val qas = (new LocationProcessor).process("test", raw, processed)
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
      val qas = (new LocationProcessor).process("test", raw, processed)
      println(processed.location.coordinateUncertaintyInMeters)
      expect(true){qas.isEmpty}
      expect("100.0"){processed.location.coordinateUncertaintyInMeters}
  }  
  
  test("Conservation Status in Victoria"){
      val raw = new FullRecord
      val processed = new FullRecord
      raw.location.decimalLatitude = "-38.5"
      raw.location.decimalLongitude = "146.2"
      processed.classification.scientificName = "Victaphanta compacta"
      processed.classification.taxonConceptID = "urn:lsid:biodiversity.org.au:afd.taxon:3809b1ca-8b60-4fcb-acf5-ca4f1dc0e263"
      (new LocationProcessor).process("test", raw, processed)
      println(processed.occurrence.stateConservation)
      expect("Endangered,Endangered"){processed.occurrence.stateConservation}
  }
  
  test("Coordinates Out Of Range"){
      val raw = new FullRecord
      val processed = new FullRecord
      raw.location.decimalLatitude = "91"
      raw.location.decimalLongitude="121"
      var qas = (new LocationProcessor).process("test",raw,processed)      
      expect(5){qas(0)code}
      
      raw.location.decimalLatitude = "-32"
      raw.location.decimalLongitude = "190"
      qas = (new LocationProcessor).process("test",raw,processed)
      expect(5){qas(0)code}
      
      raw.location.decimalLatitude = "-32"
      raw.location.decimalLongitude = "120"
      qas = (new LocationProcessor).process("test",raw,processed)
      expect(true){qas.isEmpty}
      
      raw.location.decimalLatitude = "-120"
      raw.location.decimalLongitude = "120"
      qas = (new LocationProcessor).process("test",raw,processed)
      expect(5){qas(0)code}
      
      raw.location.decimalLatitude = "-32"
      raw.location.decimalLongitude = "-200"
      qas = (new LocationProcessor).process("test",raw,processed)
      expect(5){qas(0)code}
  }
  
  test("Inverted Coordinates"){
      val raw = new FullRecord
      val processed = new FullRecord
      raw.location.decimalLatitude= "123.123"
      raw.location.decimalLongitude = "-34.29"
      val qas = (new LocationProcessor).process("test", raw, processed)
      expect(3){qas(0).code}
      expect("-34.29"){processed.location.decimalLatitude}
      expect("123.123"){processed.location.decimalLongitude}
  }
  
  test("Latitude Negated"){
      val raw = new FullRecord
      val processed = new FullRecord
      raw.location.decimalLatitude="35.23"
      raw.location.decimalLongitude="149.099"
      raw.location.coordinateUncertaintyInMeters = "100"
      raw.location.country="Australia"
      val qas = (new LocationProcessor).process("test", raw, processed)
      expect(true){qas.size > 0 }
      expect(true){qas.map(q => q.code).contains(1)}
      expect("-35.23"){processed.location.decimalLatitude}
  }
    test("Longitude Negated"){
      val raw = new FullRecord
      val processed = new FullRecord
      raw.location.decimalLatitude="-35.23"
      raw.location.decimalLongitude="-149.099"
      raw.location.coordinateUncertaintyInMeters = "100"
      raw.location.country="Australia"
      val qas = (new LocationProcessor).process("test", raw, processed)
      expect(true){qas.size > 0 }
      expect(true){qas.map(q => q.code).contains(2)}
      expect("149.099"){processed.location.decimalLongitude}
  }
    
   test("Habitat Mismatch"){
       val raw = new FullRecord
       val processed = new FullRecord
       val locationProcessor = new LocationProcessor
       processed.classification.taxonConceptID = "urn:lsid:biodiversity.org.au:afd.taxon:aa745ff0-c776-4d0e-851d-369ba0e6f537"
       raw.location.decimalLatitude="-40.857"
       raw.location.decimalLongitude ="145.52"
       raw.location.coordinateUncertaintyInMeters = "100"
       var qas = locationProcessor.process("test", raw, processed)
       expect(19){qas(0).code}
       raw.location.decimalLatitude="-23.73750"
       raw.location.decimalLongitude="133.85720"
       qas = locationProcessor.process("test", raw, processed)
       expect(true){qas.isEmpty}
   }
   
    test("zero coordinates"){
      val raw = new FullRecord
      var processed = new FullRecord
      raw.location.decimalLatitude = "0.0"
      raw.location.decimalLongitude = "0.0"
      raw.location.coordinateUncertaintyInMeters = "100"
      val qas = (new LocationProcessor).process("test", raw, processed)
      expect(4){qas(0).code}
  }
   
   test("unknown country name"){
      val raw = new FullRecord
      var processed = new FullRecord
      raw.location.decimalLatitude="-40.857"
      raw.location.decimalLongitude ="145.52"
      raw.location.coordinateUncertaintyInMeters = "100"
      raw.location.country = "dummy"
      val qas = (new LocationProcessor).process("test", raw, processed)
      expect(6){qas(0).code}
  }
   
   test("stateProvince coordinate mismatch"){
      val raw = new FullRecord
      var processed = new FullRecord
      raw.location.decimalLatitude="-31.2532183"
      raw.location.decimalLongitude ="146.921099"
      raw.location.coordinateUncertaintyInMeters = "100"
      raw.location.country="Australia"
      raw.location.stateProvince="Australian Capital Territory"  
      val qas = (new LocationProcessor).process("test", raw, processed)
      expect(18){qas(0).code}
  }
   
   test("coordinates center of stateprovince"){
      val raw = new FullRecord
      var processed = new FullRecord
      raw.location.decimalLatitude="-31.2532183"
      raw.location.decimalLongitude ="146.921099"
      raw.location.coordinateUncertaintyInMeters = "100"
      raw.location.country="Australia"
      raw.location.stateProvince="New South Wales"  
      val qas = (new LocationProcessor).process("test", raw, processed)
      expect(22){qas(0).code}
  }
   
   test("uncertainty range mismatch"){
      val raw = new FullRecord
      var processed = new FullRecord
      raw.location.decimalLatitude="-31.2532183"
      raw.location.decimalLongitude ="146.921099"
      raw.location.coordinateUncertaintyInMeters = "-1"
      val qas = (new LocationProcessor).process("test", raw, processed)
      expect(24){qas(0).code}
  }
   
   test("uncertainty not speccified"){
      val raw = new FullRecord
      var processed = new FullRecord
      raw.location.decimalLatitude="-31.2532183"
      raw.location.decimalLongitude ="146.921099"
      val qas = (new LocationProcessor).process("test", raw, processed)
      expect(27){qas(0).code}
  }
}
