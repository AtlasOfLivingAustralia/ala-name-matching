package au.org.ala.biocache

import org.scalatest.FunSuite

/**
 * Demonstrator paging code.  Need to find a way of running this as tests.
 * 
 * @author Dave Martin (David.Martin@csiro.au)
 */
class PagingTests extends FunSuite {

  /*
  test("Paging of first ten records"){
    var count = 0
    OccurrenceDAO.pageOverAll(Raw, fullRecord => {
        val occurrence = fullRecord.get.o
        val classification = fullRecord.get.c
        val location = fullRecord.get.l
        val event = fullRecord.get.e
        println(occurrence.uuid+"\t"+classification.genus+"\t"+classification.specificEpithet+"\t"+classification.scientificName)
        count +=1
        if(count>10) exit(1)
      }
    )
  }

  test("Paging over all versions"){
    var count = 0
    OccurrenceDAO.pageOverAllVersions(recordVersions => {
        val versions = recordVersions.get
        expect(3){versions.length}
        for(fullRecord <- versions){
          val occurrence = fullRecord.o
          val classification = fullRecord.c
          val location = fullRecord.l
          val event = fullRecord.e
          println(occurrence.uuid+"\t"+classification.genus+"\t"+classification.specificEpithet+"\t"+classification.scientificName)
        }
        count +=1
        if(count>10) exit(1)
      }
    )
  }
  */
}