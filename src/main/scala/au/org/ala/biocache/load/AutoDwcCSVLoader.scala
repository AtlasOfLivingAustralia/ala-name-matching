package au.org.ala.biocache.load

//import au.au.biocache.load.DataLoader
import java.io._
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.io.{FilenameUtils,FileUtils}
import org.apache.commons.compress.utils.IOUtils
import au.org.ala.biocache.Config
import au.com.bytecode.opencsv.CSVReader
import scala.Some
import scala.Console
import collection.mutable.ArrayBuffer
import au.org.ala.biocache.util.{OptionParser, FileHelper}
import au.org.ala.biocache.vocab.DwC

/**
 * Loads a data resource that provides reloads and incremental updates.
 * 
 *  Files of this form are produced according to the standard that Bryn developed.
 *  The archive will have:
 *  <ul>
 *  <li> data files - contains the information that need to be inserted or updated </li>
 *  <li>  id files - contain the identifying fields for all the current records for the data resource. 
 *  Records that don't have identifiers in the field will need to be removed from cassandra
 *  </ul>
 *  
 *  The id files will be treated as if they are data files. They will contain all the identifiers 
 *  (collection code, institution code and catalogue numbers) for the occurrences. Thus when they are loaded
 *  the last modified date will be updated and the record will be considered current. 
 */
object AutoDwcCSVLoader {
def main(args:Array[String]){

        var dataResourceUid = ""
        var localFilePath:Option[String] = None
        var processIds = false;
            
        val parser = new OptionParser("import darwin core headed CSV") {
            arg("<data-resource-uid>", "the data resource to import", {v: String => dataResourceUid = v})
            opt("l", "local", "skip the download and use local file", {v:String => localFilePath = Some(v) })
            opt("ids", "load the ids files", {processIds=true})
        }

        if(parser.parse(args)){
            val l = new AutoDwcCSVLoader
            try{
                localFilePath match {
                    case None => l.load(dataResourceUid, processIds)
                    case Some(v) => l.loadLocalFile(dataResourceUid, v, processIds)
                }
                //initialise the delete
                //update the collectory information               
                l.updateLastChecked(dataResourceUid)
            }
            catch{
                case e:Exception =>e.printStackTrace
            }
            finally{
                l.pm.shutdown
                Console.flush()
                Console.err.flush()
                exit(0)
            }
        }
    }
}

class AutoDwcCSVLoader extends DataLoader{
    import FileHelper._
    //val loadPattern ="""([\x00-\x7F\s]*_dwc.csv)""".r //"""(dwc-data[\x00-\x7F\s]*.gz)""".r
    val loadPattern = """([\x00-\x7F\s]*dwc[\x00-\x7F\s]*.csv[\x00-\x7F\s]*)""".r
    println(loadPattern.toString())
    def load(dataResourceUid:String, includeIds:Boolean=true, forceLoad:Boolean = false){
        //TODO support complete reload by looking up webservice
        val (protocol, urls, uniqueTerms, params, customParams,lastChecked) = retrieveConnectionParameters(dataResourceUid)
        val strip = params.getOrElse("strip", false).asInstanceOf[Boolean]
        //clean out the dr load directory before downloading the new file.
        emptyTempFileStore(dataResourceUid)
        //remove the old file 
        deleteOldRowKeys(dataResourceUid)
        var loaded =false
        var maxLastModifiedDate:java.util.Date = null
        //the supplied url should be an sftp string to the directory that contains the dumps
        urls.foreach(url=>{
          if(url.startsWith("sftp")){
            val fileDetails = sftpLatestArchive(url, dataResourceUid,if(forceLoad)None else lastChecked)
            if(fileDetails.isDefined){              
              val (filePath,date) = fileDetails.get
              if(maxLastModifiedDate == null || date.after(maxLastModifiedDate))
                  maxLastModifiedDate = date
              loadAutoFile(new File(filePath), dataResourceUid, uniqueTerms, params, includeIds,strip)
              loaded = true
            }
          }
          else
            logger.error("Unable to process " + url + " with the auto loader")
        })
        //now update the last checked and if necessary data currency dates
        updateLastChecked(dataResourceUid, if(loaded) Some(maxLastModifiedDate) else None)
        if(!loaded)
            setNotLoadedForOtherPhases(dataResourceUid)
    }
    
    def loadLocalFile(dataResourceUid:String, filePath:String, includeIds:Boolean){
        val (protocol, urls, uniqueTerms, params, customParams, lastChecked) = retrieveConnectionParameters(dataResourceUid)
        val strip = params.getOrElse("strip", false).asInstanceOf[Boolean]
        //remove the old file 
        deleteOldRowKeys(dataResourceUid)
        loadAutoFile(new File(filePath),dataResourceUid, uniqueTerms, params, includeIds,strip) 
    }
    
    def loadAutoFile(file:File, dataResourceUid:String, uniqueTerms:List[String], params:Map[String,String], includeIds:Boolean, stripSpaces:Boolean){
        //From the file extract the files to load
        val baseDir = file.getParent
        val csvLoader = new DwcCSVLoader
        if(file.getName.endsWith(".tar.gz") || file.getName().endsWith(".zip")){
            //set up the lists of data and id files
            val dataFiles = new scala.collection.mutable.ListBuffer[File]
            //val idFiles = new scala.collection.mutable.ListBuffer[File]
            
            //now find a list of files that obey the data load file
            val tarInputStream = {
              if(file.getName.endsWith("tar.gz")){
                  //gunzip the file
                  val unzipedFile= file.extractGzip
                  new ArchiveStreamFactory().createArchiveInputStream("tar", new FileInputStream(unzipedFile))
              }
              else{
                new ArchiveStreamFactory().createArchiveInputStream("zip", new FileInputStream(file))
              }
              }
            var entry = tarInputStream.getNextEntry
            while(entry != null){
                val name:String = FilenameUtils.getName(entry.getName)
                logger.debug("FILE from archive name: " +name)
                name match{
                    case loadPattern(filename) => dataFiles += extractTarFile(tarInputStream, baseDir,entry.getName)
                    case _ => //do nothing with the file
                }
                entry = tarInputStream.getNextEntry
            }
            logger.info(dataFiles.toString())

            //val dwcfiles = dataFiles.fil
             var validRowKeys= List[String]()
            //NOw take the load files and use the DwcCSVLoader to load them
            dataFiles.foreach(dfile =>{ 
                if((!dfile.getName().contains("dwc-id") && !dfile.getName().contains("dwcid"))){
                  val storeKeys = !dfile.getName().contains("dwc-id") && !dfile.getName().contains("dwcid")
                  logger.info("Loading " + dfile.getName() + " storing the keys for reprocessing: " + storeKeys)
                  if(dfile.getName.endsWith("gz")){
                      csvLoader.loadFile(dfile.extractGzip, dataResourceUid, uniqueTerms, params,stripSpaces,logRowKeys=storeKeys)
                  }
                  else{
                      csvLoader.loadFile(dfile, dataResourceUid, uniqueTerms, params, stripSpaces,logRowKeys=storeKeys)
                  }
                } else{
                  //load the id's into a list of valid rowKeys
                  validRowKeys ++= extractValidRowKeys(if(dfile.getName.endsWith(".gz")) dfile.extractGzip else dfile, dataResourceUid, uniqueTerms,params, stripSpaces)
                  logger.info("Number of validRowKeys: " + validRowKeys.size)
                }
            })

          //Create the list of distinct row keys that are no longer in the source system
          //This is achieved by extracting all the current row keys from the index and removing the values that we loaded from the current id files.

          val listbuf = new ArrayBuffer[String]()
          logger.info("Retrieving the current rowKeys for the dataResource...")
          Config.indexDAO.streamIndex(map=>{
            listbuf += map.get("row_key").toString
            true
          }, Array("row_key"),"data_resource_uid:"+dataResourceUid,Array(),Array(),None)
          logger.info("Number of currentrowKeys: " + listbuf.size)
          val deleted =  listbuf.toSet &~ validRowKeys.toSet
          logger.info("Number to delete deleted " + deleted.size)
          if (deleted.size >0){
            val writer = getDeletedFileWriter(dataResourceUid)
            deleted.foreach(line =>writer.write(line +"\n"))
            writer.flush()
            writer.close()
          }
        }
        
        //TODO use the id files to find out which records need to be deleted
        // File contains a complete list of the current ids will need to mark records and ten delete all records that have not been marked???
        
        //update the last time this data resource was loaded in the collectory
        //updateLastChecked(dataResourceUid)
    }

  /**
   * Takes a DWC CSV file and outputs a list of all the unique identifiers for the records
   * @param file
   * @param dataResourceUid
   * @param uniqueTerms
   * @param params
   * @param stripSpaces
   * @return
   */
    def extractValidRowKeys(file:File, dataResourceUid:String,uniqueTerms:List[String], params:Map[String,String], stripSpaces:Boolean):List[String]={
      logger.info("Extracting the valid row keys from " + file.getAbsolutePath)
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

      var currentLine = reader.readNext

      logger.info("Unique terms: " + uniqueTerms)
      logger.info("Column headers: " + dwcTermHeaders)

      val validConfig = uniqueTerms.forall(t => dwcTermHeaders.contains(t))
      if(!validConfig){
        throw new RuntimeException("Bad configuration for file: "+ file.getName + " for resource: " +
          dataResourceUid+". CSV file is missing unique terms.")
      }
      val listbuf = new ArrayBuffer[String]()
      while(currentLine!=null){

        val columns = currentLine.toList
        if (columns.length >= dwcTermHeaders.size - 1){
          val map = (dwcTermHeaders zip columns).toMap[String,String].filter( {
            case (key,value) => {
              if(value != null){
                val upperCased = value.trim.toUpperCase
                upperCased != "NULL" && upperCased != "N/A" && upperCased != "\\N" && upperCased != ""
              } else {
                false
              }
            }
          })

        val uniqueTermsValues = uniqueTerms.map(t => map.getOrElse(t,""))
        listbuf += createUniqueID(dataResourceUid,uniqueTermsValues,stripSpaces)
        currentLine = reader.readNext()
        }
      }

      listbuf.toList
    }
    def extractTarFile(io:InputStream,baseDir:String, filename:String):File={        
        //construct the file
        val file = new File(baseDir+ System.getProperty("file.separator") + filename)
        FileUtils.forceMkdir(file.getParentFile)
        val fos = new FileOutputStream(file)
        IOUtils.copy(io,fos)
        fos.flush
        fos.close
        file
    }
}