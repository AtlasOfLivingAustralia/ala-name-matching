package au.org.ala.biocache.util

import java.io.File
import scala.collection.mutable.ArrayBuffer

/**
 * Created by mar759 on 17/02/2014.
 */
class FileDelete(fileName: String) extends RecordDeletor {

  import FileHelper._

  //import the file constructs to allow lines to be easily iterated over
  override def deleteFromPersistent() = {
    new File(fileName).foreachLine(line =>
      occurrenceDAO.delete(line, false, true)
    )
  }

  override def deleteFromIndex {
    val buf = new ArrayBuffer[String]

    new File(fileName).foreachLine(line => {
      buf += line
      if (buf.size > 999) {
        val query = "row_key:\"" + buf.mkString("\" OR row_key:\"") + "\""
        indexer.removeByQuery(query)
        buf.clear()
      }
    })
  }
}
