package au.org.ala.util

import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.{PostMethod, GetMethod}
import org.codehaus.jackson.map.ObjectMapper
import java.util
import collection.mutable.{ListBuffer, ArrayBuffer}
import java.text.{DateFormat, SimpleDateFormat, MessageFormat}
import collection.JavaConversions._
import au.org.ala.biocache.{Json, AssertionCodes, QualityAssertion, Config}
import util.TimeZone

/**
 * Created with IntelliJ IDEA.
 * User: ChrisF
 * Date: 5/10/12
 * Time: 2:07 PM
 * To change this template use File | Settings | File Templates.
 */

object ExpertDistributionOutlierTool {
  val DISTRIBUTION_DETAILS_URL = Config.layersServiceUrl + "/distributions"
  val RECORDS_URL_TEMPLATE = Config.biocacheServiceUrl + "/occurrences/search?q=taxon_concept_lsid:{0}%20AND%20lat_long:%5B%2A%20TO%20%2A%5D&fl=id,row_key,latitude,longitude,coordinate_uncertainty&facet=off&pageSize={1}"
  val DISTANCE_URL_TEMPLATE = Config.layersServiceUrl + "/distribution/outliers/{0}"

  // key to use when storing outlier row keys for an LSID in the distribution_outliers column family
  val DISTRIBUTION_OUTLIERS_COLUMN_FAMILY_KEY = "rowkeys"

  // Threshold value to use for detection of outliers. An occurrence is only considered an outlier if it is found to be over 50km outside of the expert distribution
  val OUTLIER_THRESHOLD = 50000

  def main(args: Array[String]) {
    val tool = new ExpertDistributionOutlierTool();

    var speciesLsid: String = null

    val parser = new OptionParser("Find expert distribution outliers") {
      opt("l", "specieslsid", "Species LSID. If supplied, outlier detection is only performed for occurrences of the species with the supplied taxon concept LSID ", {
        v: String => speciesLsid = v
      })
    }

    if (parser.parse(args)) {
      tool.findOutliers(speciesLsid)
    }
  }
}

class ExpertDistributionOutlierTool {

  /**
   * Entry point for the tool. Find distribution outliers for all records, or for a single species identified by its LSID
   * @param speciesLsid If supplied, restrict identification of outliers to occurrence records associated by a single species, as identified by its LSID.
   */
  def findOutliers(speciesLsid: String) {
    //record lsids for distributions that caused errors while finding outliers
    val errorLsids = new ListBuffer[String]()

    val distributionLsids = getExpertDistributionLsids();

    if (speciesLsid != null) {
      if (distributionLsids.contains(speciesLsid)) {
        try {
          findOutliersForLsid(speciesLsid)
        } catch {
          case ex: Exception => {
            Console.err.println("ERROR OCCURRED WHILE FINDING OUTLIERS FOR LSID " + speciesLsid)
            ex.printStackTrace(Console.err)
            errorLsids += speciesLsid
          }
        }
      } else {
        throw new IllegalArgumentException("No expert distribution for species with taxon concept LSID " + speciesLsid)
      }
    } else {
      for (lsid <- distributionLsids) {
        try {
          findOutliersForLsid(lsid)
        } catch {
          case ex: Exception => {
            Console.err.println("ERROR OCCURRED WHILE FINDING OUTLIERS FOR LSID " + lsid)
            ex.printStackTrace(Console.err)
            errorLsids += lsid
          }
        }
      }
    }

    if (!errorLsids.isEmpty) {
      Console.err.println("RETRYING OUTLIER IDENTIFICATION FOR LSIDS THAT FAILED WITH ERRORS")
      for (errorLsid <- errorLsids) {
        Console.err.println("ERROR OCCURRED WHILE FINDING OUTLIERS FOR LSID " + errorLsid)
        try {
          findOutliersForLsid(errorLsid)
        } catch {
          case ex: Exception => {
            Console.err.println("ERROR OCCURRED WHILE FINDING OUTLIERS FOR LSID " + errorLsid)
            ex.printStackTrace(Console.err)
            errorLsids += errorLsid
          }
        }
      }
    }
  }

  /**
   * Find outlier records associated with a species as identified by a taxon concept lsid
   * @param lsid a taxon concept lsid
   */
  def findOutliersForLsid(lsid: String) {
    Console.err.println("Finding distribution outliers for " + lsid)
    val recordsMap = getRecordsForLsid(lsid)
    val outlierRecordDistances = getOutlierRecordDistances(lsid, recordsMap)
    markOutlierOccurrences(lsid, outlierRecordDistances, recordsMap)
  }

  /**
   * @return The list of taxon concept LSIDS for which an expert distribution has been loaded into the ALA
   */
  def getExpertDistributionLsids(): ArrayBuffer[String] = {

    val httpClient = new HttpClient()
    val get = new GetMethod(ExpertDistributionOutlierTool.DISTRIBUTION_DETAILS_URL)
    try {
      val responseCode = httpClient.executeMethod(get)
      if (responseCode == 200) {
        val dataJSON = get.getResponseBodyAsString();
        val mapper = new ObjectMapper();
        val listClass = classOf[java.util.List[java.util.Map[String, String]]]
        val distributionList = mapper.readValue(dataJSON, listClass)

        val retBuffer = new ArrayBuffer[String]()
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

  /**
   * Get details of all the occurrence records for a species identified by a taxon concept LSID.
   * @param lsid A taxon concept lsid
   * @return Occurrence record detail. Only the following fields are retreived: id,row_key,latitude,longitude,coordinate_uncertainty. Only records that have a location (lat/long) are returned.
   */
  def getRecordsForLsid(lsid: String): scala.collection.mutable.Map[String, Map[String, Object]] = {
    val url = MessageFormat.format(ExpertDistributionOutlierTool.RECORDS_URL_TEMPLATE, lsid, java.lang.Integer.MAX_VALUE.toString)

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
  def markOutlierOccurrences(lsid: String, outlierDistances: scala.collection.mutable.Map[String, Double], recordsMap: scala.collection.mutable.Map[String, Map[String, Object]]) {

    val newOutlierRowKeys = new ListBuffer[String]()

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

          // Print rowKey to stdout so that output of this application can be used for reindexing.
          println(rowKey)
        }
      }
    }

    // Remove outlier information from any records that are no longer outliers
    val oldRowKeysJson: String = Config.persistenceManager.get(lsid, "distribution_outliers", ExpertDistributionOutlierTool.DISTRIBUTION_OUTLIERS_COLUMN_FAMILY_KEY).getOrElse(null)
    if (oldRowKeysJson != null) {
      val oldRowKeys = Json.toList(oldRowKeysJson, classOf[String].asInstanceOf[java.lang.Class[AnyRef]]).asInstanceOf[List[String]]

      val noLongerOutlierRowKeys = oldRowKeys diff newOutlierRowKeys

      for (rowKey <- noLongerOutlierRowKeys) {
        Console.err.println(rowKey + " is no longer an outlier")
        Config.persistenceManager.deleteColumns(rowKey, "occ", "distanceOutsideExpertRange.p")
        Config.occurrenceDAO.removeSystemAssertion(rowKey, AssertionCodes.SPECIES_OUTSIDE_EXPERT_RANGE)

        // Print rowKey to stdout so that output of this application can be used for reindexing.
        println(rowKey)
      }
    }

    // Store row keys for the LSID in the distribution_outliers column family
    val newRowKeysJson = Json.toJSON(newOutlierRowKeys.toList)
    Config.persistenceManager.put(lsid, "distribution_outliers", ExpertDistributionOutlierTool.DISTRIBUTION_OUTLIERS_COLUMN_FAMILY_KEY, newRowKeysJson)

  }

}


