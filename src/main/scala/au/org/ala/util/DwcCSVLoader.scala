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
import java.io.{FilenameFilter, FileReader, File,InputStreamReader, FileInputStream}
import org.apache.commons.io.{FilenameUtils, FileUtils}
import collection.mutable.ArrayBuffer
import au.org.ala.biocache.Config


object DwcCSVLoader {
    
    def main(args:Array[String]){

        var dataResourceUid = ""
        var localFilePath:Option[String] = None
        var updateLastChecked = false
        var bypassConnParamLookup = false
        var testFile = false
        var logRowKeys = false
            
        val parser = new OptionParser("import darwin core headed CSV") {
            arg("<data-resource-uid>", "the data resource to import", {v: String => dataResourceUid = v})
            opt("l", "local", "skip the download and use local file", {v:String => localFilePath = Some(v) } )
            booleanOpt("u", "updateLastChecked", "update registry with last loaded date", {v:Boolean => updateLastChecked = v } )
            booleanOpt("b", "bypassConnParamLookup", "bypass connection param lookup", {v:Boolean => bypassConnParamLookup = v } )
            opt("test", "test the file only do not load", {testFile=true})
            opt("log","log row keys to file - allows processing/indexing of changed records",{logRowKeys = true})
        }

        if(parser.parse(args)){
            val l = new DwcCSVLoader
            l.deleteOldRowKeys(dataResourceUid)
            try {
                if (bypassConnParamLookup && !localFilePath.isEmpty){
                  l.loadFile(new File(localFilePath.get),dataResourceUid, List(), Map(),false,logRowKeys,testFile)
                } else {
                  localFilePath match {
                      case None => l.load(dataResourceUid,logRowKeys,testFile)
                      case Some(v) => l.loadLocalFile(dataResourceUid, v,logRowKeys,testFile)
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

    def loadLocalFile(dataResourceUid:String, filePath:String,logRowKeys:Boolean=false, testFile:Boolean=false){
        val (protocol, urls, uniqueTerms, params, customParams, lastChecked) = retrieveConnectionParameters(dataResourceUid)
        val strip = params.getOrElse("strip", false).asInstanceOf[Boolean]
        val incremental = params.getOrElse("incremental",false).asInstanceOf[Boolean]
        loadFile(new File(filePath),dataResourceUid, uniqueTerms, params,strip, incremental || logRowKeys, testFile) 
    }
    
    def load(dataResourceUid:String,logRowKeys:Boolean=false, testFile:Boolean=false, forceLoad:Boolean=false){
      //remove the old files
      emptyTempFileStore(dataResourceUid)
      //delete the old file
      deleteOldRowKeys(dataResourceUid)
        val (protocol, urls, uniqueTerms, params, customParams,lastChecked) = retrieveConnectionParameters(dataResourceUid)
        val strip = params.getOrElse("strip", false).asInstanceOf[Boolean]
        val incremental = params.getOrElse("incremental",false).asInstanceOf[Boolean]
        var loaded =false
        var maxLastModifiedDate:java.util.Date = null
        urls.foreach(url => {
          val (fileName,date) = downloadArchive(url,dataResourceUid,if(forceLoad)None else lastChecked)
          if(maxLastModifiedDate == null || date.after(maxLastModifiedDate))
            maxLastModifiedDate = date
          println("File last modified date: " + maxLastModifiedDate)
          if(fileName != null){
            val directory = new File(fileName)
            loadDirectory(directory,dataResourceUid, uniqueTerms, params,strip,incremental||logRowKeys,testFile)
            //directory.listFiles.foreach(file => if(file.isFile())loadFile(file,dataResourceUid, uniqueTerms, params,strip,incremental||logRowKeys,testFile) else logger.warn("Unable to load file " + file.getAbsolutePath()))
            loaded = true
          }
        })
        //now update the last checked and if necessary data currency dates
        if(!testFile){
          updateLastChecked(dataResourceUid, if(loaded) Some(maxLastModifiedDate) else None)
          if(!loaded)
            setNotLoadedForOtherPhases(dataResourceUid)
        }
    }
    //loads all the files in the subdirectories that are not multimedia
    def loadDirectory(directory:File, dataResourceUid:String, uniqueTerms:List[String], params:Map[String,String], stripSpaces:Boolean=false, logRowKeys:Boolean=false, test:Boolean=false){
        directory.listFiles.foreach(file => if(file.isFile()&& !MediaStore.isMediaFile(file))loadFile(file,dataResourceUid, uniqueTerms, params,stripSpaces,logRowKeys,test) else if(file.isDirectory) loadDirectory(file,dataResourceUid, uniqueTerms, params,stripSpaces,logRowKeys,test) else logger.warn("Unable to load as CSV: " + file.getAbsolutePath()))
    }
    
    
    def loadFile(file:File, dataResourceUid:String, uniqueTerms:List[String], params:Map[String,String], stripSpaces:Boolean=false, logRowKeys:Boolean=false, test:Boolean=false){
      
        val rowKeyWriter = getRowKeyWriter(dataResourceUid, logRowKeys)
        
        val quotechar = params.getOrElse("csv_text_enclosure", "\"").head
        val separator = {
          val separatorString = params.getOrElse("csv_delimiter", ",")
          if (separatorString == "\\t") '\t'
          else separatorString.toCharArray.head
        }
        val escape = params.getOrElse("csv_escape_char","|").head
        val reader =  new CSVReader(new InputStreamReader(new org.apache.commons.io.input.BOMInputStream(new FileInputStream(file))), separator, quotechar, escape)
    
        
        logger.info("Using CSV reader with the following settings quotes: " + quotechar + " separator: " + separator + " escape: " + escape)
        //match the column headers to dwc terms
        val dwcTermHeaders = {
        	val headerLine = reader.readNext
        	if(headerLine != null){
        	  val columnHeaders = headerLine.map(t => t.replace(" ", "").trim).toList
        	  DwC.retrieveCanonicals(columnHeaders)
        	} else {
        	  null
        	}
        }
        
        if(dwcTermHeaders == null){
          logger.warn("No content in file.")
          return
        }
        
        var currentLine = reader.readNext
        
        logger.info("Unique terms: " + uniqueTerms)
        logger.info("Column headers: " + dwcTermHeaders)
        
        val validConfig = uniqueTerms.forall(t => dwcTermHeaders.contains(t))
        if(!validConfig){
            throw new RuntimeException("Bad configuration for file: "+ file.getName + " for resource: " +
              dataResourceUid+". CSV file is missing unique terms.")
        }
        
        val institutionCodes = Config.indexDAO.getDistinctValues("data_resource_uid:"+dataResourceUid, "institution_code",100).getOrElse(List()).toSet[String]
        
        val collectionCodes = Config.indexDAO.getDistinctValues("data_resource_uid:"+dataResourceUid, "collection_code",100).getOrElse(List()).toSet[String]

        logger.info("The current institution codes for the data resource: " + institutionCodes)
        logger.info("The current collection codes for the data resource: " + collectionCodes)
        
        val newCollCodes=new scala.collection.mutable.HashSet[String]
        val newInstCodes= new scala.collection.mutable.HashSet[String]
        
        var counter = 1
        var newCount = 0
        var noSkipped = 0
        var startTime = System.currentTimeMillis
        var finishTime = System.currentTimeMillis
        while(currentLine!=null){
            counter += 1
            
            val columns = currentLine.toList
            if (columns.length >= dwcTermHeaders.size - 1){
                val map = (dwcTermHeaders zip columns).toMap[String,String].filter( {
                	case (key,value) => {
                    if(value!=null){
                      val upperCased = value.trim.toUpperCase
                      upperCased != "NULL" && upperCased != "N/A" && upperCased != "\\N" && upperCased != ""
                    } else {
                      false
                    }
                  }
                })
                
                //only continue if there is at least one nonnull unique term
                if(uniqueTerms.find(t => map.getOrElse(t,"").length>0).isDefined || uniqueTerms.length==0){
                   
                    val uniqueTermsValues = uniqueTerms.map(t => map.getOrElse(t,""))
                    
                                       
                    if(test){                      
                      newInstCodes.add(map.getOrElse("institutionCode", "<NULL>"))                      
                      newCollCodes.add(map.getOrElse("collectionCode", "<NULL>"))
                      val (uuid, isnew) =Config.occurrenceDAO.createOrRetrieveUuid(createUniqueID(dataResourceUid, uniqueTermsValues, stripSpaces))
                      if(isnew)
                        newCount +=1
                    }
                    
//                    if(!uniqueTerms.forall(t => map.getOrElse(t,"").length>0)){
//                     logger.warn("There is at least one null key " + uniqueTermsValues.mkString("|"))
//                   }
                  if(!test){
  	                val fr = FullRecordMapper.createFullRecord("", map, Versions.RAW)
  	                //load(dataResourceUid, fr, uniqueTermsValues)
  
                    if (fr.occurrence.associatedMedia != null){
                      //check for full resolvable http paths
                      val filesToImport = fr.occurrence.associatedMedia.split(";")
                      val filePathsInStore = filesToImport.map(fileName => {
                        //if the file name isnt a HTTP URL construct file absolute file paths
                        if(!fileName.startsWith("http://")){
                          val filePathBuffer = new ArrayBuffer[String]
                          filePathBuffer += "file:///"+file.getParent+File.separator+fileName
  
                          //val filePath = MediaStore.save(fr.uuid, dataResourceUid, "file:///"+file.getParent+File.separator+fileName)
                          //do multiple formats exist? check for files of the same name, different extension
                          val directory = file.getParentFile
                          val differentFormats = directory.listFiles(new au.org.ala.biocache.SameNameDifferentExtensionFilter(fileName))
                          differentFormats.foreach(file => {
                            filePathBuffer += "file:///" + file.getParent+File.separator + file.getName
                          })
  
                          filePathBuffer.toArray[String]
                        } else {
                          Array(fileName)
                        }
                      }).flatten
  
                      fr.occurrence.associatedMedia = filePathsInStore.mkString(";")
                    }
  
  	                //println(FullRecordMapper.fullRecord2Map(fr, Versions.RAW))
  	               
                    load(dataResourceUid, fr, uniqueTermsValues,true, false,stripSpaces,rowKeyWriter)
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
                println("Skipping line: " +counter + " incorrect number of columns (" +
                  columns.length + ")...headers (" + dwcTermHeaders.length + ")")
                println("First element : "+columns(0) +"...headers :" + dwcTermHeaders(0))
                println("last element : "+columns.last +"...headers :" + dwcTermHeaders.last)
            }
            //read next
            currentLine = reader.readNext
        }
        if(rowKeyWriter.isDefined){
          rowKeyWriter.get.flush
          rowKeyWriter.get.close
        }
        //check to see if the inst/coll codes are new
        if(test){
          val unknownInstitutions = newInstCodes &~ institutionCodes
          val unknownCollections = newCollCodes &~ collectionCodes
          if(unknownInstitutions.size > 0)
            logger.warn("Warning there are new institution codes in the set. " + unknownInstitutions)
          if(unknownCollections.size > 0)
            logger.warn("Warning there are new collection codes in the set. " + unknownCollections)
          logger.info("There are " + counter + " records in the file. The number of NEW records: " + newCount)
        }
        logger.info("Load finished for " + file.getName())
    }
}
