package au.org.ala.biocache.export

import java.io.{File, FileWriter}
import au.org.ala.biocache.Config
import au.org.ala.biocache.util.OptionParser

object ExportFromIndex {

  def main(args: Array[String]) {

    var outputFilePath = ""
    var query = "*:*"
    var fieldsToExport = Array[String]()
    var counter = 0

    val parser = new OptionParser("load flickr resource") {
      arg("<output-file>", "The UID of the data resource to load", {
        v: String => outputFilePath = v
      })
      arg("<list of fields>", "The UID of the data resource to load", {
        v: String => fieldsToExport = v.split(" ").toArray
      })
      opt("q", "query", "The SOLR query to use", {
        v: String => query = v
      })
    }
    if (parser.parse(args)) {
      val fileWriter = new FileWriter(new File(outputFilePath))
      Config.indexDAO.pageOverIndex(map => {
        counter += 1
        if (counter % 1000 == 0) {
          println("Exported :" + counter); fileWriter.flush;
        }
        val outputLine = fieldsToExport.map(f => getFromMap(map, f))
        fileWriter.write(outputLine.mkString("\t"))
        fileWriter.write("\n")
        true
      }, fieldsToExport, query, Array())
      Config.indexDAO.shutdown
      fileWriter.flush
      fileWriter.close
    }
  }

  def getFromMap(map: java.util.Map[String, AnyRef], key: String): String = {
    val value = map.get(key)
    if (value == null) "" else value.toString
  }
}


