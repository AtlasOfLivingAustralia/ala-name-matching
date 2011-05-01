package au.org.ala.biocache

import org.scalatest.FunSuite
import org.junit.Ignore

/**
 * Demonstrator paging code.  Need to find a way of running this as tests.
 * 
 * @author Dave Martin (David.Martin@csiro.au)
 */
@Ignore
class PagingTests extends FunSuite {

  val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
  val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]

  test("Paging of first ten raw records"){
    var count = 0
    occurrenceDAO.pageOverAll(Raw, fullRecord => {
        val occurrence = fullRecord.get.occurrence
        val classification = fullRecord.get.classification
        val location = fullRecord.get.location
        val event = fullRecord.get.event
        println(fullRecord.get.uuid+"\t"+classification.genus+"\t"+classification.specificEpithet+"\t"+classification.scientificName)
        count +=1
        if(count>10) {
            false
        } else {
            true
        }
      }, 10
    )
    persistenceManager.shutdown
  }

  test("Paging over all versions"){
    var count = 0
    occurrenceDAO.pageOverAllVersions(recordVersions => {
        val versions = recordVersions.get
        expect(3){versions.length}
        for(fullRecord <- versions){
          val occurrence = fullRecord.occurrence
          val classification = fullRecord.classification
          val location = fullRecord.location
          val event = fullRecord.event
          println(fullRecord.uuid+"\t"+classification.genus+"\t"+classification.specificEpithet+"\t"+classification.scientificName)
        }
        count +=1
        if(count>10) {
            false
        } else {
            true
        }
      }, 10
    )
    persistenceManager.shutdown
  }
}