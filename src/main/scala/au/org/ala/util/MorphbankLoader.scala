package au.org.ala.util
import au.org.ala.biocache.DataLoader
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import scala.xml.Elem
import scala.xml.XML
import java.text.MessageFormat
import scala.xml.Node

object MorphbankLoader extends DataLoader {

    val idsRequest = "http://morphbank-svc.ala.org.au/mb3/request?method=search&objecttype=Image&objecttype=Specimen&keywords=Australia&limit=-1&firstResult=0&user=&group=&change=&lastDateChanged=&numChangeDays=&id=&taxonName=&format=id"
    val objectRequestTemplate = "http://morphbank-svc.ala.org.au/mb3/request?method=id&id={0}&format=svc"

    val ID_KEY = "id"
    val OBJECT_KEY = "object"
    val TYPE_KEY = "type"    
        
    val SPECIMEN_TYPE = "Specimen"
    val IMAGE_TYPE = "Image"    

    def main(args: Array[String]) {
        val loader = new MorphbankLoader
        loader.load(idsRequest)
    }
}

class MorphbankLoader extends DataLoader {

    def load(idsRequest: String) {
        val idsXml = getXMLFromWebService(idsRequest)

        val idNodes = (idsXml \\ MorphbankLoader.ID_KEY)
        val ids = for (idNode <- idNodes) yield idNode.text

        for (id <- ids) {
            val recordXml = getXMLFromWebService(MessageFormat.format(MorphbankLoader.objectRequestTemplate, id))
            processRecord(recordXml)
        }
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

    def processRecord(record: Elem) {
        val obj = (record \\ MorphbankLoader.OBJECT_KEY).first

        val typ = obj.attribute(MorphbankLoader.TYPE_KEY).first.text
        println(typ)
    }
    
    def processSpecimen(specimen: Node) {
        
    }
    
    def processImage(image: Node) {
        
    }
}