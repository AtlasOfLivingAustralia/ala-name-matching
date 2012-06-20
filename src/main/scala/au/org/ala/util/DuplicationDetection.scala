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
import org.codehaus.jackson.map.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import au.org.ala.biocache.AssertionCodes
import au.org.ala.biocache.QualityAssertion
import org.codehaus.jackson.map.annotate.JsonSerialize
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ArrayBlockingQueue
import org.slf4j.LoggerFactory
import org.apache.commons.io.FileUtils
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
  val logger = LoggerFactory.getLogger("DuplicateDetection")
  def main(args:Array[String])={
    var all = false
    var exist = false
    var guid:Option[String] = None
    var speciesFile:Option[String] = None
    var threads = 4
    var cleanup = false
    //Options to perform on all "species", select species, use existing file or download
    val parser = new OptionParser("Duplication Detection - Detects duplication based on a matched species.") {
      opt("all", "detct duplicates for all species", { all = true })
      opt("g", "guid", "A single guid to test for duplications", { v: String => guid = Some(v)})
      opt("exist","use existing occurrence dumps",{exist = true})
      opt("cleanup","cleanup the temporary files that get created",{cleanup = true})
      opt("f","file","A file that contains a list of species guids to detect duplication for",{v: String => speciesFile = Some(v)})
      intOpt("t","threads" ," The number of concurrent species duplications to perform.",{v:Int => threads=v})
      
    }
    if(parser.parse(args)){
      //ensure that we have either all, guidsToTest or speciesFile
      if(all){
        //download all the species guids
        val filename = "/tmp/dd_all_species_guids"
        ExportFacet.main(Array("species_guid",filename))
        //now detect the duplicates
        detectDuplicates(new File(filename), threads,exist,cleanup)
      }
      else if(guid.isDefined){
        //just a single detection - ignore the thread settings etc...
        new DuplicationDetection().detect(guid.get,shouldDownloadRecords= !exist ,cleanup=cleanup)
        //println(new DuplicationDetection().getCurrentDuplicates(guid.get))
      }
      else if(speciesFile.isDefined){
        detectDuplicates(new File(speciesFile.get), threads, exist, cleanup)
      }
      else{
        parser.showUsage
      }
    }
  //new DuplicationDetection().detect("urn:lsid:biodiversity.org.au:afd.taxon:b76f8dcf-fabd-4e48-939c-fd3cafc1887a")
  //new DuplicationDetection().detect("urn:lsid:biodiversity.org.au:afd.taxon:9e23c727-f3b0-4c29-a345-9cbd306eed84")
    //new DuplicationDetection().detect("urn:lsid:biodiversity.org.au:apni.taxon:425841")
    
  }
  
  def detectDuplicates(file:File, threads:Int, exist:Boolean, cleanup:Boolean){
    val queue = new ArrayBlockingQueue[String](100)
    //create the consumer threads
    
    //val pool = Array.fill(threads){ val p = new GuidConsumer(queue,{guid => new DuplicationDetection().detect(guid,!exist)}); p.start }
    var ids=0
    val pool:Array[GuidConsumer] = Array.fill(threads){ val p = new GuidConsumer(queue,ids,{guid => new DuplicationDetection().detect(guid,shouldDownloadRecords= !exist ,cleanup=cleanup)});ids +=1;p.start;p }
    
    file.foreachLine(line =>{
      //add to the queue
      queue.put(line.trim)
    }) 
    pool.foreach(t =>t.shouldStop = true)
  }
}
class GuidConsumer(q:BlockingQueue[String],id:Int,proc:String=>Unit) extends Thread{  
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
        case e:Exception=>
      }
    }
  }
  
}
//TODO Use the "sensitive" coordinates for sensitive species
class DuplicationDetection{
  val baseDir = "/tmp"
  val duplicatesToReindex = "duplicatesreindex.txt"
  val filePrefix = "dd_data.txt"
  val fieldsToExport = Array("row_key", "id", "species_guid", "year", "month", "occurrence_date", "point-1", "point-0.1", 
                             "point-0.01","point-0.001", "point-0.0001","lat_long", "collectors")
  val speciesFilters = Array("lat_long:[* TO *]")
  // we have decided that a subspecies can be evalutated as part of the species level duplicates
  val subspeciesFilters = Array("lat_long:[* TO *]", "-species_guid:[* TO *]") 

  def detect(lsid:String, shouldDownloadRecords:Boolean = false, field:String="species_guid", cleanup:Boolean=false){
    DuplicationDetection.logger.info("Starting to detect duplicates for " + lsid)
    //get a list of the current records that are considered duplicates
    val oldDuplicates = getCurrentDuplicates(lsid)
    val directory = baseDir + "/" +  lsid.replaceAll("[\\.:]","_") + "/"
    val dirFile = new File(directory)
    FileUtils.forceMkdir(dirFile)
    val filename = directory + filePrefix
    val dupFilename =directory + duplicatesToReindex 
    val duplicateWriter = new FileWriter(new File(dupFilename))
    val duplicateIdBuffer = new ArrayBuffer[String]
    if(shouldDownloadRecords){
      val fileWriter = new FileWriter(new File(filename))
      
      ExportByFacetQuery.downloadSingleTaxon(lsid, fieldsToExport ,field,if(field == "species_guid") speciesFilters else subspeciesFilters,Some("row_key"),Some("asc"), fileWriter)
      fileWriter.close
    }
    //open the tmp file that contains the information about the lsid
    val reader =  new CSVReader(new FileReader(filename),'\t', '`', '~')

     var currentLine = reader.readNext //first line is header
     val buff = new ArrayBuffer[DuplicateRecordDetails]
     var counter =0
     while(currentLine !=  null){
       counter +=1
       if(counter % 10000 == 0)
         DuplicationDetection.logger.debug("Loaded into memory : " + counter +" + records")
       val rowKey = currentLine(0)
       val uuid = currentLine(1)
       val taxon_lsid = currentLine(2)
       val year = StringUtils.trimToNull(currentLine(3))
       val month = StringUtils.trimToNull(currentLine(4))
       
       val date:java.util.Date = try{DateUtils.parseDate(currentLine(5),
        "EEE MMM dd hh:mm:ss zzz yyyy")}catch{
          case _=> null
        }
       val day = if(date != null) Integer.toString(date.getDate()) else null
       val collector = StringUtils.trimToNull(currentLine(12))
       buff + new DuplicateRecordDetails(rowKey,uuid,taxon_lsid,year,month,day,currentLine(6), currentLine(7),
           currentLine(8),currentLine(9),currentLine(10),currentLine(11), collector)
       currentLine = reader.readNext
     }
    DuplicationDetection.logger.info("Read in " + counter + " records for " + lsid)
    //at this point we have all the records for a species that should be considered for duplication
    val allRecords = buff.toList
    val yearGroups = allRecords.groupBy{r => if(r.year != null) r.year else "UNKNOWN"}
    DuplicationDetection.logger.debug("There are " + yearGroups.size + " year groups")
    val threads = new ArrayBuffer[Thread]
    yearGroups.foreach{case(year, yearList)=>{
      val t =new Thread(new YearGroupDetection(year,yearList,duplicateIdBuffer))
      t.start();
      threads + t
    }}
    //now wait for each thread to finish
    threads.foreach(_.join)
    duplicateIdBuffer.foreach(d =>duplicateWriter.write(d + "\n"))
    
    //revert the records that are no longer considered duplicates
    revertNonDuplicateRecords(oldDuplicates, duplicateIdBuffer.toSet, duplicateWriter)
    
    duplicateWriter.flush
    duplicateWriter.close
    //index the duplicate records
    IndexRecords.indexList(new File(dupFilename))
    //remove the directory that we used 
    if(cleanup)
      FileUtils.deleteDirectory(dirFile)
  }
  /*
   * Changes the stored values for the "old" duplicates that are no longer considered duplicates
   */
  def revertNonDuplicateRecords(oldDuplicates:Set[String], currentDuplicates:Set[String], write:FileWriter){
    val nonDuplicates = oldDuplicates -- currentDuplicates
    nonDuplicates.foreach(nd =>{
      DuplicationDetection.logger.warn(nd + " is no longer a duplicate")
      //remove the duplication columns
      Config.persistenceManager.deleteColumns(nd, "occ","associatedOccurrences.p","duplicationStatus.p","duplicationType.p")
      //now remove the system assertion if necessary
      Config.occurrenceDAO.removeSystemAssertion(nd, AssertionCodes.INFERRED_DUPLICATE_RECORD)
      write.write(nd +"\n")
    })
  }
  
  //gets a list of current duplicates so that records no longer considered a duplicate can be reset
  def getCurrentDuplicates(lsid:String):Set[String]= {
    val startKey = lsid+"|"
    val endKey = lsid+"|~"
    val mapper = new ObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL)
    val buf = new ArrayBuffer[String]
  
     Config.persistenceManager.pageOverAll("duplicates",(guid,map) =>{
      DuplicationDetection.logger.debug("Getting old duplicates for " + guid)
      map.values.foreach(v=>{
        //turn it into a DuplicateRecordDetails
        val rd = mapper.readValue[DuplicateRecordDetails](v, classOf[DuplicateRecordDetails])
        buf + rd.rowKey
        rd.duplicates.foreach(d => buf + d.rowKey)
      })
      true
    }, startKey, endKey,100)
    
    buf.toSet
  
  }
}
//Each year is handled separately so they can be processed in a threaded manner
class YearGroupDetection(year:String,records:List[DuplicateRecordDetails], duplicateBuffer:ArrayBuffer[String]) extends Runnable{
  val latLonPattern="""(\-?\d+(?:\.\d+)?),\s*(\-?\d+(?:\.\d+)?)""".r
  val alphaNumericPattern ="[^\\p{L}\\p{N}]".r
  val unknownPatternString = "(null|UNKNOWN OR ANONYMOUS)"
  val mapper = new ObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL)
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
                  buffGroups + dayList.head
                }
              //}
            }
              
            }
          }
          else{
            buffGroups + monthList.head
          }
        //}
    }
    }
    DuplicationDetection.logger.debug("Number of distinct records for year " + year + " is " + buffGroups.size)
    
    buffGroups.foreach(record =>{
      if(record.duplicates != null && record.duplicates.size > 0){
        val hasYear = StringUtils.isEmpty(record.year)
        val hasMonth = StringUtils.isEmpty(record.month)
        val hasDay = StringUtils.isEmpty(record.day)
        val (primaryRecord,duplicates) = markRecordsAsDuplicates(record)
        val uuidList = duplicates.map(r => r.uuid)
        addRowKeysToBuffer(primaryRecord)
        //add the items for the PRIMARY record
        //println(primaryRecord.uuid,mapper.writeValueAsString(primaryRecord))
        val stringValue = mapper.writeValueAsString(primaryRecord)
        Config.persistenceManager.put(primaryRecord.uuid, "occ_duplicates","value",stringValue)
        Config.persistenceManager.put(primaryRecord.taxonConceptLsid+"|"+primaryRecord.year+"|"+primaryRecord.month +"|" +primaryRecord.day, "duplicates", primaryRecord.uuid,stringValue)
        Config.persistenceManager.put(primaryRecord.rowKey, "occ",Map("associatedOccurrences.p"->uuidList.mkString("|"),"duplicationStatus.p"->"R"))
        //val primaryUuid = duplicates.
        duplicates.foreach(r =>{
          val types = if(r.dupTypes != null)r.dupTypes.map(t => t.id) else List()
          Config.persistenceManager.put(r.rowKey, "occ", Map("associatedOccurrences.p"->primaryRecord.uuid,"duplicationStatus.p"->"D","duplicationType.p"->mapper.writeValueAsString(types)))
          //add a system message for the record - a duplication does not change the kosher fields and should always be displayed thus don't "checkExisting"
          Config.occurrenceDAO.addSystemAssertion(r.rowKey, QualityAssertion(AssertionCodes.INFERRED_DUPLICATE_RECORD,"Record has been inferred as a duplicate of " + primaryRecord.uuid),false)
          
        }

        )
      }
        
        //println("RECORD: " + record.rowKey + " has " + record.duplicates.size + " duplicates")
    })
//    duplicateWriter.synchronized{
//      duplicateWriter.flush()
//    }
  }
  def addRowKeysToBuffer(rr:DuplicateRecordDetails){
    duplicateBuffer.synchronized{
     //duplicateWriter.write(rr.rowKey + "\n")
      duplicateBuffer + rr.rowKey
     //rr.duplicates.foreach(dup =>duplicateWriter.write(dup.rowKey + "\n"))
      rr.duplicates.foreach(dup =>duplicateBuffer + dup.rowKey)
     
    }
  }
  def markRecordsAsDuplicates(record: DuplicateRecordDetails):(DuplicateRecordDetails,List[DuplicateRecordDetails])={
    //find the "representative" record for the duplicate
    var highestPrecision = determinePrecision(record.latLong)
    var representativeRecord = record
    val duplicates = record.duplicates
    duplicates.foreach(r =>{
      val pre = determinePrecision(r.latLong)
      if(pre > highestPrecision){
        highestPrecision = pre
        representativeRecord.status = "D"
        representativeRecord = r
      }
      else
        r.status = "D"
    })
    representativeRecord.status = "R"
    
    if(representativeRecord != record){
      record.duplicates =null
      duplicates += record
      duplicates -= representativeRecord
      representativeRecord.duplicates = duplicates
    }
//    else{
//      duplicates - representativeRecord
//      representativeRecord.duplicates = duplicates      
//    }
      
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
      case e:Exception => DuplicationDetection.logger.error("ISSUE WITH " + latLong,e); 0
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


sealed case class DupType(id:Int)

object DuplicationTypes{
  val MISSING_YEAR = DupType(1)
  val MISSING_MONTH = DupType(2)
  val MISSING_DAY   = DupType(3)
  val EXACT_COORD = DupType(4)
  val DIFFERENT_PRECISION = DupType(5)
  val EXACT_COLLECTOR = DupType(6)
  val FUZZY_COLLECTOR = DupType(7)
  val MISSING_COLLECTOR = DupType(8)
}

class DuplicateRecordDetails(@BeanProperty var rowKey:String, @BeanProperty var uuid:String, @BeanProperty var taxonConceptLsid:String,
                    @BeanProperty var year:String, @BeanProperty var month:String, @BeanProperty var day:String,
                    @BeanProperty var point1:String, @BeanProperty var point0_1:String, 
                    @BeanProperty var point0_01:String, @BeanProperty var point0_001:String, 
                    @BeanProperty var point0_0001:String,@BeanProperty var latLong:String, @BeanProperty var collector:String){
  
  def this() =this(null,null,null,null,null,null,null,null,null,null,null,null,null)
  
  @BeanProperty var status="U"
  var duplicateOf:String = null
  @BeanProperty var duplicates:ArrayBuffer[DuplicateRecordDetails]=null
  @BeanProperty var dupTypes:List[DupType]=_
  def addDuplicate(dup:DuplicateRecordDetails) ={
    if(duplicates == null)
      duplicates = new ArrayBuffer[DuplicateRecordDetails]
    duplicates + dup
  }
  def addDupType(dup:DupType){
    if(dupTypes == null)
      dupTypes = List(dup)
    else
      dupTypes =dupTypes :+ dup
  }
  
  
}

class DuplicationDetectionOld {
  import JavaConversions._
 
  def detect(lsid:String){
    //only interested in considering records that have been matched to the same concept
    //Also only want records that have coordinates. Locality information can be too broad to correctly detect duplications
    //We may wish to consider this in the future...
    //TO DO lft and rgt values...
    val query = "taxon_concept_lsid:" + ClientUtils.escapeQueryChars(lsid) + " AND lat_long:[* TO *]"
    
    //
    
    Config.indexDAO.pageOverFacet((value,count)=>{
      //so year facet exists need to
      if(count >0){
        val fq = Array("year:"+value) 
        
        val yearRecords = retrieveRecordGroup(query,fq)
        //now groupBy the value in the year
        val groups = yearRecords.groupBy{_.getOrElse("year","UNKNOWN")}
        
        //group by date
//        Config.indexDAO.pageOverFacet((value2,count2)=>{
//          if(count2>0){
//            //now we need to grab all the records and perform the duplicate groupings.
//            val fq2:Array[String] = fq ++ Array("occurrence_date:"+value2)
//            
//          }
//          true
//        },"occurrence_date",query,fq)
        
        //need to handle the null dates
      }
      true
    },"year",  query ,Array())
    
    //also need to consider the null year
    
    //Config.indexDAO.pageOverIndex(map=>{true},Array("row_key","id","taxon_name","year","month","occurrence_date","collector","lat_long","point-0.0001","point-0.001","point-0.01","point-0.1","point-1"),"taxon_concept_lsid:"+SolrUtils.)
  }
  
  def processYearGroup(yearGroup:List[Map[String,String]]){
    //get them into month groups
    val monthGroups = yearGroup.groupBy(_.getOrElse("month","UNKNOWN"))
    val unknownGroup = monthGroups.getOrElse("UNKNOWN",List())
    monthGroups.foreach{case (month, monthList) =>{
      //val (month, monthList) = values
        if(month != "UNKNOWN"){
          //now group by days
          val dayGroups = monthList.groupBy(_.getOrElse("occurrence_date","UNKNOWN"))
          dayGroups.foreach{case (day, dayList) =>{
            //detect the duplicates within the same group
            
          }} 
        }
    }}
  }
  
  def retrieveRecordGroup(query:String, fqs:Array[String]):List[Map[String,String]]={
    val buffer= new scala.collection.mutable.ArrayBuffer[Map[String,String]]
    //ret
    Config.indexDAO.pageOverIndex(map=>{
      //TODO special preprocessing of collectors names if necessary
       val smap = map.toMap[String,AnyRef].mapValues[String](value => {if(value.getClass == classOf[java.util.Date])
         //store the day of month only
           org.apache.commons.lang.time.DateFormatUtils.format(value.asInstanceOf[java.util.Date], "dd")
         else
           value.toString
         })
       buffer += smap
       true
       },Array("row_key","id","taxon_name","year","month","occurrence_date","collector","lat_long","point-0.0001","point-0.001","point-0.01","point-0.1","point-1"),
       query,fqs)
              
     buffer.toList
  }
  
  def groupDuplicates(sampleList:List[Map[String,String]],duplicateBuffer:ArrayBuffer[List[Map[String,String]]]):List[Map[String,String]]={
    //based on the remaining occurences in the smapleList get the next group of potential duplicates.
    val list = new scala.collection.mutable.LinkedList[Map[String,String]]
    val recordToWorkWith = sampleList.head
    val points = Array(recordToWorkWith.getOrElse("point-1",""),
                       recordToWorkWith.getOrElse("point-0.1",""),
                       recordToWorkWith.getOrElse("point-0.01",""),
                       recordToWorkWith.getOrElse("point-0.001",""),
                       recordToWorkWith.getOrElse("point-0.0001",""),
                       recordToWorkWith.getOrElse("lat_long",""))
    val collector = recordToWorkWith.getOrElse("collector","")
    sampleList.foreach(map=>{
      if(recordToWorkWith.getOrElse("id","") != map.getOrElse("id","A")){
        //check for duplication
        //check the lat lon
        val mpoints = Array(map.getOrElse("point-1",""),
                       map.getOrElse("point-0.1",""),
                       map.getOrElse("point-0.01",""),
                       map.getOrElse("point-0.001",""),
                       map.getOrElse("point-0.0001",""),
                       map.getOrElse("lat_long",""))
        val mcollector =  map.getOrElse("collector","")
//        val mdiff = mpoints.diff(points)
//        val pdiff = points(mpoints.indexOf(mdiff))
        
        //if(isSpatialDuplicate(points, mpoints) && isCollectorDuplicate(collector,mcollector)) 
      }
    })
    List()
  }
  def isCollectorDuplicate(collector1:String, collector2:String): Boolean ={
    //if(collector1 is null )
    false;
  }
  def isSpatialDuplicate(points:Array[String], pointsb:Array[String]):Boolean ={
    for(i <- 0 to 5){
      if(points(i) != pointsb(i)){
        //check to see if the precision is different
        if(i>0){
          //one of the current points has the same coordinates as the previous precision
          if(points(i) == points(i-1) || pointsb(i) == pointsb(i-1)){
            //indicates that we have a precision difference
            return true
          }
          //now check if we have a rounding error by look at the subsequent coordinates...
          return false  
        }
          
      }
    }
    true
  }
}