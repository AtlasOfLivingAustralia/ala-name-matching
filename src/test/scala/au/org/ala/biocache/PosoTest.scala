package au.org.ala.biocache

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import au.org.ala.util.DuplicateRecordDetails
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper

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
    
    test("query attribution with date"){
      val aq = new AssertionQuery()
      val date = DateParser.parseStringToDate("2012-01-01T10:22:00")
      aq.setCreatedDate(date.get)
      val map = aq.toMap
      expect("2012-01-01T10:22:00Z"){map.getOrElse("createdDate","")}
    }

    test("DuplicateDetails Serialisations"){

      val d = new DuplicateRecordDetails()
      val mapper = new ObjectMapper()
      mapper.registerModule(new DefaultScalaModule())

      println(mapper.writeValueAsString(d))
    }
}
