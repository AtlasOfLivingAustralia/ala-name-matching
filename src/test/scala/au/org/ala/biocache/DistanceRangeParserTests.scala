package au.org.ala.biocache

import org.scalatest.FunSuite

class DistanceRangeParserTests extends FunSuite {

    test("Test range parse"){
        expect(1.2222f) { DistanceRangeParser.parse("1.2222").get }
        expect(444f) { DistanceRangeParser.parse("444").get }
        expect(12000f) {DistanceRangeParser.parse("1-12km").get }
        expect(10f) {DistanceRangeParser.parse("1-10m").get }
        expect(10f) {DistanceRangeParser.parse("1m-10m").get }
        expect(10000f) {DistanceRangeParser.parse("1km-10km").get }
        expect(10500f) {DistanceRangeParser.parse("1km-10.5km").get }

        expect(10000f) {DistanceRangeParser.parse(">10km").get }
        expect(10000f) {DistanceRangeParser.parse("<10km").get }
        //expect(11500f) {DistanceRangeParser.parse(">11.5km").get }
    }
}