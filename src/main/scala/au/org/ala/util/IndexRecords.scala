package au.org.ala.util
import org.slf4j.LoggerFactory
import au.org.ala.biocache._
import java.io.File
import java.util.Date
import scala.collection.mutable.HashMap

/**
 * Runnable for optimising the index.
 */
object OptimiseIndex {
  def main(args: Array[String]): Unit = {
    println("Starting optimise....")
    val indexer = Config.getInstance(classOf[IndexDAO]).asInstanceOf[IndexDAO]
    indexer.optimise
    println("Optimise complete.")
  }
}

/**
 * Index the Cassandra Records to conform to the fields
 * as defined in the schema.xml file.
 *
 * @author Natasha Carter
 */
object IndexRecords {

  import FileHelper._

  val logger = LoggerFactory.getLogger("IndexRecords")
  val indexer = Config.getInstance(classOf[IndexDAO]).asInstanceOf[IndexDAO]
  val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
  val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]

  def main(args: Array[String]): Unit = {
    var startUuid:Option[String] = None
    var dataResource:Option[String] = None
    var empty:Boolean = false
    var check:Boolean = false
    var startDate:Option[String] = None
    var pageSize = 1000
    var uuidFile:String = ""
    var rowKeyFile:String = ""
    val parser = new OptionParser("index records options") {
        opt("empty", "empty the index first", {empty=true})
        opt("check","check to see if the record is deleted before indexing",{check=true})
        opt("s", "start","The record to start with", {v:String => startUuid = Some(v)})
        opt("dr", "resource", "The data resource to process", {v:String => dataResource = Some(v)})
        opt("date", "date", "The earliest modification date for records to be indexed. Date in the form yyyy-mm-dd",
          {v:String => startDate = Some(v)})
        intOpt("ps", "pageSize", "The page size for indexing", {v:Int => pageSize = v })
        opt("if", "file-uuids-to-index","Absolute file path to fle containing UUIDs to index", {v:String => uuidFile = v})
        opt("rf", "file-rowkeys-to-index","Absolute file path to fle containing rowkeys to index", {v:String => rowKeyFile = v})
    }

    if(parser.parse(args)){
        //delete the content of the index
        if(empty){
           logger.info("Emptying index")
           indexer.emptyIndex
        }
        if (uuidFile != ""){
          indexListOfUUIDs(new File(uuidFile))
        } else if (rowKeyFile != ""){
          indexList(new File(rowKeyFile))
        } else {
          index(startUuid, dataResource, false, false, startDate, check, pageSize)
        }
        //shut down pelops and index to allow normal exit
        indexer.shutdown
        persistenceManager.shutdown
     }
  }

  def index(startUuid:Option[String], dataResource:Option[String], optimise:Boolean = false, shutdown:Boolean = false,
            startDate:Option[String]=None, checkDeleted:Boolean=false, pageSize:Int = 1000) = {

    val startKey = {
        if(startUuid.isEmpty && !dataResource.isEmpty) {
          dataResource.get +"|"
        } else {
          startUuid.getOrElse("")
        }
    }

    var date:Option[Date]=None
    if(!startDate.isEmpty){
        date = DateParser.parseStringToDate(startDate.get +" 00:00:00")
        if(date.isEmpty)
            throw new Exception("Date is in incorrect format. Try yyyy-mm-dd")
        logger.info("Indexing will be restricted to records changed after " + date.get)
    }

    val endKey = if(dataResource.isEmpty) "" else dataResource.get +"|~"
    if(startKey == ""){
       logger.info("Starting full index")
    } else {
       logger.info("Starting to index " + startKey + " until " + endKey)
    }
    indexRange(startKey, endKey, date, checkDeleted)
    //index any remaining items before exiting
    indexer.finaliseIndex(optimise, shutdown)  
  }

  def indexRange(startUuid:String, endUuid:String, startDate:Option[Date]=None, checkDeleted:Boolean=false, pageSize:Int = 1000)={
    var counter = 0
    val start = System.currentTimeMillis
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis
    performPaging( (guid, map) => {
        counter += 1
        //println("Indexing doc: " + counter)
        val fullMap = new HashMap[String, String]
        fullMap ++= map
        ///convert EL and CL properties at this stage
//        fullMap ++= Json.toStringMap(map.getOrElse("el.p", "{}"))
//        fullMap ++= Json.toStringMap(map.getOrElse("cl.p", "{}"))
        val mapToIndex = fullMap.toMap

        indexer.indexFromMap(guid, mapToIndex, startDate=startDate)
        if (counter % pageSize == 0) {
          finishTime = System.currentTimeMillis
          logger.info(counter + " >> Last key : " + guid + ", records per sec: " +
            pageSize.toFloat / (((finishTime - startTime).toFloat) / 1000f))
          startTime = System.currentTimeMillis
        }
        true
    }, startUuid, endUuid, checkDeleted = checkDeleted, pageSize = pageSize)

    finishTime = System.currentTimeMillis
    logger.info("Total indexing time " + ((finishTime-start).toFloat)/1000f + " seconds")
  }

  /**
   * Page over records function
   */
  def performPaging(proc: ((String, Map[String, String]) => Boolean), startKey: String = "",
                    endKey: String = "", pageSize: Int = 1000, checkDeleted: Boolean = false) {
    if (checkDeleted) {
      persistenceManager.pageOverSelect("occ", (guid, map) => {
        if (map.getOrElse(FullRecordMapper.deletedColumn, "false").equals("false")) {
          val map = persistenceManager.get(guid, "occ")
          if (!map.isEmpty) {
            proc(guid, map.get)
          }
        }
        true
      }, startKey, endKey, pageSize, "uuid", "rowKey", FullRecordMapper.deletedColumn)
    } else {
      println("****** Performing selective paging with list of fields.....")
      persistenceManager.pageOverAll("occ", (guid, map) => {
        proc(guid, map)
      }, startKey, endKey, pageSize)
    }
  }

  /**
   * Indexes the supplied list of rowKeys
   */
  def indexList(file: File) {
    var counter = 0
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis

    file.foreachLine(line => {
      counter += 1
      val map = persistenceManager.get(line, "occ")
      if (!map.isEmpty) indexer.indexFromMap(line, map.get)

      if (counter % 1000 == 0) {
        finishTime = System.currentTimeMillis
        logger.info(counter + " >> Last key : " + line + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
        startTime = System.currentTimeMillis
      }
    })

    indexer.finaliseIndex(false, true)
  }

  /**
   * Indexes the supplied list of rowKeys
   */
  def indexListOfUUIDs(file: File) {
    var counter = 0
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis

    file.foreachLine(line => {
      counter += 1
      val map = persistenceManager.getByIndex(line, "occ", "uuid")
      if (!map.isEmpty) indexer.indexFromMap(line, map.get)

      if (counter % 1000 == 0) {
        finishTime = System.currentTimeMillis
        logger.info(counter + " >> Last key : " + line + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
        startTime = System.currentTimeMillis
      }
    })

    println("Finalising index.....")
    indexer.finaliseIndex(false, true)
    println("Finalised index.")
  }
}