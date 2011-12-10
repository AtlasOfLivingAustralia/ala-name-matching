package au.org.ala.util

import au.org.ala.biocache.Config
import scala.collection
import scala.collection.parallel.mutable
import au.com.bytecode.opencsv.{CSVWriter,CSVReader}
import scala.collection.mutable.HashSet
import java.io.{FileReader, FileWriter}

object ResampleSensitiveRecords {

  def main(args:Array[String]){

    val records = new CSVWriter(new FileWriter("/tmp/records-resampled.txt"))
    val distinctPoints = new HashSet[(String, String)]

    //iterate through records
    //produce two CSV files: 1) Rowkeys and  2) distinct decimalLatitude.p, decimalLongitude.p
    Config.persistenceManager.pageOverSelect("occ", (guid,map) => {
      if (map.getOrElse("originalSensitiveValues","") != ""){
        distinctPoints += ((map.getOrElse("decimalLongitude.p",""), map.getOrElse("decimalLatitude.p","")))
        records.writeNext(Array(guid))
      }
      true
    }, "","", 1000, "originalSensitiveValues", "decimalLatitude.p", "decimalLongitude.p")
    records.flush
    records.close

    //produce a distinct list of coordinates from first CSV
    val distinctPointsFile = new CSVWriter(new FileWriter("/tmp/points-resampled.txt"))
    distinctPoints.foreach(c => distinctPointsFile.writeNext(Array(c._1,c._2)))
    distinctPointsFile.flush
    distinctPointsFile.close

    //sample with the supplied points
    val sampling = new Sampling
    sampling.sampling("/tmp/points-resampled.txt", "/tmp/points-resampled-sampled.txt")
    sampling.loadSampling("/tmp/points-resampled-sampled.txt")

    //reprocess the records listed in first CSV
    val pointsReader = new CSVReader(new FileReader("/tmp/records-resampled.txt"))
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
  }
}