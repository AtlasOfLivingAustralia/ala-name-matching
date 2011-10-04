package au.org.ala.biocache

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
//import org.junit.Ignore

/**
 * Performs some Location Processing tests
 */
@RunWith(classOf[JUnitRunner])
class ProcessLocationTest extends ConfigFunSuite {
  override def prepare() {
    //WS will automatically grab the location details that are necessary
    //Add conservation status
    val taxonProfile = new TaxonProfile
    taxonProfile.setGuid("urn:lsid:biodiversity.org.au:afd.taxon:3809b1ca-8b60-4fcb-acf5-ca4f1dc0e263")
    taxonProfile.setScientificName("Victaphanta compacta")
    taxonProfile.setConservation(Array(new ConservationSpecies("Victoria", "aus_states/Victoria", "Endangered", "Endangered")
      , new ConservationSpecies("Victoria", "aus_states/Victoria", null, "Listed under FFG Act")))
    TaxonProfileDAO.add(taxonProfile)

    val tp = new TaxonProfile
    tp.setGuid("urn:lsid:biodiversity.org.au:afd.taxon:aa745ff0-c776-4d0e-851d-369ba0e6f537")
    tp.setScientificName("Macropus rufus")
    tp.setHabitats(Array("Non-marine"))
    TaxonProfileDAO.add(tp);
    pm.put("-35.21667|144.81060", "loc", "stateProvince","New South Wales")
    pm.put("-35.2|144.8", "loc", "stateProvince","New South Wales")
  }

    
  test("Sensitive Species Generalise"){
      val raw = new FullRecord
      var processed = new FullRecord
      processed.classification.setScientificName("Crex crex")
      processed.classification.setTaxonConceptID("urn:lsid:biodiversity.org.au:afd.taxon:2ef4ac9c-7dfb-4447-8431-e337355ac1ca")
      processed.classification.setTaxonRankID("7000")
      raw.location.decimalLatitude = "-35.21667"
      raw.location.decimalLongitude = "144.81060"
      raw.location.locationRemarks="test remarks"
      raw.attribution.dataResourceUid = "dr359"
      raw.rowKey ="test"
      raw.event.day="21"
      raw.event.month="12"
      raw.event.year="2000"
      (new EventProcessor).process("test", raw, processed)
      (new LocationProcessor).process("test", raw, processed)
      expect("-35.2"){processed.location.decimalLatitude}
      expect("144.8"){processed.location.decimalLongitude}
      expect(true){processed.occurrence.dataGeneralizations != null && processed.occurrence.dataGeneralizations.length>0}
      expect(true){processed.event.day.isEmpty}
      expect("12"){processed.event.month}
      expect("2000"){processed.event.year}
      
      val stringValues = pm.get("test","occ","originalSensitiveValues");
      expect(true){!stringValues.isEmpty}
      
//      println(processed.occurrence.dataGeneralizations)
//      raw.location.decimalLatitude = "-35.21667"
//      raw.location.decimalLongitude = "144.81060"
//      processed.classification.scientificName="Calyptorhynchus banksii"
//      processed.classification.taxonConceptID = "urn:lsid:biodiversity.org.au:afd.taxon:638f5293-5842-4850-8dad-9f10c0e4dcbc"
//      (new LocationProcessor).process("test", raw, processed)
//      

  }

  test("Not Sensitive") {
    val raw = new FullRecord
    val processed = new FullRecord
    processed.classification.setScientificName("Cataxia maculata")
    processed.classification.setTaxonConceptID("urn:lsid:biodiversity.org.au:afd.taxon:41cc4a69-06d4-4591-9afe-7af431b7153c")
    processed.classification.setTaxonRankID("7000")
    raw.location.decimalLatitude = "-27.56"
    raw.location.decimalLongitude = "152.28"
    (new LocationProcessor).process("test", raw, processed)
    expect("-27.56") {
      processed.location.decimalLatitude
    }
    expect("152.28") {
      processed.location.decimalLongitude
    }
    expect(true) {
      processed.occurrence.dataGeneralizations == null
    }
  }

  test("Already Generalised") {
    val raw = new FullRecord
    var processed = new FullRecord
    processed.classification.setScientificName("Crex crex")
    processed.classification.setTaxonConceptID("urn:lsid:biodiversity.org.au:afd.taxon:2ef4ac9c-7dfb-4447-8431-e337355ac1ca")
    processed.classification.setTaxonRankID("7000")
    raw.location.decimalLatitude = "-35.2"
    raw.location.decimalLongitude = "144.8"
    (new LocationProcessor).process("test", raw, processed)
    expect("-35.2") {
      processed.location.decimalLatitude
    }
    expect("144.8") {
      processed.location.decimalLongitude
    }
    expect(true) {
      processed.occurrence.dataGeneralizations.startsWith("Location in NSW is already generalised")
    }
  }

  test("Uncertainty in Precision") {
    val raw = new FullRecord
    var processed = new FullRecord
    raw.location.decimalLatitude = "-35.21667"
    raw.location.decimalLongitude = "144.81060"
    raw.location.coordinatePrecision = "100.66";
    val qas = (new LocationProcessor).process("test", raw, processed)
    println(processed.location.coordinateUncertaintyInMeters)
    println(qas(0))
    expect(25) {
      qas(0).code
    }
    expect("100") {
      processed.location.coordinateUncertaintyInMeters
    }
  }

  test("Uncertainty in meter") {
    val raw = new FullRecord
    var processed = new FullRecord
    raw.location.decimalLatitude = "-35.21667"
    raw.location.decimalLongitude = "144.81060"
    raw.location.coordinateUncertaintyInMeters = "100 meters";
    val qas = (new LocationProcessor).process("test", raw, processed)
    println(processed.location.coordinateUncertaintyInMeters)
    expect(true) {
      qas.isEmpty
    }
    expect("100.0") {
      processed.location.coordinateUncertaintyInMeters
    }
  }

  test("Coordinates Out Of Range") {
    val raw = new FullRecord
    val processed = new FullRecord
    raw.location.decimalLatitude = "91"
    raw.location.decimalLongitude = "121"
    var qas = (new LocationProcessor).process("test", raw, processed)
    expect(5) {
      qas(0) code
    }

    raw.location.decimalLatitude = "-32"
    raw.location.decimalLongitude = "190"
    qas = (new LocationProcessor).process("test", raw, processed)
    expect(5) {
      qas(0) code
    }

    raw.location.decimalLatitude = "-32"
    raw.location.decimalLongitude = "120"
    qas = (new LocationProcessor).process("test", raw, processed)
    expect(true) {
      qas.isEmpty
    }

    raw.location.decimalLatitude = "-120"
    raw.location.decimalLongitude = "120"
    qas = (new LocationProcessor).process("test", raw, processed)
    expect(5) {
      qas(0) code
    }

    raw.location.decimalLatitude = "-32"
    raw.location.decimalLongitude = "-200"
    qas = (new LocationProcessor).process("test", raw, processed)
    expect(5) {
      qas(0) code
    }

  }

  test("Inverted Coordinates") {
    val raw = new FullRecord
    val processed = new FullRecord
    raw.location.decimalLatitude = "123.123"
    raw.location.decimalLongitude = "-34.29"
    val qas = (new LocationProcessor).process("test", raw, processed)
    expect(3) {
      qas(0).code
    }
    expect("-34.29") {
      processed.location.decimalLatitude
    }
    expect("123.123") {
      processed.location.decimalLongitude
    }

  }

  test("Latitude Negated") {
    val raw = new FullRecord
    val processed = new FullRecord
    raw.location.decimalLatitude = "35.23"
    raw.location.decimalLongitude = "149.099"
    raw.location.coordinateUncertaintyInMeters = "100"
    raw.location.country = "Australia"
    val qas = (new LocationProcessor).process("test", raw, processed)
    expect(true) {
      qas.size > 0
    }
    expect(true) {
      qas.map(q => q.code).contains(1)
    }
    expect("-35.23") {
      processed.location.decimalLatitude
    }
  }
  test("Longitude Negated") {
    val raw = new FullRecord
    val processed = new FullRecord
    raw.location.decimalLatitude = "-35.23"
    raw.location.decimalLongitude = "-149.099"
    raw.location.coordinateUncertaintyInMeters = "100"
    raw.location.country = "Australia"
    val qas = (new LocationProcessor).process("test", raw, processed)
    expect(true) {
      qas.size > 0
    }
    expect(true) {
      qas.map(q => q.code).contains(2)
    }
    expect("149.099") {
      processed.location.decimalLongitude
    }
  }

  test("Habitat Mismatch") {
    val raw = new FullRecord
    val processed = new FullRecord
    val locationProcessor = new LocationProcessor
    processed.classification.taxonConceptID = "urn:lsid:biodiversity.org.au:afd.taxon:aa745ff0-c776-4d0e-851d-369ba0e6f537"
    raw.location.decimalLatitude = "-40.857"
    raw.location.decimalLongitude = "145.52"
    raw.location.coordinateUncertaintyInMeters = "100"
    var qas = locationProcessor.process("test", raw, processed)
    expect(19) {
      qas(0).code
    }
    raw.location.decimalLatitude = "-23.73750"
    raw.location.decimalLongitude = "133.85720"
    qas = locationProcessor.process("test", raw, processed)
    expect(true) {
      qas.isEmpty
    }
  }
  test("zero coordinates") {
    val raw = new FullRecord
    var processed = new FullRecord
    raw.location.decimalLatitude = "0.0"
    raw.location.decimalLongitude = "0.0"
    raw.location.coordinateUncertaintyInMeters = "100"
    val qas = (new LocationProcessor).process("test", raw, processed)
    expect(4) {
      qas(0).code
    }
  }

  test("unknown country name") {
    val raw = new FullRecord
    var processed = new FullRecord
    raw.location.decimalLatitude = "-40.857"
    raw.location.decimalLongitude = "145.52"
    raw.location.coordinateUncertaintyInMeters = "100"
    raw.location.country = "dummy"
    val qas = (new LocationProcessor).process("test", raw, processed)
    expect(6) {
      qas(0).code
    }
  }

  test("stateProvince coordinate mismatch") {
    val raw = new FullRecord
    var processed = new FullRecord
    raw.location.decimalLatitude = "-31.2532183"
    raw.location.decimalLongitude = "146.921099"
    raw.location.coordinateUncertaintyInMeters = "100"
    raw.location.country = "Australia"
    raw.location.stateProvince = "Australian Capital Territory"
    val qas = (new LocationProcessor).process("test", raw, processed)
    expect(18) {
      qas(0).code
    }
  }

  test("coordinates center of stateprovince") {
    val raw = new FullRecord
    var processed = new FullRecord
    raw.location.decimalLatitude = "-31.2532183"
    raw.location.decimalLongitude = "146.921099"
    raw.location.coordinateUncertaintyInMeters = "100"
    raw.location.country = "Australia"
    raw.location.stateProvince = "New South Wales"
    val qas = (new LocationProcessor).process("test", raw, processed)
    expect(22) {
      qas(0).code
    }
  }

  test("uncertainty range mismatch") {
    val raw = new FullRecord
    var processed = new FullRecord
    raw.location.decimalLatitude = "-31.2532183"
    raw.location.decimalLongitude = "146.921099"
    raw.location.coordinateUncertaintyInMeters = "-1"
    val qas = (new LocationProcessor).process("test", raw, processed)
    expect(24) {
      qas(0).code
    }
  }

  test("uncertainty not specified") {
    val raw = new FullRecord
    var processed = new FullRecord
    raw.location.decimalLatitude = "-31.2532183"
    raw.location.decimalLongitude = "146.921099"
    val qas = (new LocationProcessor).process("test", raw, processed)
    expect(27) {
      qas(0).code
    }
  }
}
