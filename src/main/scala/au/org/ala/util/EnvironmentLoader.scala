

package au.org.ala.util

import au.org.ala.biocache.LocationDAO
import java.io.File
import org.wyki.cassandra.pelops.Pelops

object EnvironmentLoader {

  def main(args: Array[String]): Unit = {
    
    println("Starting Environment Loader....")
    processFile("/data/biocache/bioclim_bio11_values.csv", "bioclim_bio11")
    processFile("/data/biocache/bioclim_bio12_values.csv", "bioclim_bio12")
    processFile("/data/biocache/bioclim_bio34_values.csv", "bioclim_bio34")
    processFile("/data/biocache/mean-temperature_cars2009a-band1_values.csv", "mean_temperature_cars2009a_band1")
    processFile("/data/biocache/mean-oxygen_cars2006-band1_values.csv", "mean_oxygen_cars2006_band1")
  
    Pelops.shutdown
  }
  def processFile(fileName :String, fieldName:String)={
    import FileHelper._
    println("Loading Environment file: " + fileName)
    val file = new File(fileName)
    var counter = 0
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis
      file.foreachLine { line => {
        counter += 1
        //add point with details to
        val parts = line.split(',')
        if(parts.length>0){
        
        val longitude = parts(0).substring(1, parts(0).length-1).toFloat
        val latitude = parts(1).substring(1, parts(1).length-1).toFloat

          //LocationDAO.addTagToLocation(latitude, longitude, fieldName, parts(2).substring(1, parts(2).length-1))

          LocationDAO.addRegionToPoint(latitude, longitude, Map[String, String](fieldName->parts(2).substring(1, parts(2).length-1)))
        if (counter % 1000 == 0) {
          finishTime = System.currentTimeMillis
          println(counter +": "+latitude+"|"+longitude+", mapping: "+ parts(2) + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
          startTime = System.currentTimeMillis

        }
        }
        else{
          println("Problem line: "+ counter)
        }

      }
    }
    println(counter)
  }


}
