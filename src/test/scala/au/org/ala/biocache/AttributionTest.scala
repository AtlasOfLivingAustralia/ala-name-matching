package au.org.ala.biocache
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.junit.Ignore

@RunWith(classOf[JUnitRunner])
class AttributionTest extends ConfigFunSuite{
       
    test("Test DR lookup in collectory"){
        val dr= AttributionDAO.getDataResourceFromWS("dr367")        
        expect(true){dr.get.hasMappedCollections}
        expect("dp33"){dr.get.dataProviderUid}
        
        val dr376 = AttributionDAO.getDataResourceFromWS("dr376")
        println(dr376)
        println(dr376.get.taxonomicHints)
    }
    
    test("Collection lookup"){
        var raw = new FullRecord
        var processed = new FullRecord
        raw.attribution.dataResourceUid="dr367"
        raw.occurrence.collectionCode="WINC"
        (new AttributionProcessor).process("test", raw, processed)
        expect("dp33"){processed.attribution.dataProviderUid}
        expect("co74"){processed.attribution.collectionUid}
        
        raw = new FullRecord
        processed = new FullRecord
        raw.attribution.dataResourceUid = "dr360"
        raw.occurrence.collectionCode="TEST"
        val qas = (new AttributionProcessor).process("test", raw, processed)
        expect("dp29"){processed.attribution.dataProviderUid}
        expect(0){qas.size}
    }
    
    test("Default DWC Values in DR Lookup"){
        val dr = AttributionDAO.getDataResourceFromWS("dr92")
        expect(Some("MachineObservation")){dr.get.defaultDwcValues.get("basisOfRecord")}
    }
    
    
}
