package au.org.ala.util

import java.io.{FileInputStream, InputStreamReader, FileWriter, File}
import org.apache.commons.lang.StringUtils
import au.org.ala.biocache._
import au.com.bytecode.opencsv.{CSVWriter, CSVReader}
import scala.collection.mutable.{ArrayBuffer, HashSet}
import org.ala.layers.client.Client
import scala.Some
import org.slf4j.LoggerFactory
import org.ala.layers.dao.IntersectCallback
import collection.mutable

/**
 * Executable for running the sampling for a data resource.
 */
object Sampling {

  protected val logger = LoggerFactory.getLogger("Sampling")

  def main(args: Array[String]) {

    var dataResourceUid = ""
    var locFilePath = ""
    var singleLayerName = ""
    var rowKeyFile = ""
    var keepFiles = false
    var singleRowKey=""

    val parser = new OptionParser("Sample coordinates against geospatial layers") {
      opt("dr", "data-resource-uid", "the data resource to sample for", {
        v: String => dataResourceUid = v
      })
      opt("cf", "coordinates-file", "the file containing coordinates", {
        v: String => locFilePath = v
      })
      opt("l", "single-layer-sample", "sample a single layer only", {
        v: String => singleLayerName = v
      })
      opt("rf", "row-key-file", "The row keys which to sample", {
        v: String => rowKeyFile = v
      })
      opt("keep", "Keep the files produced from the sampling",{
        keepFiles = true
      })
      opt("rk","key","the single rowkey to sample",{
        v:String => singleRowKey = v
      })

    }
    if (parser.parse(args)) {
      val s = new Sampling
      //for this data resource
      val fileSufffix = {
        if (dataResourceUid != "") dataResourceUid
        else "all"
      }

      if (locFilePath == "") {
        locFilePath = "/tmp/loc-" + fileSufffix + ".txt"
        if (rowKeyFile == "" && singleRowKey == "") {
          s.getDistinctCoordinatesForResource(locFilePath, dataResourceUid)
        } else if (singleRowKey != "") {
          s.getDistinctCoordiantesForRowKey(singleRowKey)
          exit(0)
        } else {
          s.getDistinctCoordinatesForFile(locFilePath, rowKeyFile)
        }
      }
      val samplingFilePath = "/tmp/sampling-" + fileSufffix + ".txt"
      //generate sampling
      s.sampling(locFilePath, samplingFilePath, singleLayerName=singleLayerName)
      //load the loc table
      s.loadSampling(samplingFilePath)
      //clean up the file
      if(!keepFiles){
        logger.info("Removing temporary file: " + samplingFilePath)
        (new File(samplingFilePath)).delete()
        (new File(locFilePath)).delete()
      }
    }
  }

  def sampleDataResource(dataResourceUid: String, callback:IntersectCallback = null, singleLayerName: String = "") {
    val locFilePath = "/tmp/loc-" + dataResourceUid + ".txt"
    val s = new Sampling
    s.getDistinctCoordinatesForResource(locFilePath, dataResourceUid)
    val samplingFilePath = "/tmp/sampling-" + dataResourceUid + ".txt"
    //generate sampling
    s.sampling(locFilePath, samplingFilePath, callback, singleLayerName)
    //load the loc table
    s.loadSampling(samplingFilePath)
    //clean up the file
    logger.info("Removing temporary file: " + samplingFilePath)
    (new File(samplingFilePath)).delete()
    (new File(locFilePath)).delete()
  }
}

class Sampling {
  val logger = LoggerFactory.getLogger("Sampling")

  import FileHelper._

  def handleLatLongInMap(map:Map[String,String], coordinates:mutable.HashSet[String], lp:LocationProcessor){
    val latLongWithOption = lp.processLatLong(map.getOrElse("decimalLatitude", null),
      map.getOrElse("decimalLongitude", null),
      map.getOrElse("geodeticDatum", null),
      map.getOrElse("verbatimLongitude", null),
      map.getOrElse("verbatimLongitude", null),
      map.getOrElse("verbatimSRS", null),
      map.getOrElse("easting", null),
      map.getOrElse("northing", null),
      map.getOrElse("zone", null),
      new ArrayBuffer[QualityAssertion]
    )
    latLongWithOption match {
      case Some(latLong) => {
        coordinates += (latLong._2 + "," + latLong._1) // write long lat (x,y)
        coordinates += (latLong._2.toFloat.toString.trim+ ","+latLong._1.toFloat.toString.trim)
      }
      case None => {}
    }
  }
  def handleRecordMap(map:Map[String,String], coordinates:HashSet[String], lp:LocationProcessor){
    handleLatLongInMap(map, coordinates, lp)

    val originalSensitiveValues = map.getOrElse("originalSensitiveValues", "")
    if (originalSensitiveValues != "") {
      val sensitiveLatLong = Json.toMap(originalSensitiveValues)
      val lat = sensitiveLatLong.getOrElse("decimalLatitude", null)
      val lon = sensitiveLatLong.getOrElse("decimalLongitude", null)
      if (lat != null && lon != null) {
        coordinates += (lon + "," + lat)
        val newMap = map ++ Map("decimalLatitude"-> lat.toString, "decimalLongitude"->lon.toString)
        handleLatLongInMap(newMap, coordinates,lp)
      }
    }

    //legacy storage of old lat/long original values before SDS processing - superceded by originalSensitiveValues
    val originalDecimalLatitude = map.getOrElse("originalDecimalLatitude", "")
    val originalDecimalLongitude = map.getOrElse("originalDecimalLongitude", "")
    if (originalDecimalLatitude != "" && originalDecimalLongitude != "") {
      coordinates += (originalDecimalLongitude + "," + originalDecimalLatitude)
    }

    //add the processed values
    val processedDecimalLatitude = map.getOrElse("decimalLatitude.p", "")
    val processedDecimalLongitude = map.getOrElse("decimalLongitude.p", "")
    if (processedDecimalLatitude != "" && processedDecimalLongitude != "") {
      coordinates += (processedDecimalLongitude + "," + processedDecimalLatitude)
    }
  }

  val properties = Array("decimalLatitude", "decimalLongitude",
    "decimalLatitude.p", "decimalLongitude.p",
    "verbatimLatitude", "verbatimLongitude",
    "originalDecimalLatitude", "originalDecimalLongitude",
    "originalSensitiveValues", "geodeticDatum", "verbatimSRS", "easting", "northing", "zone")

  def getDistinctCoordiantesForRowKey(rowKey:String){
    val values = Config.persistenceManager.getSelected(rowKey, "occ", properties)
    if(values.isDefined){
      val coordinates = new HashSet[String]
      handleRecordMap(values.get, coordinates, new LocationProcessor)
      println(coordinates)
    }

  }

  def getDistinctCoordinatesForFile(locFilePath: String, rowKeyFile: String) {
    logger.info("Creating distinct list of coordinates for row keys in " + rowKeyFile)
    var counter = 0
    var passed = 0
    val rowKeys = new File(rowKeyFile)
    val coordinates = new HashSet[String]
    val lp = new LocationProcessor
    rowKeys.foreachLine(line => {
      val values = Config.persistenceManager.getSelected(line, "occ", properties)
      if (values.isDefined) {
        def map = values.get
        handleRecordMap(map, coordinates, lp)

        if (counter % 10000 == 0 && counter > 0) logger.debug("Distinct coordinates counter: " + counter + ", current count:" + coordinates.size)
        counter += 1
        passed += 1
      }
    })

    try {
      val fw = new FileWriter(locFilePath)
      coordinates.foreach(c => {
        fw.write(c)
        fw.write("\n")
      })
      fw.flush
      fw.close
    } catch {
      case e:Exception =>  logger.error("failed to write - " + e.getMessage, e)
    }
  }

  /**
   * Get the distinct coordinates for this resource
   * and write them to file.
   */
  def getDistinctCoordinatesForResource(locFilePath: String, dataResourceUid: String = "") {
    logger.info("Creating distinct list of coordinates....")
    var counter = 0
    var passed = 0

    val startUuid: String = {
      if (dataResourceUid == "") ""
      else dataResourceUid + "|"
    }
    val endUuid: String = {
      if (dataResourceUid == "") ""
      else dataResourceUid + "|~"
    }

    val lp = new LocationProcessor
    val coordinates = new HashSet[String]

    Config.persistenceManager.pageOverSelect("occ", (guid, map) => {
      handleRecordMap(map, coordinates,lp)

      if (counter % 10000 == 0 && counter > 0){
        logger.debug("Distinct coordinates counter: " + counter + ", current count:" + coordinates.size)
      }
      counter += 1
      passed += 1
      Integer.MAX_VALUE > counter
    }, startUuid, endUuid, 1000, "decimalLatitude", "decimalLongitude","decimalLatitude.p", "decimalLongitude.p",
      "verbatimLatitude", "verbatimLongitude","originalDecimalLatitude", "originalDecimalLongitude",
      "originalSensitiveValues")

    try {
      val fw = new FileWriter(locFilePath)
      coordinates.foreach(c => {
        fw.write(c)
        fw.write("\n")
      })
      fw.flush
      fw.close
    } catch {
      case e:Exception =>  logger.error(e.getMessage,e)
    }
  }

  /**
   * Run the sampling with a file
   */
  def sampling(filePath: String, outputFilePath: String, callback:IntersectCallback = null,singleLayerName: String = "") {

    logger.info("********* START - TEST BATCH SAMPLING FROM FILE ***************")
    //load the CSV of points into memory
    val points = loadPoints(filePath)
    //do the sampling
    if (singleLayerName != "") {
      processBatch(outputFilePath, points, Array(singleLayerName), callback)
    } else {
      processBatch(outputFilePath, points, Config.fieldsToSample, callback)
    }
    logger.info("********* END - TEST BATCH SAMPLING FROM FILE ***************")
  }

  /**
   * Load a set of points from a CSV file
   */
  private def loadPoints(filePath: String): Array[Array[Double]] = {
    //load the CSV of points into memory
    logger.info("Loading points from file: " + filePath)
    val csvReader = new CSVReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"))
    var current: Array[String] = csvReader.readNext
    val points: ArrayBuffer[Array[Double]] = new ArrayBuffer[Array[Double]]
    while (current != null) {
      try {
        points += current.map(x => x.toDouble)
      } catch {
        case e: Exception => logger.error("Error reading point: " + current)
      }
      current = csvReader.readNext
    }
    csvReader.close
    points.toArray
  }

  private def processBatch(outputFilePath: String, points: Array[Array[Double]], fields: Array[String], callback:IntersectCallback=null): Unit = {

    val writer = new CSVWriter(new FileWriter(outputFilePath))
    writer.writeNext(Array("longitude", "latitude") ++ fields)

    //process a batch of points
    val layerIntersectDAO = org.ala.layers.client.Client.getLayerIntersectDao()
    val samples: java.util.ArrayList[String] = layerIntersectDAO.sampling(fields, points, callback)
    val columns: Array[Array[String]] = Array.ofDim(samples.size, points.length)

    for (i <- 0 until samples.size) {
      columns(i) = samples.get(i).split('\n')
    }

    for (i <- 0 until points.length) {
      val sampledPoint = Array.fill(2 + columns.length)("")
      sampledPoint(0) = points(i)(0).toString()
      sampledPoint(1) = points(i)(1).toString()
      for (j <- 0 until columns.length) {
        if (i < columns(j).length) {
          if (columns(j)(i) != "n/a") {
            sampledPoint(j + 2) = columns(j)(i)
          }
        }
      }
      writer.writeNext(sampledPoint.toArray)
    }
    writer.flush
    writer.close
  }

  /**
   * Load the sampling into the loc table
   */
  def loadSampling(inputFileName: String) {
    var startTime = System.currentTimeMillis
    var nextTime = System.currentTimeMillis
    try {
      val csvReader = new CSVReader(new InputStreamReader(new FileInputStream(inputFileName), "UTF-8"));
      var header = csvReader.readNext
      var counter = 0
      var line = csvReader.readNext

      while (line != null) {
        try {
          val map = (header zip line).filter(x => !StringUtils.isEmpty(x._2.trim) && x._1 != "latitude" && x._1 != "longitude").toMap
          val el = map.filter(x => x._1.startsWith("el")).map(y => y._1 -> y._2.toFloat).toMap
          val cl = map.filter(x => x._1.startsWith("cl")).toMap
          LocationDAO.addLayerIntersects(line(1), line(0), cl, el)
          if (counter % 1000 == 0) {
            logger.debug("writing to loc:" + counter + ": " + line(1) + "|" + line(0) +
              ", records per sec: " + 1000f / (((System.currentTimeMillis - nextTime).toFloat) / 1000f))
            nextTime = System.currentTimeMillis
          }
          counter += 1
        } catch {
          case e: Exception => {
            logger.error(e.getMessage,e)
            logger.error("Problem writing line: " + counter + ", line length: " + line.length + ", header length: " + header.length)
          }
        }
        line = csvReader.readNext
      }
      csvReader.close
    }
    logger.info("Finished loading: " + inputFileName + " in " + (System.currentTimeMillis - startTime) + "ms");
  }
}