package au.org.ala.test

import scala.actors.Actor
import scala.actors.Actor._
import au.org.ala.biocache.HabitatMap
import junit.framework.TestCase

class VocabTest extends TestCase {
  
  def testVocabs {
	println(HabitatMap.isCompatible("non-marine", "terrestrial"))
  }
  
}
