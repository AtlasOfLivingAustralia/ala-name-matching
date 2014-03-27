package au.org.ala.biocache.export

import au.org.ala.biocache.util.OptionParser
import au.com.bytecode.opencsv.CSVReader
import java.io.{FileWriter, FileReader, File}
import au.org.ala.biocache.Config

/**
 * Utility to export from index based on facet and optional filter.
 */
object ExportByFacetQuery {

  def main(args: Array[String]) {

    var facetField = "species_guid"
    var facetInputFile = "/tmp/facet-output-" + facetField + ".txt"
    var recordOutputFile = "/tmp/record-output-" + facetField + ".txt"
    var fieldsToExport = Array[String]()
    var filterQueries: Array[String] = Array()

    val parser = new OptionParser("ExportByFacetQuery - Exports records based a file of inputs and specified field to facet query") {
      arg("<facet-field>", "The field to facet on", {
        v: String => facetField = v
      })
      arg("<facet-input-file>", "The field to facet on", {
        v: String => facetInputFile = v
      })
      arg("<record-output-file>", "The file to dump records to", {
        v: String => recordOutputFile = v
      })
      arg("<list of fields>", "Space separated list of fields to export", {
        v: String => fieldsToExport = v.split(" ").toArray
      })
      opt("fq", "filter query", "An additional filter query to use when exporting", {
        v: String => filterQueries = v.split("&")
      })
    }
    if (parser.parse(args)) {
      //iterate through the facet export
      var counter = 0
      val csvReader = new CSVReader(new FileReader(facetInputFile), '\t')
      val fileWriter = new FileWriter(new File(recordOutputFile))
      var taxonID = getTaxonID(csvReader)
      while (taxonID != null) {
        Config.indexDAO.pageOverIndex(map => {
          counter += 1
          if (counter % 1000 == 0) {
            println("Exported :" + counter); fileWriter.flush;
          }
          val outputLine = fieldsToExport.map(f => getFromMap(map, f))
          fileWriter.write(outputLine.mkString("\t"))
          fileWriter.write("\n")
          true
        }, fieldsToExport, facetField + ":\"" + taxonID + "\"", filterQueries)

        taxonID = getTaxonID(csvReader)
      }
      Config.indexDAO.shutdown
      fileWriter.flush
      fileWriter.close
    }
  }

  def downloadSingleTaxonByStream(query: String = null, taxonID: String, fieldsToExport: Array[String], facetField: String, filterQueries: Array[String], sortFields: Array[String], fileWriter: FileWriter, subspeciesWriter: Option[FileWriter] = None, multivaluedFields: Option[Array[String]] = None) {
    val q = if (query != null) query else facetField + ":\"" + taxonID + "\""
    Config.indexDAO.streamIndex(map => {
      val outputLine = fieldsToExport.map(f => getFromMap(map, f)).mkString("\t")
      fileWriter.write(outputLine)
      fileWriter.write("\n")
      if (subspeciesWriter.isDefined) {
        val subspecies = map.get("subspecies_guid")
        if (subspecies != null) {
          subspeciesWriter.get.write(outputLine)
          subspeciesWriter.get.write("\n")
        }

      }
      true
    }, fieldsToExport, q, filterQueries, sortFields, multivaluedFields)
  }

  def downloadSingleTaxon(taxonID: String, fieldsToExport: Array[String], facetField: String, filterQueries: Array[String], sortField: Option[String] = None, sortDir: Option[String] = None, fileWriter: FileWriter, multivaluedFields: Option[Array[String]] = None) {
    var counter = 0
    Config.indexDAO.pageOverIndex(map => {
      counter += 1
      if (counter % 1000 == 0) {
        println("Exported :" + counter); fileWriter.flush;
      }
      val outputLine = fieldsToExport.map(f => getFromMap(map, f))
      fileWriter.write(outputLine.mkString("\t"))
      fileWriter.write("\n")
      true
    }, fieldsToExport, facetField + ":\"" + taxonID + "\"", filterQueries, sortField, sortDir, multivaluedFields)

    fileWriter.flush
  }

  def getTaxonID(csvReader: CSVReader): String = {
    val row = csvReader.readNext()
    if (row != null && row.length > 0) row.head
    else null
  }

  def getFromMap(map: java.util.Map[String, AnyRef], key: String): String = {
    val value = map.get(key)
    if (value == null) "" else value.toString
  }
}
