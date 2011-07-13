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

    def main(args: Array[String]): Unit = {

        var resourceUid = ""
        var localFilePath:Option[String] = None

        val parser = new OptionParser("load darwin core archive") {
            arg("<data resource UID>", "The UID of the data resource to load", {v: String => resourceUid = v})
            opt("l", "local", "skip the download and use local file", {v:String => localFilePath = Some(v) } )
        }
        if(parser.parse(args)){
            val l = new DwCALoader
            if(localFilePath.isEmpty){
            	l.load(resourceUid)
            } else {
                l.loadLocal(resourceUid, localFilePath.get)
            }
            //initialise the delete
            //update the collectory information
            l.updateLastChecked(resourceUid)
        }
    }
} 
    
class DwCALoader extends DataLoader {
    
    import FileHelper._
    import ReflectBean._
    
    def load(resourceUid:String){
    	val (protocol, url, uniqueTerms, params) = retrieveConnectionParameters(resourceUid)
    	val conceptTerms = mapConceptTerms(uniqueTerms)
        //download 
    	val fileName = downloadArchive(url,resourceUid)
        //load the DWC file
    	loadArchive(fileName, resourceUid, conceptTerms)
    }
    
    def loadLocal(resourceUid:String, fileName:String){
    	val (protocol, url, uniqueTerms, params) = retrieveConnectionParameters(resourceUid)
    	val conceptTerms = mapConceptTerms(uniqueTerms)
        //load the DWC file
    	loadArchive(fileName, resourceUid, conceptTerms)
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
            val uniqueTermValues = uniqueTerms.map(t => dwc.getProperty(t))
            
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

            //add the record uuid to the map
            fieldTuples + ("uuid"->recordUuid)
            //add the data resouce uid
            fieldTuples + ("dataResourceUid"-> resourceUid)
            //add last load time
            fieldTuples + ("lastLoadTime"-> loadTime)

            val rowKey = if(uniqueID.isEmpty) resourceUid + "|" + recordUuid else uniqueID.get
           
            //val recordUuid = UUID.randomUUID.toString
            val fullRecord = FullRecordMapper.createFullRecord(rowKey, fieldTuples.toArray, Raw)
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
}