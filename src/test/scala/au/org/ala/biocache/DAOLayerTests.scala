package au.org.ala.biocache
import org.scalatest.FunSuite
import org.junit.Ignore

@Ignore
class DAOLayerTests extends FunSuite {

    val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
    val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]

    test("Test store and location lookup") {
        val point = LocationDAO.getByLatLon("-33.25", "135.85")
        if(!point.isEmpty){
            println(point.get.ibra)
            println(point.get.stateProvince)
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