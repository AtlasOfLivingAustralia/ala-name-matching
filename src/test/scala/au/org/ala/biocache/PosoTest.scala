package au.org.ala.biocache

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import au.org.ala.biocache.parser.DateParser
import au.org.ala.biocache.model.{DuplicateRecordDetails, Versions, ValidationRule, Attribution}
import au.org.ala.biocache.load.FullRecordMapper

@RunWith(classOf[JUnitRunner])
class PosoTest extends ConfigFunSuite {
    
    test("Attribution toMap with array test"){
        
        val a = new Attribution
        a.dataHubUid = Array("dh1", "dh2")
        a.institutionName = "AM"
        
        val map = a.toMap
        
        expectResult("""["dh1","dh2"]"""){map("dataHubUid")}
        expectResult("AM"){map("institutionName")}
    }
    
    test("Attribution from map with array"){
    	val map = Map("dataHubUid"-> """["dh1","dh2"]""", "dataResourceUid"->"dr349", "dataProviderName"->"OZCAM provider for Museum")
    	val raw =FullRecordMapper.createFullRecord("test1234", map, Versions.RAW)
//      logger.debug(raw.attribution)
    	expectResult(2){raw.attribution.dataHubUid.size}
    }
    
    test("query attribution with date"){
      val aq = new ValidationRule()
      val date = DateParser.parseStringToDate("2012-01-01T10:22:00")
      aq.setCreatedDate(date.get)
      val map = aq.toMap
      expectResult("2012-01-01T10:22:00Z"){map.getOrElse("createdDate","")}
    }

    test("DuplicateDetails Serialisations"){

      val d = new DuplicateRecordDetails()
      val mapper = new ObjectMapper()
      mapper.registerModule(new DefaultScalaModule())

//      logger.debug(mapper.writeValueAsString(d))
    }
}
