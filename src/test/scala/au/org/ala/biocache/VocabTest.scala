package au.org.ala.biocache

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Test for vocabulary mapping and lookups.
 */
@RunWith(classOf[JUnitRunner])
class VocabTest extends FunSuite {

  test("Basis of record matching"){
      expect("isotype"){ TypeStatus.matchTerm("isotype").get.canonical}
  }

  test("Basis of record matching with junk characters"){
	  expect("syntype"){ TypeStatus.matchTerm("Syntype ?").get.canonical}
  }

  test("Expect non marine to match terrestrial"){
	  expect(true){HabitatMap.isCompatible("non-marine", "terrestrial").get}
  }

  test("Expect  marine to not match terrestrial"){
	  expect(false){HabitatMap.isCompatible("marine", "terrestrial").get}
  }

  test("Expect coordinates for QLD centre to match"){
    expect(false){StateProvinceCentrePoints.coordinatesMatchCentre("QLD","12","12")}
    expect(true){StateProvinceCentrePoints.coordinatesMatchCentre("QLD","-20.9175738","142.7027956")}
    expect(true){StateProvinceCentrePoints.coordinatesMatchCentre("QLD","-20.917573","142.702795")}
    expect(true){StateProvinceCentrePoints.coordinatesMatchCentre("QLD","-20.917","142.702")}
  }

  test("Expect coordinates for Australia centre to match"){
    expect(false){CountryCentrePoints.coordinatesMatchCentre("Australia","12","12")}
    expect(false){CountryCentrePoints.coordinatesMatchCentre("   ","12","12")}
    expect(true){CountryCentrePoints.coordinatesMatchCentre("Australia","-29.5328037","145.491477")}
    expect(true){CountryCentrePoints.coordinatesMatchCentre("Australia","-29.53280","145.4914")}
    expect(true){CountryCentrePoints.coordinatesMatchCentre("Australia","-29.532","145.491")}
  }

  test("Paratypes - case insensitive for types"){
      expect(false){ TypeStatus.matchTerm("Paratype").isEmpty}
  }
  
  test("Paratypes - plurals for types"){
      expect(false){ TypeStatus.matchTerm("Paratypes").isEmpty}
  }
  
  test("Observations - plurals for BOR"){
      expect(false){ BasisOfRecord.matchTerm("Observation").isEmpty}
  }

  test("Test Australia hemispheres"){
    expect(Set('S','E','W')){CountryCentrePoints.getHemispheres("Australia").get}
  }

  test("Match Mongolia"){
    expect("mongolia"){Countries.matchTerm("Mongolia").get.canonical.toLowerCase}
  }

  test("Match UK"){
    expect("united kingdom"){Countries.matchTerm("United Kingdom").get.canonical.toLowerCase}
  }

  test("Test UK hemispheres"){
    expect(Set('E','W', 'N')){CountryCentrePoints.getHemispheres("United Kingdom").get}
  }
}