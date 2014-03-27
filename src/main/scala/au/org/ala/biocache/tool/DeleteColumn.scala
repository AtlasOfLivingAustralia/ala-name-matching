package au.org.ala.biocache.tool

import au.org.ala.biocache.util.OptionParser
import au.org.ala.biocache.Config

/**
 * Utility to delete one or more columns.
 */
object DeleteColumn {

  def main(args: Array[String]): Unit = {

    var startUuid = ""
    var endUuid = ""
    var entityName = ""
    var columnsToDelete = Array[String]()

    val parser = new OptionParser("delete column options") {
        arg("<entity>", "the entity (column family in cassandra) to export from", { v: String => entityName = v })
        arg("<cols to delete>","A CSV list of columns to be deleted",{v:String => columnsToDelete = v.split(",")})
        opt("s", "start","The record to start with", {v:String => startUuid = v})
        opt("e", "end", "The record to end with", {v:String =>endUuid=v})
    }
    if (parser.parse(args)) {
      //val entityName = args(0)
      // val columnsToDelete = args.tail
      printf("Deleting from entity %s, columns: %s. Starting with row %s, ending with %s Hit return to proceed...",
        entityName, columnsToDelete.mkString(", "), startUuid, endUuid)
      val line = readLine
      println("proceeding.")

      Config.persistenceManager.pageOverSelect(entityName, (guid, map) => {
        map.keys.foreach(key => {
          if (!map.get(key).isEmpty) {
            Config.persistenceManager.deleteColumns(guid, entityName, key)
          }
        })
        true
      }, startUuid, endUuid, 1000, columnsToDelete: _*)


      Config.persistenceManager.shutdown
      println("Complete.")
    }
  }
}
