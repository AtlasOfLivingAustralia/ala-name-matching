package au.org.ala.biocache.tool

import au.org.ala.biocache._
import org.slf4j.LoggerFactory
import au.org.ala.biocache.index.IndexRecords
import au.org.ala.biocache.load.Loader
import au.org.ala.biocache.util.OptionParser

/**
 * Reloads/processes an existing data resource records.
 * This deletes the data for a specific resource and then reloads.
 * Only to be used for data resources without stable identifiers.
 */
object ReloadDataResources {

  val logger = LoggerFactory.getLogger("ReloadDataResources")

  def main(args: Array[String]): Unit = {
    var dataResource: String = ""
    var mark: Boolean = false
    var load: Boolean = false
    var remove: Boolean = false //remove from persistent data store
    var process: Boolean = false
    var index: Boolean = false

    val parser = new OptionParser("Reload data resource") {
      arg("<data resource UID>", "The UID of the data resource to load", {
        v: String => dataResource = v
      })
      opt("all", "perform all phases", {
        mark = true; load = true; remove = true; process = true; index = true
      })
      opt("mark", "mark the occurrences in the data store as deleted", {
        mark = true
      })
      opt("load", "reload the records from the data resource", {
        load = true
      })
      opt("remove", "remove the occurrences from the data store", {
        remove = true
      })
      opt("process", "reprocess the records for the data resource", {
        process = true
      })
      opt("index", "reindex the records for the data resource", {
        index = true
      })
    }

    if (parser.parse(args)) {

      logger.info("Reloading data resource " + dataResource + " reprocessing: " + process + " reindexing: " + index + " removing from data store: " + remove)
      completeReload(dataResource, mark, load, process, index, remove)
      if (index) {
        Config.indexDAO.shutdown
      }
    }
  }

  def completeReload(dataResourceUid: String, mark: Boolean = true, load: Boolean = true, process: Boolean = true, index: Boolean = true, remove: Boolean = true) {

    val deletor = new DataResourceVirtualDelete(dataResourceUid)
    if (mark) {
      //Step 1: Mark all records for dr as deleted
      deletor.deleteFromPersistent
    }
    if (load) {
      //Step 2: Reload the records
      val l = new Loader
      l.load(dataResourceUid)
    }
    //Step 3: Reprocess records
    if (process) {
      ProcessWithActors.processRecords(4, None, Some(dataResourceUid), true) //want to process on the not deleted records
    }
    if (index) {
      //Step4: Remove current records from the index
      deletor.deleteFromIndex
      //Step 5: Reindex dataResource
      IndexRecords.index(None, None, Some(dataResourceUid), false, false, checkDeleted = true)
    }
    if (remove) {
      //Step 6: Remove "deleted" records from persistence.
      deletor.physicallyDeleteMarkedRecords
    }
  }
}
