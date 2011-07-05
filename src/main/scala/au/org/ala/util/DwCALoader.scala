package au.org.ala.util
import scala.io.Source
import scala.util.parsing.json._
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io._
import java.util.jar.JarFile
import org.gbif.dwc.text.ArchiveFactory
import org.gbif.dwc.terms.DwcTerm
import scala.collection.mutable.ArrayBuffer
import au.org.ala.biocache._
import org.gbif.dwc.terms.TermFactory
import org.gbif.dwc.terms.ConceptTerm
import org.apache.commons.io.FilenameUtils
import scala.collection.mutable.ListBuffer
import java.util.UUID

/**
 * Loading utility for pulling in a darwin core archive file.
 * 
 * This class will retrieve details from the collectory and load in a darwin core archive.
 * The workflow is the following:
 * 
 * <ol>
 * <li>Retrieve JSON from collectory</li>
 * <li>Download the zipped archive to the local file system</li>
 * <li>Extract</li>
 * <li>Load</li>
 * </ol>
 * 
 * Optimisations - with a significant memory allocation, this loader _could_ retrieve all
 * uniqueKey to UUID mappings first.
 * 
 * This makes this class completely reliant upon configuration in the collectory.
 * 
 * @author Dave Martin
 */
object DwCALoader {

    import FileHelper._
    import ReflectBean._

    def main(args: Array[String]): Unit = {

        var resourceUid = ""
        var localFilePath:Option[String] = None
        var temporaryFileStore = "/data/biocache-load"
        
        val parser = new OptionParser("load darwin core archive") {
            arg("<data resource UID>", "The UID of the data resource to load", {v: String => resourceUid = v})
            opt("l", "local", "skip the download and use local file", {v:String => localFilePath = Some(v) } )
            opt("t", "temp-store", "temporary file store", {v:String => temporaryFileStore = v } )
        }
        if(parser.parse(args)){
            if(localFilePath.isEmpty){
            	load(resourceUid, temporaryFileStore)
            } else {
                loadLocal(resourceUid, localFilePath.get)
            }
        }
    }
    
    def downloadArchive(url:String, resourceUid:String, temporaryFileStore:String) : String = {
        val tmpStore = new File(temporaryFileStore)
        if(!tmpStore.exists){
        	FileUtils.forceMkdir(tmpStore)
        }

        print("Downloading zip file.....")
        val in = (new java.net.URL(url)).openStream
        val file = new File(temporaryFileStore + resourceUid + ".zip")
        val out = new FileOutputStream(file)
        val buffer: Array[Byte] = new Array[Byte](1024)
        var numRead = 0
        while ({ numRead = in.read(buffer); numRead != -1 }) {
            out.write(buffer, 0, numRead)
            out.flush
        }
        printf("Downloaded. File size: %skB\n", file.length / 1024)

        out.flush
        in.close
        out.close

        //extract the file
        file.extractZip

        val fileName = FilenameUtils.removeExtension(file.getAbsolutePath)
        println("Archive extracted to directory: " + fileName)
        fileName
    }
    
    def load(resourceUid:String, temporaryFileStore:String){
    	val (url, uniqueTerms) = retrieveConnectionDetails(resourceUid)
        //download 
    	val fileName = downloadArchive(url,resourceUid, temporaryFileStore)
        //load the DWC file
    	loadArchive(fileName, resourceUid, uniqueTerms)
    }
    
    def loadLocal(resourceUid:String, fileName:String){
    	val (url, uniqueTerms) = retrieveConnectionDetails(resourceUid)
        //load the DWC file
    	loadArchive(fileName, resourceUid, uniqueTerms)
    }
    
    def loadArchive(fileName:String, resourceUid:String, uniqueTerms:List[ConceptTerm]){
        val archive = ArchiveFactory.openArchive(new File(fileName))
        val iter = archive.iteratorDwc
        val terms = DwcTerm.values
        var count = 0

        var startTime = System.currentTimeMillis
        var finishTime = System.currentTimeMillis
        var currentBatch = new ArrayBuffer[au.org.ala.biocache.FullRecord]

        while (iter.hasNext) {

            count += 1
            //the newly assigned record UUID
            val dwc = iter.next

            //the details of how to construct the UniqueID belong in the Collectory
            val uniqueID = {
                //create the unique ID
                if (!uniqueTerms.isEmpty) {
                    val uniqueTermValues = uniqueTerms.map(t => dwc.getProperty(t))
                    Some((List(resourceUid) ::: uniqueTermValues).mkString("|"))
                } else {
                    None
                }
            }

            //create a map of properties
            val fieldTuples = new ListBuffer[(String, String)]() 
            terms.foreach(term => {
               val property = dwc.getterWithOption(term.simpleName)
               if (!property.isEmpty && property.get.toString.trim.length > 0)
                fieldTuples + (term.simpleName -> property.get.toString)
            })

            //lookup the column
            val recordUuid = {
                uniqueID match {
                    case Some(value) => Config.occurrenceDAO.createOrRetrieveUuid(value)
                    case None => Config.occurrenceDAO.createUuid
                }
            }
            
            //val recordUuid = UUID.randomUUID.toString
            val fullRecord = FullRecordMapper.createFullRecord(recordUuid, fieldTuples.toArray, Raw)
            //println("record UUID: "  + recordUuid)
            currentBatch += fullRecord

            //debug
            if (count % 1000 == 0 && count > 0) {
                Config.occurrenceDAO.addRawOccurrenceBatch(currentBatch.toArray)
                finishTime = System.currentTimeMillis
                println(count + ", >> last key : " + uniqueID + ", UUID: " + recordUuid + ", records per sec: " + 1000 / (((finishTime - startTime).toFloat) / 1000f))
                startTime = System.currentTimeMillis
                //clear the buffer
                currentBatch.clear
            }
        }
        //commit the batch
        Config.occurrenceDAO.addRawOccurrenceBatch(currentBatch.toArray)
        Config.persistenceManager.shutdown
        println("Finished DwC loader. Records processed: " + count)
        count
    }
    
    def retrieveConnectionDetails(resourceUid: String): (String, List[org.gbif.dwc.terms.ConceptTerm]) = {
      
      val json = Source.fromURL("http://collections.ala.org.au/ws/dataResource/" + resourceUid + ".json").getLines.mkString
      val map = JSON.parseFull(json).get.asInstanceOf[Map[String, AnyRef]]
      val connectionParameters = JSON.parseFull(map("connectionParameters").asInstanceOf[String]).get.asInstanceOf[Map[String, AnyRef]]
      val url = connectionParameters("url").asInstanceOf[String]
      val termFactory = new TermFactory
      val uniqueTerms: List[ConceptTerm] = connectionParameters.getOrElse("termsForUniqueKey", List[String]()).asInstanceOf[List[String]].map(term =>
          termFactory.findTerm(term))
      (url, uniqueTerms)
    }
}