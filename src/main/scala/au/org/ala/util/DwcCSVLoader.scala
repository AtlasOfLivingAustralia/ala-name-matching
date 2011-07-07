package au.org.ala.util
import au.org.ala.biocache.DataLoader
import org.apache.commons.io.FileUtils
import au.com.bytecode.opencsv.CSVReader
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import au.org.ala.biocache.FullRecordMapper
import java.io.File
import scala.collection.JavaConversions
import au.org.ala.biocache.Raw
import au.org.ala.biocache.Versions

object DwcCSVLoader {
    
    def main(args:Array[String]){

        var dataResourceUid = ""

        val parser = new OptionParser("import darwin core headed CSV") {
            arg("<data-resource-uid>", "the data resource to import", {v: String => dataResourceUid = v})
        }

        if(parser.parse(args)){
            val l = new DwcCSVLoader
            //process each file
            l.load(dataResourceUid)
        }
    }
}

class DwcCSVLoader extends DataLoader {

    import JavaConversions._
    import scalaj.collection.Imports._

    def load(dataResourceUid:String){
        val (protocol, url, uniqueTerms, params) = retrieveConnectionParameters(dataResourceUid)
        val fileName = downloadArchive(url,dataResourceUid)
        val directory = new File(fileName)
        directory.listFiles.foreach(file => loadFile(file,dataResourceUid, uniqueTerms, params))
    }
    
    def loadFile(file:File, dataResourceUid:String, uniqueTerms:List[String], params:Map[String,String]){
        
        val quotechar = params.getOrElse("quotechar", "\"").head
        val separator = params.getOrElse("separator", ",").head
        val reader =  new CSVReader(new FileReader(file), separator, quotechar)
        
        val uniqueTermsNormalised = uniqueTerms.map(t => t.toLowerCase.replace(" ", ""))
        val columnHeaders = reader.readNext.map(t => t.toLowerCase.replace(" ", ""))
        
        var currentLine = reader.readNext
        
        val validConfig = uniqueTermsNormalised.forall(t => columnHeaders.contains(t))
        if(!validConfig){
            println("Unique terms: " + uniqueTermsNormalised)
            println("Column headers: " + columnHeaders)
            throw new RuntimeException("Bad configuration for file: "+ file.getName + " for resource: " + dataResourceUid)
        }
        
        while(currentLine!=null){
            val columns = currentLine.toList
            if (columns.length == columnHeaders.size){
                val map = (columnHeaders zip columns).toMap[String,String].filter( { 
                	case (key,value) => value!=null && value.toString.trim.length>0 
                })
                
                if(uniqueTermsNormalised.forall(t => map.getOrElse(t,"").length>0)){
	                val uniqueTermsValues = for (t <-uniqueTermsNormalised) yield map.getOrElse(t,"")
	                val fr = FullRecordMapper.createFullRecord("", map, Versions.RAW)
	                load(dataResourceUid, fr, uniqueTermsValues)
                } else {
                    println("Skipping line: " + ", missing unique term value")
                }
            }
            //read next
            currentLine = reader.readNext
        }
    }
}
