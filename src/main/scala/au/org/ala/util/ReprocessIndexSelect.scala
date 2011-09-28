package au.org.ala.util

import au.org.ala.biocache._
import java.io.{BufferedOutputStream, FileOutputStream}
import java.io.File

/**
 * Reprocesses and Reindexes a select set of records.  The records will
 * be obtained through a query to the index.
 */
object ReprocessIndexSelect {

  def main(args: Array[String]): Unit = {
    var query: Option[String] = None;
    var threads = 4
    var exist = false;
    var indexOnly = false
    var startUuid: Option[String] = None
    val parser = new OptionParser("index records options") {
      opt("q", "query", "The query to run e.g. 'year:[2001 TO *]' or 'taxon_name:Macropus'", {
        v: String => query = Some(v)
      })
      intOpt("t", "thread", "The number of threads to use", { v: Int => threads = v })
      opt("exist", "use the existing list of row keys", { exist = true })
      opt("s", "start", "The record to start processing with", { v: String => startUuid = Some(v) })
      opt("index", "reindex only - do not reprocess", { indexOnly = true })
    }

    if (parser.parse(args)) {
      if (!query.isEmpty) {
        reprocessindex(query.get, threads, exist, startUuid, indexOnly)
      } else {
        parser.showUsage
      }
    }
  }

  def reprocessindex(query: String, threads: Int, exist: Boolean, startUuid: Option[String], indexOnly: Boolean) {
    //get the list of rowKeys to be processed.
    val indexer = Config.getInstance(classOf[IndexDAO]).asInstanceOf[IndexDAO]

    val file = new File("rowkeys.out")
    if (!exist) {
      val out = new BufferedOutputStream(new FileOutputStream(file));
      indexer.writeRowKeysToStream(query, out)
      out.flush
      out.close
    }
    if (!indexOnly)
      ProcessWithActors.processRecords(threads, file, startUuid)
    IndexRecords.indexList(file)
  }
}