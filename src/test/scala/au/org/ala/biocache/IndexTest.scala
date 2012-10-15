package au.org.ala.biocache

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Performs some Index Processing tests
 */
@RunWith(classOf[JUnitRunner])
class IndexTest extends ConfigFunSuite {
    test("Tests Index Date Ranges" ){
        val map=Map("lastModifiedTime"->"2011-07-07T10:00:00Z", "lastModifiedTime.p"->"2011-07-17T10:00:00Z")
        val indexer = new SolrIndexDAO("/data/solr/bio-proto","","")
        expect(true){indexer.shouldIndex(map, DateParser.parseStringToDate("2011-07-10T10:00:00Z"))}        
        expect(false){indexer.shouldIndex(map, DateParser.parseStringToDate("2011-07-20T10:00:00Z"))}
        expect(true){indexer.shouldIndex(map, None)}
        expect(false){indexer.shouldIndex(map, DateParser.parseStringToDate("2011-08-20T10:00:00Z"))}
        
        val map2 =Map("lastModifiedTime.p"->"2011-07-17T10:00:00Z")
        expect(true){indexer.shouldIndex(map2, DateParser.parseStringToDate("2011-07-10T10:00:00Z"))}
        expect(false){indexer.shouldIndex(map2, DateParser.parseStringToDate("2011-07-20T10:00:00Z"))}
    }
    
//    This test does not belong here
//    test("Get error code Tests"){
//        val dao = new OccurrenceDAOImpl
//        val map = Map("attr.qa"->"[]", "bor.qa"->"[12,33]", "class.qa"->"[]", "image.qa"->"[]", "loc.qa"->"[27]", "type.qa"->"[]")
//        val test = dao.getErrorCodes(map)
//        println(test +" : " + test.getClass)
//        for(value <- test)
//            println("value: " + value)
//    }
//    
    test("Raw Scientific Name"){
        var map = Map("scientificName"->"Aus bus")
        val indexer = new SolrIndexDAO("/data/solr/bio-proto","","")
        
        expect("Aus bus"){indexer.getRawScientificName(map)}
        
        map = Map("genus"->"Aus")
        expect("Aus"){indexer.getRawScientificName(map)}
        
        map = Map("genus"->"Aus", "species"->"bus")
        expect("Aus bus"){indexer.getRawScientificName(map)}
        
        map = Map("genus"->"Aus", "species"->"bus", "subspecies" ->"cus")
        expect("Aus bus cus"){indexer.getRawScientificName(map)}
        
        map = Map("scientificName"->"Aus", "genus" ->"Dus")
        expect("Aus"){indexer.getRawScientificName(map)}
        
        map = Map("genus"->"Aus", "specificEpithet"->"bus")
        expect("Aus bus"){indexer.getRawScientificName(map)}
        
        map = Map("genus"->"Aus", "specificEpithet"->"bus" , "infraspecificEpithet"->"cus")
        expect("Aus bus cus"){indexer.getRawScientificName(map)}
        
        map = Map("family" -> "Family")
        expect("Family"){indexer.getRawScientificName(map)}
    }

}