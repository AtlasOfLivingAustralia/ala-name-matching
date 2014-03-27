package au.org.ala.biocache.export

import au.org.ala.biocache.util.OptionParser
import java.io.{File, FileWriter}
import au.org.ala.biocache.Config

object ExportFromIndexStream {

  var outputFilePath = ""
  var query = "*:*"
  var fieldsToExport = Array[String]()
  var counter = 0
  var orderFields = Array("row_key")

  def main(args: Array[String]) {
    val parser = new OptionParser("export index stream") {
      arg("<output-file>", "The file name for the export file", {
        v: String => outputFilePath = v
      })
      arg("<list of fields>", "CSV list of fields to export", {
        v: String => fieldsToExport = v.split(",").toArray
      })
      opt("q", "query", "The SOLR query to use", {
        v: String => query = v
      })
    }

    if (parser.parse(args)) {
      val fileWriter = new FileWriter(new File(outputFilePath))
      Config.indexDAO.streamIndex(map => {
        counter += 1
        if (counter % 1000 == 0) {
          fileWriter.flush;
        }
        val outputLine = fieldsToExport.map(f => {
          if (map.containsKey(f)) map.get(f).toString else ""
        })
        fileWriter.write(outputLine.mkString("\t"))
        fileWriter.write("\n")
        true
      }, fieldsToExport, query, Array(), orderFields, None)
      Config.indexDAO.shutdown
      fileWriter.flush
      fileWriter.close
    }
  }
}
