package au.org.ala.biocache

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class MiscTest extends FunSuite {

    test("missing basis of record"){
        val raw = new FullRecord
        var processed = new FullRecord
        val qas = (new BasisOfRecordProcessor).process("test", raw, processed)
        expect(20001){qas(0).code}
    }
    
    test("badly formed basis of record"){
        val raw = new FullRecord
        var processed = new FullRecord
        raw.occurrence.basisOfRecord = "dummy"
        val qas = (new BasisOfRecordProcessor).process("test", raw, processed)
        expect(20002){qas(0).code}
    }
    
    test("unrecognised type status"){
        val raw = new FullRecord
        var processed = new FullRecord
        raw.identification.typeStatus = "dummy"
        val qas = (new TypeStatusProcessor).process("test", raw, processed)
        expect(20004){qas(0).code}
    }
    
    test("unrecognised collection code"){
        val raw = new FullRecord
        var processed = new FullRecord
        raw.attribution.dataResourceUid = "dr368"
        raw.occurrence.collectionCode = "dummy"
        raw.occurrence.institutionCode = "dummy"
        var qas = (new AttributionProcessor).process("test", raw, processed)
        expect(20005){qas(0).code}
    }
    
    test("invalid image url"){
        val raw = new FullRecord
        var processed = new FullRecord
        raw.occurrence.associatedMedia = "invalidimageurl.ppp"
        var qas = (new ImageProcessor).process("test", raw, processed)
        expect(20007){qas(0).code}
    }
}