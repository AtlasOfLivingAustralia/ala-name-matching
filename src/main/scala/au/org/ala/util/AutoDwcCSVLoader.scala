package au.org.ala.util
//import au.org.ala.biocache.DataLoader
import java.io.{File, FileInputStream,InputStream, FileOutputStream}
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.{FilenameUtils,FileUtils}
import org.apache.commons.compress.utils.IOUtils
import au.org.ala.biocache.DataLoader

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
    def load(dataResourceUid:String, includeIds:Boolean=false, forceLoad:Boolean = false){
        //TODO support complete reload by looking up webservice
        val (protocol, urls, uniqueTerms, params, customParams,lastChecked) = retrieveConnectionParameters(dataResourceUid)
        val strip = params.getOrElse("strip", false).asInstanceOf[Boolean]
        //clean out the dr load directory before downloading the new file.
        emptyTempFileStore(dataResourceUid)
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
    }
    
    def loadLocalFile(dataResourceUid:String, filePath:String, includeIds:Boolean){
        val (protocol, urls, uniqueTerms, params, customParams, lastChecked) = retrieveConnectionParameters(dataResourceUid)
        val strip = params.getOrElse("strip", false).asInstanceOf[Boolean]
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
            
            //NOw take the load files and use the DwcCSVLoader to load them
            dataFiles.foreach(dfile =>{ 
                if(includeIds || !dfile.getName().contains("dwc-id")){
                  if(dfile.getName.endsWith("gz")){
                      csvLoader.loadFile(dfile.extractGzip, dataResourceUid, uniqueTerms, params,stripSpaces) 
                  }
                  else{
                      csvLoader.loadFile(dfile, dataResourceUid, uniqueTerms, params, stripSpaces) 
                  }
                }
            })
        }
        
        //TODO use the id files to find out which records need to be deleted
        // File contains a complete list of the current ids will need to mark records and ten delete all records that have not been marked???
        
        //update the last time this data resource was loaded in the collectory
        //updateLastChecked(dataResourceUid)
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