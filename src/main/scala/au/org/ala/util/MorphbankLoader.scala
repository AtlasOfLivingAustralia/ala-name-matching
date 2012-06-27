package au.org.ala.util
import au.org.ala.biocache.DataLoader
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import scala.xml.Elem
import scala.xml.XML
import java.text.MessageFormat
import scala.xml.Node
import au.org.ala.biocache.FullRecordMapper
import au.org.ala.biocache.Versions
import org.apache.commons.lang3.StringUtils
import collection.mutable

object MorphbankLoader extends DataLoader {

  val ID_KEY = "id"
  val OBJECT_KEY = "object"
  val TYPE_KEY = "type"

  val SPECIMEN_TYPE = "Specimen"
  val IMAGE_TYPE = "Image"

  val DWC_NAMESPACE_PREFIX = "dwc"
  val DWCG_NAMESPACE_PREFIX = "dwcg"

  val OBJECT_ID_KEY = "sourceId"
  val SPECIMEN_ID_KEY = "specimen"
  val ASSOCIATED_MEDIA_KEY = "associatedMedia"
  val CATALOG_NUMBER_KEY = "catalogNumber"
  val OCCURRENCE_DETAILS_KEY = "occurrenceDetails"

  val OCC_NAMESPACE = "occ"

  val fieldMapping = Map(
    ("sourceId" -> "catalogNumber"),
    ("dateLastModified" -> "modified"),
    ("Collector" -> "recordedBy"),
    ("EarliestDateCollected" -> "eventDate"),
    ("CatalogNumber" -> "otherCatalogNumbers"))

  def main(args: Array[String]) {
    var dataResourceUid: String = null

    val parser = new OptionParser("Import Tasmanian Natural Values Atlas data") {
      arg("<data-resource-uid>", "the data resource to import", { v: String => dataResourceUid = v })
    }

    if (parser.parse(args)) {
      val loader = new MorphbankLoader
      loader.load(dataResourceUid)
    }
  }
}

class MorphbankLoader extends DataLoader {

  var specimenImagesMap = new scala.collection.mutable.HashMap[String, mutable.ListBuffer[String]]()

  def load(dataResourceUid: String) {
    val (protocol, urls, uniqueTerms, params, customParams) = retrieveConnectionParameters(dataResourceUid)
    var idsUrl = params("url")
    //idsUrl = idsUrl.replace("-1", "10")
    val objectUrlTemplate = customParams("objectRequestUrlTemplate")
    val imageUrlTemplate = customParams("imageRequestUrlTemplate")
    val specimenPageUrlTemplate = customParams("specimenDetailsPageUrlTemplate")

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

    println(specimenImagesMap)

    setSpecimenImages(imageUrlTemplate, dataResourceUid)

    println("Loaded " + loadedSpecimens + " specimens, and " + loadedImages + " images.")
  }

  def getXMLFromWebService(requestUrl: String): Elem = {
    var xmlContent: String = null

    val httpClient = new HttpClient()
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
    }

    XML.loadString(xmlContent)
  }

  def processSpecimen(specimen: Node, dataResourceUid: String, uniqueTerms: List[String], specimenPageUrlTemplate: String) {

    var mappedValues = Map[String, String]()
    specimen.child.foreach(node => (mappedValues = addValue(node, mappedValues)))

    val specimenPageUrl = MessageFormat.format(specimenPageUrlTemplate, mappedValues(MorphbankLoader.CATALOG_NUMBER_KEY))
    mappedValues = mappedValues + (MorphbankLoader.OCCURRENCE_DETAILS_KEY -> specimenPageUrl)
    val uniqueTermsValues = uniqueTerms.map(t => mappedValues.getOrElse(t, ""))

    // If we are reprocessing the specimen, remove any previously loaded image urls from the associated media field -
    // this field will be completely repopulated.
    pm.put(createUniqueID(dataResourceUid, uniqueTermsValues), MorphbankLoader.OCC_NAMESPACE, MorphbankLoader.ASSOCIATED_MEDIA_KEY, "")

    val fr = FullRecordMapper.createFullRecord("", mappedValues, Versions.RAW)
    load(dataResourceUid, fr, uniqueTermsValues)

    println("Loaded specimen " + mappedValues(MorphbankLoader.CATALOG_NUMBER_KEY))
  }

  def addValue(node: Node, map: Map[String, String]): Map[String, String] = {
    if (MorphbankLoader.fieldMapping.contains(node.label)) {
      val dwcKey = MorphbankLoader.fieldMapping(node.label)
      map + (dwcKey -> node.text.trim())
    } else if (node.prefix == MorphbankLoader.DWC_NAMESPACE_PREFIX || node.prefix == MorphbankLoader.DWCG_NAMESPACE_PREFIX) {
      map + (StringUtils.uncapitalize(node.label) -> node.text.trim())
    } else {
      map
    }
  }

  def processImage(image: Node) {
    val imageId = (image \\ MorphbankLoader.OBJECT_ID_KEY).head.text.trim()
    val specimenIdNodeSeq = (image \\ MorphbankLoader.SPECIMEN_ID_KEY)

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

      println("Processed image " + imageId + " for specimen " + specimenId)
    } else {
      println("ERROR: No associated specimen for image " + imageId)
    }

  }

  def setSpecimenImages(imageUrlTemplate: String, dataResourceUid: String) {
    for (specimenId <- specimenImagesMap.keySet) {
      val specimenImages = specimenImagesMap(specimenId)
      val specimenImageUrls = specimenImages.map(t => MessageFormat.format(imageUrlTemplate, t))
      println(specimenImageUrls)

      val mappedValues = Map(MorphbankLoader.CATALOG_NUMBER_KEY -> specimenId, MorphbankLoader.ASSOCIATED_MEDIA_KEY -> specimenImageUrls.mkString(";"))
      val uniqueTermsValues = List(specimenId)

      val fr = FullRecordMapper.createFullRecord("", mappedValues, Versions.RAW)
      load(dataResourceUid, fr, uniqueTermsValues)

      println("Loaded images for specimen " + specimenId)
    }
  }

  /*def processImage(image: Node, imageUrlTemplate: String, dataResourceUid: String, uniqueTerms: List[String]) {
    val imageId = (image \\ MorphbankLoader.OBJECT_ID_KEY).first.text.trim()
    val specimenIdNodeSeq = (image \\ MorphbankLoader.SPECIMEN_ID_KEY)

    if (specimenIdNodeSeq.length != 0) {
      val specimenId = specimenIdNodeSeq.first.text.trim()

      val uniqueTermsValues = List(specimenId)
      val rowKey = createUniqueID(dataResourceUid, uniqueTermsValues)

      val imageUrl = MessageFormat.format(imageUrlTemplate, imageId)

      var associatedMediaValue = pm.get(rowKey, MorphbankLoader.OCC_NAMESPACE, MorphbankLoader.ASSOCIATED_MEDIA_KEY).getOrElse("")

      if (StringUtils.isEmpty(associatedMediaValue)) {
        associatedMediaValue = imageUrl
      } else {
        associatedMediaValue = associatedMediaValue + ";" + imageUrl
      }

      val mappedValues = Map(MorphbankLoader.CATALOG_NUMBER_KEY -> specimenId, MorphbankLoader.ASSOCIATED_MEDIA_KEY -> associatedMediaValue)

      val fr = FullRecordMapper.createFullRecord("", mappedValues, Versions.RAW)
      load(dataResourceUid, fr, uniqueTermsValues)

      println("Loaded image " + imageId + " for specimen " + specimenId)
    } else {
      println("ERROR: No associated specimen for image " + imageId)
    }
  } */

}