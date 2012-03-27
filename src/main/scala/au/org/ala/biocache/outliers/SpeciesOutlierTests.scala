package au.org.ala.biocache.outliers
import java.net.URL
import au.com.bytecode.opencsv.CSVReader
import org.ala.layers.intersect.Grid
import au.org.ala.biocache.{Config, Json, QualityAssertion, AssertionCodes}
import collection.mutable.{HashMap, ListBuffer}
import org.codehaus.jackson.map.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.io.{FileWriter, FileOutputStream, File, FileReader}
import au.org.ala.util.{IndexRecords, OptionParser, FileHelper}
import au.org.ala.biocache.outliers.SampledRecord

class Timings {

  val startTime = System.currentTimeMillis()
  var lapTime = startTime

  def checkpoint(comment:String) {
    val now = System.currentTimeMillis()
    println("Time taken for ["+comment+"] : " + ((now - lapTime).toFloat / 1000.0f) + " seconds.")
    lapTime = now
  }
}

/**
 * Runnable for testing for outliers.
 *
 * TODO write the results to the DB
 */
object SpeciesOutlierTests {

  import FileHelper._
  val mandatoryHeaders = List("taxonConceptID", "uuid", "decimalLatitude", "decimalLongitude")
  val envLayerMap = Map(
    "el882" -> "/data/ala/data/layers/ready/diva/bioclim_bio15",
    "el889" -> "/data/ala/data/layers/ready/diva/bioclim_bio17",
    "el887" -> "/data/ala/data/layers/ready/diva/bioclim_bio23",
    "el865" -> "/data/ala/data/layers/ready/diva/bioclim_bio26",
    "el894" -> "/data/ala/data/layers/ready/diva/bioclim_bio32"
  )

  def main(args: Array[String]) {
    
    var taxonID:String = ""
    var taxonIDsFilepath:String = ""
    var fullDumpFilePath:String = ""
    var headerForDumpFile:List[String] = List()
    var idsToIndexFile = "/tmp/idsToReIndex.txt"

    val parser = new OptionParser("Test for outliers") {
      opt("t", "taxonID","The LSID of the species to check for outliers. This wll download from LIVE", {v:String => taxonID = v})
      opt("f","taxonIDsFilepath", "Filepath to taxon IDs. This will perform downloads for each taxonID", {v:String => taxonIDsFilepath = v})
      opt("fd","fullDumpFile", "Filepath to full extract of data", {v:String => fullDumpFilePath = v})
      opt("hfd","headerForDumpFile", "The header for the dump file, space separated headings", {
        v:String => headerForDumpFile = v.split(' ').toList
      })
      opt("if", "indexFilePath", "Filepath to file of IDs to reindex", {v:String => idsToIndexFile = v})
    }
    if(parser.parse(args)){
      if (taxonID != "") testForOutliers(taxonID)
      else if (taxonIDsFilepath != "") scala.io.Source.fromFile(taxonIDsFilepath).getLines().foreach(line => testForOutliers(taxonID))
      else if (fullDumpFilePath != "") {
        if (headerForDumpFile != null){
          runOutlierTestingForDumpFile(fullDumpFilePath, headerForDumpFile, idsToIndexFile)
        } else {
          runOutlierTestingForDumpFile(fullDumpFilePath)
        }
      }
      else parser.showUsage
    }

    //shutdown
    println("Shutting down indexing...")
    Config.indexDAO.shutdown
    println("Shutting down cassandra connection...")
    Config.persistenceManager.shutdown
    println("Finished.")
  }

  /**
   *
   * @param dumpFilePath
   * @param columnHeaders
   */
  def runOutlierTestingForDumpFile(dumpFilePath:String, columnHeaders:List[String] = List(), idsToIndexFile:String = "/tmp/idsToReIndex.txt"){

    val uuidIndexFile = new File(idsToIndexFile)
    val idsWriter = new FileWriter(uuidIndexFile)
    val reader:CSVReader = new CSVReader(new FileReader(dumpFilePath), '\t')
    val headers = if (columnHeaders.isEmpty) reader.readNext.toList else columnHeaders
      
    if (!mandatoryHeaders.forall(headers.contains(_))){
      throw new RuntimeException("Missing mandatory headers " + mandatoryHeaders.mkString(",") +
        ", Got: " + headers.mkString(","))
    }
    
    //get a list of variables
    val variables = headers.filter { !mandatoryHeaders.contains(_) }

    //iterate through file
    var finished = false
    var nextTaxonConceptID = ""
    val timings = new Timings
    var lastLine = Array[String]()
    
    while(!finished){

      val (taxonConceptID, lines, nextLine) = readAllForTaxon(reader,nextTaxonConceptID,lastLine)
      lastLine = nextLine

      println(taxonConceptID + ", records: " + lines.size)
      println(lines.head.mkString(","))

      val resultsBuffer = new ListBuffer[(String,Seq[SampledRecord],JackKnifeStats)]

        //run jacknife for each variable
      variables.foreach(variable => {
        //println("Testing with variable name: " + variable)
        
        //load the grid file
        val grid = new Grid(envLayerMap(variable))
        //println(headers)
        //get the column Idx for variable
        val variableIdx = headers.indexOf(variable)
        //println(headers.indexOf(variable))
        val latitudeIdx = headers.indexOf("decimalLatitude")
        val longitudeIdx = headers.indexOf("decimalLongitude")
        val pointBuffer = new ListBuffer[SampledRecord]

        //create a set of points
        lines.foreach(line => {
          val variableValue = line(variableIdx - 1)
          val latitude = line(latitudeIdx - 1)
          val longitude = line(longitudeIdx - 1)
          if (variableValue != "" && variableValue != null && latitude != "" && longitude != ""){
            val cellId = getCellId(grid, latitude.toFloat, longitude.toFloat)
            pointBuffer += SampledRecord(line(0), variableValue.toFloat, cellId)
          }
        })

        //we gots points - lets run that mofo
        val (recordsIDs, stats) = performJacknife(pointBuffer)
        if (!stats.isEmpty){
          resultsBuffer += ((variable, recordsIDs, stats.get))
          recordsIDs.foreach { x => idsWriter.write(x.id); idsWriter.write("\n") }
          idsWriter.flush
        }

        //println("Time taken for [Jacknife]: " + (now - startTime)/1000)
        timings checkpoint "jacknife with " + variable
        
        println(">>> For layer: " + variable + ", we've detected: " + recordsIDs.length + " outliers out of " + lines.size+ " records tested.")

        if(recordsIDs.length  >  lines.size){
          println(">>> records: " + recordsIDs.length + ", distinct values: " + recordsIDs.toSet.size)

          throw new RuntimeException("Error in processing")
        }

      })

      //store the results for this taxon
      storeResultsWithStats(taxonConceptID, resultsBuffer)


      if (nextLine == null) finished = true
      else nextTaxonConceptID = nextLine.head
    }

    idsWriter.close

    //reindex the records that have been marked as outliers
    println("Starting the indexing of marked records....")
    IndexRecords.indexListOfUUIDs(uuidIndexFile)
    println("Finished the indexing of marked records.")
  }

  /**
   * Reads all the records for the supplied taxonConceptID
   * 
   * @param reader
   * @param taxonConceptID
   * @return
   */
  def readAllForTaxon(reader:CSVReader, taxonConceptID:String, lastLine:Array[String] = Array()): (String,  Seq[Array[String]], Array[String]) = {

    var currentLine:Array[String] = reader.readNext()
    val idForBatch:String = if (taxonConceptID != ""){
      taxonConceptID
    } else {
      currentLine.head
    }
    
    println("###### Running for:" + idForBatch)

    val buffer = new ListBuffer[Array[String]]
    if (!lastLine.isEmpty) buffer += lastLine.tail

    while(currentLine !=null && currentLine.head == idForBatch){
      buffer += currentLine.tail
      currentLine = reader.readNext()
    }
    
    (idForBatch, buffer, currentLine)
  }

  def testForOutliers(lsid:String){
    val variablesToTest = Array("el889", "el882", "el887", "el865", "el894")
    val requiredFields = Array("id", "latitude", "longitude") ++ variablesToTest

    val u = new URL("http://biocache.ala.org.au/ws/webportal/occurrences.gz?pageSize=3000000&q=lsid:\"" + lsid + "\"&fl=" + requiredFields.mkString(","))

    val in = u.openStream
    val file = new File("/tmp/occurrences.gz")
    val out = new FileOutputStream(file)
    val buffer: Array[Byte] = new Array[Byte](1024)
    var numRead = 0
    while ({ numRead = in.read(buffer); numRead != -1 }) {
      out.write(buffer, 0, numRead)
      out.flush
    }
    out.close()
    printf("\nDownloaded. File size: ", file.length() / 1024 + "kB, " + file.getAbsolutePath)

    //decompress
    val extractedFile = file.extractGzip

    //outlier values for each layer
    val outlierValues: Seq[(String, Seq[SampledRecord])] = variablesToTest.map(variable => {
     // println("************ test with " + variable)
      val (list,stats) = performJacknife(variable, extractedFile)
      variable -> list
    })

    //create the inverse e.g UUID -> layer1, layer2
    //store this
    val record2Layer: Map[SampledRecord, String] = invertLayer2Record(outlierValues)

    storeResults(lsid, record2Layer)

    val recordLayerCounts = filterOutliersForXLayers(outlierValues).toList
//
//    val outlier5 = recordLayerCounts.filter(x => x._2 == 5).map(x => x._1).toList.sorted
//    val outlier4 = recordLayerCounts.filter(x => x._2 == 4).map(x => x._1).toList.sorted
//    val outlier3 = recordLayerCounts.filter(x => x._2 == 3).map(x => x._1).toList.sorted
//    val outlier2 = recordLayerCounts.filter(x => x._2 == 2).map(x => x._1).toList.sorted
//    val outlier1 = recordLayerCounts.filter(x => x._2 == 1).map(x => x._1).toList.sorted
//
//    printLinks(5, outlier5)
//    printLinks(4, outlier4)
//    printLinks(3, outlier3)
//    printLinks(2, outlier2)
//    printLinks(1, outlier1)
  }

  def storeResultsWithStats(taxonID:String, results:Seq[(String, Seq[SampledRecord], JackKnifeStats)] ){

    val mapper = new ObjectMapper
    mapper.registerModule(DefaultScalaModule)

   // println("Storing results for : " + taxonID)

    val jackKnifeStatsMap = results.map(x => x._1 -> x._3).toMap[String, JackKnifeStats]

    //results.foreach ( x => {
      //Config.persistenceManager.put(taxonID, "outliers", x._1, Json.toJSON(x._3))
    //})

    Config.persistenceManager.put(taxonID, "outliers", "jackKnifeStats", mapper.writeValueAsString(jackKnifeStatsMap))

    //recordUUID -> list of layers
    val variableResults = results.map(x => (x._1, x._2))
    val record2OutlierLayer:Map[SampledRecord, String] = invertLayer2Record(variableResults)

    val previous = {
      try {
        Config.persistenceManager.get(taxonID, "outliers", "jackKnifeOutliers")
      } catch {
        case _ => None
      }
    }

    if (!previous.isEmpty) Config.persistenceManager.put(taxonID, "outliers", "previous", previous.get)
    
    //mark up records
    Config.persistenceManager.put(taxonID, "outliers", "jackKnifeOutliers", mapper.writeValueAsString(variableResults))

    val recordStats = record2OutlierLayer.map(x => {
      val (sampledRecord, layerId) = (x._1, x._2)
      //lookup stats for this record
      val jackKnifeStats = jackKnifeStatsMap.get(layerId).get
      //need to get the value for this record
      RecordJackKnifeStats(sampledRecord.id,
        layerId,
        sampledRecord.value,
        jackKnifeStats.sampleSize,
        jackKnifeStats.min,
        jackKnifeStats.max,
        jackKnifeStats.mean,
        jackKnifeStats.stdDev,
        jackKnifeStats.range,
        jackKnifeStats.threshold,
        jackKnifeStats.outlierValues)
    })

    recordStats.groupBy(_.uuid).foreach(x => {
      val rowKey = Config.occurrenceDAO.getRowKeyFromUuid(x._1)
      if (!rowKey.isEmpty){
        val layerIds = x._2.map(_.layerId).toList
        Config.persistenceManager.put(rowKey.get, "occ", "outlierForLayers.p", mapper.writeValueAsString(layerIds))
        Config.occurrenceDAO.addSystemAssertion(rowKey.get, QualityAssertion(AssertionCodes.DETECTED_OUTLIER, "Outlier for " + x._2.size + " layers"))
        Config.persistenceManager.put(x._1, "occ_outliers", "jackKnife", mapper.writeValueAsString(x._2))
      } else {
        println("Row key lookup failed for : "  + x._1)
      }
    })

    //reset the records that are no longer considered outliers -
    //do an ID diff between the results
    val previousIDs = {
      //mapper.readValue(previous.getOrElse("{}"), classOf[Map[String, List[String]]]).keys
      List[String]()
    }
    val currentIDs = record2OutlierLayer.keys

    //IDs in the previous not in current need to be reset
    val newIDs = previousIDs.toList diff currentIDs.toList
    
    println("[WARNING] Number of old IDs not marked as outliers anymore: " + newIDs.size)

    newIDs.foreach(recordID =>  {
      val rowKey = Config.occurrenceDAO.getRowKeyFromUuid(recordID)
      if (!rowKey.isEmpty){
        Config.persistenceManager.deleteColumns(rowKey.get, "occ", "outlierForLayers.p")
        //remove the system assertions
        Config.occurrenceDAO.removeSystemAssertion(rowKey.get, AssertionCodes.DETECTED_OUTLIER)
      }
    })
  }

  def storeResults(taxonID:String, record2Layers:Map[SampledRecord, String] ){
  //  println("Storing results for : " + taxonID)
    Config.persistenceManager.put(taxonID, "outliers", "current", record2Layers.toString)
  }

  def invertLayer2Record(layer2Record: Seq[(String, Seq[SampledRecord])]) : Map[SampledRecord, String] = {
    //group by recordUuid
    val record2Layer = new HashMap[SampledRecord, String]
    
    layer2Record.foreach( layer2Record => {
      val (layerId, records) = (layer2Record._1, layer2Record._2)
      records.foreach(record => {
        record2Layer.put(record,layerId)
      })
    })

    record2Layer.toMap
  }

  def printLinks(count: Int, records: Seq[String]) {
    println()
    println("************************ outlier for : " + count + " ******************")
    records.foreach(c => println("http://biocache.ala.org.au/occurrences/" + c))
    println("************************ end of outlier for : " + count + " ******************")
    println()
  }

  def filterOutliersForXLayers(outliersMap: Seq[(String, Seq[SampledRecord])]): Seq[(SampledRecord, Int)] = {
    //convert the map into a map keyed on recordID
    val recordIds = outliersMap.map(a => a._2).flatten.toList.distinct
    val counts = Array.fill(recordIds.size)(0)
    outliersMap.foreach(layerId2Records => {
      layerId2Records._2.foreach(record => {
        val recordIdx = recordIds.indexOf(record)
        counts(recordIdx) = counts(recordIdx) + 1
      })
    })
    recordIds zip counts
  }

  /**
   * Returns a list of UUIDs for records which are outliers.
   *
   * @param variableName
   * @param file
   * @return
   */
  def performJacknife(variableName: String, file: File): (Seq[SampledRecord],Option[JackKnifeStats]) = {
    val r = new CSVReader(new FileReader(file))
    if (r.readNext() == null) return (List[SampledRecord](), None)
    val headers = r.readNext().toList
    val grid = new Grid(envLayerMap(variableName))

    val points = new ListBuffer[SampledRecord]
    var line = r.readNext()
    while (line != null) {

      val fields = (headers zip line).toMap

      //println(fields)
      if (fields.contains("latitude") && fields("latitude") != "" && fields.getOrElse(variableName, "") != "") {
        val id = fields("id")
        val x = fields("longitude").toFloat
        val y = fields("latitude").toFloat
        val elValue = fields(variableName).toFloat
        val cellId = getCellId(x.toDouble, y.toDouble, grid.xmin, grid.xmax, grid.xres, grid.ymin, grid.ymax, grid.yres, grid.nrows, grid.ncols)

        points += SampledRecord(id,elValue,cellId)
      }
      line = r.readNext()
    }
    r.close

    performJacknife(points)
  }

  /**
   * Run jacknife on these points, returning the IDs of the records that are outliers.
   *
   * @param points
   * @return a list of UUIDs for records that are outliers
   */
  def performJacknife(points:Seq[SampledRecord]) : (Seq[SampledRecord], Option[JackKnifeStats]) = {
    
    //we have a points, group by on cellId as we only want to sample once per cell
    val pointsGroupedByCell = points.groupBy(p => p.cellId)

    //create a cell -> value map
    val cellToValue = pointsGroupedByCell.map(g => g._1 -> g._2.head.value).toMap[Int, Float]

    //create a value -> cell map    
    val valuesToCells = cellToValue.groupBy(x => x._2).map(x => x._1 -> x._2.keys.toSet[Int]).toMap

    //the environmental properties to throw at Jackknife test
    val valuesToTest = cellToValue.values.filter(x => x != "").map(y => y.toFloat).toList

    //do jacknife test
    val jacknife = new JackKnife

    //FIXME - this is adding the same record to the buffer more than once....
    jacknife.jackknife(valuesToTest) match {
      case Some(stats) => {
        val outliers = new ListBuffer[SampledRecord]
        stats.outlierValues.foreach(x => {
          //get the cell
          val cellIds = valuesToCells.getOrElse(x, Set())
          cellIds.foreach(cellId => {
            val points = pointsGroupedByCell.get(cellId).get
            points.foreach(point => outliers += point)
          })
        })
        (outliers.distinct, Some(stats))
      }
      case None => (List(), None)
    }
  }

  def getCellId(grid:Grid, latitude:Float, longitude:Float): Int = {
    getCellId(longitude.toDouble, latitude.toDouble, grid.xmin, grid.xmax, grid.xres, grid.ymin, grid.ymax, grid.yres, grid.nrows, grid.ncols)
  }

  /**
   * Get a cell id
   *
   * @param fid e.g. el882
   * @param latitude
   * @param longitude
   * @return
   */
  def getCellId(fid:String, latitude:Float, longitude:Float): Int = {
    val grid = new Grid(envLayerMap(fid))
    getCellId(longitude.toDouble, latitude.toDouble, grid.xmin, grid.xmax, grid.xres, grid.ymin, grid.ymax, grid.yres, grid.nrows, grid.ncols)
  }

  /**
   * Retrieves a cell id within a grid file.
   */
  def getCellId(x: Double, y: Double, xmin: Double, xmax: Double, xres: Double, ymin: Double, ymax: Double, yres: Double, nrows: Int, ncols: Int): Int = {
    //handle invalid inputs
    if (x < xmin || x > xmax || y < ymin || y > ymax) {
      -1
    } else {

      var col = ((x - xmin) / xres).toInt
      var row = nrows - 1 - ((y - ymin) / yres).toInt

      //limit each to 0 and ncols-1/nrows-1
      if (col < 0) {
        col = 0
      }
      if (row < 0) {
        row = 0
      }
      if (col >= ncols) {
        col = ncols - 1;
      }
      if (row >= nrows) {
        row = nrows - 1;
      }

      row * ncols + col
    }
  }
}
