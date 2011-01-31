package au.org.ala.biocache

import org.scalatest.FunSuite
import org.wyki.cassandra.pelops.Pelops

/**
 * Created by IntelliJ IDEA.
 * User: davejmartin2
 * Date: 31/01/2011
 * Time: 14:15
 * To change this template use File | Settings | File Templates.
 */

class VersionTests extends FunSuite {

  test("Store and retrieval of all versions"){

    val uuid = "version-test-uuid"

    var raw = new FullRecord
    raw.o.uuid = uuid
    raw.c.scientificName = "Raw version"
    var processed = new FullRecord
    processed.o.uuid = uuid
    processed.c.scientificName = "Processed version"
    var consensus = new FullRecord
    consensus.o.uuid = uuid
    consensus.c.scientificName = "Consenus version"

    val assertions = Array(QualityAssertion(AssertionCodes.GEOSPATIAL_COORDINATES_OUT_OF_RANGE, false, "Coordinates bad"))

    OccurrenceDAO.updateOccurrence(uuid,raw,Raw)
    OccurrenceDAO.updateOccurrence(uuid,processed,assertions,Processed)
    OccurrenceDAO.updateOccurrence(uuid,consensus,Consensus)

    //retrieve and test
    val r = OccurrenceDAO.getAllVersionsByUuid(uuid)
    if(r.isEmpty) fail("Empty result")

    val array = r.get
    expect("Raw version"){array(0).c.scientificName}
    expect("Processed version"){array(1).c.scientificName}
    expect("Consenus version"){array(2).c.scientificName}

    expect(1){array(0).assertions.length}
    expect(1){array(1).assertions.length}
    expect(1){array(2).assertions.length}

    Pelops.shutdown
  }
}