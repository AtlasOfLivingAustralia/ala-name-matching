package au.org.ala.util

import au.org.ala.biocache.Config
import org.apache.commons.lang.time.DateUtils
import au.com.bytecode.opencsv.CSVReader
import java.io.{FileReader, File, FileWriter}

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
      }, fieldsToExport, "*:*")
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

object ExportByFacet {

  def main(args:Array[String]){

    var facetField = "species_guid"
    var facetOutputFile = "/tmp/facet-output-" +facetField+".txt"
    var recordOutputFile = "/tmp/record-output-" +facetField+".txt"
    var fieldsToExport = Array[String]()

    val parser = new OptionParser("load flickr resource") {
      arg("<facet-field>", "The field to facet on", { v: String => facetField = v })
      arg("<facet-output-file>", "The field to facet on", { v: String => facetOutputFile = v })
      arg("<record-output-file>", "The file to dump records to", { v: String => recordOutputFile = v })
      arg("<list of fields>", "Space separated list of fields to export", { v: String => fieldsToExport = v.split(" ").toArray })
    }
    if (parser.parse(args)) {

      //do the facet query
      val facetWriter = new FileWriter(new File(facetOutputFile))
      Config.indexDAO.pageOverFacet( (label, count) => {
        facetWriter.write(label)
        facetWriter.write("\n")
        true
      }, facetField, "*:*")
      facetWriter.close

      var counter = 0
      val csvReader = new CSVReader(new FileReader(facetOutputFile), '\t')
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
        }, fieldsToExport, facetField + ":\""+taxonID+"\"")

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
