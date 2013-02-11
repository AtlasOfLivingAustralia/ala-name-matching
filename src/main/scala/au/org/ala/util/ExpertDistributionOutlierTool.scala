package au.org.ala.util

import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.{PostMethod, GetMethod}
import org.codehaus.jackson.map.ObjectMapper
import java.util
import collection.mutable.{ListBuffer, ArrayBuffer}
import java.text.{DateFormat, SimpleDateFormat, MessageFormat}
import collection.JavaConversions._
import au.org.ala.biocache.{Json, AssertionCodes, QualityAssertion, Config}
import util.concurrent.CountDownLatch
import util.TimeZone
import actors.Actor._
import actors.Actor

/**
 * Created with IntelliJ IDEA.
 * User: ChrisF
 * Date: 5/10/12
 * Time: 2:07 PM
 * To change this template use File | Settings | File Templates.
 */

object ExpertDistributionOutlierTool {
  val DISTRIBUTION_DETAILS_URL = Config.layersServiceUrl + "/distributions"
  val RECORDS_URL_TEMPLATE = Config.biocacheServiceUrl + "/occurrences/search?q=taxon_concept_lsid:{0}%20AND%20lat_long:%5B%2A%20TO%20%2A%5D&fl=id,row_key,latitude,longitude,coordinate_uncertainty&facet=off&startIndex={1}&pageSize={2}"
  val DISTANCE_URL_TEMPLATE = Config.layersServiceUrl + "/distribution/outliers/{0}"

  // key to use when storing outlier row keys for an LSID in the distribution_outliers column family
  val DISTRIBUTION_OUTLIERS_COLUMN_FAMILY_KEY = "rowkeys"

  // Threshold value to use for detection of outliers. An occurrence is only considered an outlier if it is found to be over 50km outside of the expert distribution
  val OUTLIER_THRESHOLD = 50000

  // Some distributions have an extremely large number of records associated with them. Handle the records one "page" at a time.
  val RECORDS_PAGE_SIZE = 5000

  def main(args: Array[String]) {
    val tool = new ExpertDistributionOutlierTool();
    var speciesLsid: String = null
    var numThreads = 1

    val parser = new OptionParser("Find expert distribution outliers") {
      opt("l", "specieslsid", "Species LSID. If supplied, outlier detection is only performed for occurrences of the species with the supplied taxon concept LSID ", {
        v: String => speciesLsid = v
      })
      intOpt("t", "numThreads", "Number of threads to use when detecting outliers", {
        v: Int => numThreads = v
      })
    }

    if (parser.parse(args)) {
      tool.findOutliers(speciesLsid, numThreads)
    }
  }
}

class ExpertDistributionOutlierTool {

  /**
   * Entry point for the tool. Find distribution outliers for all records, or for a single species identified by its LSID
   * @param speciesLsid If supplied, restrict identification of outliers to occurrence records associated by a single species, as identified by its LSID.
   */
  def findOutliers(speciesLsid: String, numThreads: Int) {

    val countDownLatch = new CountDownLatch(numThreads);

    actor {
      val distributionLsids = getExpertDistributionLsids();

      if (speciesLsid != null) {
        // If we are only finding outliers for a single lsid, one worker actor will suffice, no need to partition the work.
        if (distributionLsids.contains(speciesLsid)) {
          val speciesLsidInList = new ListBuffer[String]
          speciesLsidInList += speciesLsid
          val a = new ExpertDistributionActor(0, self);
          a.start()
          a ! (speciesLsidInList)
        } else {
          throw new IllegalArgumentException("No expert distribution for species with taxon concept LSID " + speciesLsid)
        }
      } else {
        // Partition the lsids for which outliers need to be processed, and get an actor to work on each batch
        val partitionSize = scala.math.ceil(distributionLsids.length / (numThreads * 1.0)).toInt

        for (i <- 0 to numThreads - 1) {
          val lowerBound = partitionSize * i
          val upperBound = lowerBound + partitionSize

          val lsidPartition = new ListBuffer[String]
          if (upperBound >= distributionLsids.length) {
            lsidPartition ++= distributionLsids.subList(lowerBound, distributionLsids.length)
          } else {
            lsidPartition ++= distributionLsids.subList(lowerBound, upperBound)
          }

          val a = new ExpertDistributionActor(i, self);
          a.start()
          a ! (lsidPartition)
        }
      }

      var completedThreads = 0;
      loopWhile(completedThreads < numThreads) {
        receive {
          case rowKeysForReindexing: ListBuffer[String] => {
            for (rowKey <- rowKeysForReindexing) {
              Console.println(rowKey)
            }
          }
          case "COMPLETED" => {
            completedThreads += 1
            Console.err.println("THREAD COMPLETE")
            countDownLatch.countDown()
          }
          case msg: String => Console.err.println(msg)
        }
      }
    }

    // Calling thread must wait until all actors have finished processing.
    countDownLatch.await()
  }

  /**
   * @return The list of taxon concept LSIDS for which an expert distribution has been loaded into the ALA
   */
  def getExpertDistributionLsids(): ListBuffer[String] = {

    val httpClient = new HttpClient()
    val get = new GetMethod(ExpertDistributionOutlierTool.DISTRIBUTION_DETAILS_URL)
    try {
      val responseCode = httpClient.executeMethod(get)
      if (responseCode == 200) {
        val dataJSON = get.getResponseBodyAsString();
        val mapper = new ObjectMapper();
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
        retBuffer
      } else {
        throw new Exception("getExpertDistributionLsids Request failed (" + responseCode + ")")
      }
    } finally {
      get.releaseConnection()
    }
  }

}

class ExpertDistributionActor(val id: Int, val caller: Actor) extends Actor {

  def act() {
    receive {
      case (distributionLsids: ListBuffer[String]) => {
        //record lsids for distributions that caused errors while finding outliers
        val errorLsids = new ListBuffer[String]()

        for (lsid <- distributionLsids) {
          try {
            caller ! id + " Finding distribution outliers for " + lsid
            val rowKeysForIndexing = findOutliersForLsid(lsid)
            caller ! rowKeysForIndexing
          } catch {
            case ex: Exception => {
              caller ! id + " ERROR OCCURRED WHILE FINDING OUTLIERS FOR LSID " + lsid
              ex.printStackTrace(Console.err)
              errorLsids += lsid
            }
          }
        }

        if (!errorLsids.isEmpty) {
          caller ! id + " RETRYING OUTLIER IDENTIFICATION FOR LSIDS THAT FAILED WITH ERRORS"
          for (errorLsid <- errorLsids) {
            caller ! id + " RETRYING LSID " + errorLsid
            try {
              val rowKeysForIndexing = findOutliersForLsid(errorLsid)
              caller ! rowKeysForIndexing
            } catch {
              case ex: Exception => {
                caller ! id + " ERROR OCCURRED WHILE FINDING OUTLIERS FOR LSID " + errorLsid
                ex.printStackTrace(Console.err)
              }
            }
          }
        }

        caller ! id + " Completed"
        caller ! "COMPLETED"
      }
    }
  }

  /**
   * Find outlier records associated with a species as identified by a taxon concept lsid
   * @param lsid a taxon concept lsid
   */
  def findOutliersForLsid(lsid: String): ListBuffer[String] = {
    val rowKeysForIndexing = new ListBuffer[String]

    // Some distributions have an extremely large number of records associated with them. Handle the records one "page" at a time.
    var recordsMap = getRecordsForLsid(lsid, ExpertDistributionOutlierTool.RECORDS_PAGE_SIZE, 0)

    if (!recordsMap.isEmpty) {
      val outlierRecordDistances = getOutlierRecordDistances(lsid, recordsMap)
      rowKeysForIndexing ++= markOutlierOccurrences(lsid, outlierRecordDistances, recordsMap)
    }

    var pageNumber = 1

    while (!recordsMap.isEmpty) {
      recordsMap = getRecordsForLsid(lsid, ExpertDistributionOutlierTool.RECORDS_PAGE_SIZE, pageNumber * ExpertDistributionOutlierTool.RECORDS_PAGE_SIZE)

      if (!recordsMap.isEmpty) {
        val outlierRecordDistances = getOutlierRecordDistances(lsid, recordsMap)
        rowKeysForIndexing ++= markOutlierOccurrences(lsid, outlierRecordDistances, recordsMap)
      }

      pageNumber = pageNumber + 1
    }

    rowKeysForIndexing
  }

  /**
   * Get details of all the occurrence records for a species identified by a taxon concept LSID.
   * @param lsid A taxon concept lsid
   * @return Occurrence record detail. Only the following fields are retreived: id,row_key,latitude,longitude,coordinate_uncertainty. Only records that have a location (lat/long) are returned.
   */
  def getRecordsForLsid(lsid: String, pageSize: Int, startIndex: Int): scala.collection.mutable.Map[String, Map[String, Object]] = {
    val url = MessageFormat.format(ExpertDistributionOutlierTool.RECORDS_URL_TEMPLATE, lsid, startIndex.toString, pageSize.toString)
    val httpClient = new HttpClient()
    val get = new GetMethod(url)
    try {
      val responseCode = httpClient.executeMethod(get)
      if (responseCode == 200) {
        val dataJSON = get.getResponseBodyAsString();
        val mapper = new ObjectMapper();
        val mapClass = classOf[java.util.Map[_, _]]
        val responseMap = mapper.readValue(dataJSON, mapClass)
        val occurrencesList = responseMap.get("occurrences").asInstanceOf[java.util.List[java.util.Map[String, Object]]]

        var retMap = scala.collection.mutable.Map[String, Map[String, Object]]()
        for (m <- occurrencesList.toArray) {
          val occurrenceMap = m.asInstanceOf[java.util.Map[String, Object]]
          val uuid = occurrenceMap.get("uuid").asInstanceOf[String]
          val rowKey = occurrenceMap.get("rowKey")
          val decimalLatitude = occurrenceMap.get("decimalLatitude")
          val decimalLongitude = occurrenceMap.get("decimalLongitude")
          val coordinateUncertaintyInMeters = occurrenceMap.get("coordinateUncertaintyInMeters")

          retMap(uuid) = Map("rowKey" -> rowKey, "decimalLatitude" -> decimalLatitude, "decimalLongitude" -> decimalLongitude, "coordinateUncertaintyInMeters" -> coordinateUncertaintyInMeters)
        }
        retMap
      } else {
        throw new Exception("getRecordsForLsid Request failed (" + responseCode + ")")
      }
    } finally {
      get.releaseConnection()
    }
  }

  /**
   * For a series of occurrence records associated with a single taxon concept lsid, find which records are outside the expert distribution associated with
   * the taxon concept lsid, and how far outside the expert distribution the outliers occur.
   * @param lsid the taxon concept LSID
   * @param recordsMap the occurrence records data
   * @return A map of outlier record uid to distance outside the expert distribution
   */
  def getOutlierRecordDistances(lsid: String, recordsMap: scala.collection.mutable.Map[String, Map[String, Object]]): scala.collection.mutable.Map[String, Double] = {
    val mapper = new ObjectMapper();

    val recordsMapWithoutRowKeys = new java.util.HashMap[String, java.util.Map[String, Object]]()
    for ((k, v) <- recordsMap) {
      recordsMapWithoutRowKeys.put(k, ((v - "rowKey") - "coordinateUncertaintyInMeters"))
    }

    val recordsMapWithoutRowKeysJSON = mapper.writeValueAsString(recordsMapWithoutRowKeys)

    val httpClient = new HttpClient()

    val url = MessageFormat.format(ExpertDistributionOutlierTool.DISTANCE_URL_TEMPLATE, lsid)
    val post = new PostMethod(url)
    post.addParameter("pointsJson", recordsMapWithoutRowKeysJSON)
    try {
      val responseCode = httpClient.executeMethod(post)
      if (responseCode == 200) {
        val dataJSON = post.getResponseBodyAsString();
        val mapper = new ObjectMapper();
        val mapClass = classOf[java.util.Map[String, Double]]
        val distancesMapJava = mapper.readValue(dataJSON, mapClass)
        val distancesMap: scala.collection.mutable.Map[String, Double] = distancesMapJava

        distancesMap
      } else {
        throw new Exception("getOutlierRecordDistances Request failed (" + responseCode + ")")
      }
    } finally {
      post.releaseConnection()
    }
  }

  /**
   * Mark outlier records as outliers in the biocache. Note that a record is not considered an outlier if its distance outside the expert distribution is less than
   * the record's coordinate uncertainty in metres.
   * @param lsid The taxon concept lsid associated with the occurrence records and expert distribution
   * @param outlierDistances A map of outlier record uid to distance outside the expert distribution
   * @param recordsMap the occurrence records data
   */
  def markOutlierOccurrences(lsid: String, outlierDistances: scala.collection.mutable.Map[String, Double], recordsMap: scala.collection.mutable.Map[String, Map[String, Object]]): ListBuffer[String] = {

    val newOutlierRowKeys = new ListBuffer[String]()
    val rowKeysForIndexing = new ListBuffer[String]

    // Mark records as outliers
    for ((uuid, distance) <- outlierDistances) {

      //Round distance from distribution to nearest metre. Any occurrences outside the distribution by less than a metre are not considered outliers.
      val roundedDistance = scala.math.round(distance)

      if (roundedDistance > 0) {
        var coordinateUncertaintyInMeters: Double = 0;
        if (recordsMap(uuid)("coordinateUncertaintyInMeters") != null) {
          coordinateUncertaintyInMeters = recordsMap(uuid)("coordinateUncertaintyInMeters").asInstanceOf[java.lang.Double]
        }

        // The occurrence is only considered an outlier if its distance from the distribution is greater than its coordinate uncertainty
        if ((roundedDistance - coordinateUncertaintyInMeters) > ExpertDistributionOutlierTool.OUTLIER_THRESHOLD) {

          val rowKey = recordsMap(uuid)("rowKey").asInstanceOf[String]

          Console.err.println("Outlier: " + uuid + "(" + rowKey + ") " + roundedDistance + " metres")

          // Add data quality assertion
          Config.occurrenceDAO.addSystemAssertion(rowKey, QualityAssertion(AssertionCodes.SPECIES_OUTSIDE_EXPERT_RANGE, roundedDistance + " metres outside of expert distribution range"))

          // Record distance against record
          Config.persistenceManager.put(rowKey, "occ", Map("distanceOutsideExpertRange.p" -> roundedDistance.toString()))

          newOutlierRowKeys += rowKey
        }
      }
    }

    rowKeysForIndexing ++= newOutlierRowKeys

    // Remove outlier information from any records that are no longer outliers
    val oldRowKeysJson: String = Config.persistenceManager.get(lsid, "distribution_outliers", ExpertDistributionOutlierTool.DISTRIBUTION_OUTLIERS_COLUMN_FAMILY_KEY).getOrElse(null)
    if (oldRowKeysJson != null) {
      val oldRowKeys = Json.toList(oldRowKeysJson, classOf[String].asInstanceOf[java.lang.Class[AnyRef]]).asInstanceOf[List[String]]

      val noLongerOutlierRowKeys = oldRowKeys diff newOutlierRowKeys

      for (rowKey <- noLongerOutlierRowKeys) {
        Console.err.println(rowKey + " is no longer an outlier")
        Config.persistenceManager.deleteColumns(rowKey, "occ", "distanceOutsideExpertRange.p")
        Config.occurrenceDAO.removeSystemAssertion(rowKey, AssertionCodes.SPECIES_OUTSIDE_EXPERT_RANGE)
        rowKeysForIndexing += rowKey
      }
    }

    // Store row keys for the LSID in the distribution_outliers column family
    val newRowKeysJson = Json.toJSON(newOutlierRowKeys.toList)
    Config.persistenceManager.put(lsid, "distribution_outliers", ExpertDistributionOutlierTool.DISTRIBUTION_OUTLIERS_COLUMN_FAMILY_KEY, newRowKeysJson)

    rowKeysForIndexing
  }

}


