package au.org.ala.util
import java.io._
import org.gbif.dwc.text.ArchiveFactory
import org.gbif.dwc.terms.DwcTerm
import scala.collection.mutable.ArrayBuffer
import au.org.ala.biocache._
import org.gbif.dwc.terms.ConceptTerm
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions
import org.apache.commons.lang3.StringUtils

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
    var logRowKeys = false;
    var testFile =false
    val parser = new OptionParser("load darwin core archive") {
      arg("<data resource UID>", "The UID of the data resource to load", {v: String => resourceUid = v})
      opt("l", "local", "skip the download and use local file", {v:String => localFilePath = Some(v) } )
      opt("log","log row keys to file - allows processing/indexing of changed records",{logRowKeys = true})
      opt("test", "test the file only do not load", {testFile=true})
    }
    if(parser.parse(args)){
      val l = new DwCALoader
      l.deleteOldRowKeys(resourceUid)
      if(localFilePath.isEmpty){
        l.load(resourceUid, logRowKeys,testFile)
      } else {
        l.loadLocal(resourceUid, localFilePath.get, logRowKeys,testFile)
      }
      //initialise the delete
      //update the collectory information
      l.updateLastChecked(resourceUid)
    }

      //shut down the persistence manager after all the files have been loaded.
    Config.persistenceManager.shutdown
  }
} 
    
class DwCALoader extends DataLoader {

  import FileHelper._
  import ReflectBean._
  import JavaConversions._

  def load(resourceUid:String, logRowKeys:Boolean=false, testFile:Boolean=false, forceLoad:Boolean = false){
    //remove the old files
    emptyTempFileStore(resourceUid)
    //remove the old row keys:
    deleteOldRowKeys(resourceUid)
    val (protocol, urls, uniqueTerms, params, customParams, lastChecked) = retrieveConnectionParameters(resourceUid)
    val conceptTerms = mapConceptTerms(uniqueTerms)
    val incremental = params.getOrElse("incremental", false).asInstanceOf[Boolean]
    val strip = params.getOrElse("strip", false).asInstanceOf[Boolean]
    var loaded = false
    var maxLastModifiedDate:java.util.Date = null
    urls.foreach(url => {
        //download
      val (fileName,date) = downloadArchive(url,resourceUid,if(forceLoad)None else lastChecked)
      if(maxLastModifiedDate == null || date.after(maxLastModifiedDate))
          maxLastModifiedDate = date
      println("File last modified date: " + maxLastModifiedDate)
      if(fileName != null){
        //load the DWC file
        loadArchive(fileName, resourceUid, conceptTerms, strip, logRowKeys||incremental,testFile)
        loaded = true
      }
    })
    //now update the last checked and if necessary data currency dates
    if(!testFile){
      updateLastChecked(resourceUid, if(loaded) Some(maxLastModifiedDate) else None)
      if(!loaded)
          setNotLoadedForOtherPhases(resourceUid)
    }
  }

  def loadLocal(resourceUid:String, fileName:String, logRowKeys:Boolean, testFile:Boolean){
    val (protocol, url, uniqueTerms, params, customParams,lastChecked) = retrieveConnectionParameters(resourceUid)
    val conceptTerms = mapConceptTerms(uniqueTerms)
    val strip = params.getOrElse("strip", false).asInstanceOf[Boolean]
      //load the DWC file
    loadArchive(fileName, resourceUid, conceptTerms, strip, logRowKeys, testFile)
  }

  def loadArchive(fileName:String, resourceUid:String, uniqueTerms:List[ConceptTerm], stripSpaces:Boolean, logRowKeys:Boolean, testFile:Boolean){
    println("Loading archive " + fileName + " for resource " + resourceUid + " with unique terms " + uniqueTerms + " stripping spaces " + stripSpaces + " incremental " + logRowKeys + " testing " + testFile)
    val rowKeyWriter = getRowKeyWriter(resourceUid, logRowKeys)
    val archive = ArchiveFactory.openArchive(new File(fileName))
    val iter = archive.iterator
    val terms = DwcTerm.values
    var count = 0
    var newCount=0

    val fieldMap =archive.getCore().getFields()
    val fieldShortName = fieldMap.keySet().toList
    val biocacheModelValues = DwC.retrieveCanonicals(fieldShortName.map(_.simpleName))
    val fieldToModelMap = (fieldShortName zip biocacheModelValues).toMap
    println(fieldShortName zip biocacheModelValues)


    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis
    var currentBatch = new ArrayBuffer[au.org.ala.biocache.FullRecord]

    val institutionCodes = Config.indexDAO.getDistinctValues("data_resource_uid:"+resourceUid, "institution_code",100).getOrElse(List()).toSet[String]

    val collectionCodes = Config.indexDAO.getDistinctValues("data_resource_uid:"+resourceUid, "collection_code",100).getOrElse(List()).toSet[String]

    println("The current institution codes for the data resource: " + institutionCodes)
    println("The current collection codes for the data resource: " + collectionCodes)

    val newCollCodes=new scala.collection.mutable.HashSet[String]
    val newInstCodes= new scala.collection.mutable.HashSet[String]

    while (iter.hasNext) {

      count += 1
      //the newly assigned record UUID
      val star = iter.next

      //the details of how to construct the UniqueID belong in the Collectory
      //val uniqueTermValues = uniqueTerms.map(t => dwc.getProperty(t))
      val uniqueID = {
          //create the unique ID
          if (!uniqueTerms.isEmpty) {
              val uniqueTermValues = uniqueTerms.map(t => star.core.value(t))
              val id =(List(resourceUid) ::: uniqueTermValues).mkString("|").trim
              Some(if(stripSpaces) id.replaceAll("\\s","") else id)
          } else {
              None
          }
      }

      if(testFile){
        //check to see if the key has at least on distinguishing value
        val icode = star.core.value(org.gbif.dwc.terms.DwcTerm.institutionCode)
        newInstCodes.add(if(icode == null)"<NULL>" else icode)
        val ccode = star.core.value(org.gbif.dwc.terms.DwcTerm.collectionCode)
        newCollCodes.add(if(ccode == null) "<NULL>" else ccode)
      }

      //create a map of properties
      val fieldTuples = new ListBuffer[(String, String)]()
      //NEED to use the star iterator so that custom field types are available
      fieldToModelMap.foreach(v=>{
        val (src,model) = v
        val property = star.core.value(src)
        if(StringUtils.isNotBlank(property))
          fieldTuples += (model -> property)
      })

      //lookup the column
      val (recordUuid, isNew) = {
          uniqueID match {
              case Some(value) => Config.occurrenceDAO.createOrRetrieveUuid(value)
              case None => (Config.occurrenceDAO.createUuid, true)
          }
      }

      //add the record uuid to the map
      fieldTuples += ("uuid" -> recordUuid)
      //add the data resouce uid
      fieldTuples += ("dataResourceUid"-> resourceUid)
      //add last load time
      fieldTuples += ("lastModifiedTime"-> loadTime)
      if(isNew){
          fieldTuples += ("firstLoaded"-> loadTime)
          newCount +=1
      }

      val rowKey = if(uniqueID.isEmpty) resourceUid + "|" + recordUuid else uniqueID.get
      if(rowKeyWriter.isDefined)
        rowKeyWriter.get.write(rowKey+"\n")
      //val recordUuid = UUID.randomUUID.toString
      if(!testFile){
        val fullRecord = FullRecordMapper.createFullRecord(rowKey, fieldTuples.toArray, Raw)
        //println("record UUID: "  + recordUuid)
        currentBatch += fullRecord
      }

      //debug
      if (count % 1000 == 0 && count > 0) {
        if(!testFile){
          Config.occurrenceDAO.addRawOccurrenceBatch(currentBatch.toArray)
        }
        finishTime = System.currentTimeMillis
        println(count + ", >> last key : " + uniqueID + ", UUID: " + recordUuid + ", records per sec: " + 1000 / (((finishTime - startTime).toFloat) / 1000f))
        startTime = System.currentTimeMillis
        //clear the buffer
        currentBatch.clear
      }
    }
    if(rowKeyWriter.isDefined){
      rowKeyWriter.get.flush
      rowKeyWriter.get.close
    }

    //check to see if the inst/coll codes are new
    if(testFile){
      val unknownInstitutions = newInstCodes &~ institutionCodes
      val unknownCollections = newCollCodes &~ collectionCodes
      if(unknownInstitutions.size > 0)
        println("Warning there are new institution codes in the set: " + unknownInstitutions.mkString(","))
      if(unknownCollections.size > 0)
        println("Warning there are new collection codes in the set: " + unknownCollections.mkString(","))

       //Report the number of new/existing records
        println("There are " + count + " records in the file. The number of NEW records: " + newCount)
    }
    //commit the batch
    Config.occurrenceDAO.addRawOccurrenceBatch(currentBatch.toArray)
    println("Finished DwC loader. Records processed: " + count)
    count
  }
}