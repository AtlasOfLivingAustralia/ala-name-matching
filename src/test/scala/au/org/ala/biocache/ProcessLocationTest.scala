package au.org.ala.biocache

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll

//import org.junit.Ignore

/**
 * Performs some Location Processing tests
 */
@RunWith(classOf[JUnitRunner])
class ProcessLocationTest extends ConfigFunSuite with BeforeAndAfterAll {
// override def beforeAll() {
//    //WS will automatically grab the location details that are necessary
//    //Add conservation status
//    println("The PM before ProcessLocationTest: " + pm)
//    val taxonProfile = new TaxonProfile
//    taxonProfile.setGuid("urn:lsid:biodiversity.org.au:afd.taxon:3809b1ca-8b60-4fcb-acf5-ca4f1dc0e263")
//    taxonProfile.setScientificName("Victaphanta compacta")
//    taxonProfile.setConservation(Array(new ConservationSpecies("Victoria", "aus_states/Victoria", "Endangered", "Endangered")
//      , new ConservationSpecies("Victoria", "aus_states/Victoria", null, "Listed under FFG Act")))
//    TaxonProfileDAO.add(taxonProfile)
//
//    val tp = new TaxonProfile
//    tp.setGuid("urn:lsid:biodiversity.org.au:afd.taxon:aa745ff0-c776-4d0e-851d-369ba0e6f537")
//    tp.setScientificName("Macropus rufus")
//    tp.setHabitats(Array("Non-marine"))
//    TaxonProfileDAO.add(tp);
//
//    pm.put("-35.21667|144.81060", "loc", "stateProvince", "New South Wales")
//    pm.put("-35.21667|144.81060", "loc", "cl927", "New South Wales")
//    pm.put("-35.21667|144.8106", "loc", "cl927", "New South Wales")
//    pm.put("-35.2|144.8", "loc", "stateProvince", "New South Wales")
//    pm.put("-35.2|144.8", "loc", "cl927", "New South Wales")
//    pm.put("-40.857|145.52","loc","cl21","onwater")
//    pm.put("-23.73750|133.85720","loc","cl20","onland")
//    
//    pm.put("-31.2532183|146.921099","loc","cl927","New South Wales")
//    pm.put("-31.253218|146.9211","loc","cl927","New South Wales")//NC 20130515: There is an issue where by our location cache converst toFLoat and loses accurancy.  This is the point above after going through the system
//    
//    
//    println("THE pm after prepare: " +pm)
//  }

  test("State based sensitivity") {
    val raw = new FullRecord
    var processed = new FullRecord
    raw.classification.scientificName = "Diuris disposita"
    processed.classification.setScientificName("Diuris disposita")
    processed.classification.setTaxonConceptID("urn:lsid:biodiversity.org.au:apni.taxon:167966")
    processed.classification.setTaxonRankID("7000")
    raw.location.stateProvince = "NSW"
    raw.location.locality = "My test locality"
    (new LocationProcessor).process("test", raw, processed)
    println(processed.toMap)
    expect(true) {
      processed.occurrence.dataGeneralizations.length() > 0
    }
  }

  test("Sensitive Species Generalise") {
    val raw = new FullRecord
    var processed = new FullRecord
    processed.classification.setScientificName("Crex crex")
    processed.classification.setTaxonConceptID("urn:lsid:biodiversity.org.au:afd.taxon:2ef4ac9c-7dfb-4447-8431-e337355ac1ca")
    processed.classification.setTaxonRankID("7000")
    raw.location.decimalLatitude = "-35.21667"
    raw.location.decimalLongitude = "144.81060"
    raw.location.locationRemarks = "test remarks"
    raw.attribution.dataResourceUid = "dr359"
    raw.rowKey = "test"
    raw.event.day = "21"
    raw.event.month = "12"
    raw.event.year = "2000"
    (new EventProcessor).process("test", raw, processed)
    (new LocationProcessor).process("test", raw, processed)
    expect("-35.2") {
      processed.location.decimalLatitude
    }
    expect("144.8") {
      processed.location.decimalLongitude
    }
    expect(true) {
      processed.occurrence.dataGeneralizations != null && processed.occurrence.dataGeneralizations.length > 0
    }
    expect(true) {
      processed.event.day.isEmpty
    }
    expect("12") {
      processed.event.month
    }
    expect("2000") {
      processed.event.year
    }

    val stringValues = Config.persistenceManager.get("test", "occ", "originalSensitiveValues");    
    expect(true) {
      !stringValues.isEmpty
    }

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
    raw.attribution.dataResourceUid = "dr359"
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
    raw.attribution.dataResourceUid = "dr359"
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
    expect(true) {
     qas.find(_.code == 25) != None
      //qas(0).code
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
    expect(None) {
      qas.find(_.code == 27)
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
    raw.location.coordinateUncertaintyInMeters = "1000"
    var qas = (new LocationProcessor).process("test", raw, processed)

    expect(true) {
      qas.find(_.code == 5) != None
    }

    raw.location.decimalLatitude = "-32"
    raw.location.decimalLongitude = "190"
    qas = (new LocationProcessor).process("test", raw, processed)
    expect(true) {
      qas.find(_.code == 5) != None
    }

    raw.location.decimalLatitude = "-32"
    raw.location.decimalLongitude = "120"
    qas = (new LocationProcessor).process("test", raw, processed)
    expect(None) {
      qas.find(_.code == 5)
    }

    raw.location.decimalLatitude = "-120"
    raw.location.decimalLongitude = "120"
    qas = (new LocationProcessor).process("test", raw, processed)
    expect(true) {
      qas.find(_.code == 5) != None
    }

    raw.location.decimalLatitude = "-32"
    raw.location.decimalLongitude = "-200"
    qas = (new LocationProcessor).process("test", raw, processed)
    expect(true) {
      qas.find(_.code == 5) != None
    }

  }

  test("Inverted Coordinates") {
    val raw = new FullRecord
    val processed = new FullRecord
    raw.location.decimalLatitude = "123.123"
    raw.location.decimalLongitude = "-34.29"
    val qas = (new LocationProcessor).process("test", raw, processed)
    expect(true) {
      qas.find(_.code == 3) != None
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
    //qas.foreach(qa=>println( "HABITATAT: " +qa.code))
    //println(pm)
    expect(true) {
      qas.find(_.code == 19) != None
    }
    raw.location.decimalLatitude = "-23.73750"
    raw.location.decimalLongitude = "133.85720"
    qas = locationProcessor.process("test", raw, processed)
    expect(None) {
      qas.find(_.code == 19)
    }
  }
  test("zero coordinates") {
    val raw = new FullRecord
    var processed = new FullRecord
    raw.location.decimalLatitude = "0.0"
    raw.location.decimalLongitude = "0.0"
    raw.location.coordinateUncertaintyInMeters = "100"
    val qas = (new LocationProcessor).process("test", raw, processed)
    expect(true) {
      qas.find(_.code == 4) != None
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
    expect(true) {
      qas.find(_.code == 6) != None
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
    expect(true) {
      qas.find(_.code == 18) != None
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
    expect(true) {
      qas.find(_.code == 22) != None
    }
  }

  test("uncertainty range mismatch") {
    val raw = new FullRecord
    var processed = new FullRecord
    raw.location.decimalLatitude = "-31.2532183"
    raw.location.decimalLongitude = "146.921099"
    raw.location.coordinateUncertaintyInMeters = "-1"
    val qas = (new LocationProcessor).process("test", raw, processed)
    expect(true) {
      qas.find(_.code == 24) != None
    }
  }

  test("uncertainty not specified") {
    val raw = new FullRecord
    var processed = new FullRecord
    raw.location.decimalLatitude = "-31.2532183"
    raw.location.decimalLongitude = "146.921099"
    val qas = (new LocationProcessor).process("test", raw, processed)
    expect(true) {
      qas.find(_.code == 27) != None
    }
  }

  test("non numeric depth") {
    val raw = new FullRecord
    var processed = new FullRecord
    raw.location.verbatimDepth = "test"
    val qas = (new LocationProcessor).process("test", raw, processed)
    expect(15) {
      qas(0).code
    }
  }

  test("non numeric altitude") {
    val raw = new FullRecord
    var processed = new FullRecord
    raw.location.verbatimElevation = "test"
    val qas = (new LocationProcessor).process("test", raw, processed)
    expect(14) {
      qas(0).code
    }
  }

  test("depth out of range") {
    val raw = new FullRecord
    var processed = new FullRecord
    raw.location.verbatimDepth = "20000"
    var qas = (new LocationProcessor).process("test", raw, processed)
    expect(11) {
      qas(0).code
    }
    raw.location.verbatimDepth = "200"
    qas = (new LocationProcessor).process("test", raw, processed)
    expect(0) {
      qas.size
    }
  }

  test("altitude out of range") {
    val raw = new FullRecord
    var processed = new FullRecord
    raw.location.verbatimElevation = "20000"
    var qas = (new LocationProcessor).process("test", raw, processed)
    expect(7) {
      qas(0).code
    }
    raw.location.verbatimElevation = "-200"
    qas = (new LocationProcessor).process("test", raw, processed)
    expect(7) {
      qas(0).code
    }
    raw.location.verbatimElevation = "100"
    qas = (new LocationProcessor).process("test", raw, processed)
    expect(0) {
      qas.size
    }
  }

  test("transposed min and max") {
    val raw = new FullRecord
    var processed = new FullRecord
    raw.location.minimumDepthInMeters = "20"
    raw.location.maximumDepthInMeters = "10"
    var qas = (new LocationProcessor).process("test", raw, processed)
    expect(12) {
      qas(0).code
    }
    raw.location.maximumDepthInMeters = "100"
    raw.location.minimumElevationInMeters = "100"
    raw.location.maximumElevationInMeters = "20"
    qas = (new LocationProcessor).process("test", raw, processed)
    expect(9) {
      qas(0).code
    }
    raw.location.maximumElevationInMeters = "test"
    qas = (new LocationProcessor).process("test", raw, processed)
    expect(0) {
      qas.size
    }
  }

  test("Calculate lat/long from easting and northing") {
    val raw = new FullRecord
    var processed = new FullRecord
    processed.classification.setScientificName("Crex crex")
    processed.classification.setTaxonConceptID("urn:lsid:biodiversity.org.au:afd.taxon:2ef4ac9c-7dfb-4447-8431-e337355ac1ca")
    processed.classification.setTaxonRankID("7000")
    raw.location.easting = "539514.0"
    raw.location.northing = "5362674.0"
    raw.location.zone = "55"
    //No verbatim SRS supplied, GDA94 should be assumed
    raw.attribution.dataResourceUid = "dr359"
    raw.rowKey = "test"
    val assertions = (new LocationProcessor).process("test", raw, processed)
    expect("-41.88688") {
      processed.location.decimalLatitude
    }
    expect("147.47628") {
      processed.location.decimalLongitude
    }
    expect(true) {
      assertions(0).getName == "decimalLatLongCalculatedFromEastingNorthing"
    }
    expect(false) {
      var coordinatesInverted = false
      for (qa <- assertions) {
        if (qa.getName == "invertedCoordinates") {
          coordinatesInverted = true
        }
      }
      coordinatesInverted
    }
  }

  test("Calculate lat/long from verbatim lat/long supplied in degrees, minutes, seconds") {
    val raw = new FullRecord
    var processed = new FullRecord
    processed.classification.setScientificName("Crex crex")
    processed.classification.setTaxonConceptID("urn:lsid:biodiversity.org.au:afd.taxon:2ef4ac9c-7dfb-4447-8431-e337355ac1ca")
    processed.classification.setTaxonRankID("7000")
    raw.location.verbatimLatitude = "22° 2' 56\" N"
    raw.location.verbatimLongitude = "92° 25' 11\" E"
    raw.location.locationRemarks = "test remarks"
    raw.rowKey = "test"
    val assertions = (new LocationProcessor).process("test", raw, processed)
    expect("22.04889") {
      processed.location.decimalLatitude
    }
    expect("92.41972") {
      processed.location.decimalLongitude
    }
    //WGS 84 should be assumed
    expect("EPSG:4326") {
      processed.location.geodeticDatum
    }
    expect(true) {
      assertions(0).getName == "decimalLatLongCalculatedFromVerbatim"
    }
  }

  test("Reproject decimal lat/long from AGD66 to WGS84") {
    val raw = new FullRecord
    var processed = new FullRecord
    processed.classification.setScientificName("Crex crex")
    processed.classification.setTaxonConceptID("urn:lsid:biodiversity.org.au:afd.taxon:2ef4ac9c-7dfb-4447-8431-e337355ac1ca")
    processed.classification.setTaxonRankID("7000")
    raw.location.decimalLatitude = "-35.126"
    raw.location.decimalLongitude = "150.681"
    raw.location.geodeticDatum = "EPSG:4202"
    raw.rowKey = "test"
    val assertions = (new LocationProcessor).process("test", raw, processed)
    expect("-35.125") {
      processed.location.decimalLatitude
    }
    expect("150.682") {
      processed.location.decimalLongitude
    }
    expect("EPSG:4326") {
      processed.location.geodeticDatum
    }
    expect(true) {
      assertions(0).getName == "decimalLatLongConverted"
    }
    expect(false) {
      var coordinatesInverted = false
      for (qa <- assertions) {
        if (qa.getName == "invertedCoordinates") {
          coordinatesInverted = true
        }
      }
      coordinatesInverted
    }
  }

  test("Calculate decimal latitude/longitude by reprojecting verbatim latitude/longitude") {
    val raw = new FullRecord
    var processed = new FullRecord
    processed.classification.setScientificName("Crex crex")
    processed.classification.setTaxonConceptID("urn:lsid:biodiversity.org.au:afd.taxon:2ef4ac9c-7dfb-4447-8431-e337355ac1ca")
    processed.classification.setTaxonRankID("7000")
    raw.location.verbatimLatitude = "-35.126"
    raw.location.verbatimLongitude = "150.681"
    raw.location.verbatimSRS = "EPSG:4202"
    raw.rowKey = "test"
    val assertions = (new LocationProcessor).process("test", raw, processed)
    expect("-35.125") {
      processed.location.decimalLatitude
    }
    expect("150.682") {
      processed.location.decimalLongitude
    }
    expect("EPSG:4326") {
      processed.location.geodeticDatum
    }
    expect(true) {
      assertions(0).getName == "decimalLatLongCalculatedFromVerbatim"
    }
    expect(false) {
      var coordinatesInverted = false
      for (qa <- assertions) {
        if (qa.getName == "invertedCoordinates") {
          coordinatesInverted = true
        }
      }
      coordinatesInverted
    }
  }

  test("Assume WGS84 when no CRS supplied") {
    val raw = new FullRecord
    var processed = new FullRecord
    processed.classification.setScientificName("Crex crex")
    processed.classification.setTaxonConceptID("urn:lsid:biodiversity.org.au:afd.taxon:2ef4ac9c-7dfb-4447-8431-e337355ac1ca")
    processed.classification.setTaxonRankID("7000")
    raw.location.decimalLatitude = "-34.9666709899902"
    raw.location.decimalLongitude = "138.733337402344"
    raw.rowKey = "test"
    val assertions = (new LocationProcessor).process("test", raw, processed)
    expect("-34.9666709899902") {
      processed.location.decimalLatitude
    }
    expect("138.733337402344") {
      processed.location.decimalLongitude
    }
    expect("EPSG:4326") {
      processed.location.geodeticDatum
    }
    expect(true) {
      assertions(0).getName == "geodeticDatumAssumedWgs84"
    }
  }

  test("Convert verbatim lat/long in degrees then reproject to WGS84") {
    val raw = new FullRecord
    var processed = new FullRecord
    processed.classification.setScientificName("Crex crex")
    processed.classification.setTaxonConceptID("urn:lsid:biodiversity.org.au:afd.taxon:2ef4ac9c-7dfb-4447-8431-e337355ac1ca")
    processed.classification.setTaxonRankID("7000")
    raw.location.verbatimLatitude = "43°22'06\" S"
    raw.location.verbatimLongitude = "145°47'11\" E"
    raw.location.verbatimSRS = "EPSG:4202"
    raw.rowKey = "test"
    val assertions = (new LocationProcessor).process("test", raw, processed)
    expect("-43.36697") {
      processed.location.decimalLatitude
    }
    expect("145.78746") {
      processed.location.decimalLongitude
    }
    expect("EPSG:4326") {
      processed.location.geodeticDatum
    }
    expect(true) {
      assertions(0).getName == "decimalLatLongCalculatedFromVerbatim"
    }
  }

  test("Test recognition of AGD66 as geodeticDatum") {
    val raw = new FullRecord
    var processed = new FullRecord
    processed.classification.setScientificName("Crex crex")
    processed.classification.setTaxonConceptID("urn:lsid:biodiversity.org.au:afd.taxon:2ef4ac9c-7dfb-4447-8431-e337355ac1ca")
    processed.classification.setTaxonRankID("7000")
    raw.location.decimalLatitude = "-35.126"
    raw.location.decimalLongitude = "150.681"
    raw.location.geodeticDatum = "AGD66"
    raw.rowKey = "test"
    val assertions = (new LocationProcessor).process("test", raw, processed)
    expect("-35.125") {
      processed.location.decimalLatitude
    }
    expect("150.682") {
      processed.location.decimalLongitude
    }
    expect("EPSG:4326") {
      processed.location.geodeticDatum
    }
    expect(true) {
      assertions(0).getName == "decimalLatLongConverted"
    }
  }

  test("Test recognition of AGD66 as verbatimSRS") {
    val raw = new FullRecord
    var processed = new FullRecord
    processed.classification.setScientificName("Crex crex")
    processed.classification.setTaxonConceptID("urn:lsid:biodiversity.org.au:afd.taxon:2ef4ac9c-7dfb-4447-8431-e337355ac1ca")
    processed.classification.setTaxonRankID("7000")
    raw.location.verbatimLatitude = "-35.126"
    raw.location.verbatimLongitude = "150.681"
    raw.location.verbatimSRS = "AGD66"
    raw.rowKey = "test"
    val assertions = (new LocationProcessor).process("test", raw, processed)
    expect("-35.125") {
      processed.location.decimalLatitude
    }
    expect("150.682") {
      processed.location.decimalLongitude
    }
    expect("EPSG:4326") {
      processed.location.geodeticDatum
    }
    expect(true) {
      assertions(0).getName == "decimalLatLongCalculatedFromVerbatim"
    }
  }

  test("Test bad geodeticDatum") {
    val raw = new FullRecord
    var processed = new FullRecord
    processed.classification.setScientificName("Crex crex")
    processed.classification.setTaxonConceptID("urn:lsid:biodiversity.org.au:afd.taxon:2ef4ac9c-7dfb-4447-8431-e337355ac1ca")
    processed.classification.setTaxonRankID("7000")
    raw.location.decimalLatitude = "-35.126"
    raw.location.decimalLongitude = "150.681"
    raw.location.geodeticDatum = "FOO"
    raw.rowKey = "test"
    val assertions = (new LocationProcessor).process("test", raw, processed)
    expect("-35.126") {
      processed.location.decimalLatitude
    }
    expect("150.681") {
      processed.location.decimalLongitude
    }
    expect("FOO") {
      processed.location.geodeticDatum
    }

    expect(true) {
      assertions(0).getName == "unrecognizedGeodeticDatum"
    }
  }

  test("Test bad verbatimSRS") {
    val raw = new FullRecord
    var processed = new FullRecord
    processed.classification.setScientificName("Crex crex")
    processed.classification.setTaxonConceptID("urn:lsid:biodiversity.org.au:afd.taxon:2ef4ac9c-7dfb-4447-8431-e337355ac1ca")
    processed.classification.setTaxonRankID("7000")
    raw.location.verbatimLatitude = "-35.126"
    raw.location.verbatimLongitude = "150.681"
    raw.location.verbatimSRS = "FOO"
    raw.rowKey = "test"
    val assertions = (new LocationProcessor).process("test", raw, processed)
    expect(null) {
      processed.location.decimalLatitude
    }
    expect(null) {
      processed.location.decimalLongitude
    }
    expect(null) {
      processed.location.geodeticDatum
    }

    expect(true) {
      assertions(0).getName == "decimalLatLongCalculationFromVerbatimFailed"
    }
  }

}
