package au.org.ala.util

import au.org.ala.biocache.Config
import au.com.bytecode.opencsv.{CSVWriter,CSVReader}
import scala.collection.mutable.HashSet
import java.io.{FileReader, FileWriter}

/**
 * A re-sampler for sensitive records.
 */
object ResampleSensitiveRecords extends ResampleRecords {

  def sensitiveFilter(map:Map[String, String]) : Boolean = (map.getOrElse("originalSensitiveValues","") != "")
  
  

  def main(args:Array[String]){

    var dataResourceUid:String = ""
    val parser = new OptionParser("index records options") {
        opt("dr","data-resource-uid", "The data resource to process", {v:String => dataResourceUid = v})
    }

    if(parser.parse(args)){

      val startKey = {
        if (dataResourceUid != "") dataResourceUid +"|"
        else ""
      }
      val endKey = {
        if (dataResourceUid != "") dataResourceUid +"|~"
        else ""
      }

      val r = new ResampleRecords
      r.resamplePointsByFilter(sensitiveFilter, Array("originalSensitiveValues"),startKey, endKey)      
    }
  }
}
/**
 * A resampler for records that have had their coordinates changed during processing.
 */
object ResampleChangedCoordinates {
  def changeCoordinatesFilter(map:Map[String,String]) : Boolean ={
    val rawLat = map.getOrElse("decimalLatitude","")
    val rawLon = map.getOrElse("decimalLongitude","")
    val proLat = map.getOrElse("decimalLatitude.p","")
    val proLon = map.getOrElse("decimalLongitude.p","")
    if(rawLat!="" && rawLon != "" && proLat != "" && proLon != "")
      rawLat != proLat || rawLon != proLon
    false
  }
  def main(args:Array[String]){
    var dataResourceUid:String = ""
    val parser = new OptionParser("index records options") {
        opt("dr","data-resource-uid", "The data resource to process", {v:String => dataResourceUid = v})
    }

    if(parser.parse(args)){

      val startKey = {
        if (dataResourceUid != "") dataResourceUid +"|"
        else ""
      }
      val endKey = {
        if (dataResourceUid != "") dataResourceUid +"|~"
        else ""
      }

      val r = new ResampleRecords
      r.resamplePointsByFilter(changeCoordinatesFilter, Array("decimalLatitude","decimalLongitude"),startKey, endKey)
    }
  }
}

/**
 * Class that supports re-sampling of records based on a supplied filter. This class does the following:
 *
 * 1) Retrieves a distinct list of points based on a filter
 * 2) Retrieve a list of record based on a filter
 * 3) Performs sampling for the list of points
 * 4) Loads the results of the sampling
 * 5) Reprocesses the records in the supplied list
 *
 * Note: if the filter relies on certain properties, these properties must be listed in the
 * fieldsRequired.
 */
class ResampleRecords {

  val recordsSampledFilePath = "/tmp/records-resampled.txt"
  val pointsSampledFilePath = "/tmp/points-resampled.txt"
  val pointsResampledFilePath = "/tmp/points-resampled-sampled.txt"

  /**
   * Resample and reprocess records matching the filter
   */
  def resamplePointsByFilter(filter:Map[String, String] => Boolean, fieldsRequired:Array[String], startKey:String="", endKey:String=""){

    println("Starting the re-sampling.....")

    val records = new CSVWriter(new FileWriter(recordsSampledFilePath))
    val distinctPoints = new HashSet[(String, String)]

    val fields = Array("decimalLatitude.p", "decimalLongitude.p") ++ fieldsRequired

    //iterate through records
    //produce two CSV files: 1) Rowkeys and  2) distinct decimalLatitude.p, decimalLongitude.p
    Config.persistenceManager.pageOverSelect("occ", (guid,map) => {
      if (filter(map)){
        distinctPoints += ((map.getOrElse("decimalLongitude.p",""), map.getOrElse("decimalLatitude.p","")))
        records.writeNext(Array(guid))
      }
      true
    }, startKey, endKey, 1000, fields:_*)
    records.flush
    records.close

    //produce a distinct list of coordinates from first CSV
    val distinctPointsFile = new CSVWriter(new FileWriter(pointsSampledFilePath))
    distinctPoints.foreach(c => distinctPointsFile.writeNext(Array(c._1,c._2)))
    distinctPointsFile.flush
    distinctPointsFile.close

    //sample with the supplied points
    val sampling = new Sampling
    sampling.sampling(pointsSampledFilePath, pointsResampledFilePath)
    sampling.loadSampling(pointsResampledFilePath)

    //reprocess the records listed in first CSV
    val pointsReader = new CSVReader(new FileReader(recordsSampledFilePath))
    val rp = new RecordProcessor
    var current = pointsReader.readNext
    while (current != null){
      if(current.length == 1){
        Config.occurrenceDAO.getRawProcessedByRowKey(current(0)) match {
          case Some(rawProcessed) => {
            rp.processRecord(rawProcessed(0), rawProcessed(1))
          }
          case None => {}
        }
      }
      current = pointsReader.readNext
    }
    pointsReader.close
    Config.persistenceManager.shutdown //close DB connections
    println("Finished the re-sampling.")
  }
}
