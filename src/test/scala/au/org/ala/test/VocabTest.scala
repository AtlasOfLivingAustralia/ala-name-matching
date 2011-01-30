package au.org.ala.test

import au.org.ala.biocache.HabitatMap
import org.scalatest.FunSuite

class VocabTest extends FunSuite {
  
  test("Expect non marine to match terrestrial"){
	  expect(true){HabitatMap.isCompatible("non-marine", "terrestrial").get}
  }

  test("Expect  marine to not match terrestrial"){
	  expect(false){HabitatMap.isCompatible("marine", "terrestrial").get}
  }
}
