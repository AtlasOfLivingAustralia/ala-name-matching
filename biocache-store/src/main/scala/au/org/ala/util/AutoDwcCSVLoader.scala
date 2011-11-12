package au.org.ala.util
import au.org.ala.biocache.DataLoader
import java.io.{File, FileInputStream,InputStream, FileOutputStream}
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.{FilenameUtils,FileUtils}
import org.apache.commons.compress.utils.IOUtils

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
 */
object AutoDwcCSVLoader {
def main(args:Array[String]){

        var dataResourceUid = ""
        var localFilePath:Option[String] = None
            
        val parser = new OptionParser("import darwin core headed CSV") {
            arg("<data-resource-uid>", "the data resource to import", {v: String => dataResourceUid = v})
            opt("l", "local", "skip the download and use local file", {v:String => localFilePath = Some(v) } )
        }

        if(parser.parse(args)){
            val l = new AutoDwcCSVLoader
            try{
                localFilePath match {
                    case None => println("Unsupported option")
                    case Some(v) => l.loadLocalFile(dataResourceUid, v)
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
    val loadPattern = """(dwc-data[\x00-\x7F\s]*.gz)""".r
    
    def load(dataResourceUid:String){
        //TODO support complete reload by looking up webservice
        val (protocol, url, uniqueTerms, params, customParams) = retrieveConnectionParameters(dataResourceUid)
        //clean out the dr load directory before downloading the new file.
        
        //the url supplied should give a list of files. We need to grab the file with the latest timestamp
    }
    
    def loadLocalFile(dataResourceUid:String, filePath:String){
        val (protocol, urls, uniqueTerms, params, customParams) = retrieveConnectionParameters(dataResourceUid)
        loadAutoFile(new File(filePath),dataResourceUid, uniqueTerms, params) 
    }
    
    def loadAutoFile(file:File, dataResourceUid:String, uniqueTerms:List[String], params:Map[String,String]){
        //From the file extract the files to load
        val baseDir = file.getParent
        val csvLoader = new DwcCSVLoader
        if(file.getName.endsWith(".tar.gz")){
            //set up the lists of data and id files
            val dataFiles = new scala.collection.mutable.ListBuffer[File]
            val idFiles = new scala.collection.mutable.ListBuffer[File]
            //gunzip the file
            val unzipedFile= file.extractGzip
            //now find a list of files that obey the data load file
            val tarInputStream = new ArchiveStreamFactory().createArchiveInputStream("tar", new FileInputStream(unzipedFile)).asInstanceOf[TarArchiveInputStream]
            var entry = tarInputStream.getNextEntry
            while(entry != null){
                val name:String = FilenameUtils.getName(entry.getName)                
                name match{
                    case loadPattern(filename) => dataFiles += extractTarFile(tarInputStream, baseDir,entry.getName)
                    case _ => //do nothing with the file
                }
                entry = tarInputStream.getNextEntry
            }
            println(dataFiles)
            
            //NOw take the load files and use the DwcCSVLoader to load them
            dataFiles.foreach(dfile =>{                
                if(dfile.getName.endsWith("gz")){
                    csvLoader.loadFile(dfile.extractGzip, dataResourceUid, uniqueTerms, params) 
                }
                else{
                    csvLoader.loadFile(dfile, dataResourceUid, uniqueTerms, params) 
                }
            })
        }
        
        //TODO use the id files to find out which records need to be deleted
        // File contains a complete list of the current ids will need to mark records and ten delete all records that have not been marked???
        
        //update the last time this data resource was loaded in the collectory
        updateLastChecked(dataResourceUid)
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