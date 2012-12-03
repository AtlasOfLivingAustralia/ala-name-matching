package au.org.ala.util

import collection.mutable.ListBuffer
import java.text.MessageFormat
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import au.org.ala.biocache.Config
import org.apache.commons.lang.StringEscapeUtils
import org.apache.commons.codec.net.URLCodec
import org.geotools.geometry.jts.JTSFactoryFinder
import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory}

/**
 * Created with IntelliJ IDEA.
 * User: ChrisF
 * Date: 29/11/12
 * Time: 4:13 PM
 * To change this template use File | Settings | File Templates.
 */
object EndemismLayerHelper {
  val FACET_DOWNLOAD_URL_TEMPLATE = Config.biocacheServiceUrl + "/occurrences/facets/download?q={0}&facets={1}"
  val FACET_DOWNLOAD_WITH_COUNT_URL_TEMPLATE = Config.biocacheServiceUrl + "/occurrences/facets/download?q={0}&facets={1}&count=true"
  val SPECIES_BOUNDING_BOX_URL_TEMPLATE = Config.biocacheServiceUrl + "/mapping/bbox?q=taxon_concept_lsid:{0}"

  def main(args: Array[String]) {
    val helper = new EndemismLayerHelper();
    helper.doThing()
  }
}

class EndemismLayerHelper {

  val geometryFactory = new GeometryFactory()


  def doThing() {
    // get list of species
    val speciesLsidsCSV = doFacetDownload("taxon_concept_lsid:[* TO *]", "taxon_concept_lsid", false)
    val speciesLsids = new ListBuffer[String]

    speciesLsidsCSV.split("\n").foreach(s => speciesLsids += s)
    // remove first line as this will contain the text "taxon_concept_id"
    speciesLsids.remove(0)

    for (lsid <- speciesLsids) {
      println(lsid)
      val occurrencePointCSV = doFacetDownload("taxon_concept_lsid:" + lsid, "point-0.1", false)
      val occurrencePoints = new ListBuffer[String]
      occurrencePointCSV.split("\n").foreach(p => occurrencePoints += p)
      // remove first line as this will contain the text "taxon_concept_id"
      occurrencePoints.remove(0)

      var pointsSet = Set[String]()

      for (point <- occurrencePoints) {
        val splitPoint = point.split(",")
        val strLatitude = splitPoint(0)
        val strLongitude = splitPoint(1)

        val roundedLatitude = math.ceil(java.lang.Double.parseDouble(strLatitude))
        val roundedLongitude = math.floor(java.lang.Double.parseDouble(strLongitude))

        pointsSet += roundedLatitude + "," + roundedLongitude
      }
      println(pointsSet.size)

      println(getSpeciesArea(lsid))
    }

    // for each species,
    // get no of grid cells occurring in
    // get area of bounding box of records - rounded out to complete 1 degree cells.
    // record data
  }

  def doFacetDownload(query: String, facet: String, includeCount: Boolean): String = {
    val urlCodec = new URLCodec()

    val url = {
      if (includeCount) {
        MessageFormat.format(EndemismLayerHelper.FACET_DOWNLOAD_WITH_COUNT_URL_TEMPLATE, urlCodec.encode(query), urlCodec.encode(facet))
      } else {
        MessageFormat.format(EndemismLayerHelper.FACET_DOWNLOAD_URL_TEMPLATE, urlCodec.encode(query), urlCodec.encode(facet))
      }
    }

    val httpClient = new HttpClient()
    val get = new GetMethod(url)
    try {
      val responseCode = httpClient.executeMethod(get)
      if (responseCode == 200) {
        val csv = get.getResponseBodyAsString

        csv
      } else {
        throw new Exception("facet download request failed (" + responseCode + ")")
      }
    } finally {
      get.releaseConnection()
    }
  }

  def getSpeciesArea(lsid: String) : Double = {
    val bboxString = getSpeciesBbox(lsid)
    val bboxCoords = bboxString.split(",")
    val strMinLong = bboxCoords(0)
    val strMinLat = bboxCoords(1)
    val strMaxLong = bboxCoords(2)
    val strMaxLat = bboxCoords(3)

    // Round the bounding box corners to the nearest degree as we are interested in the area of the 1 degree squares (graticules)
    // in which the occurrences fall
    val roundedMinLong = math.floor(java.lang.Double.parseDouble(strMinLong))
    val roundedMinLat = math.floor(java.lang.Double.parseDouble(strMinLat))
    val roundedMaxLong = math.ceil(java.lang.Double.parseDouble(strMaxLong))
    val roundedMaxLat = math.ceil(java.lang.Double.parseDouble(strMaxLat))

    val coord1 = new Coordinate(roundedMinLong, roundedMaxLat)
    val coord2 = new Coordinate(roundedMaxLong, roundedMaxLat)
    val coord3 = new Coordinate(roundedMaxLong, roundedMinLat)
    val coord4 = new Coordinate(roundedMinLong, roundedMinLat)
    val coord5 = new Coordinate(roundedMinLong, roundedMaxLat)

    println("POLYGON((" + roundedMinLong + " " + roundedMaxLat + ", " + roundedMaxLong + " " + roundedMaxLat + ", " + roundedMaxLong + " " + roundedMinLat + ", " + roundedMinLong + " " + roundedMinLat + ", " + roundedMinLong + " " + roundedMaxLat + "))")

    val coordArray = Array(coord1, coord2, coord3, coord4, coord5)

    val polygonBoundary = geometryFactory.createLinearRing(coordArray)
    val polygon = geometryFactory.createPolygon(polygonBoundary, null)

    polygon.getArea()
  }

  def getSpeciesBbox(lsid: String) : String = {
    val url = MessageFormat.format(EndemismLayerHelper.SPECIES_BOUNDING_BOX_URL_TEMPLATE, lsid)

    val httpClient = new HttpClient()
    val get = new GetMethod(url)
    try {
      val responseCode = httpClient.executeMethod(get)
      if (responseCode == 200) {
        val bbox = get.getResponseBodyAsString

        bbox
      } else {
        throw new Exception("facet download request failed (" + responseCode + ")")
      }
    } finally {
      get.releaseConnection()
    }
  }

}