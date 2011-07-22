package au.org.ala.biocache
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class AttributionTests extends FunSuite{

    test("Test DR lookup in collectory"){
        val dr= AttributionDAO.getDataResourceFromWS("dr367")        
        expect(true){dr.get.hasMappedCollections}
        expect("dp33"){dr.get.dataProviderUid}
    }
    
    test("Collection lookup"){
        var raw = new FullRecord
        var processed = new FullRecord
        raw.attribution.dataResourceUid="dr367"
        raw.occurrence.collectionCode="WINC"
        AttributionProcessor.process("test", raw, processed)        
        expect("dp33"){processed.attribution.dataProviderUid}
        expect("co74"){processed.attribution.collectionUid}
        
        raw = new FullRecord
        processed = new FullRecord
        raw.attribution.dataResourceUid = "dr360"
        raw.occurrence.collectionCode="TEST"
        val qas = AttributionProcessor.process("test", raw, processed)
        expect("dp29"){processed.attribution.dataProviderUid}
        expect(0){qas.size}
    }
    
}