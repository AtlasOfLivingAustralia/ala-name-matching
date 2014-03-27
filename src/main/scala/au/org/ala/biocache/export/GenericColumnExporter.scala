package au.org.ala.biocache.export

import au.org.ala.biocache.util.OptionParser
import java.io.{File, FileWriter}
import au.com.bytecode.opencsv.CSVWriter
import au.org.ala.biocache.Config
import scala.collection.mutable.HashSet

/**
 * A util for export a column/field
 */
object GenericColumnExporter {

  def main(args: Array[String]) {
    var filePath = ""
    var entity = ""
    val parser = new OptionParser("export") {
      arg("<entity>", "the entity (column family in cassandra) to export from", {
        v: String => entity = v
      })
      arg("<file-path>", "file to export to", {
        v: String => filePath = v
      })
    }
    if (parser.parse(args)) {
      val cols = getColumns(entity)
      val outWriter = new FileWriter(new File(filePath))
      val writer = new CSVWriter(outWriter, '\t', '"')
      ExportUtil.export(writer, entity, cols, List(), List(), maxRecords = Integer.MAX_VALUE)
    }
  }

  def getColumns(entity: String): List[String] = {
    println("Getting the columns for " + entity)
    val pm = Config.persistenceManager
    val myset = new HashSet[String]
    var count = 0
    pm.pageOverAll(entity, (guid, map) => {
      myset ++= map.keySet
      count += 1
      true
    }, "", "", 1000)
    println("Finished cycling through " + count + " records")
    println("The columns to export " + myset)
    myset.toList
  }
}