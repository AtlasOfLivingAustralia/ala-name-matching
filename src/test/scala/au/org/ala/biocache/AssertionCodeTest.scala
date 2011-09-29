package au.org.ala.biocache
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class AssertionCodeTest extends ConfigFunSuite {
    
    test("Test the geospatially kosher test") {

        val assertions1 = Array( QualityAssertion(AssertionCodes.GEOSPATIAL_ISSUE), QualityAssertion(AssertionCodes.INVERTED_COORDINATES) )
        expect(false){ AssertionCodes.isGeospatiallyKosher(assertions1) }

        val assertions2 = Array( QualityAssertion(AssertionCodes.INVERTED_COORDINATES) )
        expect(true){ AssertionCodes.isGeospatiallyKosher(assertions2) }
    }
    
    test("Test kosher based on list of int codes"){
        val codes1 = Array(1,2,3,30)
        expect(true){AssertionCodes.isGeospatiallyKosher(codes1)}
        val codes2 = Array(1,2,0)
        expect(false){AssertionCodes.isGeospatiallyKosher(codes2)}
    }
}