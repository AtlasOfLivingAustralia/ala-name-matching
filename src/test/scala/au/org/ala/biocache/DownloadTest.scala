package au.org.ala.biocache

import org.scalatest.FunSuite

/**
 * @author Dave Martin (David.Martin@csiro.au)
 */
class DownloadTest extends FunSuite {

  val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
  val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]

  test("Download to CSV") {

    val uuids = Array(
        "0000b9e7-65b4-4335-b012-60cdb13a91fb",
        "0000eb51-ea32-4693-a0ce-f9dbf025d212",
        "0001b51b-32d7-48a8-9f67-3563cba731f3")

    println("Raw values")
    occurrenceDAO.writeToStream(System.out, "\t", "\n", uuids, Array("uuid","scientificName", "eventDate"))

    println("Processed values")
    occurrenceDAO.writeToStream(System.out, "\t", "\n", uuids, Array("uuid","scientificName", "eventDate"))

    persistenceManager.shutdown
  }
}