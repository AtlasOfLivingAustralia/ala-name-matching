package au.org.ala.util

import au.org.ala.biocache.{Config}
import org.apache.commons.io.FileUtils
import java.io.{File, FileReader}
import au.com.bytecode.opencsv.CSVReader
import scala.collection.JavaConversions

/**
 * Load a CSV file into the BioCache store
 */
object ImportUtil {

  def main(args: Array[String]) {

    var entity = ""
    var fieldsToImport = List[String]()
    var filesToImport = List[String]()
    var linesToSkip = 0
    var quotechar: Option[Char] = None
    var separator:Char = ','
    var idColumnIdx = 0

    val parser = new OptionParser("import") {
      arg("<entity>", "the entity (column family in cassandra) to export from", { v: String => entity = v })
      arg("<files-to-import>", "the file(s) to import, space separated", { v: String => filesToImport = v.split(" ").toList })
      opt("c", "columns", "<column1 column2 ...>", "column headers", { columns: String => fieldsToImport = columns.split(",").toList })
      //opt("sp", "separator", "column separator", "column separator for file to import", { v: String => separator = v.charAt(0) })
      opt("cf", "column header file", "e.g. /data/headers.txt", "column headers", {
        v: String => fieldsToImport = FileUtils.readFileToString(new File(v)).trim.split(',').toList
      })
      intOpt("s", "skip-line", "number of lines to skip before importing", { v: Int => linesToSkip = v })
      intOpt("id", "id-column-idx", "id column index. indexed from 0", { v: Int => idColumnIdx = v })

    }

    if (parser.parse(args)) {
      val pm = Config.persistenceManager
      //process each file
      filesToImport.foreach {
        importFile(entity, fieldsToImport, separator, quotechar, _, idColumnIdx)
      }
      pm.shutdown
    }
  }

  def importFile(entity: java.lang.String, fieldsToImport: List[String], separator: Char,  quotechar: Option[Char], filepath: String, idColumnIdx:Int = 0) {
    val reader = quotechar.isEmpty match {
      case false => new CSVReader(new FileReader(filepath), separator, quotechar.get)
      case _ => new CSVReader(new FileReader(filepath), separator)
    }

    var currentLine = reader.readNext
    var counter = 1
    while (currentLine != null) {
      //println("Reading line: " + currentLine)
      val columns = currentLine.toList
      //println(columns)
      if (columns.length == fieldsToImport.length) {
        val map = (fieldsToImport zip columns).toMap[String, String].filter {
          case (key, value) => value != null && value.toString.trim.length > 0
        }
        Config.persistenceManager.put(columns(idColumnIdx), entity, map)
      } else {
        println("Problem loading line: " + counter + ", cols:fields = " + columns.length +":"+ fieldsToImport.length)
      }
      counter += 1
      //read next
      currentLine = reader.readNext
    }
  }
}