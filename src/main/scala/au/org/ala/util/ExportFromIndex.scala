package au.org.ala.util

import au.org.ala.biocache.Config
import au.com.bytecode.opencsv.CSVReader
import java.io.{FileReader, File, FileWriter}
import org.apache.commons.lang3.time.DateUtils
import java.util.Date
import java.text.SimpleDateFormat
import java.util.concurrent.ArrayBlockingQueue
import org.apache.commons.io.FileUtils

object ExportAllRecordFacetFilter {
  import FileHelper._
  def main(args:Array[String]){
    var exportDirectory = "/data/offline/exports"
    var facet=""
    var threads = 4
    var filter:Option[String]=None
    val fieldsToExport= Array("row_key", "id", "species_guid","subspecies_guid", "year", "month", "occurrence_date", "point-1", "point-0.1",
      "point-0.01","point-0.001", "point-0.0001","lat_long","raw_taxon_name", "collectors", "duplicate_status", "duplicate_record", "latitude","longitude",
      "el882","el889","el887","el865","el894")
    val parser = new OptionParser("Export based on facet and optional filter") {
      arg("<output directory>", "the output directory for the exports", {v:String => exportDirectory = v})
      arg("<facet>", "The facet to base the download",{v:String => facet =v})
      intOpt("t","threads","the number of threads/files to have for the exports", {v:Int => threads =v})
      opt("f","filter", "optional filter to apply to the list", {v:String => filter = Some(v)})
    }
    //last_load_date:["+lastRunDate.get+" TO *]
    if (parser.parse(args)){
      val filename = exportDirectory+ File.separator + "species-guids.txt"
      FileUtils.forceMkdir(new File(exportDirectory))
      val args2 = if(filter.isDefined) Array(facet,filename, "-fq",filter.get,"--open") else Array(facet,filename,"--open")
      println(new Date() + " Exporting the facets to be ued in the download")
      ExportFacet.main(args2)
      //now based on the number of threads download the other data
      val queue = new ArrayBlockingQueue[String](100)
      var ids=0
      val pool:Array[Thread] = Array.fill(threads){
        val file = new File(exportDirectory +  File.separator + ids + File.separator+"species.out")
        val subspeciesfile = new File(exportDirectory +  File.separator + ids + File.separator+"subspecies.out")
        FileUtils.forceMkdir(file.getParentFile)
        val fileWriter = new FileWriter(file)
        val p=new StringConsumer(queue,ids,{lsid =>
          DuplicationDetection.logger.info("Starting to download the occurrences for " + lsid)
          ExportByFacetQuery.downloadSingleTaxonByStream(lsid, fieldsToExport ,"species_guid",Array("lat_long:[* TO *]"),Array("subspecies_guid","row_key"), fileWriter, Some(new FileWriter(subspeciesfile)), Some(Array("duplicate_record")))
          fileWriter.flush()
        })
        ids += 1
        p.start
        p
      }


      //add to the queue
      new File(filename).foreachLine(line => queue.put(line.trim))

      pool.foreach(t =>if(t.isInstanceOf[StringConsumer]) t.asInstanceOf[StringConsumer].shouldStop = true)
      pool.foreach(_.join)

    }
  }
}

object ExportFromIndex {

  def main(args:Array[String]){

    var outputFilePath = ""
    var query = "*:*"
    var fieldsToExport = Array[String]()
    var counter = 0
    
    val parser = new OptionParser("load flickr resource") {
      arg("<output-file>", "The UID of the data resource to load", { v: String => outputFilePath = v })
      arg("<list of fields>", "The UID of the data resource to load", { v: String => fieldsToExport = v.split(" ").toArray })
      opt("q","query", "The SOLR query to use", { v: String => query = v })
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
      }, fieldsToExport, query, Array())
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
  var includeCounts = false
  var indexDateField = "first_loaded_date"
  var closeIndex = true

  var fieldsToExport = Array[String]()

  val parser = new OptionParser("ExportFacet - Exports a facet to file") {
    arg("<facet-field>", "The field to facet on", { v: String => facetField = v })
    arg("<facet-output-file>", "The field to facet on", { v: String => facetOutputFile = v })
    opt("fq","filter query", "Filter query to use", { v: String => facetQuery = v })
    opt("open","Keep the index open",{closeIndex=false})
    booleanOpt("ld","lastDay", "Only export those that have had new records in the last day", { v: Boolean => lastDay = v })
    booleanOpt("ld","lastWeek", "Only export those that have had new records in the last week", { v: Boolean => lastWeek = v })
    booleanOpt("ld","lastMonth", "Only export those that have had new records in the last month", { v: Boolean => lastMonth = v })
    booleanOpt("c","incCounts", "Include the counts of the facet", { v: Boolean => includeCounts = v })
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
        if(includeCounts)
          facetWriter.write("\t" + count)
        facetWriter.write("\n")
        facetWriter.flush
        true
      }, facetField, facetQuery, Array(facetFilterQuery))
      facetWriter.flush
      facetWriter.close
      if(closeIndex)
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

  def downloadSingleTaxonByStream(taxonID:String, fieldsToExport:Array[String], facetField:String, filterQueries:Array[String],sortFields:Array[String], fileWriter:FileWriter, subspeciesWriter:Option[FileWriter]=None, multivaluedFields:Option[Array[String]]=None){
    Config.indexDAO.streamIndex(map => {
      val outputLine = fieldsToExport.map(f => getFromMap(map,f)).mkString("\t")
      fileWriter.write(outputLine)
      fileWriter.write("\n")
      if (subspeciesWriter.isDefined){
        val subspecies = map.get("subspecies_guid")
        if (subspecies != null) {
          subspeciesWriter.get.write(outputLine)
          subspeciesWriter.get.write("\n")
        }

      }
      true
    }, fieldsToExport, facetField + ":\""+taxonID+"\"", filterQueries, sortFields,multivaluedFields)
  }

  def downloadSingleTaxon(taxonID:String, fieldsToExport:Array[String], facetField:String, filterQueries:Array[String],sortField:Option[String]=None, sortDir:Option[String]=None, fileWriter:FileWriter, multivaluedFields:Option[Array[String]]=None){    
    var counter =0
    Config.indexDAO.pageOverIndex(map  => {
          counter += 1
          if (counter % 1000 == 0) { println("Exported :" + counter); fileWriter.flush; }
          val outputLine = fieldsToExport.map(f => getFromMap(map,f))
          fileWriter.write(outputLine.mkString("\t"))
          fileWriter.write("\n")
          true
        }, fieldsToExport, facetField + ":\""+taxonID+"\"", filterQueries, sortField,sortDir,multivaluedFields)

       fileWriter.flush
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
