package au.org.ala.biocache.outliers
import java.net.URL
import au.com.bytecode.opencsv.CSVReader
import org.ala.layers.intersect.Grid
import au.org.ala.biocache.{Config, Json, QualityAssertion, AssertionCodes}
import collection.mutable.{ArrayBuffer, HashMap, ListBuffer}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.{FileWriter, FileOutputStream, File, FileReader}
import au.org.ala.util.{StringConsumer, IndexRecords, OptionParser, FileHelper}
import org.slf4j.LoggerFactory
import collection.mutable
import java.util.concurrent.ArrayBlockingQueue
import org.apache.commons.lang3.time.DateUtils
import java.util.Date
import java.text.SimpleDateFormat

class Timings {

  val startTime = System.currentTimeMillis()
  var lapTime = startTime

  def checkpoint(comment:String) {
    val now = System.currentTimeMillis()
    println("Time taken for [%s] : %f seconds.".format(comment, ((now - lapTime).toFloat / 1000.0f)))
    lapTime = now
  }
}

/**
 * Runnable for testing for outliers.
 */
object SpeciesOutlierTests {

  import FileHelper._
  val logger = LoggerFactory.getLogger("SpeciesOutlierTests")
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
    var headerForDumpFile:List[String] = List("taxonConceptID","uuid","decimalLatitude","decimalLongitude","el882","el889","el887","el865","el894")
    var idsToIndexFile = "/tmp/idsToReIndex.txt"
    var persistResults = false
    var numPassThreadWriters=16
    var numSourceThreads =4
    var taxonRank="species"
    var index=false
    var lastModifiedDate:Option[String]=None

    val parser = new OptionParser("Reverse jackknife for outliers") {
      opt("t", "taxonID","The LSID of the species to check for outliers. This wll download from LIVE", {v:String => taxonID = v})
      opt("f","taxonIDsFilepath", "Filepath to taxon IDs. This will perform downloads for each taxonID", {v:String => taxonIDsFilepath = v})
      opt("fd","fullDumpFile", "Filepath to full extract of data", {v:String => fullDumpFilePath = v})
      opt("hfd","headerForDumpFile", "The header for the dump file, space separated headings. Default is: 'taxonConceptID uuid decimalLatitude decimalLongitude el882 el889 el887 el865 el894'", {
        v:String => headerForDumpFile = v.split(' ').toList
      })
      opt("type","The type either species or subspecies", {v:String => taxonRank = v})
      intOpt("pt","passThreads","The number of threads to use when writing the passed records to the data store", {v:Int => numPassThreadWriters = v})
      intOpt("st","sourceThreads", "The number of threads that were used to create the source files.", {v:Int => numSourceThreads = v})
      opt("if", "indexFilePath", "Filepath to file of IDs to reindex", {v:String => idsToIndexFile = v})
      opt("persist","persist results to the database",{persistResults = true})
      opt("index", "index the records that are marked as outliers", {index =true})
      intOpt("day","numDaysMod","Number of days since the last modified.  This will limit the records that are marked as passed.", { v:Int=>
          val sfd = new SimpleDateFormat("yyyy-MM-dd")
          val days:Int = 0 -v
          lastModifiedDate = Some(sfd.format(DateUtils.addDays(new Date(), days)) + "T00:00:00Z")
      })
    }
    if(parser.parse(args)){
      //set up threads
      val queue = new ArrayBlockingQueue[String](200000)
      var ids =0
      val pool:Array[StringConsumer] = Array.fill(numPassThreadWriters){
        var counter=0
        var startTime = System.currentTimeMillis
        var finishTime = System.currentTimeMillis
        val p = new StringConsumer(queue,ids,{uuid =>
          counter +=1

          //add the details for the records that have passed
          try{
            val rowKey = Config.occurrenceDAO.getRowKeyFromUuid(uuid)
            if(rowKey.isDefined){
              //mark the test as passed
              Config.occurrenceDAO.addSystemAssertion(rowKey.get,QualityAssertion(AssertionCodes.DETECTED_OUTLIER, 1))
            }
          } catch{
            case e:Exception => logger.error("Unable to markup " + uuid + " as passed. " + e.getMessage)
          }

          if (counter % 1000 == 0) {
            finishTime = System.currentTimeMillis
            println(counter + " >> Last key : " + uuid + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
            startTime = System.currentTimeMillis
          }
        });ids +=1;p.start;p }


      if (taxonID != "") testForOutliers(taxonID, persistResults)
      else if (taxonIDsFilepath != "") scala.io.Source.fromFile(taxonIDsFilepath).getLines().foreach(line => testForOutliers(taxonID))
      else if (fullDumpFilePath != "") {
        val file = new File(fullDumpFilePath)
        if (file.isDirectory){
          var dids = 0
          //set the threads up to detect the species outliers.
          val detectpool:Array[Thread] = Array.fill(numSourceThreads){
            val dumpFilePath = fullDumpFilePath + File.separator + dids + File.separator + taxonRank + ".out"
            val reindexFile =  idsToIndexFile + "." + dids
            val passFile = fullDumpFilePath + File.separator + dids + File.separator +"layer" + taxonRank + "-outlier-passed.out"
            val t = new Thread(){
              override def run(){
                 runOutlierTestingForDumpFile(dumpFilePath, headerForDumpFile, reindexFile , persistResults, queue, index, lastModifiedDate, taxonRank + "_guid", passFile)
              }
            }

            dids +=1
            t.start
            t
          }
          detectpool.foreach(_.join)

        } else {
          runOutlierTestingForDumpFile(fullDumpFilePath, headerForDumpFile, idsToIndexFile, persistResults,queue, index,lastModifiedDate, taxonRank+"_guid")
        }
      }
      else parser.showUsage
      //stop the threads that are adding the passed outlier information
      pool.foreach(t =>t.shouldStop = true)
      pool.foreach(_.join)
    }

    //shutdown
    logger.debug("Shutting down indexing...")
    Config.indexDAO.shutdown
    logger.debug("Shutting down cassandra connection...")
    Config.persistenceManager.shutdown
    logger.debug("Finished.")
  }

  /**
   *
   * @param dumpFilePath
   * @param columnHeaders
   */
  def runOutlierTestingForDumpFile(dumpFilePath:String, columnHeaders:List[String] = List(), 
      idsToIndexFile:String = "/tmp/idsToReIndex.txt", persistResults:Boolean=false, queue:ArrayBlockingQueue[String], index:Boolean=false, lastModifiedDate:Option[String]=None , field:String="species_guid",
      passFile:String ="/tmp/layer-outlier-pass.out"){
    //only continue if the filesize>0
    if(new File(dumpFilePath).length()>0L){
      val uuidIndexFile = new File(idsToIndexFile)
      val idsWriter = new FileWriter(uuidIndexFile)
      val passWriter = new FileWriter(passFile)
      val reader:CSVReader = new CSVReader(new FileReader(dumpFilePath), '\t', '~')

      val headers = if (columnHeaders.isEmpty) reader.readNext.toList else columnHeaders

      if (!mandatoryHeaders.forall(headers.contains(_))){
        throw new RuntimeException("Missing mandatory headers " + mandatoryHeaders.mkString(",") +
          ", Got: " + headers.mkString(","))
      }

      //get a list of variables
      val variables = headers.filter { _.startsWith("el") }// !mandatoryHeaders.contains(_) }

      //iterate through file
      var finished = false
      var nextTaxonConceptID = ""
      val timings = new Timings
      var lastLine = Array[String]()

      //the index of decimalLat and long will not change
      val latitudeIdx = headers.indexOf("decimalLatitude")
      val longitudeIdx = headers.indexOf("decimalLongitude")
      val taxonIDIdx = headers.indexOf("taxonConceptID")
      val uuidIdx = headers.indexOf("uuid")
      val rowKeyIdx =  headers.indexOf("rowKey")

      while(!finished){

        val (taxonConceptID, lines, nextLine) = readAllForTaxon(reader,nextTaxonConceptID, taxonIDIdx,lastLine)
        lastLine = nextLine

        logger.info(taxonConceptID + ", records: " + lines.size)
        logger.debug(lines.head.mkString(","))

        val resultsBuffer = new ListBuffer[(String,Seq[SampledRecord],JackKnifeStats)]
        val recordIds = new ListBuffer[String]
        var loadedIds = false
          //run jacknife for each variable
        variables.foreach(variable => {
          //println("Testing with variable name: " + variable)

          //load the grid file
          val grid = new Grid(envLayerMap(variable))
          //println(headers)
          //get the column Idx for variable
          val variableIdx = headers.indexOf(variable)
          //println(headers.indexOf(variable))

          val pointBuffer = new ListBuffer[SampledRecord]

          //create a set of points
          lines.foreach(line => {
            val variableValue = line(variableIdx)
            val latitude = line(latitudeIdx )
            val longitude = line(longitudeIdx)
            if (!loadedIds){
              recordIds += line(rowKeyIdx)
            }
            if (variableValue != "" && variableValue != null && latitude != "" && longitude != ""){
              val cellId = getCellId(grid, latitude.toFloat, longitude.toFloat)
              pointBuffer += SampledRecord(line(uuidIdx), variableValue.toFloat, cellId, Some(line(rowKeyIdx)))
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

          if(logger.isDebugEnabled()){
            logger.debug(">>> For layer: %s we've detected: %d outliers out of %d records tested.".format(variable, recordsIDs.length, lines.size))
          }

          if(recordsIDs.length  >  lines.size){
            if(logger.isDebugEnabled()){
            logger.debug(">>> records: %d, distinct values: %d".format(recordsIDs.length, recordsIDs.toSet.size))
            }
            throw new RuntimeException("Error in processing")
          }
          loadedIds = true
        })


        //now identify the records that were not outlier but were tested.
        //If outliers were NOT testsed the resutlsBuffer will be empty.
        val passedRecords = {
          if(resultsBuffer.length>0){
            recordIds.toSet &~ resultsBuffer.map{_._2}.foldLeft(ListBuffer[SampledRecord]())(_ ++ _).map(v=>v.rowKey.getOrElse(v.id)).toSet
          }
          else{
            Set[String]()
          }
        }


        logger.info("The records that passed " + passedRecords.size + " " + recordIds.size)
        //store the results for this taxon
        storeResultsWithStats(taxonConceptID, resultsBuffer, passedRecords,idsWriter, queue,lastModifiedDate,field,passWriter)


        if (nextLine == null) finished = true
        else nextTaxonConceptID = nextLine(taxonIDIdx)
      }
      //logger.debug("The records that passed " + passedRecords.size + " " + recordIds.size)
      //store the results for this taxon

      idsWriter.close
      passWriter.flush
      passWriter.close

      //reindex the records that have been marked as outliers
      if(index){
        logger.debug("Starting the indexing of marked records....")
        IndexRecords.indexListOfUUIDs(uuidIndexFile)
        logger.debug("Finished the indexing of marked records.")
      }
    }
  }

  /**
   * Reads all the records for the supplied taxonConceptID
   * 
   * @param reader
   * @param taxonConceptID
   * @return
   */
  def readAllForTaxon(reader:CSVReader, taxonConceptID:String, taxonConceptIDIdx:Int, lastLine:Array[String] = Array()): (String,  Seq[Array[String]], Array[String]) = {

    var currentLine:Array[String] = reader.readNext()
    val idForBatch:String = if (taxonConceptID != ""){
      taxonConceptID
    } else {
      currentLine(taxonConceptIDIdx)
    }
    
    logger.debug("###### Running for:" + idForBatch)

    val buffer = new ListBuffer[Array[String]]
    if (!lastLine.isEmpty) buffer += lastLine

    while(currentLine !=null && currentLine(taxonConceptIDIdx) == idForBatch){
      buffer += currentLine
      currentLine = reader.readNext()
    }
    
    (idForBatch, buffer, currentLine)
  }

  /**
   * This is for adhoc one-off testing of outlier detection.
   * This method will dynamically download data from the biocache webservices
   * and run jack knife for a selected group of layers.
   *
   * @param lsid
   */
  def testForOutliers(lsid:String, persistResults:Boolean = false){
    val variablesToTest = Array("el889", "el882", "el887", "el865", "el894")
    val requiredFields = Array("id", "latitude", "longitude") ++ variablesToTest

    val u = new URL(Config.biocacheServiceURL + "/webportal/occurrences.gz?pageSize=3000000&q=lsid:\"" + 
        lsid + "\"&fl=" + requiredFields.mkString(","))

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
    printf("\nDownloaded. File size: %d kB, Path: %s\n", file.length() / 1024, file.getAbsolutePath)

    //decompress
    val extractedFile = file.extractGzip

    //outlier values for each layer
    val outlierValues: Seq[(String, Seq[SampledRecord])] = variablesToTest.map(variable => {
     // println("************ test with " + variable)
      val (list,stats) = performJacknife(variable, extractedFile, requiredFields)
      println("Variable: %s, Outliers: %d".format(variable,list.size))
      list.foreach(println(_))
      variable -> list
    })

    //create the inverse e.g UUID -> layer1, layer2
    val record2Layer:Seq[(SampledRecord, String)] = invertLayer2Record(outlierValues)

    //store this
    if(persistResults) storeResults(lsid, record2Layer)

    val recordLayerCounts = generateOutlierCounts(record2Layer)
    val outlier5 = recordLayerCounts.filter(x => x._2 == 5).map(x => x._1).toList
    val outlier4 = recordLayerCounts.filter(x => x._2 == 4).map(x => x._1).toList
    val outlier3 = recordLayerCounts.filter(x => x._2 == 3).map(x => x._1).toList
    val outlier2 = recordLayerCounts.filter(x => x._2 == 2).map(x => x._1).toList
    val outlier1 = recordLayerCounts.filter(x => x._2 == 1).map(x => x._1).toList

    printLinksForRecords(5, outlier5)
    printLinksForRecords(4, outlier4)
    printLinksForRecords(3, outlier3)
    printLinksForRecords(2, outlier2)
    printLinksForRecords(1, outlier1)
  }

  def storeResultsWithStats(taxonID:String, results:Seq[(String, Seq[SampledRecord], JackKnifeStats)], passed:Set[String], idsWriter:FileWriter, queue:ArrayBlockingQueue[String], lastModifiedDate:Option[String], field:String, passedWriter:FileWriter ){

    val mapper = new ObjectMapper
    mapper.registerModule(DefaultScalaModule)

    val jackKnifeStatsMap = results.map(x => x._1 -> x._3).toMap[String, JackKnifeStats]

    //store the outlier stats for this taxon
    Config.persistenceManager.put(taxonID, "outliers", "jackKnifeStats", mapper.writeValueAsString(jackKnifeStatsMap))

    //recordUUID -> list of layers
    val variableResults = results.map(x => (x._1, x._2))
    val record2OutlierLayer:Seq[(SampledRecord, String)] = invertLayer2Record(variableResults)

    val previous = {
      try {
        Config.persistenceManager.get(taxonID, "outliers", "jackKnifeOutliers")
      } catch {
        case _:Exception => None
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
        logger.debug("Row key lookup failed for : "  + x._1)
      }
    })

    //reset the records that are no longer considered outliers -
    //do an ID diff between the results
 //   println(mapper.readValue(previous.get, classOf[List[(String,List[SampledRecord])]]) + " " + mapper.readValue(previous.get, classOf[List[(String,List[SampledRecord])]]).getClass)
    val previousIDs = {
      if(previous.isDefined){
//        println("PREVIOUS: " +previous.get)
      //mapper.readValue(previous.getOrElse("{}"), classOf[Map[String, List[String]]]).keys
        val tmplist = new ListBuffer[String]
        val prelist:List[mutable.Buffer[AnyRef]] = mapper.readValue(previous.get, classOf[List[(String,List[SampledRecord])]]).asInstanceOf[List[mutable.Buffer[AnyRef]]]
        //println("prelist : "+ prelist)
        prelist.foreach(_.foreach { b1 =>
	      if(b1.isInstanceOf[mutable.Buffer[Map[String,String]]]){
	        b1.asInstanceOf[mutable.Buffer[scala.collection.convert.Wrappers$JMapWrapper]]
	          .foreach(v => tmplist += v.get("id").get.toString)
	      }
        })
        tmplist.toList
      } else {
        List[String]()
      }
    }
    logger.debug("previousIDs: " + previousIDs)
    val currentIDs = record2OutlierLayer.map(_._1.id)

    //IDs in the previous not in current need to be reset
    val newIDs = previousIDs diff currentIDs
    logger.debug("previous : " + previousIDs.size + " current " + currentIDs.size + " new : " + newIDs.size)

    logger.debug("[WARNING] Number of old IDs not marked as outliers anymore: " + newIDs.size)

    newIDs.foreach(recordID =>  {
      logger.debug("Record " + recordID +" is no longer an outlier")
      val rowKey = Config.occurrenceDAO.getRowKeyFromUuid(recordID)
      if (!rowKey.isEmpty){
        //add the uuid as one that is needing reindexing:
        idsWriter.write(recordID)
        idsWriter.write("\n")
        Config.persistenceManager.deleteColumns(rowKey.get, "occ", "outlierForLayers.p")
        //remove the system assertions
        //Config.occurrenceDAO.removeSystemAssertion(rowKey.get, AssertionCodes.DETECTED_OUTLIER)
        Config.occurrenceDAO.addSystemAssertion(rowKey.get,QualityAssertion(AssertionCodes.DETECTED_OUTLIER, 1),replaceExistCode = true)
      }
    })
    val rowkeys = if(lastModifiedDate.isDefined) Some(getRecordsChangedSince(lastModifiedDate.get, field, taxonID)) else None
    passed.foreach(rowKey=>{
      if(rowkeys.isEmpty || rowkeys.get.contains(rowKey)){
        passedWriter.write(rowKey + "\n")
      }
    })
    passedWriter.flush()

    //mark the records that have passed if they have been modified since the lastModifiedDate
    //NC this is taking too ;long just generate the files and have a background process handle the update slowly
//    val uuids = if(lastModifiedDate.isDefined) Some(getRecordsChangedSince(lastModifiedDate.get, field, taxonID)) else None
//    passed.foreach(uuid => {
//      if (uuids.isEmpty || uuids.get.contains(uuid)){
//        queue.put(uuid)
//      }
//      val rowKey = Config.occurrenceDAO.getRowKeyFromUuid(uuid)
//      if(rowKey.isDefined){
//        //mark the test as passed
//        Config.occurrenceDAO.addSystemAssertion(rowKey.get,QualityAssertion(AssertionCodes.DETECTED_OUTLIER, 1))
//      }
//    })


  }

  def getRecordsChangedSince(date:String, field:String, taxonId:String):List[String]={
    val buf =new ArrayBuffer[String]()
    Config.indexDAO.pageOverFacet((value,count)=>{
      buf += value
      true
    }, "row_key", field +":\"" + taxonId +"\"", Array("last_load_date:["+date + " TO *]"))
    buf.toList
  }

  def storeResults(taxonID:String, record2Layers:Seq[(SampledRecord, String)] ){
    Config.persistenceManager.put(taxonID, "outliers", "current", record2Layers.toString)
  }

  def invertLayer2Record(layer2Record: Seq[(String, Seq[SampledRecord])]) : Seq[(SampledRecord, String)] = {
    //group by recordUuid
    val record2Layer = new ListBuffer[(SampledRecord, String)]
    
    layer2Record.foreach(layer2Record => {
      val (layerId, records) = (layer2Record._1, layer2Record._2)
      records.foreach(r => record2Layer.append((r, layerId)))  //thi
    })

    record2Layer
  }

  def printLinksForRecords(count: Int, records: Seq[String]) {
    println()
    println("## outlier for : " + count + " ##")
    records.foreach(c => println(Config.biocacheServiceURL + "/occurrences/" + c))
    println("## end of outlier for : " + count + " ##")
    println()
  }

  def printLinks(count: Int, records: Seq[String]) {
    println()
    println("************************ outlier for : " + count + " ******************")
    records.foreach(c => println(Config.biocacheServiceURL + "/occurrences/" + c))
    println("************************ end of outlier for : " + count + " ******************")
    println()
  }

  def generateOutlierCounts(outliers:Seq[(SampledRecord, String)]): Seq[(String, Int)] = {
    //convert the map into a map keyed on recordID
    val recordIds = outliers.map(x => x._1.id).toSet.toArray
    val counts = Array.fill(recordIds.size)(0)    
    outliers.foreach(r => {       
      val recordIdx = recordIds.indexOf(r._1.id)
      counts(recordIdx) = counts(recordIdx) + 1
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
  def performJacknife(variableName: String, file: File, headersFields:Seq[String] = List.empty): (Seq[SampledRecord],Option[JackKnifeStats]) = {
    val r = new CSVReader(new FileReader(file))
    if (r.readNext() == null) return (List[SampledRecord](), None)
    val headers = if(headersFields.isEmpty){
      r.readNext().toList
    } else {
      headersFields //if the supplied headers arent empty, assume first line are headers
    } 
    
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

    //the environmental properties to throw at Jack knife test
    //val valuesToTest = cellToValue.values.filter(x => x != null).map(y => y.toFloat).toList
    val valuesToTest = cellToValue.values.map(y => y.toFloat).toSeq

    //do jack knife test
    val jacknife = new JackKnife

    //this is adding the same record to the buffer more than once....
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
        col = ncols - 1
      }
      if (row >= nrows) {
        row = nrows - 1
      }

      row * ncols + col
    }
  }
}