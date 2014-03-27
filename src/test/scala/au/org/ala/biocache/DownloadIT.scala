package au.org.ala.biocache

import org.scalatest.FunSuite
import org.junit.Ignore
import au.org.ala.biocache.dao.OccurrenceDAO
import au.org.ala.biocache.persistence.PersistenceManager

/**
 * @author Dave Martin (David.Martin@csiro.au)
 */
@Ignore
class DownloadIT extends FunSuite {

  val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
  val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]

  test("Download to CSV") {

    val uuids = Array(
        "0000b9e7-65b4-4335-b012-60cdb13a91fb",
        "0000eb51-ea32-4693-a0ce-f9dbf025d212",
        "0001b51b-32d7-48a8-9f67-3563cba731f3")

    println("Raw values")
    occurrenceDAO.writeToStream(System.out, "\t", "\n", uuids, Array("uuid","scientificName", "eventDate"), Array())

    println("Processed values")
    occurrenceDAO.writeToStream(System.out, "\t", "\n", uuids, Array("uuid","scientificName", "eventDate"), Array())

    persistenceManager.shutdown
  }
}