package au.org.ala.biocache

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PosoTest extends ConfigFunSuite {
    
    test("Attribution toMap with array test"){
        
        val a = new Attribution
        a.dataHubUid = Array("dh1", "dh2")
        a.institutionName = "AM"
        
        val map = a.toMap
        
        expect("""["dh1","dh2"]"""){map("dataHubUid")}
        expect("AM"){map("institutionName")}
    }
    
    test("Attribution from map with array"){
    	val map = Map("dataHubUid"-> """["dh1","dh2"]""", "dataResourceUid"->"dr349", "dataProviderName"->"OZCAM provider for Museum")
    	val raw =FullRecordMapper.createFullRecord("test1234", map, Versions.RAW)
    	println(raw.attribution)
    	expect(2){raw.attribution.dataHubUid.size}
    }
}
