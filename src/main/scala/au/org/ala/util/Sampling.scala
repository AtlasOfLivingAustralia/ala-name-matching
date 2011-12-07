package au.org.ala.util

import java.io.{FileInputStream, InputStreamReader, FileWriter, File}
import org.apache.commons.lang.StringUtils
import au.org.ala.biocache.{LocationDAO, LocationProcessor, Config, Json}
import au.com.bytecode.opencsv.{CSVWriter, CSVReader}
import scala.collection.mutable.{ArrayBuffer, HashSet}
import org.ala.layers.client.Client

/**
 * Executable for running the sampling for a data resource.
 */
object Sampling {

  def main(args: Array[String]) {

    var dataResourceUid = ""

    val parser = new OptionParser("import darwin core headed CSV") {
      opt("dr", "data-resource-uid", "the data resource to sample for", { v:String => dataResourceUid = v })
    }

    if (parser.parse(args)) {
        val s = new Sampling
        //for this data resource
        val fileSufffix = {
          if(dataResourceUid != "") dataResourceUid
          else "all"
        }

        val locFilePath = "/tmp/loc-" + fileSufffix + ".txt"
        val samplingFilePath = "/tmp/sampling-" + fileSufffix + ".txt"
        s.getDistinctCoordinatesForResource(locFilePath, dataResourceUid)
        //generate sampling
        s.sampling(locFilePath, samplingFilePath)
        //load the loc table
        s.loadSampling(samplingFilePath)
      }
    }
}

class Sampling {

  /**
   * Get the distinct coordinates for this resource
   * and write them to file.
   */
  def getDistinctCoordinatesForResource(locFilePath: String, dataResourceUid: String = "") {
    println("Creating distinct list of coordinates....")
    var counter = 0
    var passed = 0

    val startUuid: String = {
      if(dataResourceUid == "") ""
      else dataResourceUid + "|"
    }
    val endUuid: String = {
      if(dataResourceUid == "") ""
      else dataResourceUid + "|~"
    }

    val lp = new LocationProcessor
    val coordinates = new HashSet[String]

    Config.persistenceManager.pageOverSelect("occ", (guid, map) => {
      val latLongWithOption = lp.processLatLong(map.getOrElse("decimalLatitude", null),
        map.getOrElse("decimalLongitude", null),
        map.getOrElse("verbatimLongitude", null),
        map.getOrElse("verbatimLongitude", null)
      )
      latLongWithOption match {
        case Some(latLong) => coordinates += (latLong._2 + "," + latLong._1) // write long lat (x,y)
        case None => {}
      }

      val originalSensitiveValues = map.getOrElse("originalSensitiveValues", "")
      if(originalSensitiveValues != ""){
        val sensitiveLatLong = Json.toMap(originalSensitiveValues)
        val lat = sensitiveLatLong.getOrElse("decimalLatitude", null)
        val lon = sensitiveLatLong.getOrElse("decimalLongitude", null)
        if (lat != null && lon != null) {
          coordinates += (lon + "," + lat)
        }
      }

      passed += 1
      Integer.MAX_VALUE > counter
    }, startUuid, endUuid, 1000, "decimalLatitude", "decimalLongitude", "verbatimLatitude", "verbatimLongitude", "originalSensitiveValues")

    try {
      var fw = new FileWriter(locFilePath);
      coordinates.foreach(c => {
        fw.write(c)
        fw.write("\n")
      })
      fw.flush
      fw.close
    } catch {
      case e => {
        e.printStackTrace()
        println("failed to write");
      }
    }
  }

  /**
   * Run the sampling with a file
   */
  def sampling(filePath: String, outputFilePath: String) {

    println("********* START - TEST BATCH SAMPLING FROM FILE ***************")

    val fieldsToSample = Config.fieldsToSample

    //load the CSV of points into memory
    val points = loadPoints(filePath)

    //do the sampling
    processBatch(outputFilePath, points, fieldsToSample)

    println("********* END - TEST BATCH SAMPLING FROM FILE ***************")
  }

  /**
   * Load a set of points from a CSV file
   */
  private def loadPoints(filePath:String) : Array[Array[Double]] = {
    //load the CSV of points into memory
    val csvReader = new CSVReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"))
    var current: Array[String] = csvReader.readNext
    val points: ArrayBuffer[Array[Double]] = new ArrayBuffer[Array[Double]]
    while (current != null) {
      try {
        points += current.map(x => x.toDouble)
      } catch {
        case e:Exception => println("Error reading point: " + current)
      }
      current = csvReader.readNext
    }
    csvReader.close
    points.toArray
  }

  private def processBatch(outputFilePath: String, points: Array[Array[Double]], fields: Array[String]): Unit = {

    val writer = new CSVWriter(new FileWriter(outputFilePath))
    writer.writeNext(Array("longitude", "latitude") ++ fields)

    //process a batch of points
    val layerIntersectDAO = Client.getLayerIntersectDao()
    val samples: java.util.ArrayList[String] = layerIntersectDAO.sampling(fields, points);
    var columns: Array[Array[String]] = Array.ofDim(samples.size, points.length)

    for (i <- 0 until samples.size) {
      columns(i) = samples.get(i).split('\n')
    }

    for (i <- 0 until points.length) {
      val sampledPoint = Array.fill(2 + columns.length)("")
      sampledPoint(0) = points(i)(0).toString()
      sampledPoint(1) = points(i)(1).toString()

      for (j <- 0 until columns.length) {
        //print(",")
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
            println("writing to loc:" + counter + ": " + line(1) + "|" + line(0) +
              ", records per sec: " + 1000f / (((System.currentTimeMillis - nextTime).toFloat) / 1000f))
            nextTime = System.currentTimeMillis
          }
          counter += 1
        } catch {
          case e: Exception => e.printStackTrace();println("Problem writing line: " + counter + ", line length: " + line.length + ", header length: " + header.length)
        }
        line = csvReader.readNext
      }
      csvReader.close
    }

    println("Finished loading: " + inputFileName + " in " + (System.currentTimeMillis - startTime) + "ms");
  }
}