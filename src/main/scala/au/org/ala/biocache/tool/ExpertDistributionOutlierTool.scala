package au.org.ala.biocache.tool

import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.{PostMethod, GetMethod}
import org.codehaus.jackson.map.ObjectMapper
import collection.mutable.ListBuffer
import java.text.{SimpleDateFormat, MessageFormat}
import collection.JavaConversions._
import au.org.ala.biocache.Config
import scala.actors.Actor._
import scala.actors.Actor
import java.util.concurrent.{ArrayBlockingQueue, CountDownLatch}
import org.apache.solr.client.solrj.util.ClientUtils
import org.slf4j.LoggerFactory
import scala.Array
import com.vividsolutions.jts.io.WKTReader
import com.vividsolutions.jts.operation.distance.DistanceOp
import au.org.ala.biocache.qa.QaPasser
import java.io.{FileReader, FileWriter, File}
import au.com.bytecode.opencsv.CSVReader
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang3.time.DateUtils
import java.util.Date
import au.org.ala.biocache.util.{Json, GenericConsumer, OptionParser}
import au.org.ala.biocache.vocab.AssertionCodes
import au.org.ala.biocache.model.QualityAssertion

/**
 * Companion object for ExpertDistributionOutlierTool
 */
object ExpertDistributionOutlierTool {

  val DISTRIBUTIONS_URL = Config.layersServiceUrl + "/distributions"
  val DISTRIBUTION_DETAILS_URL_TEMPLATE = Config.layersServiceUrl + "/distribution/lsid/{0}"
  val DISTANCE_URL_TEMPLATE = Config.layersServiceUrl + "/distribution/outliers/{0}"

  val RECORDS_QUERY_TEMPLATE = "species_guid:{0} OR subspecies_guid:{0}"
  val RECORDS_FILTER_QUERY_TEMPLATE = "geospatial_kosher:true"
  val DATE_RANGE_FILTER = "last_load_date:[{0} TO *]"

  // key to use when storing outlier row keys for an LSID in the distribution_outliers column family
  val DISTRIBUTION_OUTLIERS_COLUMN_FAMILY_KEY = "rowkeys"

  // Threshold value to use for detection of outliers. An occurrence is only considered an outlier if it is found to be over 50km outside of the expert distribution
  val OUTLIER_THRESHOLD = 50000

  // Some distributions have an extremely large number of records associated with them. Handle the records one "page" at a time.
  val RECORDS_PAGE_SIZE = 5000

  def main(args: Array[String]) {

    val tool = new ExpertDistributionOutlierTool
    var speciesLsid: String = null
    var numThreads = 1
    var passThreads = 16
    var test = false
    var dir:Option[String] = None
    var lastModifiedDate:Option[String] = None

    val parser = new OptionParser("Find expert distribution outliers") {
      opt("l", "specieslsid", "Species LSID. If supplied, outlier detection is only performed for occurrences of the species with the supplied taxon concept LSID ", {
        v: String => speciesLsid = v
      })
      intOpt("t", "numThreads", "Number of threads to use when detecting outliers", {
        v: Int => numThreads = v
      })
      intOpt("pt","passThreads","Number of threads to write the passed records on.", {v:Int => passThreads = v})
      opt("test","Test the outliers but don't write to Cassandra", {test =true})
      opt("d","dir","The directory in which the offline dumps are located", {v:String => dir = Some(v)})
      intOpt("day","numDaysMod","Number of days since the last modified.  This will limit the records that are marked as passed.", { v:Int=>
        val sfd = new SimpleDateFormat("yyyy-MM-dd")
        val days:Int = 0 -v
        lastModifiedDate = Some(sfd.format(DateUtils.addDays(new Date(), days)) + "T00:00:00Z")
      })
    }

    if (parser.parse(args)) {
      val qaPasser = new QaPasser(QualityAssertion(AssertionCodes.SPECIES_OUTSIDE_EXPERT_RANGE,1), passThreads)
      Config.indexDAO.init
      tool.findOutliers(speciesLsid, numThreads, test,qaPasser,dir,lastModifiedDate)
    }
  }
}

/**
 * Class for testing records against expert distributions.
 */
class ExpertDistributionOutlierTool {

  val logger = LoggerFactory.getLogger("ExpertDistributionOutlierTool")

  /**
   * Entry point for the tool. Find distribution outliers for all records, or for a single species identified by its LSID
   * @param speciesLsid If supplied, restrict identification of outliers to occurrence records associated by a single species, as identified by its LSID.
   */
  def findOutliers(speciesLsid: String, numThreads: Int, test:Boolean, qaPasser:QaPasser, directory:Option[String],lastModifiedDate:Option[String]) {

    logger.info("Starting to detect the outliers...")
    val countDownLatch = new CountDownLatch(numThreads)
    val reindexFile = new FileWriter(new File("/data/offline/expert_index_row_keys.out"))

    actor {

      var workQueue = scala.collection.mutable.Queue[String]()
      var errorLsidsQueue = scala.collection.mutable.Queue[String]()
      val distributionLsids = getExpertDistributionLsids

      if (speciesLsid != null) {
        // If we are only finding outliers for a single lsid, one worker actor will suffice, no need to partition the work.
        if (distributionLsids.contains(speciesLsid)) {
          workQueue += speciesLsid
          val a = new ExpertDistributionActor(0, self, test, qaPasser,None, List(),lastModifiedDate);
          a.start()
        } else {
          throw new IllegalArgumentException("No expert distribution for species with taxon concept LSID " + speciesLsid)
        }
      } else {
        if (directory.isEmpty){
          //only add to work queue if the directory dump files are nto provided.
          workQueue ++= distributionLsids
        }

        for (i <- 0 to numThreads - 1) {
          val a = new ExpertDistributionActor(i, self, test, qaPasser, directory,distributionLsids.toList, lastModifiedDate);
          a.start()
        }
      }

      var completedThreads = 0
      loopWhile(completedThreads < numThreads) {
        receive {
          case ("SEND JOB", actor: ExpertDistributionActor) => {
            if (!workQueue.isEmpty) {
              val lsid = workQueue.dequeue
              actor ! (lsid)
            } else if (!errorLsidsQueue.isEmpty) {
              val lsid = errorLsidsQueue.dequeue()
              logger.info("Retrying processing of previously failed lsid: " + lsid)
              actor ! (lsid)
            } else {
              actor ! "EXIT"
            }
          }
          case ("EXITED", actor: ExpertDistributionActor) => {
            completedThreads += 1
            countDownLatch.countDown()
          }
          case ("PROCESSED", speciesLsid: String, rowKeysForIndexing: ListBuffer[_], actor: ExpertDistributionActor) => {
            for (rowKey <- rowKeysForIndexing) {
              logger.debug("PROCESSED : " + rowKey)
              reindexFile.append(rowKey+"\n")
            }
            reindexFile.flush()
          }
          case ("ERROR", speciesLsid: String, actor: ExpertDistributionActor) => {
            errorLsidsQueue += speciesLsid

          }
          case msg: String => logger.error("received message " + msg)
        }
      }
    }

    // Calling thread must wait until all actors have finished processing.
    countDownLatch.await()
    //now stop the QaPasser
    qaPasser.stop()
    reindexFile.flush()
    reindexFile.close()
  }

  /**
   * @return The list of taxon concept LSIDS for which an expert distribution has been loaded into the ALA
   */
  def getExpertDistributionLsids : ListBuffer[String] = {
    logger.info("Starting to get the expert distribution LSIDS")
    val httpClient = new HttpClient()
    val get = new GetMethod(ExpertDistributionOutlierTool.DISTRIBUTIONS_URL)
    try {
      val responseCode = httpClient.executeMethod(get)
      if (responseCode == 200) {
        val dataJSON = get.getResponseBodyAsString()
        val mapper = new ObjectMapper()
        val listClass = classOf[java.util.List[java.util.Map[String, String]]]
        val distributionList = mapper.readValue(dataJSON, listClass)

        val retBuffer = new ListBuffer[String]()
        for (m <- distributionList.toArray) {
          val lsid = m.asInstanceOf[java.util.Map[String, String]].get("lsid")
          // Ignore any expert distributions for which we do not have an associated LSID.
          if (lsid != null) {
            retBuffer += lsid
          }
        }
        logger.info("Finished getting the expert distributions: " + retBuffer.size)
        retBuffer
      } else {
        throw new Exception("getExpertDistributionLsids Request failed (" + responseCode + ")")
      }
    } finally {
      get.releaseConnection()
    }
  }
}

class ExpertDistributionActor(val id: Int, val dispatcher: Actor, test:Boolean, qaPasser:QaPasser, directory:Option[String], distributionLsids:List[String], lastModifiedDate:Option[String]) extends Actor {

  val logger = LoggerFactory.getLogger("ExpertDistributionOutlierTool")

  def act() {
    logger.info("Worker(" + id + ") started")
    if(directory.isEmpty){
      // Ask the dispatcher for an initial job
      dispatcher !("SEND JOB", self)

      loop {
        react {
          case "EXIT" => {
            logger.info("Worker(" + id + ") stopping")
            dispatcher !("EXITED", self)
            exit()
          }
          case (speciesLsid: String) => {
            logger.info("Worker(" + id + ") finding distribution outliers for " + speciesLsid)

            try {
              val rowKeysForIndexing = findOutliersForLsid(speciesLsid, test)
              dispatcher !("PROCESSED", speciesLsid, rowKeysForIndexing, self)
            } catch {
              case ex: Exception => {
                logger.error("Worker(" + id + ") experienced error finding distribution outliers for " + speciesLsid, ex)
                dispatcher !("ERROR", speciesLsid, self)
              }
            }

            // ask dispatcher for next job
            dispatcher !("SEND JOB", self)
          }
        }
      }
    } else {
      //we will take out file and try to locate lsids to test
      handleFile(directory.get + File.separator + id + File.separator+"species.out",2,test)
      handleFile(directory.get + File.separator + id + File.separator+"subspecies.out",3,test)
      //tell dispatcher that we are finished
      dispatcher !("EXITED", self)
      exit()
    }
  }

  /**
   * Extracts the occurrence records details out of a file only if the lsid represents one that has a distribution.
   *
   * @param fileName The absolute path to the file to use in the detection
   * @param guidIdx The index for the guid
   * @param test Whether or not to perform a test load
   */
  def handleFile(fileName:String, guidIdx:Int, test:Boolean){

    logger.info("Starting to handle file: " + fileName)

    val uuidIdx = 1
    val rowIdx = 0
    val latIdx = 17
    val longIdx = 18
    val coorIdx = 23

    val reader:CSVReader = new CSVReader(new FileReader(fileName), '\t','~')
    var currentLsid = ""
    var currentLine = reader.readNext()
    var isValid = false
    val map = scala.collection.mutable.Map[String, Map[String, Object]]()
    while (currentLine != null) {
      if (currentLsid != currentLine(guidIdx)){
        if(!map.isEmpty){
          val rowKeysForIndexing = findOutliersForLsid(currentLsid, test,Some(map))
          dispatcher !("PROCESSED", currentLsid, rowKeysForIndexing, self)
          map.clear()
        }
        currentLsid = currentLine(guidIdx)
        isValid = distributionLsids.contains(currentLsid)
        logger.info("will gather all records for " + currentLsid + " " + isValid)
      }

      if(isValid){
        //add it to the map
        val uncertainty = StringUtils.trimToNull(currentLine(coorIdx))
        map(currentLine(uuidIdx)) = Map("rowKey" -> currentLine(rowIdx), "decimalLatitude" -> currentLine(latIdx),
          "decimalLongitude" -> currentLine(longIdx), "coordinateUncertaintyInMeters" -> (if(uncertainty == null) uncertainty else java.lang.Double.parseDouble(uncertainty).asInstanceOf[Object]))
      }
      currentLine = reader.readNext
    }

    if (!map.isEmpty){
      val rowKeysForIndexing = findOutliersForLsid(currentLsid, test,Some(map))
      dispatcher !("PROCESSED", currentLsid, rowKeysForIndexing, self)
    }
    logger.info("Finished handling file " + fileName)
  }

  /**
   * Find outlier records associated with a species as identified by a taxon concept lsid
   * @param lsid a taxon concept lsid
   */
  def findOutliersForLsid(lsid: String, test:Boolean, occPoints:Option[scala.collection.mutable.Map[String, Map[String, Object]]]=None): ListBuffer[String] = {
    logger.info("Starting to find the outliers for " +lsid)
    val rowKeysForIndexing = new ListBuffer[String]
    val rowKeyPassed = new ListBuffer[String]
    // get wkt for lsid
    logger.info("Get the WKT for " + lsid)
    val (wkt,bbox) = getExpertDistributionWkt(lsid)
    logger.info("Finished getting WKT for " + lsid)

    // Some distributions have an extremely large number of records associated with them. Handle the records one "page" at a time.
    logger.info("Get records for " + lsid)
    var recordsMap = if(occPoints.isDefined) occPoints.get else getRecordsOutsideDistribution(lsid, wkt)

    logger.info("Finished getting records fo " + lsid)

    logger.info(recordsMap.size + " records for " + lsid)

    if (!recordsMap.isEmpty) {
      val coords = getDistinctCoordinatesWithinBoundingBox(recordsMap,bbox)

      //maximum of 1000 points to be tested at once.
      val limitedMaps = coords.grouped(1000)//recordsMap.grouped(500)
      var ids = 0
      var outlierDistances: scala.collection.mutable.Map[String, Double] =scala.collection.mutable.Map[String, Double]()
      val queue = new ArrayBlockingQueue[scala.collection.mutable.Map[String, Map[String, Object]]](100)
      val pool:Array[GenericConsumer[scala.collection.mutable.Map[String, Map[String, Object]]]] = Array.fill(10){
        val thread = new GenericConsumer[scala.collection.mutable.Map[String, Map[String, Object]]](queue, ids, (value,id)=>{
          logger.info("Starting to retrieve outlier distances for " + lsid + " on thread " + id + " for " + value.size + " points")
          val outlierRecordDistances = getOutlierRecordDistances(lsid, value, wkt)
          outlierDistances.synchronized{
            outlierDistances ++= outlierRecordDistances
            //logger.info(outlierDistances.toString)
          }
          logger.info("Finished getting the distances for " + lsid + " on thread " + id)
        })
        thread.start()
        ids+=1
        thread
      }
      limitedMaps.foreach(value => queue.put(value))
      pool.foreach(t =>t.shouldStop = true)
      pool.foreach(_.join)
      logger.info("Finished all the threaded distance lookups for " + lsid)
      logger.info("Starting to retrieve outlier distances for " + lsid)
      //val outlierRecordDistances = getOutlierRecordDistances(lsid, recordsMap)
      logger.info("Finished getting the distances for " + lsid)
      rowKeysForIndexing ++= markOutlierOccurrences(lsid, outlierDistances, recordsMap, coords,test, qaPasser)
      logger.info("Finished marking the outlier records for " + lsid)
    }

    rowKeysForIndexing
  }

  def getDistinctCoordinatesWithinBoundingBox(resultMap:scala.collection.mutable.Map[String, Map[String, Object]], bbox:String) :scala.collection.mutable.Map[String, Map[String, Object]]={
    val latLong = resultMap.map{case (k,v) =>
      val latitude = v.getOrElse("decimalLatitude","")
      val longitude = v.getOrElse("decimalLongitude","")
      val key = latitude + "|" + longitude
      (key ->Map("decimalLatitude" ->latitude, "decimalLongitude"->longitude))
    }
    logger.info("unique coordinate count: "  + latLong.size)
    //now filter to the points that appear within the bounding box
    val reader = new WKTReader()
    val bbGeo = reader.read(bbox)
    val bounded = latLong.filter(p=>{
      val latitude =p._2.getOrElse("decimalLatitude","0");
      val longitude = p._2.getOrElse("decimalLongitude", "0");
      val point ="POINT(" + longitude + " " + latitude + ")"
      val pointGeo = reader.read(point)
      bbGeo.contains(pointGeo)
    })
    logger.info("consolidated coordinates to "  + bounded.size)
    bounded
  }

  /**
   * Get details of all the occurrence records for a species identified by a taxon concept LSID.
   * @param lsid A taxon concept lsid
   * @return Occurrence record detail. Only the following fields are retreived: id,row_key,latitude,longitude,coordinate_uncertainty. Only records that have a location (lat/long) are returned.
   */
  def getRecordsOutsideDistribution(lsid: String, distributionWkt: String): scala.collection.mutable.Map[String, Map[String, Object]] = {

    var resultsMap = scala.collection.mutable.Map[String, Map[String, Object]]()

    def addRecordToMap(occurrenceMap: java.util.Map[String, AnyRef]): Boolean = {
      val uuid = occurrenceMap.get("id").asInstanceOf[String]
      val rowKey = occurrenceMap.get("row_key")
      val latitude = occurrenceMap.get("latitude")
      val longitude = occurrenceMap.get("longitude")
      val coordinateUncertainty = occurrenceMap.get("coordinate_uncertainty")
      resultsMap(uuid) = Map("rowKey" -> rowKey, "decimalLatitude" -> latitude, "decimalLongitude" -> longitude, "coordinateUncertaintyInMeters" -> coordinateUncertainty)
      true
    }

    val query = MessageFormat.format(ExpertDistributionOutlierTool.RECORDS_QUERY_TEMPLATE, ClientUtils.escapeQueryChars(lsid))
    //val filterQuery = MessageFormat.format(ExpertDistributionOutlierTool.RECORDS_FILTER_QUERY_TEMPLATE, distributionWkt)
    var filterQuery = Array(ExpertDistributionOutlierTool.RECORDS_FILTER_QUERY_TEMPLATE)
    if(lastModifiedDate.isDefined){
      filterQuery = filterQuery ++ Array(MessageFormat.format(ExpertDistributionOutlierTool.DATE_RANGE_FILTER, lastModifiedDate.get))
    }

    Config.indexDAO.pageOverIndex(addRecordToMap, Array("id", "row_key", "latitude", "longitude", "coordinate_uncertainty"), query, filterQuery)

    resultsMap
  }
   //-d /data/offline/exports
  /**
   * Retrieves the outlier distances using JTS/geotools rather than a WS call to the layers-service
   * @param lsid The lsid of the species that is being tested
   * @param recordsMap The map of coordinates
   * @param wkt The WKT for the expert distrubution of the sepcies being tested
   * @return   A map of points with distances outside the expert distribution.
   */
  def getOutlierRecordDistances(lsid: String, recordsMap: scala.collection.mutable.Map[String, Map[String, Object]], wkt:String): scala.collection.mutable.Map[String, Double] = {
    val reader = new WKTReader()
    val geo = reader.read(wkt)

    val newMap = recordsMap.mapValues(v =>{
      val latitude = v.getOrElse("decimalLatitude","0");
      val longitude = v.getOrElse("decimalLongitude", "0");
      val point ="POINT(" + longitude + " " + latitude + ")"
      val pointGeo = reader.read(point)
      val points =DistanceOp.nearestPoints(geo, pointGeo)

      org.geotools.geometry.jts.JTS.orthodromicDistance(points(0), points(1),org.geotools.referencing.crs.DefaultGeographicCRS.WGS84)
    })
    scala.collection.mutable.Map[String,Double]() ++ newMap.filter(_._2 >0)
  }

  /**
   * Mark records as outliers including marking the records that have passed the validation.
   * @param lsid  The lsid of the species that was being tested
   * @param outlierDistances The map of coordinates to outlier distance. A coordinate will only appear in here if it is outside the dsitribution.
   * @param recordsMap The records for the species
   * @param testedPoints The coordinates that appear with in the bounding box of the distribution. Thus have been tested.
   * @param test Whether or not a test is being performed.  If true no values will be written to cassandra
   * @param qaPasser The passer class that is used write that a QA has passed.
   * @return a list of rowKeys that need index
   */
  def markOutlierOccurrences(lsid: String, outlierDistances: scala.collection.mutable.Map[String, Double], recordsMap: scala.collection.mutable.Map[String, Map[String, Object]], testedPoints:scala.collection.mutable.Map[String, Map[String, Object]], test:Boolean, qaPasser:QaPasser): ListBuffer[String] = {
    val newOutlierRowKeys = new ListBuffer[String]()
    val rowKeysForIndexing = new ListBuffer[String]
    val passedRowKeys = new ListBuffer[String]

    recordsMap.foreach{case (key, value)=>{
      //key is the rowKey value is a map of values that we need to use to determine outlier information
      val latitude = value.getOrElse("decimalLatitude","")
      val longitude = value.getOrElse("decimalLongitude","")
      val outKey = latitude + "|" + longitude
      val outValue = outlierDistances.get(outKey)
      val rowKey = value.getOrElse("rowKey","").asInstanceOf[String]
      if (outValue.isDefined){
        //this one may be an outlier perform the tests
        val roundedDistance = scala.math.round(outValue.get)
        if (roundedDistance > 0){
          //this record represents an outlier
          //now test against coordinate uncertainty and threshold
          val coorValue = value.getOrElse("coordinateUncertaintyInMeters","0")
          val coordinateUncertaintyInMeters:Double = if(coorValue != null) coorValue.asInstanceOf[java.lang.Double] else 0d
          // The occurrence is only considered an outlier if its distance from the distribution is greater than its coordinate uncertainty
          if ((roundedDistance - coordinateUncertaintyInMeters) > ExpertDistributionOutlierTool.OUTLIER_THRESHOLD) {
            logger.info("Outlier: (" + rowKey + ") " + roundedDistance + " metres")
            if(!test){
              // Add data quality assertion
              Config.occurrenceDAO.addSystemAssertion(rowKey, QualityAssertion(AssertionCodes.SPECIES_OUTSIDE_EXPERT_RANGE, roundedDistance + " metres outside of expert distribution range"), replaceExistCode=true)

              // Record distance against record
              Config.persistenceManager.put(rowKey, "occ", Map("distanceOutsideExpertRange.p" -> roundedDistance.toString()))
            }

            newOutlierRowKeys += rowKey
          } else{
            passedRowKeys += rowKey
          }

        } else{
          passedRowKeys += rowKey
        }
      } else{
        //check to see if the value was tested
        if(testedPoints.contains(outKey)){
          //this record has passed the test
          passedRowKeys += rowKey
        } else{
          //record was not tested so don't do anything
        }
      }
    }}

    rowKeysForIndexing ++= newOutlierRowKeys

    // Remove outlier information from any records that are no longer outliers
    if(!test){
      val oldRowKeysJson: String = Config.persistenceManager.get(lsid, "distribution_outliers", ExpertDistributionOutlierTool.DISTRIBUTION_OUTLIERS_COLUMN_FAMILY_KEY).getOrElse(null)
      if (oldRowKeysJson != null) {
        try{
          val oldRowKeys = Json.toList(oldRowKeysJson, classOf[String].asInstanceOf[java.lang.Class[AnyRef]]).asInstanceOf[List[String]]

          //only remove old outliers if it is not a incremental load
          if(lastModifiedDate.isEmpty){
            val noLongerOutlierRowKeys = oldRowKeys diff newOutlierRowKeys
            for (rowKey <- noLongerOutlierRowKeys) {
              logger.warn(rowKey + " is no longer an outlier")
              Config.persistenceManager.deleteColumns(rowKey, "occ", "distanceOutsideExpertRange.p")
              Config.occurrenceDAO.removeSystemAssertion(rowKey, AssertionCodes.SPECIES_OUTSIDE_EXPERT_RANGE)
              rowKeysForIndexing += rowKey
            }
          } else{
            //add this row key to the "oldKeys"
            newOutlierRowKeys ++= oldRowKeys
          }
        } catch {
          case e: Exception => logger.error("Unable to get parse past distribution ingto array: " + oldRowKeysJson + " for " + lsid, e)
        }
      }

      // Store row keys for the LSID in the distribution_outliers column family
      val newRowKeysJson = Json.toJSON(newOutlierRowKeys.toList)
      Config.persistenceManager.put(lsid, "distribution_outliers", ExpertDistributionOutlierTool.DISTRIBUTION_OUTLIERS_COLUMN_FAMILY_KEY, newRowKeysJson)
      logger.info("Number of records: "+ recordsMap.size + " outlier count: " +newOutlierRowKeys.size + " passed records: " + passedRowKeys.size)

      //now mark all the records that have passed
      qaPasser.markRecords(passedRowKeys.toList)
    }
    rowKeysForIndexing
  }

  /**
   * Retrieves the expert distribution and bounding box for the supplied lsid.
   * Uses the layers-service WS calls.
   *
   * @param speciesLsid
   * @return
   */
  def getExpertDistributionWkt(speciesLsid: String): (String,String) = {

    val httpClient = new HttpClient()
    val get = new GetMethod(MessageFormat.format(ExpertDistributionOutlierTool.DISTRIBUTION_DETAILS_URL_TEMPLATE, speciesLsid))
    try {
      val responseCode = httpClient.executeMethod(get)
      if (responseCode == 200) {
        val dataJSON = get.getResponseBodyAsString();
        val mapper = new ObjectMapper();
        val mapClass = classOf[java.util.Map[String, String]]
        val detailsMap = mapper.readValue(dataJSON, mapClass)

        (detailsMap.get("geometry"), detailsMap.get("bounding_box"))
      } else {
        throw new Exception("getExpertDistributionLsids Request failed (" + responseCode + ")")
      }
    } finally {
      get.releaseConnection()
    }
  }
}