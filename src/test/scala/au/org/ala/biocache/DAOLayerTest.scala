package au.org.ala.biocache
import org.scalatest.FunSuite
import org.junit.Ignore
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class DAOLayerTest extends ConfigFunSuite{ 
  val occurrenceDAO = Config.occurrenceDAO
  val persistenceManager = Config.persistenceManager
  val rowKey ="test-rowKey"
  val uuid = "35b3ff3-test-uuid"
      
  test("Write and lookup occ record"){
     val record = new FullRecord(rowKey, uuid)
     record.classification.scientificName = "Test species"
     occurrenceDAO.updateOccurrence(rowKey, record, Versions.RAW)
     val newrecord = occurrenceDAO.getByRowKey(rowKey);
     //val newrecord = occurrenceDAO.getByUuid(uuid)
     
     expect(rowKey){newrecord.get.getRowKey}
     expect(uuid){newrecord.get.uuid}
  }
  
  test ("User Assertions addition and deletion"){
      val qa = QualityAssertion(AssertionCodes.GEOSPATIAL_ISSUE, true)
      qa.comment = "My comment"
      qa.userId = "Natasha.Carter@csiro.au"
      qa.userDisplayName = "Natasha Carter"
      occurrenceDAO.addUserAssertion(rowKey, qa)
      expect(1){
          val userAssertions = occurrenceDAO.getUserAssertions(rowKey)
          userAssertions.size
      }
      val qa2 = QualityAssertion(AssertionCodes.GEOSPATIAL_ISSUE, false)
      qa2.comment = "My comment"
      qa2.userId = "Natasha.Carter@csiro.au"
      qa2.userDisplayName = "Natasha Carter"
      occurrenceDAO.addUserAssertion(rowKey, qa2)
      expect(2){
          val userAssertions = occurrenceDAO.getUserAssertions(rowKey)
          userAssertions.size
      }
      occurrenceDAO.deleteUserAssertion(rowKey, qa2.uuid)
      expect(1){
          val userAssertions = occurrenceDAO.getUserAssertions(rowKey)
          userAssertions.size
      }
      
  }
}      
