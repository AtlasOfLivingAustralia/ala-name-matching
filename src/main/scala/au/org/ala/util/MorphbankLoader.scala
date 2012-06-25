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

object MorphbankLoader extends DataLoader {

    //val idsRequest = "http://morphbank-svc.ala.org.au/mb3/request?method=search&objecttype=Image&objecttype=Specimen&keywords=Australia&limit=-1&firstResult=0&user=&group=&change=&lastDateChanged=&numChangeDays=&id=&taxonName=&format=id"
    //val objectRequestTemplate = "http://morphbank-svc.ala.org.au/mb3/request?method=id&id={0}&format=svc"
    //val imageRequestTemplate = "http://morphbank-images.ala.org.au/?id={0}&imgType=jpeg"

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

    def load(dataResourceUid: String) {
        val (protocol, urls, uniqueTerms, params, customParams) = retrieveConnectionParameters(dataResourceUid)
        val idsUrl = params("url")
        val objectUrlTemplate = customParams("objectRequestUrlTemplate")
        val imageUrlTemplate = customParams("imageRequestUrlTemplate")
        val specimenPageUrlTemplate = customParams("specimenDetailsPageUrlTemplate")

        val idsXml = getXMLFromWebService(idsUrl)

        val idNodes = (idsXml \\ MorphbankLoader.ID_KEY)
        val ids = for (idNode <- idNodes) yield idNode.text

        var loadedSpecimens = 0
        var loadedImages = 0

        for (id <- ids) {
            val recordXml = getXMLFromWebService(MessageFormat.format(objectUrlTemplate, id))
            val obj = (recordXml \\ MorphbankLoader.OBJECT_KEY).first

            val typ = obj.attribute(MorphbankLoader.TYPE_KEY).first.text

            if (typ == MorphbankLoader.SPECIMEN_TYPE) {
                processSpecimen(obj, dataResourceUid, uniqueTerms, specimenPageUrlTemplate)
                loadedSpecimens += 1
            } else if (typ == MorphbankLoader.IMAGE_TYPE) {
                processImage(obj, imageUrlTemplate, dataResourceUid, uniqueTerms)
                loadedImages += 1
            } else {
                throw new IllegalArgumentException("Unrecognised object type: " + typ)
            }
        }

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

    def processImage(image: Node, imageUrlTemplate: String, dataResourceUid: String, uniqueTerms: List[String]) {
        val imageId = (image \\ MorphbankLoader.OBJECT_ID_KEY).first.text.trim()
        val specimenId = (image \\ MorphbankLoader.SPECIMEN_ID_KEY).first.text.trim()

        val imageUrl = MessageFormat.format(imageUrlTemplate, imageId)
        val rowKey = createUniqueID(dataResourceUid, uniqueTerms)
        var associatedMediaValue = pm.get(rowKey, MorphbankLoader.OCC_NAMESPACE, MorphbankLoader.ASSOCIATED_MEDIA_KEY).getOrElse("")

        if (StringUtils.isEmpty(associatedMediaValue)) {
            associatedMediaValue = imageUrl
        } else {
            associatedMediaValue = associatedMediaValue + "; " + imageUrl
        }

        val mappedValues = Map(MorphbankLoader.CATALOG_NUMBER_KEY -> specimenId, MorphbankLoader.ASSOCIATED_MEDIA_KEY -> associatedMediaValue)

        val uniqueTermsValues = uniqueTerms.map(t => mappedValues.getOrElse(t, ""))
        val fr = FullRecordMapper.createFullRecord("", mappedValues, Versions.RAW)
        load(dataResourceUid, fr, uniqueTermsValues)
        
        println("Loaded image " + imageId + " for specimen " + specimenId)
    }

}