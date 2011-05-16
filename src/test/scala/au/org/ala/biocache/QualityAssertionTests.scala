package au.org.ala.biocache

import org.scalatest.FunSuite
import org.apache.cassandra.thrift.ConsistencyLevel
//import org.wyki.cassandra.pelops.{Policy, Mutator, Pelops}
import org.scale7.cassandra.pelops.{Pelops,Cluster}
import org.junit.Ignore

@Ignore
class QualityAssertionTests extends FunSuite {

  val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
  val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]

  test("Add and delete user systemAssertions"){

    val uuid = "test-uuid-qa-delete1"
    try {

        //FIXME to be removed - Cassandra specific!!!
        Pelops.addPool("test", new Cluster("localhost",9160), "test")
        val mutator = Pelops.createMutator("test")
        mutator.deleteColumns(uuid, "occ", "userQualityAssertion","qualityAssertion")
        mutator.execute(ConsistencyLevel.ONE)
    } catch {
        case e: Exception => e.printStackTrace
    }

    val qa1 = QualityAssertion(AssertionCodes.COORDINATE_HABITAT_MISMATCH, true)
    occurrenceDAO.addUserAssertion(uuid, qa1)

    val qa2 = QualityAssertion(AssertionCodes.COORDINATE_HABITAT_MISMATCH, false)
    occurrenceDAO.addUserAssertion(uuid, qa2)

    expect(2){
        val userAssertions = occurrenceDAO.getUserAssertions(uuid)
        userAssertions.size
    }

    //run the delete
    occurrenceDAO.deleteUserAssertion(uuid, qa2.uuid)
    val userAssertions = occurrenceDAO.getUserAssertions(uuid)
    expect(1){ userAssertions.size }

    //retrieve the record and check assertion is set and kosher values
    val fullRecord = occurrenceDAO.getByUuid("test-uuid-qa-delete1").get
    val found = fullRecord.assertions.find(ass => { ass equals AssertionCodes.COORDINATE_HABITAT_MISMATCH.name  })
    expect(AssertionCodes.COORDINATE_HABITAT_MISMATCH.name){ found.get }
    expect(false){ fullRecord.geospatiallyKosher }

    Pelops.shutdown
  }
}