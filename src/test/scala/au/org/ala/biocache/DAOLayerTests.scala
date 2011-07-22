package au.org.ala.biocache
import org.scalatest.FunSuite
import org.junit.Ignore
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class DAOLayerTests extends FunSuite{
  val occurrenceDAO = Config.occurrenceDAO
  val persistenceManager = Config.persistenceManager
  val rowKey ="test-rowKey"
  val uuid = "35b3ff3-test-uuid"
  test("Write and lookup occ record"){
     
     val record = new FullRecord(rowKey, uuid)
     record.classification.scientificName = "Test species"
     occurrenceDAO.updateOccurrence(rowKey, record, Versions.RAW)
     val newrecord = occurrenceDAO.getByUuid(uuid)
     
     expect(rowKey){newrecord.get.getRowKey}
     expect(uuid){newrecord.get.uuid}

  }

  test("User Assertion addition and deletion"){
    val qa1 = QualityAssertion(AssertionCodes.COORDINATE_HABITAT_MISMATCH, true)
    occurrenceDAO.addUserAssertion(uuid, qa1)

    val qa2 = QualityAssertion(AssertionCodes.COORDINATE_HABITAT_MISMATCH, false)
    occurrenceDAO.addUserAssertion(uuid, qa2)

    expect(2){
        val userAssertions = occurrenceDAO.getUserAssertions(uuid)
        userAssertions.size
    }

    occurrenceDAO.deleteUserAssertion(uuid, qa2.uuid)
    val userAssertions = occurrenceDAO.getUserAssertions(uuid)
    expect(1){ userAssertions.size }
  }

  test("get occs"){
    val allrecords = occurrenceDAO.getAllVersionsByRowKey("dr105|HMAP|Barents & White Seas|239191")
    val uuidrecords = occurrenceDAO.getAllVersionsByUuid("3b13f451-ea24-4699-a531-a20a12b252e9")
    println(uuidrecords)
  }
}

@Ignore
class OldDAOLayerTests extends FunSuite {

    val occurrenceDAO = Config.occurrenceDAO
    val persistenceManager = Config.persistenceManager

    test("Test store and location lookup") {
        val point = LocationDAO.getByLatLon("-33.25", "135.85")
        if(!point.isEmpty){
            val (loc, el,cl) = point.get
            println(loc.ibra)
            println(loc.stateProvince)
        } else {
            println("No matching point")
        }
        persistenceManager.shutdown
    }

    test("Get by UUID") {
        val ot1 = occurrenceDAO.getByUuid("3480993d-b0b1-4089-9faf-30b4eab050ae", Raw)
        if(!ot1.isEmpty){
            val rawOccurrence = ot1.get.occurrence
            val rawClassification = ot1.get.classification
            println(">> The bean set scientific name: " + rawClassification.scientificName)
            println(">> The bean set class name: " + rawClassification.classs)
        } else {
            println("failed")
        }

        val ot2 = occurrenceDAO.getByUuid("3480993d-b0b1-4089-9faf-30b4eab050ae", Processed)
        if(!ot2.isEmpty){
            val o = ot1.get.occurrence
            val c = ot1.get.classification
            println(">> (processed) The bean set scientific name: " + c.scientificName)
            println(">> (processed) The bean set class name: " + c.classs)
        } else {
            println("failed")
        }

        val ot3 = occurrenceDAO.getByUuid("3480993d-b0b1-4089-9faf-30b4eab050ae", Consensus)
        if(!ot3.isEmpty){
            val o = ot1.get.occurrence
            val c = ot1.get.classification
            println(">> (consensus) The bean set scientific name: " + c.scientificName)
            println(">> (consensus) The bean set class name: " + c.classs)
        } else {
            println("failed")
        }

        var qa = QualityAssertion(AssertionCodes.ALTITUDE_IN_FEET)
        qa.comment = "My comment"
        qa.userId = "David.Martin@csiro.au"
        qa.userDisplayName = "Dave Martin"

        occurrenceDAO.addSystemAssertion("3480993d-b0b1-4089-9faf-30b4eab050ae",qa)
        occurrenceDAO.addSystemAssertion("3480993d-b0b1-4089-9faf-30b4eab050ae",qa)

        var qa2 = QualityAssertion(AssertionCodes.COORDINATES_OUT_OF_RANGE)
        qa2.comment = "My comment"
        qa2.userId = "David.Martin@csiro.au"
        qa2.userDisplayName = "Dave Martin"

        occurrenceDAO.addSystemAssertion("3480993d-b0b1-4089-9faf-30b4eab050ae",qa2 )

        persistenceManager.shutdown
    }
}