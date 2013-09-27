package au.org.ala.biocache

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DistanceRangeParserTest extends ConfigFunSuite {

    test("Test range parsing"){
        expectResult(1.2222f) { DistanceRangeParser.parse("1.2222").get._1 }
        expectResult(444f) { DistanceRangeParser.parse("444").get._1 }
        expectResult(12000f) {DistanceRangeParser.parse("1-12km").get._1 }
        expectResult(10f) {DistanceRangeParser.parse("1-10m").get._1 }
        expectResult(10f) {DistanceRangeParser.parse("1m-10m").get._1 }
        expectResult(10000f) {DistanceRangeParser.parse("1km-10km").get._1 }
        expectResult(10500f) {DistanceRangeParser.parse("1km- 10.5km").get._1 }
        expectResult(10000f) {DistanceRangeParser.parse("> 10km").get._1 }
        expectResult(10000f) {DistanceRangeParser.parse("< 10km").get._1 }
        expectResult(11500f) {DistanceRangeParser.parse(">11.5km").get._1 }
        expectResult(11500f) {DistanceRangeParser.parse("10.2 - 11.5km").get._1 }
        expectResult(1000f) {DistanceRangeParser.parse("1000 meters").get._1 }
        expectResult(10000f) {DistanceRangeParser.parse("10 kilometers").get._1 }
        var (value, unit) = DistanceRangeParser.parse("300 ft").get
        expectResult(Feet){unit}
        expectResult(91.44f){value}
    }
}