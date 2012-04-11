package au.org.ala.biocache
import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class VerbatimLatLonTest extends ConfigFunSuite {

    test("30° S"){
       expect(false) { VerbatimLatLongParser.parse("30° 01' S").isEmpty }
    }

    test("30° 01' S"){
       expect(false) { VerbatimLatLongParser.parse("30° 01' S").isEmpty }
    }

    test("153° 12' E"){
       expect(false) { VerbatimLatLongParser.parse("153° 12' E").isEmpty }
    }

    test("""145° 44' 55.85" E"""){
       expect(false) { VerbatimLatLongParser.parse("""145° 44' 55.85" E""").isEmpty }
    }

    test("""16° 52' 37" S"""){
       expect(false) { VerbatimLatLongParser.parse("""16° 52' 37" S""").isEmpty }
    }

    test("""41 05 54.03S"""){
       expect(false) { VerbatimLatLongParser.parse("""41 05 54.03S""").isEmpty }
    }

    test("""121d 10' 34" W"""){
       expect(false) { VerbatimLatLongParser.parse("""121d 10' 34" W""").isEmpty }
    }

    test("""-37º 3' 48' S"""){
       expect(false) { VerbatimLatLongParser.parse("""-37º 3' 48' S""").isEmpty }
    }

    test("""-37º 3' 48'' S - weird degree symbol"""){
       expect(false) { VerbatimLatLongParser.parse("""37º 10' 48" S""").isEmpty }
    }

    test("""-37º 3' 48'' S"""){
       expect(false) { VerbatimLatLongParser.parse("""-37º 3' 48'' S""").isEmpty }
    }

    test("""149º 54' 14'' E"""){
      expect(false) { VerbatimLatLongParser.parse("""149º 54' 14'' E""").isEmpty }
    }
}