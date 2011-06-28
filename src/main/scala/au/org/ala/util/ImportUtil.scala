package au.org.ala.util

import au.org.ala.biocache.{Config}
import org.apache.commons.io.FileUtils
import java.io.{File, FileReader, BufferedReader}

/**
 * Load a CSV file into the BioCache store
 */
object ImportUtil {

    def main(args:Array[String]){

        if(args.length < 2){
            println("Please supplied a absolute file path to CSV file to load.")
            exit(1)
        }

        //read column headers
        val columnHeaders = FileUtils.readFileToString( new File(args(0)) ).split("\t").toList

        val pm = Config.persistenceManager

        //process each file
        for (filepath <- args.tail){
            val reader =  new BufferedReader(new FileReader(filepath))

            var currentLine = reader.readLine
            while(currentLine!=null){
                val columns = currentLine.split("\t").toList
                if (columns.length == columnHeaders.length){
                    val map = (columnHeaders zip columns).toMap[String,String].filter( { case (key,value) => { value!=null && value.toString.trim.length>0 } })
                    //println(map)
                    pm.put(columns(0), "occ", map)

                }
                //read next
                currentLine = reader.readLine
            }
        }
        pm.shutdown
    }
}