package au.org.ala.biocache

import org.wyki.cassandra.pelops.Pelops
import org.scalatest.FunSuite

class QualityAssertionTests extends FunSuite {

  test("Store and retrieval of system assertions"){

    val qa1 = Array(QualityAssertion(AssertionCodes.COORDINATE_HABITAT_MISMATCH),QualityAssertion(AssertionCodes.ALTITUDE_IN_FEET))

    CassandraPersistenceManager.putArray("uuid-cassandra-pm-test","occ","qualityTest", qa1.asInstanceOf[Array[AnyRef]], true)

    val theClass = (Array(new QualityAssertion())).getClass.asInstanceOf[Class[AnyRef]]

    val assertions = CassandraPersistenceManager.getArray("uuid-cassandra-pm-test","occ","qualityTest", theClass)

    expect(2){assertions.size}

    val qa2 = Array(QualityAssertion(AssertionCodes.DEPTH_NON_NUMERIC),QualityAssertion(AssertionCodes.BADLY_FORMED_ALTITUDE))

    //add more
    CassandraPersistenceManager.putArray("uuid-cassandra-pm-test","occ","qualityTest", qa2.asInstanceOf[Array[AnyRef]], false)

    val assertions2 = CassandraPersistenceManager.getArray("uuid-cassandra-pm-test","occ","qualityTest", theClass)

    expect(4){assertions2.size}

    Pelops.shutdown
  }
}