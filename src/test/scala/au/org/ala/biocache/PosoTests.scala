package au.org.ala.biocache

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PosoTests extends FunSuite {
    
    test("Attribution toMap with array test"){
        
        val a = new Attribution
        a.dataHubUid = Array("dh1", "dh2")
        a.institutionName = "AM"
        
        val map = a.toMap
        
        expect("""["dh1","dh2"]"""){map("dataHubUid")}
        expect("AM"){map("institutionName")}
    }
}