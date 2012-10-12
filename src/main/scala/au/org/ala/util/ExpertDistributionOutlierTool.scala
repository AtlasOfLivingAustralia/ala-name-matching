package au.org.ala.util

import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.{PostMethod, GetMethod}
import org.codehaus.jackson.map.ObjectMapper
import java.util
import collection.mutable.ArrayBuffer
import java.text.MessageFormat
import collection.JavaConversions._
import au.org.ala.biocache.{AssertionCodes, QualityAssertion, Config}

/**
 * Created with IntelliJ IDEA.
 * User: ChrisF
 * Date: 5/10/12
 * Time: 2:07 PM
 * To change this template use File | Settings | File Templates.
 */

object ExpertDistributionOutlierTool {
  val DISTRIBUTION_DETAILS_URL = "http://spatial.ala.org.au/layers-service/distributions"
  val RECORDS_URL_TEMPLATE = "http://biocache.ala.org.au/ws/occurrences/search?q=taxon_concept_lsid:{0}&fl=id,row_key,latitude,longitude&facet=off&pageSize={1}"
  val DISTANCE_URL_TEMPLATE = "http://localhost:8080/layers-service/distribution/outliers/{0}"

  def main(args: Array[String]) {
    val tool = new ExpertDistributionOutlierTool();
    tool.findOutliers()
  }
}

class ExpertDistributionOutlierTool {

  def findOutliers() {
    val distributionLsids = getExpertDistributionLsids();
    for (lsid <- distributionLsids) {
      println("LSID: " + lsid)
      val recordsMap = getRecordsForLsid(lsid)
      val outlierRecordDistances = getOutlierRecordDistances(lsid, recordsMap)
      print(outlierRecordDistances)
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
        throw new Exception("Request failed (" + responseCode + ")")
      }
    } finally {
      get.releaseConnection()
    }
  }

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

          retMap(uuid) = Map("rowKey" -> rowKey, "decimalLatitude" -> decimalLatitude, "decimalLongitude" -> decimalLongitude)
        }
        retMap
      } else {
        throw new Exception("Request failed (" + responseCode + ")")
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
    println(url)
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
        throw new Exception("Request failed (" + responseCode + ")")
      }
    } finally {
      post.releaseConnection()
    }
  }

  def markOutlierOccurrences(outlierDistances: scala.collection.mutable.Map[String, Double], recordsMap: scala.collection.mutable.Map[String, Map[String, Object]]) {
    for ((uuid, distance) <- outlierDistances) {
      val rowKey = recordsMap(uuid)("rowKey").asInstanceOf[String]
      // Add data quality assertion
      Config.occurrenceDAO.addSystemAssertion(rowKey, QualityAssertion(AssertionCodes.SPECIES_OUTSIDE_EXPERT_RANGE, distance + " metres outside of expert distribution range"))

      // Record distance against record
      Config.persistenceManager.put(rowKey, "occ",Map("distanceOutsideExpertRange.p"->distance.toString()))
    }
  }
}


