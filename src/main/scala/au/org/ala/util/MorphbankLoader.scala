package au.org.ala.util

import au.org.ala.biocache.{DataLoader, FullRecordMapper, Versions}
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import scala.xml.Elem
import scala.xml.XML
import java.text.MessageFormat
import scala.xml.Node
import org.apache.commons.lang3.StringUtils
import collection.mutable
import org.apache.commons.httpclient.params.HttpConnectionManagerParams
import org.apache.commons.codec.digest.DigestUtils

object MorphbankLoader extends DataLoader {

  val ID_KEY = "id"
  val OBJECT_KEY = "object"
  val TYPE_KEY = "type"

  val SPECIMEN_TYPE = "Specimen"
  val IMAGE_TYPE = "Image"

  val DWC_NAMESPACE_PREFIX = "dwc"
  val DWCG_NAMESPACE_PREFIX = "dwcg"

  val SOURCE_ID_KEY = "sourceId"
  val MORPHBANK_ID_KEY = "morphbank"
  val SPECIMEN_ID_KEY = "specimen"
  val DATE_LAST_MODIFIED_KEY = "dateLastModified"
  val CREATIVE_COMMONS_KEY = "creativeCommons"
  val PHOTOGRAPHER_KEY = "photographer"
  val COPYRIGHT_TEXT_KEY = "copyrightText"
  val COLLECTOR_OLDDWC_KEY = "Collector"
  val EARLIEST_DATE_COLLECTED_OLDDWC_KEY = "EarliestDateCollected"

  val ASSOCIATED_MEDIA_DWC_KEY = "associatedMedia"
  val CATALOG_NUMBER_DWC_KEY = "catalogNumber"
  val OCCURRENCE_DETAILS_DWC_KEY = "occurrenceDetails"
  val MODIFIED_DWC_KEY = "modified"
  val RECORDED_BY_DWC_KEY = "recordedBy"
  val EVENT_DATE_DWC_KEY = "eventDate"
  val OTHER_CATALOG_NUMBERS_DWC_KEY = "otherCatalogNumbers"
  val RIGHTS_DWC_KEY = "rights"
  val RIGHTS_HOLDER_DWC_KEY = "rightsholder"
  val PHOTOGRAPHER_FULLRECORD_KEY = "photographer"

  val OCC_NAMESPACE = "occ"

  // A large number of images have HTML in the licence information field, which includes a link to the following remote image. Replace such values with a textual version of the appropriate licence.
  val CREATIVE_COMMONS_IMG = "http://i.creativecommons.org/l/by-nc/3.0/88x31.png"
  val REPLACEMENT_FOR_HTML_LICENCE_TEXT = "Creative Commons Attribution-NonCommercial 3.0 Unported (CC BY-NC 3.0)"
  val PLACEHOLDER_IMAGE_MD5 = "b5e90bf8f521706a3b2a88f2602990b8"

  /* val fieldMapping = Map(
("sourceId" -> "catalogNumber"),
("dateLastModified" -> "modified"),
("Collector" -> "recordedBy"),
("EarliestDateCollected" -> "eventDate"),
("CatalogNumber" -> "otherCatalogNumbers"))*/

  def main(args: Array[String]) {
    var dataResourceUid: String = null

    val parser = new OptionParser("Import morphbank data") {
      arg("<data-resource-uid>", "the data resource to import", {
        v: String => dataResourceUid = v
      })
    }

    if (parser.parse(args)) {
      val loader = new MorphbankLoader
      loader.load(dataResourceUid)
    }
  }
}

class MorphbankLoader extends CustomWebserviceLoader {

  val specimenImagesMap = new scala.collection.mutable.HashMap[String, mutable.ListBuffer[String]]()
  val specimenLicenseMap = new scala.collection.mutable.HashMap[String, String]()
  val specimenPhotographerMap = new scala.collection.mutable.HashMap[String, String]()
  val specimenCopyrightTextMap = new scala.collection.mutable.HashMap[String, String]()

  val httpClient = new HttpClient()

  def load(dataResourceUid: String) {
    val (protocol, urls, uniqueTerms, params, customParams, lastChecked) = retrieveConnectionParameters(dataResourceUid)
    var idsUrlTemplate = params("url")
    val objectUrlTemplate = customParams("objectRequestUrlTemplate")
    val imageUrlTemplate = customParams("imageRequestUrlTemplate")
    val specimenPageUrlTemplate = customParams("specimenDetailsPageUrlTemplate")
    val allRecordSetKeywordsAsString = customParams("recordSetKeywords")

    val recordSetKeywordsList = allRecordSetKeywordsAsString.split(";")

    for (keywords <- recordSetKeywordsList) {
      println("Processing records with keywords " + keywords)

      val idsUrl = MessageFormat.format(idsUrlTemplate, keywords)
      val idsXml = getXMLFromWebService(idsUrl)

      val idNodes = (idsXml \\ MorphbankLoader.ID_KEY)
      val ids = for (idNode <- idNodes) yield idNode.text

      var loadedSpecimens = 0
      var loadedImages = 0

      for (id <- ids) {
        val url = MessageFormat.format(objectUrlTemplate, id)
        val recordXml = getXMLFromWebService(url)
        val obj = (recordXml \\ MorphbankLoader.OBJECT_KEY).head

        val typ = obj.attribute(MorphbankLoader.TYPE_KEY).head.text

        if (typ == MorphbankLoader.SPECIMEN_TYPE) {
          processSpecimen(obj, dataResourceUid, uniqueTerms, specimenPageUrlTemplate)
          loadedSpecimens += 1
        } else if (typ == MorphbankLoader.IMAGE_TYPE) {
          processImage(obj)
          loadedImages += 1
        } else {
          throw new IllegalArgumentException("Unrecognised object type: " + typ)
        }
      }

      setSpecimenImagesLicenceAndPhotographer(imageUrlTemplate, dataResourceUid)

      println("Finished processing records with keywords " + keywords + ". Loaded " + loadedSpecimens + " specimens, and " + loadedImages + " images.")
    }

  }

  def getXMLFromWebService(requestUrl: String): Elem = {
    var xmlContent: String = null

    val get = new GetMethod(requestUrl)
    try {
      val responseCode = httpClient.executeMethod(get)
      if (responseCode == 200) {
        xmlContent = get.getResponseBodyAsString();
      } else {
        throw new Exception("Request failed (" + responseCode + ")")
      }
    } finally {
      get.releaseConnection()
      httpClient.getHttpConnectionManager.closeIdleConnections(0)
    }

    XML.loadString(xmlContent)
  }

  def processSpecimen(specimen: Node, dataResourceUid: String, uniqueTerms: List[String], specimenPageUrlTemplate: String) {

    var mappedValues = Map[String, String]()
    specimen.child.foreach(node => (mappedValues = addValue(node, mappedValues)))

    val specimenPageUrl = MessageFormat.format(specimenPageUrlTemplate, mappedValues(MorphbankLoader.OTHER_CATALOG_NUMBERS_DWC_KEY))
    mappedValues = mappedValues + (MorphbankLoader.OCCURRENCE_DETAILS_DWC_KEY -> specimenPageUrl)
    val uniqueTermsValues = uniqueTerms.map(t => mappedValues.getOrElse(t, ""))

    // If we are reprocessing the specimen, remove any previously loaded image urls from the associated media field -
    // this field will be completely repopulated.
    pm.put(createUniqueID(dataResourceUid, uniqueTermsValues), MorphbankLoader.OCC_NAMESPACE, MorphbankLoader.ASSOCIATED_MEDIA_DWC_KEY, "")

    // clear out CatalogNumber field if it has not been populated.
    if (!mappedValues.contains(MorphbankLoader.CATALOG_NUMBER_DWC_KEY)) {
      pm.put(createUniqueID(dataResourceUid, uniqueTermsValues), MorphbankLoader.OCC_NAMESPACE, MorphbankLoader.CATALOG_NUMBER_DWC_KEY, "")
    }

    val fr = FullRecordMapper.createFullRecord("", mappedValues, Versions.RAW)
    load(dataResourceUid, fr, uniqueTermsValues)

    println("Loaded specimen " + mappedValues(MorphbankLoader.OTHER_CATALOG_NUMBERS_DWC_KEY))
  }

  //record content of an xml node if it is of interest
  def addValue(node: Node, map: Map[String, String]): Map[String, String] = {
    if (node.label.toLowerCase == MorphbankLoader.SOURCE_ID_KEY.toLowerCase) {
      // use the morphbank ID to "otherCatalogNumbers" in the biocache for the specimen as the catalog number
      val morphbankIdNode = node \\ MorphbankLoader.MORPHBANK_ID_KEY
      map + (MorphbankLoader.OTHER_CATALOG_NUMBERS_DWC_KEY -> morphbankIdNode.text.trim())
    } else if (node.label.toLowerCase == MorphbankLoader.DATE_LAST_MODIFIED_KEY.toLowerCase) {
      // map date last modified to corresponding darwin core field
      map + (MorphbankLoader.MODIFIED_DWC_KEY -> node.text.trim())
    } else if (node.label.toLowerCase == MorphbankLoader.CATALOG_NUMBER_DWC_KEY.toLowerCase()) {
      // If it is supplied, use the darwin core catalog number as the catalog number in the biocache
      map + (MorphbankLoader.CATALOG_NUMBER_DWC_KEY -> node.text.trim())
    } else if (node.label.toLowerCase == MorphbankLoader.COLLECTOR_OLDDWC_KEY.toLowerCase) {
      // Map the old darwin core "collector" field to the new "recorded by" field
      map + (MorphbankLoader.RECORDED_BY_DWC_KEY -> node.text.trim())
    } else if (node.label.toLowerCase == MorphbankLoader.EARLIEST_DATE_COLLECTED_OLDDWC_KEY.toLowerCase) {
      // Map the old darwin core "earliest date collected" field to the new "event date" field.
      map + (MorphbankLoader.EVENT_DATE_DWC_KEY -> node.text.trim())
    } else if (node.prefix == MorphbankLoader.DWC_NAMESPACE_PREFIX || node.prefix == MorphbankLoader.DWCG_NAMESPACE_PREFIX) {
      // If the node has a darwin core namespace prefix and is not any of the special cases handled above, write as is into the
      // biocache
      map + (StringUtils.uncapitalize(node.label) -> node.text.trim())
    } else {
      // node does not interest us, return the map unchanged.
      map
    }
  }

  def processImage(image: Node) {
    val imageId = (image \\ MorphbankLoader.SOURCE_ID_KEY \\ MorphbankLoader.MORPHBANK_ID_KEY).head.text.trim()
    val specimenIdNodeSeq = (image \\ MorphbankLoader.SPECIMEN_ID_KEY \\ MorphbankLoader.MORPHBANK_ID_KEY)

    if (specimenIdNodeSeq.length != 0) {
      // Record ids for all images associated with the specimen. These will later be added to the specimen
      // record in Cassandra all in one hit
      val specimenId = specimenIdNodeSeq.head.text.trim()

      if (specimenImagesMap.contains(specimenId)) {
        specimenImagesMap(specimenId).append(imageId)
      } else {
        val listBuf = new mutable.ListBuffer[String]()
        listBuf.append(imageId)
        specimenImagesMap += (specimenId -> listBuf)
      }

      // Record the creative commons licence text, copyright text and photographer name from the image against the specimen,
      // if no such information has been recorded yet for the specimen. This information will be added to the specimen
      // record in Cassandra later, at the same time that the image information is added.
      var creativeCommonsLicenceText: String = null
      var photographerText: String = null
      var copyrightText: String = null

      if (!(image \\ MorphbankLoader.CREATIVE_COMMONS_KEY).isEmpty) {
        creativeCommonsLicenceText = (image \\ MorphbankLoader.CREATIVE_COMMONS_KEY).head.text.trim()
        if (creativeCommonsLicenceText.contains(MorphbankLoader.CREATIVE_COMMONS_IMG)) {
          creativeCommonsLicenceText = MorphbankLoader.REPLACEMENT_FOR_HTML_LICENCE_TEXT
        }
      }

      if (!(image \\ MorphbankLoader.PHOTOGRAPHER_KEY).isEmpty) {
        photographerText = (image \\ MorphbankLoader.PHOTOGRAPHER_KEY).head.text.trim()
      }

      if (!(image \\ MorphbankLoader.COPYRIGHT_TEXT_KEY).isEmpty) {
        copyrightText = (image \\ MorphbankLoader.COPYRIGHT_TEXT_KEY).head.text.trim()
      }

      if (creativeCommonsLicenceText != null) {
        specimenLicenseMap += (specimenId -> creativeCommonsLicenceText)
      }

      if (photographerText != null) {
        specimenPhotographerMap += (specimenId -> photographerText)
      }

      if (copyrightText != null) {
        specimenCopyrightTextMap += (specimenId -> copyrightText)
      }

      println("Processed image " + imageId + " for specimen " + specimenId)
    } else {
      println("ERROR: No associated specimen for image " + imageId)
    }

  }

  def setSpecimenImagesLicenceAndPhotographer(imageUrlTemplate: String, dataResourceUid: String) {
    for (specimenId <- specimenImagesMap.keySet) {
      val specimenImages = specimenImagesMap(specimenId)
      val specimenImagesLicence = specimenLicenseMap.getOrElse(specimenId, null)
      val specimenImagesPhotographer = specimenPhotographerMap.getOrElse(specimenId, null)
      val specimenImagesCopyrightText = specimenCopyrightTextMap.getOrElse(specimenId, null)

      var specimenImageUrls = specimenImages.map(t => MessageFormat.format(imageUrlTemplate, t))

      // Test each image url. If it returns mimetype image/png, we know that this is a placeholder image. In this case, the image should be ignored.
      val placeholderImageUrls = new mutable.ListBuffer[String]()
      for (specimenImageUrl <- specimenImageUrls) {
        if (isPlaceholderImage(specimenImageUrl)) {
          placeholderImageUrls.append(specimenImageUrl)
          println("Ignoring placeholder image " + specimenImageUrl)
        }
      }

      for (placeholderImageUrl <- placeholderImageUrls) {
        specimenImageUrls = specimenImageUrls - placeholderImageUrl
      }

      //Remove any duplicates from the URL list
      var specimenImageUrlsSet = Set[String]()
      for (imageUrl <- specimenImageUrls) {
        specimenImageUrlsSet = specimenImageUrlsSet + imageUrl
      }
      val specimenImageUrlsNoDuplicates = specimenImageUrlsSet.toSeq.sortWith((s1, s2) => s1 < s2)

      var mappedValues = Map(MorphbankLoader.OTHER_CATALOG_NUMBERS_DWC_KEY -> specimenId, MorphbankLoader.ASSOCIATED_MEDIA_DWC_KEY -> specimenImageUrlsNoDuplicates.mkString(";"))

      if (specimenImagesLicence != null) {
        mappedValues += (MorphbankLoader.RIGHTS_DWC_KEY -> specimenImagesLicence)
      }

      if (specimenImagesPhotographer != null) {
        mappedValues += (MorphbankLoader.PHOTOGRAPHER_FULLRECORD_KEY -> specimenImagesPhotographer)
      }

      if (specimenImagesCopyrightText != null) {
        mappedValues += (MorphbankLoader.RIGHTS_HOLDER_DWC_KEY -> specimenImagesCopyrightText)
      }

      val uniqueTermsValues = List(specimenId)

      val fr = FullRecordMapper.createFullRecord("", mappedValues, Versions.RAW)
      load(dataResourceUid, fr, uniqueTermsValues)

      println("Loaded images for specimen " + specimenId)
    }
  }

  def isPlaceholderImage(imageUrl: String): Boolean = {

    val get = new GetMethod(imageUrl)
    httpClient.executeMethod(get)
    val responseBytes = get.getResponseBody()
    val md5Hex = DigestUtils.md5Hex(responseBytes)
    get.releaseConnection()
    httpClient.getHttpConnectionManager.closeIdleConnections(0)

    md5Hex == MorphbankLoader.PLACEHOLDER_IMAGE_MD5
  }

}