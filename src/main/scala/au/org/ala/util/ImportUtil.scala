package au.org.ala.util

import java.io.{FileReader, BufferedReader}
import au.org.ala.biocache.{Config}

/**
 * Load a CSV file into the BioCache store
 */
object ImportUtil {

    def main(args:Array[String]){

        if(args.length ==0){
            println("Please supplied a absolute file path to CSV file to load.")
            exit(1)
        }

        val pm = Config.persistenceManager
        val reader =  new BufferedReader(new FileReader(args(0)))
        var columnHeaders = reader.readLine().split("\t")
        var currentLine = reader.readLine
        while(currentLine!=null){
            val columns = currentLine.split("\t")
            if (columns.length == columnHeaders.length){
                val map = (columnHeaders zip columns).toMap[String,String].filter( { case (key,value) => { value!=null && value.toString.trim.length>0 } })
                //println(map)
                pm.put(columns(0), "occ", map)

            }
            //read next
            currentLine = reader.readLine
        }
        pm.shutdown
    }
}