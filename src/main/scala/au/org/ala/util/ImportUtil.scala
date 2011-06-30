package au.org.ala.util

import au.org.ala.biocache.{Config}
import org.apache.commons.io.FileUtils
import java.io.{File, FileReader, BufferedReader}
import au.com.bytecode.opencsv.CSVReader
import scala.collection.JavaConversions

/**
 * Load a CSV file into the BioCache store
 */
object ImportUtil {

    import JavaConversions._
    import scalaj.collection.Imports._

    def main(args:Array[String]){

        var entity = ""
        var fieldsToImport = List[String]()
        var filesToImport = List[String]()
        var linesToSkip = 0
        var quotechar:Option[Char] = None
        var separator = '\t'

        val parser = new OptionParser("import") {
            arg("<entity>", "the entity (column family in cassandra) to export from", {v: String => entity = v})
            arg("<files-to-import>", "the file(s) to import, space separated", {v: String => filesToImport = v.split(" ").toList})
            opt("c", "columns", "<column1 column2 ...>", "column headers", {columns:String => fieldsToImport = columns.split(" ").toList})
            opt("cf", "column header file", "e.g. /data/headers.txt", "column headers", {
                v:String => fieldsToImport = FileUtils.readFileToString(new File(v)).trim.split("\t").toList
            })
            intOpt("s", "skip-line", "number of lines to skip before importing", {v:Int => linesToSkip = v } )
        }

        if(parser.parse(args)){
            val pm = Config.persistenceManager
            //process each file
            for (filepath <- filesToImport){
                importFile(entity, fieldsToImport, quotechar, separator, filepath)
            }
            pm.shutdown
        }
    }
    
    def importFile(entity: java.lang.String, fieldsToImport: List[String], quotechar: Option[Char], separator: Char, filepath: String){
        val reader =  quotechar.isEmpty match { 
            case false => new CSVReader(new FileReader(filepath), separator, quotechar.get)
            case _ => new CSVReader(new FileReader(filepath), separator)
        }

        var currentLine = reader.readNext
        while(currentLine!=null){
            val columns = currentLine.toList
            if (columns.length == fieldsToImport.length){
                val map = (fieldsToImport zip columns).toMap[String,String].filter( { 
                	case (key,value) => value!=null && value.toString.trim.length>0 
                })
                //println(map)
                Config.persistenceManager.put(columns(0), entity, map)
            }
            //read next
            currentLine = reader.readNext
        }
    }
}