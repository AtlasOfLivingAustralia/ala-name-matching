package au.org.ala.util
import au.org.ala.biocache.DataLoader
import au.com.bytecode.opencsv.CSVReader
import au.com.bytecode.opencsv.CSVReader
import au.org.ala.biocache.FullRecordMapper
import scala.collection.JavaConversions
import au.org.ala.biocache.Raw
import au.org.ala.biocache.Versions
import au.org.ala.biocache.DwC
import au.org.ala.biocache.MediaStore
import java.io.{FilenameFilter, FileReader, File}
import org.apache.commons.io.{FilenameUtils, FileUtils}


object DwcCSVLoader {
    
    def main(args:Array[String]){

        var dataResourceUid = ""
        var localFilePath:Option[String] = None
        var updateLastChecked = false
        var bypassConnParamLookup = false
            
        val parser = new OptionParser("import darwin core headed CSV") {
            arg("<data-resource-uid>", "the data resource to import", {v: String => dataResourceUid = v})
            opt("l", "local", "skip the download and use local file", {v:String => localFilePath = Some(v) } )
            booleanOpt("u", "updateLastChecked", "update registry with last loaded date", {v:Boolean => updateLastChecked = v } )
            booleanOpt("b", "bypassConnParamLookup", "bypass connection param lookup", {v:Boolean => bypassConnParamLookup = v } )
        }

        if(parser.parse(args)){
            val l = new DwcCSVLoader
            try {
                if (bypassConnParamLookup && !localFilePath.isEmpty){
                  l.loadFile(new File(localFilePath.get),dataResourceUid, List(), Map())
                } else {
                  localFilePath match {
                      case None => l.load(dataResourceUid)
                      case Some(v) => l.loadLocalFile(dataResourceUid, v)
                  }
                  //initialise the delete/update the collectory information
                  if (updateLastChecked){
                    l.updateLastChecked(dataResourceUid)
                  }
                }
            } catch {
                case e:Exception => e.printStackTrace
            } finally {
                l.pm.shutdown
                Console.flush()
                Console.err.flush()
                exit(0)
            }
        }
    }
}

class DwcCSVLoader extends DataLoader {

    import JavaConversions._
    import scalaj.collection.Imports._

    def loadLocalFile(dataResourceUid:String, filePath:String){
        val (protocol, urls, uniqueTerms, params, customParams) = retrieveConnectionParameters(dataResourceUid)
        loadFile(new File(filePath),dataResourceUid, uniqueTerms, params) 
    }
    
    def load(dataResourceUid:String){
        val (protocol, urls, uniqueTerms, params, customParams) = retrieveConnectionParameters(dataResourceUid)
        urls.foreach(url => {
          val fileName = downloadArchive(url,dataResourceUid)
          val directory = new File(fileName)
          directory.listFiles.foreach(file => loadFile(file,dataResourceUid, uniqueTerms, params))
        })
    }
    
    def loadFile(file:File, dataResourceUid:String, uniqueTerms:List[String], params:Map[String,String]){
        
        val quotechar = params.getOrElse("csv_text_enclosure", "\"").head
        val separator = params.getOrElse("csv_delimiter", ",").head
        val escape = params.getOrElse("csv_escape_char","\\").head
        val reader =  new CSVReader(new FileReader(file), separator, quotechar, escape)
        
        println("Using CSV reader with the following settings quotes: " + quotechar + " separator: " + separator + " escape: " + escape)
        //match the column headers to dwc terms
        val dwcTermHeaders = {
            val columnHeaders = reader.readNext.map(t => t.replace(" ", "")).toList
            DwC.retrieveCanonicals(columnHeaders)
        }
        
        var currentLine = reader.readNext
        
        println("Unique terms: " + uniqueTerms)
        println("Column headers: " + dwcTermHeaders)
        
        val validConfig = uniqueTerms.forall(t => dwcTermHeaders.contains(t))
        if(!validConfig){
            throw new RuntimeException("Bad configuration for file: "+ file.getName + " for resource: " + dataResourceUid)
        }
        
        var counter = 1
        var noSkipped = 0
        var startTime = System.currentTimeMillis
        var finishTime = System.currentTimeMillis
        while(currentLine!=null){
            counter += 1
            
            val columns = currentLine.toList
            if (columns.length == dwcTermHeaders.size){

                val map = (dwcTermHeaders zip columns).toMap[String,String].filter( { 
                	case (key,value) => value!=null && value.toString.trim.length>0 
                })
                
                if(uniqueTerms.forall(t => map.getOrElse(t,"").length>0)){
	                val uniqueTermsValues = uniqueTerms.map(t => map.getOrElse(t,"")) //for (t <-uniqueTerms) yield map.getOrElse(t,"")
	                val fr = FullRecordMapper.createFullRecord("", map, Versions.RAW)
	                load(dataResourceUid, fr, uniqueTermsValues)

                  //is there any associatedMedia
                  if (fr.occurrence.associatedMedia != null){
                    //load it and store it
                    val directory = file.getParentFile
                    println("Associated media: " + fr.occurrence.associatedMedia)
                    val filesToImport = fr.occurrence.associatedMedia.split(";")
                    val filePathsInStore = filesToImport.map(fileName =>{
                      println(fileName)
                      if(fileName.startsWith("http://")){
                        MediaStore.save(fr.uuid, dataResourceUid,fileName)
                      } else {
                        val filePath = MediaStore.save(fr.uuid, dataResourceUid, "file:///"+file.getParent+File.separator+fileName)
                        //do multiple formats exist? check for files of the same name, different extension
                        val differentFormats = directory.listFiles(new au.org.ala.biocache.SameNameDifferentExtensionFilter(fileName))
                        differentFormats.foreach ( file => {
                          MediaStore.save(fr.uuid, dataResourceUid, "file:///"+file.getParent+File.separator+file.getName)
                        })
                        if(filePath.isEmpty)
                          fileName
                        else
                          filePath.get
                      }
                    })
                    fr.occurrence.associatedMedia = filePathsInStore.mkString(";")
                    load(dataResourceUid, fr, uniqueTermsValues)
                  }

                  if (counter % 1000 == 0 && counter > 0) {
                      finishTime = System.currentTimeMillis
                      println(counter + ", >> last key : " + dataResourceUid + "|" +
                        uniqueTermsValues.mkString("|") + ", records per sec: " +
                        1000 / (((finishTime - startTime).toFloat) / 1000f))
                      startTime = System.currentTimeMillis
                  }
                } else {
                    noSkipped += 1
                    print("Skipping line: " + counter + ", missing unique term value. Number skipped: "+ noSkipped)
                    uniqueTerms.foreach(t => print("," + t +":"+map.getOrElse(t,"")))
                    println
                }
            }
            else{
                println("Skipping line: " +counter + " incorrect number of columns ("+columns.length+")...headers (" + dwcTermHeaders.length +")")
                println("First element : "+columns(0) +"...headers :" + dwcTermHeaders(0))
                println("last element : "+columns.last +"...headers :" + dwcTermHeaders.last)
            }
            //read next
            currentLine = reader.readNext
        }
        
        println("Load finished")
    }
}
