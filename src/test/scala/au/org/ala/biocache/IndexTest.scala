package au.org.ala.biocache

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import au.org.ala.biocache.parser.DateParser
import au.org.ala.biocache.index.SolrIndexDAO

/**
 * Performs some Index Processing tests
 */
@RunWith(classOf[JUnitRunner])
class IndexTest extends ConfigFunSuite {
    test("Tests Index Date Ranges" ){
        val map=Map("lastModifiedTime"->"2011-07-07T10:00:00Z", "lastModifiedTime.p"->"2011-07-17T10:00:00Z")
        val indexer = new SolrIndexDAO("/data/solr/bio-proto","","")
        expectResult(true){indexer.shouldIndex(map, DateParser.parseStringToDate("2011-07-10T10:00:00Z"))}
        expectResult(false){indexer.shouldIndex(map, DateParser.parseStringToDate("2011-07-20T10:00:00Z"))}
        expectResult(true){indexer.shouldIndex(map, None)}
        expectResult(false){indexer.shouldIndex(map, DateParser.parseStringToDate("2011-08-20T10:00:00Z"))}
        
        val map2 =Map("lastModifiedTime.p"->"2011-07-17T10:00:00Z")
        expectResult(true){indexer.shouldIndex(map2, DateParser.parseStringToDate("2011-07-10T10:00:00Z"))}
        expectResult(false){indexer.shouldIndex(map2, DateParser.parseStringToDate("2011-07-20T10:00:00Z"))}
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
        
        expectResult("Aus bus"){indexer.getRawScientificName(map)}
        
        map = Map("genus"->"Aus")
        expectResult("Aus"){indexer.getRawScientificName(map)}
        
        map = Map("genus"->"Aus", "species"->"bus")
        expectResult("Aus bus"){indexer.getRawScientificName(map)}
        
        map = Map("genus"->"Aus", "species"->"bus", "subspecies" ->"cus")
        expectResult("Aus bus cus"){indexer.getRawScientificName(map)}
        
        map = Map("scientificName"->"Aus", "genus" ->"Dus")
        expectResult("Aus"){indexer.getRawScientificName(map)}
        
        map = Map("genus"->"Aus", "specificEpithet"->"bus")
        expectResult("Aus bus"){indexer.getRawScientificName(map)}
        
        map = Map("genus"->"Aus", "specificEpithet"->"bus" , "infraspecificEpithet"->"cus")
        expectResult("Aus bus cus"){indexer.getRawScientificName(map)}
        
        map = Map("family" -> "Family")
        expectResult("Family"){indexer.getRawScientificName(map)}
    }

}