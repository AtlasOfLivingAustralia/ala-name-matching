package au.org.ala.biocache
import org.scalatest.FunSuite

class AssertionCodeTests extends FunSuite {

    test("Test the geospatially kosher test") {

        val assertions1 = Array( QualityAssertion(AssertionCodes.GEOSPATIAL_ISSUE), QualityAssertion(AssertionCodes.INVERTED_COORDINATES) )
        expect(false){ AssertionCodes.isGeospatiallyKosher(assertions1) }

        val assertions2 = Array( QualityAssertion(AssertionCodes.INVERTED_COORDINATES) )
        expect(true){ AssertionCodes.isGeospatiallyKosher(assertions2) }
    }
}