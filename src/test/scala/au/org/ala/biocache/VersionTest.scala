package au.org.ala.biocache

import org.scalatest.FunSuite
import org.junit.Ignore
import au.org.ala.biocache.dao.OccurrenceDAO
import au.org.ala.biocache.model._
import au.org.ala.biocache.persistence.PersistenceManager
import au.org.ala.biocache.vocab.AssertionCodes
import scala.Some

@Ignore
class VersionTest extends FunSuite {

  val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
  val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]

  test("Store and retrieval of all versions"){

    val rowKey = "version-test-rowKey"
    val uuid = "version-test-uuid"

    var raw = new FullRecord(rowKey,uuid)
    raw.classification.scientificName = "Raw version"
    var processed = new FullRecord(rowKey,uuid)
    processed.classification.scientificName = "Processed version"
    var consensus = new FullRecord(rowKey,uuid)
    consensus.classification.scientificName = "Consenus version"

    val assertions = Array(QualityAssertion(AssertionCodes.COORDINATES_OUT_OF_RANGE, true, "Coordinates bad"))

    occurrenceDAO.updateOccurrence(rowKey,raw,Raw)
    occurrenceDAO.updateOccurrence(rowKey,processed,Some(Map("loc"->assertions)),Processed)
    occurrenceDAO.updateOccurrence(rowKey,consensus,Consensus)

    //retrieve and test
    val r = occurrenceDAO.getAllVersionsByUuid(uuid)
    if(r.isEmpty) fail("Empty result")

    val array = r.get
    expectResult("Raw version"){array(0).classification.scientificName}
    expectResult("Processed version"){array(1).classification.scientificName}
    expectResult("Consenus version"){array(2).classification.scientificName}

    expectResult(1){array(0).assertions.length}
    expectResult(1){array(1).assertions.length}
    expectResult(1){array(2).assertions.length}

    persistenceManager.shutdown
  }
}