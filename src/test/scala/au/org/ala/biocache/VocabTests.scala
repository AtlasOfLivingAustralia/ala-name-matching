package au.org.ala.biocache

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Test for vocabulary mapping and lookups.
 */
@RunWith(classOf[JUnitRunner])
class VocabTests extends FunSuite {

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
    expect(false){StateCentrePoints.coordinatesMatchCentre("QLD","12","12")}
    expect(true){StateCentrePoints.coordinatesMatchCentre("QLD","-20.9175738","142.7027956")}
    expect(true){StateCentrePoints.coordinatesMatchCentre("QLD","-20.917573","142.702795")}
    expect(true){StateCentrePoints.coordinatesMatchCentre("QLD","-20.917","142.702")}
  }
}