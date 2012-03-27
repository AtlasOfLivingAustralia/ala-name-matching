package au.org.ala.util

import au.org.ala.biocache.Config
import au.com.bytecode.opencsv.CSVReader
import java.io.{FileReader, File, FileWriter}
import org.apache.commons.lang3.time.DateUtils
import java.util.Date
import java.text.SimpleDateFormat

object ExportFromIndex {

  def main(args:Array[String]){

    var outputFilePath = ""
    var fieldsToExport = Array[String]()
    var counter = 0
    
    val parser = new OptionParser("load flickr resource") {
      arg("<output-file>", "The UID of the data resource to load", { v: String => outputFilePath = v })
      arg("<list of fields>", "The UID of the data resource to load", { v: String => fieldsToExport = v.split(" ").toArray })
    }
    if (parser.parse(args)) {
      val fileWriter = new FileWriter(new File(outputFilePath))
      Config.indexDAO.pageOverIndex(map => {
        counter += 1
        if (counter % 1000 == 0) { println("Exported :" + counter); fileWriter.flush; }
        val outputLine = fieldsToExport.map(f => getFromMap(map,f))
        fileWriter.write(outputLine.mkString("\t"))
        fileWriter.write("\n")
        true
      }, fieldsToExport, "*:*", Array())
      Config.indexDAO.shutdown
      fileWriter.flush
      fileWriter.close
    }
  }
  
  def getFromMap(map:java.util.Map[String, AnyRef],key:String) : String = {
    val value = map.get(key)
    if (value == null) "" else value.toString
  }
}

object ExportFacet {

  var facetField = "species_guid"
  var facetQuery = "*:*"
  var facetOutputFile = "/tmp/facet-output-" +facetField+".txt"
  var lastDay = false
  var lastWeek = false
  var lastMonth = false
  var indexDateField = "first_loaded_date"

  var fieldsToExport = Array[String]()

  val parser = new OptionParser("ExportFacet - Exports a facet to file") {
    arg("<facet-field>", "The field to facet on", { v: String => facetField = v })
    arg("<facet-output-file>", "The field to facet on", { v: String => facetOutputFile = v })
    opt("fq","filter query", "Filter query to use", { v: String => facetQuery = v })
    booleanOpt("ld","lastDay", "Only export those that have had new records in the last day", { v: Boolean => lastDay = v })
    booleanOpt("ld","lastWeek", "Only export those that have had new records in the last week", { v: Boolean => lastWeek = v })
    booleanOpt("ld","lastMonth", "Only export those that have had new records in the last month", { v: Boolean => lastMonth = v })
    opt("df","date field to use", "The indexed date field to use e.g. first_loaded_Date", { v: String => indexDateField = v })
  }

  def main(args:Array[String]){
    if (parser.parse(args)) {
      // first_loaded_date:[2012-03-26T00:00:00Z%20TO%20*]
      val sfd = new SimpleDateFormat()
      var facetFilterQuery =  ""
      if (lastDay) facetFilterQuery = indexDateField + ":[" + sfd.format(DateUtils.addDays(new Date(), -1), "yyyy-MM-dd") + "T00:00:00Z%20TO%20*]"
      else if (lastWeek) facetFilterQuery = indexDateField + ":[" + sfd.format(DateUtils.addWeeks(new Date(), -1), "yyyy-MM-dd") + "T00:00:00Z%20TO%20*]"
      else if (lastMonth) facetFilterQuery = indexDateField + ":[" + sfd.format(DateUtils.addMonths(new Date(), -1), "yyyy-MM-dd") + "T00:00:00Z%20TO%20*]"

      //do the facet query
      val facetWriter = new FileWriter(new File(facetOutputFile))
      Config.indexDAO.pageOverFacet( (label, count) => {
        facetWriter.write(label)
        facetWriter.write("\n")
        facetWriter.flush
        true
      }, facetField, facetQuery, Array(facetFilterQuery))
      facetWriter.flush
      facetWriter.close
      Config.indexDAO.shutdown
    }
  }
}

object ExportByFacetQuery {

  def main(args:Array[String]){

    var facetField = "species_guid"
    var facetQuery = "*:*"
    var facetInputFile = "/tmp/facet-output-" +facetField+".txt"
    var recordOutputFile = "/tmp/record-output-" +facetField+".txt"
    var fieldsToExport = Array[String]()
    var filterQueries:Array[String] = Array()

    val parser = new OptionParser("ExportByFacetQuery - Exports records based a file of inputs and specified field to facet query") {
      arg("<facet-field>", "The field to facet on", { v: String => facetField = v })
      arg("<facet-input-file>", "The field to facet on", { v: String => facetInputFile = v })
      arg("<record-output-file>", "The file to dump records to", { v: String => recordOutputFile = v })
      arg("<list of fields>", "Space separated list of fields to export", { v: String => fieldsToExport = v.split(" ").toArray })
      opt("fq","filter query", "An additional filter query to use when exporting", { v: String => filterQueries = v.split("&") })
    }
    if (parser.parse(args)) {
      //iterate through the facet export
      var counter = 0
      val csvReader = new CSVReader(new FileReader(facetInputFile), '\t')
      val fileWriter = new FileWriter(new File(recordOutputFile))
      var taxonID = getTaxonID(csvReader)
      while(taxonID != null){
        Config.indexDAO.pageOverIndex(map  => {
          counter += 1
          if (counter % 1000 == 0) { println("Exported :" + counter); fileWriter.flush; }
          val outputLine = fieldsToExport.map(f => getFromMap(map,f))
          fileWriter.write(outputLine.mkString("\t"))
          fileWriter.write("\n")
          true
        }, fieldsToExport, facetField + ":\""+taxonID+"\"", filterQueries)

        taxonID = getTaxonID(csvReader)
      }
      Config.indexDAO.shutdown
      fileWriter.flush
      fileWriter.close
    }
  }

  def getTaxonID(csvReader:CSVReader): String = {
    val row = csvReader.readNext()
    if (row !=null && row.length>0) row.head
    else null
  }

  def getFromMap(map:java.util.Map[String, AnyRef],key:String) : String = {
    val value = map.get(key)
    if (value == null) "" else value.toString
  }
}
