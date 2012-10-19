package au.org.ala.util

import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.{PostMethod, GetMethod}
import org.codehaus.jackson.map.ObjectMapper
import java.util
import collection.mutable.ArrayBuffer
import java.text.{DateFormat, SimpleDateFormat, MessageFormat}
import collection.JavaConversions._
import au.org.ala.biocache.{AssertionCodes, QualityAssertion, Config}
import util.TimeZone

/**
 * Created with IntelliJ IDEA.
 * User: ChrisF
 * Date: 5/10/12
 * Time: 2:07 PM
 * To change this template use File | Settings | File Templates.
 */

object ExpertDistributionOutlierTool {
  val DISTRIBUTION_DETAILS_URL = "http://spatial.ala.org.au/layers-service/distributions"
  val RECORDS_URL_TEMPLATE = "http://sandbox.ala.org.au/biocache-service/occurrences/search?q=taxon_concept_lsid:{0}%20AND%20lat_long:%5B%2A%20TO%20%2A%5D&fl=id,row_key,latitude,longitude,coordinate_uncertainty&facet=off&pageSize={1}"
  val RECORD_URL_WITH_DATE_FILTER_TEMPLATE = "http://sandbox.ala.org.au/biocache-service/occurrences/search?q=taxon_concept_lsid:{0}%20AND%20lat_long:%5B%2A%20TO%20%2A%5D%20AND%20last_load_date:%5B{1}%20TO%20%2A%5D%20AND%20last_processed_date:%5B{2}%20TO%20%2A%5D&fl=id,row_key,latitude,longitude,coordinate_uncertainty&facet=off&pageSize={3}"
  //val RECORDS_URL_TEMPLATE = "http://biocache.ala.org.au/ws/occurrences/search?q=taxon_concept_lsid:{0}%20AND%20lat_long:%5B%2A%20TO%20%2A%5D&fl=id,row_key,latitude,longitude,coordinate_uncertainty&facet=off&pageSize={1}"
  //val RECORD_URL_WITH_DATE_FILTER_TEMPLATE = "http://biocache.ala.org.au/ws/occurrences/search?q=taxon_concept_lsid:{0}%20AND%20lat_long:%5B%2A%20TO%20%2A%5D%20AND%20last_load_date:%5B{1}%20TO%20%2A%5D%20AND%20last_processed_date:%5B{2}%20TO%20%2A%5D&fl=id,row_key,latitude,longitude,coordinate_uncertainty&facet=off&pageSize={3}"
  val DISTANCE_URL_TEMPLATE = "http://spatial-dev.ala.org.au/layers-service/distribution/outliers/{0}"
  val LAST_SUCCESSFUL_BUILD_URL = "http://ala-macropus.it.csiro.au:8080/jenkins/job/Biocache%20Index%20Optimise/lastStableBuild/api/json"

  def main(args: Array[String]) {
    val tool = new ExpertDistributionOutlierTool();

    var examineAllRecords = false

    val parser = new OptionParser("Find expert distribution outliers") {
      booleanOpt("a", "examineallrecords", "Examine all records. Default behaviour is to examine only those records that have been loaded or processed since the last run of the Jenkins job that kicks off this tool", {
        v: Boolean => examineAllRecords = v
      })
    }

    if (parser.parse(args)) {
      tool.findOutliers(examineAllRecords)
    }
  }

  def getDateOfLastSuccessfulRun(): java.util.Date = {
    val httpClient = new HttpClient()
    val get = new GetMethod(ExpertDistributionOutlierTool.LAST_SUCCESSFUL_BUILD_URL)
    try {
      val responseCode = httpClient.executeMethod(get)
      if (responseCode == 200) {
        val dataJSON = get.getResponseBodyAsString();
        val mapper = new ObjectMapper();
        val mapClass = classOf[java.util.Map[_, _]]
        val responseMap = mapper.readValue(dataJSON, mapClass)

        val timestamp = responseMap.get("timestamp").asInstanceOf[java.lang.Long]
        val time = new java.util.Date(timestamp)
        time
      } else {
        throw new Exception("getDateOfLastSuccessfulRun Request failed (" + responseCode + ")")
      }
    } finally {
      get.releaseConnection()
    }
  }
}

class ExpertDistributionOutlierTool {

  // Solr index requires dates to be in ISO 8601 at UTC, with 'Z' timezone identifier
  val dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
  dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"))
  val dateLastSuccessfulRun = ExpertDistributionOutlierTool.getDateOfLastSuccessfulRun()

  def findOutliers(examineAllRecords: Boolean) {
    val distributionLsids = getExpertDistributionLsids();
    for (lsid <- distributionLsids) {
      val recordsMap = getRecordsForLsid(lsid, examineAllRecords)
      val outlierRecordDistances = getOutlierRecordDistances(lsid, recordsMap)
      markOutlierOccurrences(outlierRecordDistances, recordsMap)
    }
  }

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

  def getRecordsForLsid(lsid: String, examineAllRecords: Boolean): scala.collection.mutable.Map[String, Map[String, Object]] = {
    var url = ""

    if (examineAllRecords) {
      url = MessageFormat.format(ExpertDistributionOutlierTool.RECORDS_URL_TEMPLATE, lsid, java.lang.Integer.MAX_VALUE.toString)
    } else {
      // and filter for records that were loaded or processed after the last successful run of the Jenkins job associated with this tool.
      val formattedDate = dateFormatter.format(dateLastSuccessfulRun)
      url = MessageFormat.format(ExpertDistributionOutlierTool.RECORD_URL_WITH_DATE_FILTER_TEMPLATE, lsid, formattedDate, formattedDate, java.lang.Integer.MAX_VALUE.toString)
    }

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

  def getOutlierRecordDistances(lsid: String, recordsMap: scala.collection.mutable.Map[String, Map[String, Object]]): scala.collection.mutable.Map[String, Double] = {
    val mapper = new ObjectMapper();

    val recordsMapWithoutRowKeys = new java.util.HashMap[String, java.util.Map[String, Object]]()
    for ((k, v) <- recordsMap) {
      recordsMapWithoutRowKeys.put(k, (v - "rowKey"))
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

  def markOutlierOccurrences(outlierDistances: scala.collection.mutable.Map[String, Double], recordsMap: scala.collection.mutable.Map[String, Map[String, Object]]) {
    for ((uuid, distance) <- outlierDistances) {

      //Round distance from distribution to nearest metre. Any occurrences outside the distribution by less than a metre are not considered outliers.
      val roundedDistance = scala.math.round(distance)

      if (roundedDistance > 0) {
        var coordinateUncertaintyInMeters: Double = 0;
        if (recordsMap(uuid)("coordinateUncertaintyInMeters") != null) {
          coordinateUncertaintyInMeters = recordsMap(uuid)("coordinateUncertaintyInMeters").asInstanceOf[java.lang.Double]
        }

        // The occurrence is only considered an outlier if its distance from the distribution is greater than its coordinate uncertainty
        if ((roundedDistance - coordinateUncertaintyInMeters) > 0) {
          val rowKey = recordsMap(uuid)("rowKey").asInstanceOf[String]
          // Add data quality assertion
          Config.occurrenceDAO.addSystemAssertion(rowKey, QualityAssertion(AssertionCodes.SPECIES_OUTSIDE_EXPERT_RANGE, distance + " metres outside of expert distribution range"))

          // Record distance against record
          Config.persistenceManager.put(rowKey, "occ", Map("distanceOutsideExpertRange.p" -> roundedDistance.toString()))

          // Print rowKey to stdout so that output of this application can be used for reindexing.
          println(rowKey)
        }
      }
    }
  }

}


