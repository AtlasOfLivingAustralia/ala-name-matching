package au.org.ala.util

import au.org.ala.biocache._
import java.io.{ FileReader, File, InputStreamReader, FileInputStream }
import au.com.bytecode.opencsv.CSVReader
import org.apache.commons.lang.StringUtils

/**
 * load the environment and contextual layers in to the location cache
 */
object EnvironmentLoader {

    private val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]

    private val nonDefaultFieldMap = Map[String, String]("aus1" -> "stateProvince", "aus2" -> "lga", "ibra_reg_shape" -> "ibra", "ibra_merged" -> "ibra", "imcra4_pb" -> "imcra", "ne_world" -> "country")

    //TODO Move this name mapping so that the biocache-service can use it to map layers to indexed items
    private val fieldMap = fileToMap("/layers.txt")

    /**
     * Maps the layers file to a map of names to ids
     */
    protected def fileToMap(file:String) :Map[String, String] = {
      val lines =scala.io.Source.fromURL(getClass.getResource(file), "utf-8").getLines
      var fields = new scala.collection.mutable.HashMap[String, String]
      for(line:String <-lines){
        val values = line.split(",")
        val mapping = nonDefaultFieldMap.getOrElse(values(1), getPrefix(values(2))+values(0))
        fields += values(1).toLowerCase-> mapping
      }
      fields.toMap
    }
    //The prefix for the layer id
    def getPrefix(value:String) = if(value == "Environmental") "el" else "cl"


    def main(args: Array[String]): Unit = {
    	
        if(args.length == 0 || !(new File(args(0))).exists){
        	println("Please supply a valid filepath or directory path. e.g. /data/biocache/sampling_batch/110523/aus1.csv")
        	exit(1)
        }
        
        val fileName = args(0) //"/data/biocache/sampling_batch/110523/aus1.csv"
        val file = new File(args(0))
        
        //takes a directory as argument
        if (file.isDirectory) {
            for (name <- file.list) {
                try {
                    val filepath = fileName + File.separator + name
                    val fieldName = name.toLowerCase.substring(0, name.indexOf("."))//name.substring(0, name.indexOf(".")).replaceAll("-", "_")                    
                    processFile(filepath, getFieldName(fieldName))
                } catch {
                    case e: Exception => println("WARNING: Unable to load " + name.replaceAll("-", "_"))
                }
            }
        } else {
            val fieldName = fileName.substring(fileName.lastIndexOf(File.separator) + 1, fileName.indexOf(".")).replaceAll("-", "_")           
            processFile(fileName, getFieldName(fieldName))
        }
        persistenceManager.shutdown
    }
    
    def getFieldName(name: String) = {
      val fn = fieldMap.get(name)
      if(fn.isEmpty){
            println("Warning unable to locate a field name for : " + name)
            name
      }
        else{
            fn.get
        }

    }
    
    def processFile(fileName: String, fieldName: String) = {
        import FileHelper._
        println("Loading Environment file: " + fileName)
        println("######" + fieldName)
        val file = new File(fileName)
        val csvReader = new CSVReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
        var counter = 0
        var startTime = System.currentTimeMillis
        var finishTime = System.currentTimeMillis
        val columnsHeaders = csvReader.readNext //ignore the firs line
        var parts: Array[String] = csvReader.readNext

    
        while (parts != null) {
            counter += 1
            //add point with details to

            if (parts.length > 2 && !StringUtils.isEmpty(parts(2).trim)) {

                val longitude = parts(1)
                val latitude = parts(0)

                LocationDAO.addRegionToPoint(latitude, longitude, Map[String, String](fieldName -> parts(2)))
                if (counter % 100000 == 0) {
                    finishTime = System.currentTimeMillis
                    println(counter + ": " + latitude + "|" + longitude + ", mapping: " + parts(2) + ", records per sec: " + 100000f / (((finishTime - startTime).toFloat) / 1000f))
                    startTime = System.currentTimeMillis

                }
            } else {
                //println("Problem line: "+ counter)
            }
            parts = csvReader.readNext

        }
        println("Processed " + counter + " lines")
    }
}
