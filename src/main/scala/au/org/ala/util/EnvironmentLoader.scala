

package au.org.ala.util

import au.org.ala.biocache._
import java.io.{FileReader, File, InputStreamReader, FileInputStream}
import au.com.bytecode.opencsv.CSVReader
import org.apache.commons.lang.StringUtils

/**
 * load the environment and contextual layers in to the location cache
 *
 *
 */
object EnvironmentLoader {
private val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]
private val fieldMap = Map[String,String]("aus1"->"StateProvince", "aus2"->"lga", "ibra_reg_shape"->"ibra", "imcra4_pb"->"imcra")
  def main(args: Array[String]): Unit = {

    //takes a directory as argument
    val fileName = if(args.length == 1) args(0) else "/data/biocache/sampling_batch/110523/aus1.csv"
    val fieldName = if(args.length == 2) args(1) else "StateProvince"
    val file = new File(fileName)
    if(file.isDirectory){
      for(name <- file.list){
        try{
          processFile(fileName + "/"+name, getRealFieldName(name.substring(0, name.indexOf(".")).replaceAll("-", "_")))
        }
        catch{
          case e:Exception=> println("WARNING: Unable to load " + name.replaceAll("-", "_"))
        }
      }
    }
    else
      processFile(fileName, getRealFieldName(fieldName))
  
    persistenceManager.shutdown
  }
  def getRealFieldName(name:String)= if(fieldMap.contains(name)) fieldMap.get(name).get else name
  def processFile(fileName :String , fieldName:String)={
    import FileHelper._
    println("Loading Environment file: " + fileName)
    println("######" + fieldName)
    val file = new File(fileName)
    val csvReader = new CSVReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
    var counter = 0
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis
     val columnsHeaders = csvReader.readNext //ignore the firs line
    var parts:Array[String] = csvReader.readNext

    //val occMap = new scala.collection.mutable.HashMap[String, Map[String, String]]()
    //val drMap = new scala.collection.mutable.HashMap[String, Map[String, String]]()
    while (parts != null) {
        counter += 1
        //add point with details to
        
        if(parts.length>2 && ! StringUtils.isEmpty(parts(2).trim)){
       
        val longitude = parts(1).toDouble
        val latitude = parts(0).toDouble
       

        LocationDAO.addRegionToPoint(latitude, longitude, Map[String, String](fieldName->parts(2)))
        if (counter % 100000 == 0) {
          finishTime = System.currentTimeMillis
          println(counter +": "+latitude+"|"+longitude+", mapping: "+ parts(2) + ", records per sec: " + 100000f / (((finishTime - startTime).toFloat) / 1000f))
          startTime = System.currentTimeMillis

        }
        }
        else{
          //println("Problem line: "+ counter)
        }
        parts = csvReader.readNext
      
    }
    println("Processed " + counter + " lines")
  }


}
