package au.org.ala.util
import au.org.ala.biocache.Config
import org.apache.solr.client.solrj.util.ClientUtils
import scala.collection.JavaConversions
import scala.collection.mutable.ArrayBuffer
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.reflect.BeanProperty
import java.io.FileWriter
import java.io.File
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.DateUtils
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import au.org.ala.biocache.AssertionCodes
import au.org.ala.biocache.QualityAssertion
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ArrayBlockingQueue
import org.slf4j.LoggerFactory
import org.apache.commons.io.FileUtils
import java.util.ArrayList
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.annotation.JsonInclude.Include
import au.org.ala.biocache.qa.QaPasser

/**
 * 
 * Duplication detection is only possible if latitude and longitude are provided.
 * Step 1
 *  a) Get a distinct  list of species lsids that have been matched
 *  b) Get a distinct list of subspecies lsids (without species lsisds) that have been matched
 * Step 2
 *  a) Break down all the records into groups based on the occurrence year - all null year (thus date) records will be handled together. 
 * Step 3
 *  a) within the year groupings break down into groups based on months - all nulls will be placed together
 *  b) within month groupings break down into groups based on event date - all nulls will be placed together
 * Step 4 
 *  a) With the smallest grained group from Step 3 group all the similar "collectors" together null or unknown collectors will be handled together
 *  b) With the collector groups determine which of the 
 */

object DuplicationDetection{
  import FileHelper._
  import JavaConversions._
  val logger = LoggerFactory.getLogger("DuplicateDetection")
  var rootDir = "/data/dedup/"

  def main(args:Array[String]){
    var all = false
    var exist = false
    var guid:Option[String] = None
    var speciesFile:Option[String] = None
    var threads = 4
    var cleanup = false
    var load = false
    var incremental = false
    var removeObsoleteData =false
    var offlineDir = "/data/offline/exports"

    //Options to perform on all "species", select species, use existing file or download
    val parser = new OptionParser("Duplication Detection - Detects duplication based on a matched species.") {
      opt("all", "detect duplicates for all species", { all = true })
      opt("g", "guid", "A single guid to test for duplications", { v: String => guid = Some(v)})
      opt("exist","use existing occurrence dumps",{exist = true})
      opt("inc","perform an incremental duplication detection based on the last time it was run",{incremental = true})
      opt("cleanup","cleanup the temporary files that get created",{cleanup = true})
      opt("load", "load to duplicates into the database",{load=true})
      opt("f","file","A file that contains a list of species guids to detect duplication for",{v: String => speciesFile = Some(v)})
      opt("removeold", "Removes the duplicate information for records that are no longer duplicates",{removeObsoleteData = true})
      opt("od","offlinedir","The offline directory that contains the export files.",{v:String => offlineDir =v})
      intOpt("t","threads" ," The number of concurrent species duplications to perform.",{v:Int => threads=v})
    }

    if(parser.parse(args)){
      //ensure that we have either all, guidsToTest or speciesFile
      val lastRunDate:Option[String] = if(incremental) Config.duplicateDAO.getLastDuplicationRun() else None
      if(removeObsoleteData){
        removeObsoleteDuplicates(speciesFile)
      }
      else if(all){
        //download all the species guids
        val filename = rootDir + "dd_all_species_guids"
        //val args = if(lastRunDate.isDefined) Array("species_guid",filename, "-fq","last_load_date:["+lastRunDate.get+" TO *]","--open") else Array("species_guid",filename,"--open")
        //export the scientific names that have had records (re)loaded since the last run OR export all species names.
        //ExportFacet.main(args)
        //now detect the duplicates
        detectDuplicates(new File(filename), threads,exist,cleanup,load,offlineDir)
      }
      else if(guid.isDefined){
        //just a single detection - ignore the thread settings etc...
        val dd = new DuplicationDetection
        val datafilename = rootDir+"dd_data_"+guid.get.replaceAll("[\\.:]","_") + ".txt"
        val passedfilename = rootDir+"passed"+guid.get.replaceAll("[\\.:]","_") + ".txt"
        val dupfilename = rootDir+"duplicates_"+guid.get.replaceAll("[\\.:]","_") + ".txt"
        val indexfilename = rootDir+"reindex_"+guid.get.replaceAll("[\\.:]","_") + ".txt"
        val olddup = rootDir+"olddup_"+guid.get.replaceAll("[\\.:]","_") + ".txt"
        if(load){
          dd.loadDuplicates(guid.get, threads, dupfilename, new FileWriter(indexfilename), new FileWriter(olddup))
          IndexRecords.indexList(new File(indexfilename), false)
          updateLastDuplicateTime()
        }
        else
          dd.detect(datafilename,new FileWriter(dupfilename), new FileWriter(passedfilename),guid.get,shouldDownloadRecords= !exist ,cleanup=cleanup)
        //println(new DuplicationDetection().getCurrentDuplicates(guid.get))

        Config.persistenceManager.shutdown
        Config.indexDAO.shutdown
      }
      else if(speciesFile.isDefined){
        detectDuplicates(new File(speciesFile.get), threads, exist, cleanup,load)
      }
      else{
        parser.showUsage
      }
    }
  //new DuplicationDetection().detect("urn:lsid:biodiversity.org.au:afd.taxon:b76f8dcf-fabd-4e48-939c-fd3cafc1887a")
  //new DuplicationDetection().detect("urn:lsid:biodiversity.org.au:afd.taxon:9e23c727-f3b0-4c29-a345-9cbd306eed84")
    //new DuplicationDetection().detect("urn:lsid:biodiversity.org.au:apni.taxon:425841")
  }

  
  def removeObsoleteDuplicates(filename:Option[String] ){
     val olddupfilename = filename.getOrElse(rootDir + "olddups.txt")
     val file = new File(olddupfilename)
     file.foreachLine(line =>{
       val parts = line.split("\t")
       val uuid = parts(1)       
       Config.duplicateDAO.deleteObsoleteDuplicate(uuid)
     })
  }
  
  def detectDuplicates(file:File, threads:Int, exist:Boolean, cleanup:Boolean, load:Boolean, offlineDir:String=""){
    //val queue = new ArrayBlockingQueue[String](100)
    //create the consumer threads
    
    //val pool = Array.fill(threads){ val p = new GuidConsumer(queue,{guid => new DuplicationDetection().detect(guid,!exist)}); p.start }
    var ids=0
    val pool:Array[Thread] = Array.fill(threads){
      val dir = rootDir + ids + File.separator
      FileUtils.forceMkdir(new File(dir))
      val sourceFile = dir + "dd_data.txt"
      val dupfilename = dir + "duplicates.txt"
      val passedfilename = dir + "passed.txt"
      val indexfilename = dir + "reindex.txt"
      val olddupfilename = dir + "olddups.txt"
      
      //val duplicateWriter = new FileWriter(new File(dupfilename))      
      val p = if(load){
        new Thread(){
          override def run(){
            new DuplicationDetection().loadMulitpleDuplicatesFromFile(dupfilename, passedfilename, threads,new FileWriter(new File(indexfilename)), new FileWriter(new File(olddupfilename)))
            //now reindex all the items
            IndexRecords.indexList(new File(indexfilename),false)
            }
        }
      } else {
        val sourceFileName = offlineDir + File.separator + ids +File.separator+ "species.out"
        new Thread(){
          override def run() {
            //the writers should override the files because they will only ever have one instance...
            new DuplicationDetection().detectMultipleDuplicatesFromFile(sourceFileName, new FileWriter(dupfilename), new FileWriter(passedfilename), threads)
          }
        }
//        FileUtils.deleteQuietly(new File(dupfilename))
//        new StringConsumer(queue,ids,{guid =>
//
//        val dd = new DuplicationDetection();
//        dd.detect(sourceFile,new FileWriter(dupfilename, true), new FileWriter(passedfilename, true),guid,shouldDownloadRecords= !exist ,cleanup=cleanup)})
      }

      ids += 1
      p.start
      p
    }
    
//    if(!load){
//      //add to the queue
//      file.foreachLine(line => queue.put(line.trim))
//    }
    pool.foreach(t =>if(t.isInstanceOf[StringConsumer]) t.asInstanceOf[StringConsumer].shouldStop = true)
    pool.foreach(_.join)
    if(load){
      //need to update the last duplication detection time
      updateLastDuplicateTime()
      //need to merge all the obsolete duplicates into 1 file
      val baseFile = new File(rootDir + "olddups.txt")
      for(i <- 0 to threads-1){
        val ifile = new File(rootDir + i + File.separator+"olddups.txt")
        baseFile.append(ifile)
      }
    }
    Config.persistenceManager.shutdown
    Config.indexDAO.shutdown
  }
  
  def updateLastDuplicateTime(){
    val date = DateUtils.truncate(new java.util.Date(), java.util.Calendar.DAY_OF_MONTH)
    val cal = new java.util.GregorianCalendar()
    cal.setTime(date)
    cal.add(java.util.Calendar.HOUR, -24)   
    Config.duplicateDAO.setLastDuplicationRun(cal.getTime())
  }
}

class GenericConsumer[T](q:BlockingQueue[T], id:Int, proc: (T,Int) =>Unit) extends Thread {
  var shouldStop = false
  override def run(){
    while(!shouldStop || q.size()>0){
      try{
        //wait 1 second before assuming that the queue is empty
        val value = q.poll(1,java.util.concurrent.TimeUnit.SECONDS)
        if(value !=null){
          DuplicationDetection.logger.debug("Generic Consumer " + id + " is handling " + value)
          proc(value, id)
        }
      }
      catch{
        case e:Exception=> e.printStackTrace()
      }
    }
    println("Stopping " + id)
  }
}

//A generic threaded consumer that takes a string and calls the supplied proc
class StringConsumer(q:BlockingQueue[String],id:Int,proc:String=>Unit) extends Thread{
  var shouldStop = false;
  override def run(){
    while(!shouldStop || q.size()>0){
      try{
        //wait 1 second before assuming that the queue is empty
        val guid = q.poll(1,java.util.concurrent.TimeUnit.SECONDS)
        if(guid !=null){
        DuplicationDetection.logger.debug("Guid Consumer " + id + " is handling " + guid)
        proc(guid)        
        }
      }
      catch{
        case e:Exception=> e.printStackTrace()
      }
    }
    println("Stopping " + id)
  }
}

class CountAwareFacetConsumer(q:BlockingQueue[String],id:Int,proc:Array[String]=>Unit, countSize:Int=0, minSize:Int=1) extends Thread{
  var shouldStop =false
  override def run(){
    val buf = new ArrayBuffer[String]()
    var counter =0
    var batchSize=0
    while(!shouldStop || q.size()>0){
      try{
        //wait 1 second before assuming that the queue is empty
        val value = q.poll(1,java.util.concurrent.TimeUnit.SECONDS)
        if(value !=null){
          DuplicationDetection.logger.debug("Count Aware Consumer " + id + " is handling " + value)
          val values = value.split("\t")
          val count = Integer.parseInt(values(1))
          if (count >= minSize){
            counter+=count
            batchSize+=1
            buf+= values(0)
            if(counter >=countSize || batchSize ==200){
              val array = buf.toArray
              buf.clear()
              counter =0
              batchSize=0
              proc(array)
            }
          }
        }
      }
      catch{
        case e:Exception=> e.printStackTrace()
      }
    }
    println("Stopping " + id)
  }
}

//TODO Use the "sensitive" coordinates for sensitive species
class DuplicationDetection{
  import JavaConversions._
  import FileHelper._
  val baseDir = "/tmp"
  val duplicatesFile = "duplicates.txt"
  val duplicatesToReindex = "duplicatesreindex.txt"
  val filePrefix = "dd_data.txt"
  val fieldsToExport = Array("row_key", "id", "species_guid", "year", "month", "occurrence_date", "point-1", "point-0.1", 
                             "point-0.01","point-0.001", "point-0.0001","lat_long","raw_taxon_name", "collectors", "duplicate_status", "duplicate_record")
  val speciesFilters = Array("lat_long:[* TO *]")
  // we have decided that a subspecies can be evalutated as part of the species level duplicates
  val subspeciesFilters = Array("lat_long:[* TO *]", "-species_guid:[* TO *]") 
  
  val mapper = new ObjectMapper
  //mapper.registerModule(DefaultScalaModule)
  mapper.setSerializationInclusion(Include.NON_NULL)


  /**
   * Takes the a dumpfile that was generated from the ExportAllRecordFacetFilter in mutiple threads
   * Each file will be ordered by species guid
   *
   * @param sourceFileName
   * @param threads
   */
  def detectMultipleDuplicatesFromFile(sourceFileName:String, duplicateWriter:FileWriter, passedWriter:FileWriter, threads:Int){


    val reader =  new CSVReader(new FileReader(sourceFileName),'\t', '`', '~')

    var currentLine = reader.readNext //first line is header
    val buff = new ArrayBuffer[DuplicateRecordDetails]
    var counter =0
    var currentLsid=""
    while(currentLine !=  null){
      /*
      Array("row_key", "id", "species_guid","subspecies_guid", "year", "month", "occurrence_date", "point-1", "point-0.1",
      "point-0.01","point-0.001", "point-0.0001","lat_long","raw_taxon_name", "collectors", "duplicate_status", "duplicate_record", "latitude","longitude",
      "el882","el889","el887","el865","el894")
       */
      if(currentLine.size>=16){
        counter +=1
        if(counter % 10000 == 0) {
          DuplicationDetection.logger.info("Loaded into memory : " + counter +" + records")
        }
        val rowKey = currentLine(0)
        val uuid = currentLine(1)
        val taxon_lsid = currentLine(2)
        if (currentLsid != taxon_lsid ){
          if(buff.size>0){
            DuplicationDetection.logger.info("Read in " + counter + " records for " + currentLsid)
            //perform the duplication detection with the records that we have loaded.
            performDetection(buff.toList, duplicateWriter, passedWriter)
            buff.clear()
          }
          currentLsid = taxon_lsid
          counter=1
          DuplicationDetection.logger.info("Starting to detect duplicates for " + currentLsid)
        }
        val year = StringUtils.trimToNull(currentLine(4))
        val month = StringUtils.trimToNull(currentLine(5))

        val date: java.util.Date = try {
          DateUtils.parseDate(currentLine(6),"EEE MMM dd hh:mm:ss zzz yyyy")
        } catch {
          case _:Exception => null
        }
        val day = if(date != null) Integer.toString(date.getDate()) else null
        val rawName = StringUtils.trimToNull(currentLine(13))
        val collector = StringUtils.trimToNull(currentLine(14))
        val oldStatus = StringUtils.trimToNull(currentLine(15))
        val oldDuplicateOf = StringUtils.trimToNull(currentLine(16).replaceAll("\\[","").replaceAll("\\]",""))
        buff += new DuplicateRecordDetails(rowKey,uuid,taxon_lsid,year,month,day,currentLine(7), currentLine(8),
          currentLine(9),currentLine(10),currentLine(11),currentLine(12),rawName, collector, oldStatus,oldDuplicateOf)
      }
      else{
        DuplicationDetection.logger.warn("lsid " + currentLine(0) + " line " + counter + " has incorrect column number: " + currentLine.size)
      }
      currentLine = reader.readNext
    }
    DuplicationDetection.logger.info("Read in " + counter + " records for " + currentLsid)
    //at this point we have all the records for a species that should be considered for duplication
    if(buff.size>0) {
      performDetection(buff.toList, duplicateWriter, passedWriter)
    }
    duplicateWriter.close

  }


  def performDetection(allRecords:List[DuplicateRecordDetails], duplicateWriter:FileWriter, passedWriter:FileWriter){
    val yearGroups = allRecords.groupBy{r => if(r.year != null) r.year else "UNKNOWN"}
    DuplicationDetection.logger.debug("There are " + yearGroups.size + " year groups")
    val threads = new ArrayBuffer[Thread]
    yearGroups.foreach{case(year, yearList)=>{
      val t = new Thread(new YearGroupDetection(year,yearList,duplicateWriter, passedWriter))
      t.start();
      threads += t
    }}
    //now wait for each thread to finish
    threads.foreach(_.join)
    DuplicationDetection.logger.debug("Finished processing each year")

    duplicateWriter.flush
  }

  /**
   * Loads the duplicates from a file that contains duplicates from multiple taxon concepts
   */
  def loadMulitpleDuplicatesFromFile(dupFilename:String, passedFilename:String, threads:Int, reindexWriter:FileWriter, oldDuplicatesWriter:FileWriter){
    var currentLsid=""
     val queue = new ArrayBlockingQueue[String](100)
    var ids=0
    var buffer = new ArrayBuffer[String] // The buffer to store all the rowKeys that need to be reindexed
    var allDuplicates = new ArrayBuffer[String]
    //"taxonConceptLsid":"urn:lsid:catalogueoflife.org:taxon:df43e19e-29c1-102b-9a4a-00304854f820:ac2010"
    val conceptPattern = """"taxonConceptLsid":"([A-Za-z0-9\-:\.]*)"""".r
    var oldDuplicates:Set[String] =null
    var oldDupMap:Map[String,String]=null
    val pool:Array[StringConsumer] = Array.fill(threads){ val p = new StringConsumer(queue,ids,{duplicate =>loadDuplicate(duplicate, reindexWriter, buffer, allDuplicates)});ids +=1;p.start;p }
    new File(dupFilename).foreachLine(line =>{
      val lsidMatch = conceptPattern.findFirstMatchIn(line)
      if(lsidMatch.isDefined){
        val strlsidMatch = lsidMatch.get.group(1)
        if(currentLsid != strlsidMatch){
          //wait for the queue to be empty
          while(queue.size()>0)
            Thread.sleep(200)
          if(oldDuplicates != null){
            buffer.foreach(v=>reindexWriter.write(v + "\n"))
            //revert the old duplicates that don't exist
            revertNonDuplicateRecords(oldDuplicates,oldDupMap, allDuplicates.toSet, reindexWriter, oldDuplicatesWriter)
            DuplicationDetection.logger.info("REVERTING THE OLD duplicates for " + currentLsid)
            buffer.reduceToSize(0) 
            allDuplicates.reduceToSize(0)
            reindexWriter.flush          
            oldDuplicatesWriter.flush
          }
          //get new old duplicates          
          currentLsid = strlsidMatch
          DuplicationDetection.logger.info("STARTING to process the all the duplicates for " + currentLsid)
          val olddds = getCurrentDuplicates(currentLsid)
          oldDuplicates = olddds._1
          oldDupMap = olddds._2
        }
        //add line to queue
        queue.put(line)
      }
    })
    
    pool.foreach(t =>t.shouldStop = true)
    pool.foreach(_.join)
    val olddds = getCurrentDuplicates(currentLsid)
    oldDuplicates = olddds._1
    oldDupMap = olddds._2
    buffer.foreach(v=>reindexWriter.write(v + "\n"))
    revertNonDuplicateRecords(oldDuplicates,oldDupMap, buffer.toSet, reindexWriter,oldDuplicatesWriter)
    reindexWriter.flush
    reindexWriter.close
    oldDuplicatesWriter.flush
    oldDuplicatesWriter.close

    //now load all the records that passed duplication detection
    //NC will load thepassed records external to the duplication process.
    /*val passer = new QaPasser(QualityAssertion(AssertionCodes.INFERRED_DUPLICATE_RECORD, 1), 10,deleteColumns = Some(List("associatedOccurrences.p","duplicationStatus.p","duplicationType.p")))
    val buf = new ArrayBuffer[String]()
    new File(passedFilename).foreachLine(line =>{
      buf+= line
      if (buf.size ==1000){
        passer.markRecords(buf.toList)
        buf.clear()
      }


      //each line represents a row key that is not considered a duplicate
      //Config.occurrenceDAO.addSystemAssertion(line, QualityAssertion(AssertionCodes.INFERRED_DUPLICATE_RECORD, 1), replaceExistCode = true)
      //Config.persistenceManager.deleteColumns(line, "occ","associatedOccurrences.p","duplicationStatus.p","duplicationType.p")

    })
    passer.markRecords(buf.toList)
    passer.stop()*/
  }
  
  //loads the dupicates from the lsid based on the tmp file being populated - this is based on a single lsid being in the file
  def loadDuplicates(lsid:String, threads:Int, dupFilename:String, reindexWriter:FileWriter, oldDupWriter:FileWriter){
    //get a list of the current records that are considered duplicates   
    val (oldDuplicates,oldDupMap) = getCurrentDuplicates(lsid)
    val directory = baseDir + "/" +  lsid.replaceAll("[\\.:]","_") + "/"
    //val dupFilename =directory + duplicatesFile
    //val reindexWriter = new FileWriter(directory + duplicatesToReindex)
    val queue = new ArrayBlockingQueue[String](100)
    var ids=0
    val buffer = new ArrayBuffer[String]
    val allDuplicates = new ArrayBuffer[String]
    val pool:Array[StringConsumer] = Array.fill(threads){ val p = new StringConsumer(queue,ids,{duplicate =>loadDuplicate(duplicate, reindexWriter, buffer, allDuplicates)});ids +=1;p.start;p }
    new File(dupFilename).foreachLine(line => queue.put(line))
    pool.foreach(t =>t.shouldStop = true)
    pool.foreach(_.join)
    buffer.foreach(v=>reindexWriter.write(v + "\n"))
    reindexWriter.flush()
    revertNonDuplicateRecords(oldDuplicates,oldDupMap, allDuplicates.toSet, reindexWriter,oldDupWriter)
    reindexWriter.flush
    reindexWriter.close
    oldDupWriter.flush
    oldDupWriter.close   
    
  }
  //Loads the specific duplicate - allows duplicates to be loaded in a threaded manner
  def loadDuplicate(dup:String, writer:FileWriter, buffer:ArrayBuffer[String], allDuplicates:ArrayBuffer[String])={
    //turn the duplicate into the object    
    val primaryRecord = mapper.readValue[DuplicateRecordDetails](dup, classOf[DuplicateRecordDetails])
    DuplicationDetection.logger.debug("HANDLING " + primaryRecord.rowKey)
    //get the duplicates that are not already part of the primary record
    allDuplicates.synchronized{
      allDuplicates += primaryRecord.rowKey
      primaryRecord.duplicates.foreach(d => allDuplicates += d.rowKey)
    }
    val newduplicates = primaryRecord.duplicates.filter(_.oldDuplicateOf != primaryRecord.uuid)
    //println("primaryrecord uuid: "+ primaryRecord.uuid)
    //newduplicates.foreach(i =>println(i.oldDuplicateOf))
    val uuidList = primaryRecord.duplicates.map(r => r.uuid)
    try{
    if(newduplicates.size() >0 || primaryRecord.oldDuplicateOf == null || primaryRecord.duplicates.size != primaryRecord.oldDuplicateOf.split(",").size){
      buffer.synchronized{buffer += primaryRecord.rowKey}
      Config.persistenceManager.put(primaryRecord.uuid, "occ_duplicates","value",dup)
      Config.persistenceManager.put(primaryRecord.taxonConceptLsid+"|"+primaryRecord.year+"|"+primaryRecord.month +"|" +primaryRecord.day, "duplicates", primaryRecord.uuid,dup)
      Config.persistenceManager.put(primaryRecord.rowKey, "occ",Map("associatedOccurrences.p"->uuidList.mkString("|"),"duplicationStatus.p"->"R"))
      
      
      
      newduplicates.foreach(r =>{
        val types = if(r.dupTypes != null)r.dupTypes.toList.map( t => t.getId.toString ).toArray[String] else Array[String]()
        Config.persistenceManager.put(r.rowKey, "occ", Map("associatedOccurrences.p"->primaryRecord.uuid,"duplicationStatus.p"->"D","duplicationType.p"->mapper.writeValueAsString(types)))
        //add a system message for the record - a duplication does not change the kosher fields and should always be displayed thus don't "checkExisting"
        Config.occurrenceDAO.addSystemAssertion(r.rowKey, QualityAssertion(AssertionCodes.INFERRED_DUPLICATE_RECORD,"Record has been inferred as closely related to  " + primaryRecord.uuid),false)
        buffer.synchronized{buffer += r.rowKey}
      })
    }
    }
    catch{
      case e:Exception=> e.printStackTrace();println(dup)
    }
    
  }
  
  def downloadRecords(sourceFileName:String, lsid:String, field:String){
    val file = new File(sourceFileName)
    FileUtils.forceMkdir(file.getParentFile)
    val fileWriter = new FileWriter(file)
    DuplicationDetection.logger.info("Starting to download the occurrences for " + lsid)
    ExportByFacetQuery.downloadSingleTaxonByStream(null,lsid, fieldsToExport ,field,if(field == "species_guid") speciesFilters else subspeciesFilters,Array("row_key"), fileWriter, None, Some(Array("duplicate_record")))
    fileWriter.close
  }
  
  /*
   * Performs the duplicate detection - each year of records is processed on a separate thread.
   * WARNING as of 2013-08-30 This method shoudl only be used to detect the duplicates for a single species.
   */
  def detect(sourceFileName:String, duplicateWriter:FileWriter, passedWriter:FileWriter,lsid:String, shouldDownloadRecords:Boolean = false, field:String="species_guid", cleanup:Boolean=false){
    DuplicationDetection.logger.info("Starting to detect duplicates for " + lsid)

//    val directory = baseDir + "/" +  lsid.replaceAll("[\\.:]","_") + "/"
//    val dirFile = new File(directory)
//    FileUtils.forceMkdir(dirFile)
//    val filename = directory + filePrefix
//    val dupFilename =directory + duplicatesFile
    //val duplicateWriter = new FileWriter(new File(dupFilename))

    if(shouldDownloadRecords){
      downloadRecords(sourceFileName, lsid, field)
//      val file = new File(sourceFileName)
//      FileUtils.forceMkdir(file.getParentFile)
//      val fileWriter = new FileWriter(file)
//      DuplicationDetection.logger.info("Starting to download the occurrences for " + lsid)
//      ExportByFacetQuery.downloadSingleTaxon(lsid, fieldsToExport ,field,if(field == "species_guid") speciesFilters else subspeciesFilters,Some("row_key"),Some("asc"), fileWriter)
//      fileWriter.close
    }
    //open the tmp file that contains the information about the lsid
    val reader =  new CSVReader(new FileReader(sourceFileName),'\t', '`', '~')

     var currentLine = reader.readNext //first line is header
     val buff = new ArrayBuffer[DuplicateRecordDetails]
     var counter =0
     while(currentLine !=  null){
       if(currentLine.size>=16){
         counter +=1
         if(counter % 10000 == 0)
           DuplicationDetection.logger.debug("Loaded into memory : " + counter +" + records")
         val rowKey = currentLine(0)
         val uuid = currentLine(1)
         val taxon_lsid = currentLine(2)
         val year = StringUtils.trimToNull(currentLine(3))
         val month = StringUtils.trimToNull(currentLine(4))
         
         val date: java.util.Date = try {
           DateUtils.parseDate(currentLine(5),"EEE MMM dd hh:mm:ss zzz yyyy")
         } catch {
           case _:Exception => null
         }
         val day = if(date != null) Integer.toString(date.getDate()) else null
         val rawName = StringUtils.trimToNull(currentLine(12))
         val collector = StringUtils.trimToNull(currentLine(13))
         val oldStatus = StringUtils.trimToNull(currentLine(14))
         val oldDuplicateOf = StringUtils.trimToNull(currentLine(15).replaceAll("\\[","").replaceAll("\\]",""))
         buff += new DuplicateRecordDetails(rowKey,uuid,taxon_lsid,year,month,day,currentLine(6), currentLine(7),
             currentLine(8),currentLine(9),currentLine(10),currentLine(11),rawName, collector, oldStatus,oldDuplicateOf)
       }
       else{
         DuplicationDetection.logger.warn("lsid " + lsid + " line " + counter + " has incorrect column number: " + currentLine.size)
       }
       currentLine = reader.readNext
     }
    DuplicationDetection.logger.info("Read in " + counter + " records for " + lsid)
    //at this point we have all the records for a species that should be considered for duplication
    val allRecords = buff.toList
    val yearGroups = allRecords.groupBy{r => if(r.year != null) r.year else "UNKNOWN"}
    DuplicationDetection.logger.debug("There are " + yearGroups.size + " year groups")
    val threads = new ArrayBuffer[Thread]
    yearGroups.foreach{case(year, yearList)=>{
      val t = new Thread(new YearGroupDetection(year,yearList,duplicateWriter, passedWriter))
      t.start();
      threads += t
    }}
    //now wait for each thread to finish
    threads.foreach(_.join)
    DuplicationDetection.logger.debug("Finished processing each year")

    duplicateWriter.flush
    duplicateWriter.close
//    //index the duplicate records
//    IndexRecords.indexList(new File(dupFilename))
    //remove the directory that we used 
//    if(cleanup)
//      FileUtils.deleteDirectory(dirFile)
  }
  /*
   * Changes the stored values for the "old" duplicates that are no longer considered duplicates
   */
  def revertNonDuplicateRecords(oldDuplicates:Set[String], oldDupMap:Map[String,String], currentDuplicates:Set[String], write:FileWriter, oldWriter:FileWriter){
    val nonDuplicates = oldDuplicates -- currentDuplicates
    nonDuplicates.foreach(nd =>{
      DuplicationDetection.logger.warn(nd + " is no longer a duplicate")
      //remove the duplication columns
      Config.persistenceManager.deleteColumns(nd, "occ","associatedOccurrences.p","duplicationStatus.p","duplicationType.p")
      //now remove the system assertion if necessary
      Config.occurrenceDAO.removeSystemAssertion(nd, AssertionCodes.INFERRED_DUPLICATE_RECORD)
      write.write(nd +"\n")
      oldWriter.write(nd + "\t" + oldDupMap(nd) +"\n")
    })
  }
  
  //gets a list of current duplicates so that records no longer considered a duplicate can be reset
  def getCurrentDuplicates(lsid:String):(Set[String], Map[String,String])= {
    val startKey = lsid+"|"
    val endKey = lsid+"|~"
    
    val buf = new ArrayBuffer[String]
    val uuidMap = new scala.collection.mutable.HashMap[String,String]()
  
     Config.persistenceManager.pageOverAll("duplicates",(guid,map) =>{
      DuplicationDetection.logger.debug("Getting old duplicates for " + guid)
      map.values.foreach(v=>{
        //turn it into a DuplicateRecordDetails
        val rd = mapper.readValue[DuplicateRecordDetails](v, classOf[DuplicateRecordDetails])
        buf += rd.rowKey
        uuidMap += rd.rowKey->rd.uuid
        rd.duplicates.toList.foreach(d =>{
          buf += d.rowKey
          uuidMap += d.rowKey -> d.uuid
          })
      })
      true
    }, startKey, endKey,100)
    
    (buf.toSet,uuidMap.toMap[String,String])
  
  }
}

//Each year is handled separately so they can be processed in a threaded manner
class YearGroupDetection(year:String,records:List[DuplicateRecordDetails], duplicateWriter:FileWriter, passedWriter:FileWriter) extends Runnable{
  import JavaConversions._
  val latLonPattern="""(\-?\d+(?:\.\d+)?),\s*(\-?\d+(?:\.\d+)?)""".r
  val alphaNumericPattern ="[^\\p{L}\\p{N}]".r
  val unknownPatternString = "(null|UNKNOWN OR ANONYMOUS)"
  val mapper = new ObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.setSerializationInclusion(Include.NON_NULL)
  override def run()={
    DuplicationDetection.logger.debug("Starting deduplication for " + year)
    val monthGroups = records.groupBy(r=> if(r.month != null) r.month else "UNKNOWN")
        
    val unknownGroup = monthGroups.getOrElse("UNKNOWN",List())
    val buffGroups = new ArrayBuffer[DuplicateRecordDetails]
    monthGroups.foreach{case (month, monthList)=>{
      //val (month, monthList) = values
        //if(month != "UNKNOWN"){
          //if there is more than 1 record group by days
          if(monthList.size>1){
            val dayGroups = monthList.groupBy(r=> if(r.day != null) r.day else "UNKNOWN")
            val unknownDays = dayGroups.getOrElse("UNKNOWN",List())
            dayGroups.foreach{case (day, dayList)=>{
              //if(day != "UNKNOWN"){
                if(dayList.size > 1){
                  //need to check for duplicates
                  buffGroups ++= checkDuplicates(dayList)
                }
                else{
                  buffGroups += dayList.head
                }
              //}
            }
              
            }
          }
          else{
            buffGroups += monthList.head
          }
        //}
    }
    }
    DuplicationDetection.logger.debug("Number of distinct records for year " + year + " is " + buffGroups.size)
    
    buffGroups.foreach(record =>{
      if(record.duplicates != null && record.duplicates.size > 0){        
        val (primaryRecord,duplicates) = markRecordsAsDuplicatesAndSetTypes(record)
        val stringValue = mapper.writeValueAsString(primaryRecord)
        //write the duplicate to file to be handled at a later time
        duplicateWriter.synchronized{
          duplicateWriter.write(stringValue +"\n")
        }
      } else if((record.duplicates == null || record.duplicates.size() ==0) && StringUtils.isBlank(record.duplicateOf) ){
        //this record has passed its duplicate detection
        passedWriter.synchronized{
          passedWriter.write(record.getRowKey+"\n")
        }

      }
        
        //println("RECORD: " + record.rowKey + " has " + record.duplicates.size + " duplicates")
    })
    duplicateWriter.synchronized{
      duplicateWriter.flush()
    }
    passedWriter.synchronized{
      passedWriter.flush()
    }
  }
  
  def setDateTypes(r:DuplicateRecordDetails,hasYear:Boolean, hasMonth:Boolean, hasDay:Boolean){
    if(hasYear && hasMonth && !hasDay)
       r.addDupType(DuplicationTypes.MISSING_DAY)
    else if(hasYear && !hasMonth)
       r.addDupType(DuplicationTypes.MISSING_MONTH)
    else if(!hasYear)
       r.addDupType(DuplicationTypes.MISSING_YEAR)
  }
  def markRecordsAsDuplicatesAndSetTypes(record: DuplicateRecordDetails):(DuplicateRecordDetails,List[DuplicateRecordDetails])={
    //find the "representative" record for the duplicate
    var highestPrecision = determinePrecision(record.latLong)
    record.precision = highestPrecision
    var representativeRecord = record
    val duplicates = record.duplicates
    
    //find out whether or not record has date components
    val hasYear = StringUtils.isNotEmpty(record.year)
    val hasMonth = StringUtils.isNotEmpty(record.month)
    val hasDay = StringUtils.isNotEmpty(record.day)
    setDateTypes(record,hasYear,hasMonth,hasDay)
    duplicates.foreach(r =>{
      setDateTypes(r,hasYear,hasMonth,hasDay)
      r.precision = determinePrecision(r.latLong)
      if(r.precision > highestPrecision){
        highestPrecision = r.precision
        //representativeRecord.status = "D"
        representativeRecord = r
      }
//      else
//        r.status = "D"
    })
    representativeRecord.status = "R"
    
    if(representativeRecord != record){
      record.duplicates =null
      duplicates += record
      duplicates -= representativeRecord
      representativeRecord.duplicates = duplicates
      //set the duplication types of the old rep record 
      record.dupTypes = representativeRecord.dupTypes
    }

    //set the duplication type based data resource uid
    duplicates.foreach(d => {
      d.status = if(d.druid == representativeRecord.druid) "D1" else "D2"
      d.addDupType(if(d.precision == representativeRecord.precision) DuplicationTypes.EXACT_COORD else DuplicationTypes.DIFFERENT_PRECISION)
      })
    
    (representativeRecord,duplicates.toList)
  }
  //reports the maximum number of decimal places that the lat/long are reported to
  def determinePrecision(latLong:String):Int ={
    try{
     val latLonPattern(lat,long) = latLong
     val latp = if(lat.contains("."))lat.split("\\.")(1).length else 0
     val lonp = if(long.contains("."))long.split("\\.")(1).length else 0
     if(latp > lonp) latp else lonp
    }
    catch{
      case e: Exception => DuplicationDetection.logger.error("ISSUE WITH " + latLong,e); 0
    }
  }
  def checkDuplicates(recordGroup:List[DuplicateRecordDetails]):List[DuplicateRecordDetails]={
    
    recordGroup.foreach(record=>{
      if(record.duplicateOf == null){
        //this record needs to be considered for duplication 
        findDuplicates(record, recordGroup)
      }
    })    
    recordGroup.filter(_.duplicateOf == null)
  }
  def findDuplicates(record:DuplicateRecordDetails, recordGroup:List[DuplicateRecordDetails]){
    
    val points =Array(record.point1,record.point0_1,record.point0_01,record.point0_001,record.point0_0001,record.latLong)
    recordGroup.foreach(otherRecord =>{
      if(otherRecord.duplicateOf == null && record.rowKey != otherRecord.rowKey){
          val otherpoints =Array(otherRecord.point1,otherRecord.point0_1,otherRecord.point0_01,otherRecord.point0_001,otherRecord.point0_0001,otherRecord.latLong)
//          val spatial =  isSpatialDuplicate(points,otherpoints)
//          if(spatial)            
//              println("Testing " + record.rowKey + " against " + otherRecord.rowKey + " " +  isSpatialDuplicate(points,otherpoints) +" "+points.toList + " " + otherpoints.toList)          
          if(isSpatialDuplicate(points,otherpoints) && isCollectorDuplicate(record, otherRecord)){
            otherRecord.duplicateOf = record.rowKey
            record.addDuplicate(otherRecord)
          }
      }
    })
  }
  
  def isEmptyUnknownCollector(in:String):Boolean ={
    StringUtils.isEmpty(in) || in.matches(unknownPatternString)
  }
  
    def isCollectorDuplicate(r1:DuplicateRecordDetails, r2:DuplicateRecordDetails): Boolean ={
      
      //if one of the collectors haven't been supplied assume that they are the same.
      if(isEmptyUnknownCollector(r1.collector) || isEmptyUnknownCollector(r2.collector)){
        if(isEmptyUnknownCollector(r2.collector))
          r2.addDupType(DuplicationTypes.MISSING_COLLECTOR)
        true
      }
      else{
      val (col1,col2) = prepareCollectorsForLevenshtein(r1.collector, r2.collector)
      val distance =StringUtils.getLevenshteinDistance(col1,col2)
      //allow 3 differences in the collector name
      if(distance <= 3){
      
      //println("DISTANCE: " + distance)
        if(distance > 0){
          //println("COL1: " + collector1 + " COL2: " + collector2)
          r2.addDupType(DuplicationTypes.FUZZY_COLLECTOR)
        }
        else
          r2.addDupType(DuplicationTypes.EXACT_COLLECTOR)
        true
      }
      else
        false
      }
  }
  def prepareCollectorsForLevenshtein(c1:String,c2:String):(String,String)={
    //remove all the non alphanumeric characters
    var c11=alphaNumericPattern.replaceAllIn(c1,"")
    var c21 = alphaNumericPattern.replaceAllIn(c2, "")
    var length =if(c11.size>c21.size)c21.size else c11.size
    (c11.substring(0,length),c21.substring(0,length))
  }
  def isSpatialDuplicate(points:Array[String], pointsb:Array[String]):Boolean ={
    for(i <- 0 to 5){
      if(points(i) != pointsb(i)){
        //println(points(i) + " DIFFERENT TO " + pointsb(i))
        //check to see if the precision is different
        if(i>0){
          //one of the current points has the same coordinates as the previous precision
          if(points(i) == points(i-1) || pointsb(i) == pointsb(i-1)){
            if(i<5){
            //indicates that we have a precision difference
            if(points(i) == points(i+1) || pointsb(i) == points(i+1))
                return true
            }
            else
              return true
          }
          //now check if we have a rounding error by look at the subsequent coordinates...
          return false  
        }
        else{
          //at the largest granularity the coordinates are different
          return false;
        }
          
      }
    }
    true
  }
  
  //TODO
  def compareValueWithUnknown(value:DuplicateRecordDetails,unknownGroup:List[DuplicateRecordDetails], currentDuplicateList:List[DuplicateRecordDetails]):(List[DuplicateRecordDetails],List[DuplicateRecordDetails])={
    (List(),List())
  }
}


class IncrementalYearGroupDetection(year:String,records:List[DuplicateRecordDetails], duplicateWriter:FileWriter,passedWriter:FileWriter) extends YearGroupDetection(year, records,duplicateWriter,passedWriter:FileWriter){
  override def checkDuplicates(recordGroup:List[DuplicateRecordDetails]):List[DuplicateRecordDetails]={
    
    recordGroup.foreach(record=>{
      if(record.duplicateOf == null){
        //this record needs to be considered for duplication 
        findDuplicates(record, recordGroup)
      }
    }) 
    //return records that are not duplicates
    recordGroup.filter(_.duplicateOf == null)
  }
  
  def findDuplicateGroup(record:DuplicateRecordDetails){
    //find existing group and check if it is still a duplicate
    
    //otherwise find all the existing 
  }
}

/**
 * Processes the record that have been reloaded since the last duplication detection.
 * 
 * This should be a much quicker process than the full duplication detection
 * 
 */
class IncrementalDuplicationDetection(lastDupDate:String) extends DuplicationDetection{
  /**
   * Incremental duplication detection has different logic around downloading the records for consideration 
   */
  override def downloadRecords(sourceFileName:String, lsid:String, field:String){
    val file = new File(sourceFileName)
    FileUtils.forceMkdir(file.getParentFile)
    val fileWriter = new FileWriter(file)
    DuplicationDetection.logger.info("Starting to download the occurrences for " + lsid)
    def filters = (if(field == "species_guid") speciesFilters else subspeciesFilters) ++ Array("last_load_date:["+lastDupDate+" TO *]")
    ExportByFacetQuery.downloadSingleTaxon(lsid, fieldsToExport ,field,filters,Some("row_key"),Some("asc"), fileWriter)
    fileWriter.close
  }
}

sealed case class DupType(@BeanProperty var id:Int){
  def this() = this(-1)
}

object DuplicationTypes {
  val MISSING_YEAR = DupType(1)//"Occurrences were compared without dates."
  val MISSING_MONTH = DupType(2)// "Occurrences were compared without a month and day component.")
  val MISSING_DAY   = DupType(3)//,"Occurrences were compared without a day component.")
  val EXACT_COORD = DupType(4)//," The coordinates were identical.")
  val DIFFERENT_PRECISION = DupType(5)//, "The precision between the occurrences was different.")
  val EXACT_COLLECTOR = DupType(6)//, "Occurrences had identical collectors.")
  val FUZZY_COLLECTOR = DupType(7) // The occurrences had collectors that are similar
  val MISSING_COLLECTOR = DupType(8)// At least one of the occurrences was missing a collector
}

class DuplicateRecordDetails(
                    @BeanProperty var rowKey:String,
                    @BeanProperty var uuid:String,
                    @BeanProperty var taxonConceptLsid:String,
                    @BeanProperty var year:String,
                    @BeanProperty var month:String,
                    @BeanProperty var day:String,
                    @BeanProperty var point1:String,
                    @BeanProperty var point0_1:String,
                    @BeanProperty var point0_01:String,
                    @BeanProperty var point0_001:String,
                    @BeanProperty var point0_0001:String,
                    @BeanProperty var latLong:String,
                    @BeanProperty var rawScientificName:String,
                    @BeanProperty var collector:String,
                    @BeanProperty var oldStatus:String,
                    @BeanProperty var oldDuplicateOf:String){
  
  def this() = this(null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null)
  
  @BeanProperty var status = "U"
  @BeanProperty var druid:String = if(rowKey != null) rowKey.split("\\|")(0) else null
  @BeanProperty var duplicates:ArrayList[DuplicateRecordDetails] = null
  @BeanProperty var dupTypes:ArrayList[DupType] = _

  @BeanProperty var duplicateOf:String = null
  //stores the precision so that coordinate dup types can be established - we don't want to persist this property (NC TODO regression issue after scala upgrade persists these so I added @BeanProperty)
  @BeanProperty var precision = 0

  def addDuplicate(dup:DuplicateRecordDetails) = {
    if(duplicates == null)
      duplicates = new ArrayList[DuplicateRecordDetails]
    duplicates.add(dup)
  }

  def addDupType(dup:DupType){
    if(dupTypes == null)
      dupTypes = new ArrayList[DupType]()
    dupTypes.add(dup)
  }
}