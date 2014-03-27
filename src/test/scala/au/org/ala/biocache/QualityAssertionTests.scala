package au.org.ala.biocache

import org.scalatest.FunSuite
import org.scale7.cassandra.pelops.{Pelops}
import org.junit.Ignore
import au.org.ala.biocache.dao.OccurrenceDAO
import au.org.ala.biocache.model.{QualityAssertion, FullRecord}
import au.org.ala.biocache.persistence.PersistenceManager
import au.org.ala.biocache.vocab.AssertionCodes

/**
 * This is an integration test - hence it needs a running cassandra DB to work.
 */
@Ignore
class QualityAssertionTests extends FunSuite {

  val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
  val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]

  test("Add and delete user systemAssertions"){

    val uuid = "test-uuid-qa-delete1"
    val fr = new FullRecord
    fr.uuid = uuid
    fr.rowKey = uuid
    occurrenceDAO.addRawOccurrenceBatch(Array(fr))

    val qa1 = QualityAssertion(AssertionCodes.COORDINATE_HABITAT_MISMATCH, true)
    qa1.userId = "user1"
    occurrenceDAO.addUserAssertion(uuid, qa1)

    val qa2 = QualityAssertion(AssertionCodes.COORDINATE_HABITAT_MISMATCH, false)
    qa2.userId = "user2"
    occurrenceDAO.addUserAssertion(uuid, qa2)

    expectResult(2){ occurrenceDAO.getUserAssertions(uuid).size }

    //run the delete
    occurrenceDAO.deleteUserAssertion(uuid, qa2.uuid)
    val userAssertions = occurrenceDAO.getUserAssertions(uuid)
    expectResult(1){ userAssertions.size }

    //retrieve the record and check assertion is set and kosher values
    val fullRecord = occurrenceDAO.getByUuid("test-uuid-qa-delete1").get
    val found = fullRecord.assertions.find(ass => { ass equals AssertionCodes.COORDINATE_HABITAT_MISMATCH.name  })
    expectResult(AssertionCodes.COORDINATE_HABITAT_MISMATCH.name){ found.get }
    expectResult(false){ fullRecord.geospatiallyKosher }

    //cleanup
    Pelops.shutdown
  }
}