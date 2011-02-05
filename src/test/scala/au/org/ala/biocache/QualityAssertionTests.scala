package au.org.ala.biocache

import org.scalatest.FunSuite
import org.apache.cassandra.thrift.ConsistencyLevel
import org.wyki.cassandra.pelops.{Policy, Mutator, Pelops}

class QualityAssertionTests extends FunSuite {

  test("Add and delete user assertions"){

    val uuid = "test-uuid-qa-delete1"
    try {
        Pelops.addPool("test", Array("localhost"), 9160, false, "occ", new Policy)
        val mutator = Pelops.createMutator("test","occ")
        mutator.deleteColumns(uuid, "occ", "userQualityAssertion","qualityAssertion")
        mutator.execute(ConsistencyLevel.ONE)
    } catch {
        case e: Exception => e.printStackTrace
    }

    val qa1 = QualityAssertion(AssertionCodes.COORDINATE_HABITAT_MISMATCH, false)
    OccurrenceDAO.addUserQualityAssertion(uuid, qa1)

    val qa2 = QualityAssertion(AssertionCodes.COORDINATE_HABITAT_MISMATCH, true)
    OccurrenceDAO.addUserQualityAssertion(uuid, qa2)

    expect(2){
        val userAssertions = OccurrenceDAO.getUserQualityAssertions(uuid)
        userAssertions.size
    }

    //run the delete
    OccurrenceDAO.deleteUserQualityAssertion(uuid, qa2.uuid)

    expect(1){
        val userAssertions = OccurrenceDAO.getUserQualityAssertions(uuid)
        userAssertions.size
    }

    Pelops.shutdown
  }
}