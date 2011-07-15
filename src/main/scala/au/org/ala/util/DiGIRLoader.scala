package au.org.ala.util
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.PostMethod
import java.net.URLEncoder
import org.apache.commons.httpclient.methods.GetMethod
import scala.xml.XML
/**
 * A work in progress...
 */
object DiGIRLoader {

    def main(args: Array[String]) {

        val digirEndpoint = "http://iobis.marine.rutgers.edu/digir2/DiGIR.php"
            
        //retrieve the metadata
        val metadataRequest = createMetadataRequest("1.0", digirEndpoint)
        val http = new HttpClient
        val encodedRequest = URLEncoder.encode(metadataRequest.toString)

        val get = new GetMethod(digirEndpoint + "?request=" + encodedRequest)
        val status = http.executeMethod(get)
        val response = get.getResponseBodyAsString
       // println("response:" + response)
        
        val xml = XML.loadString(response)
        val resources = xml \\ "resource"
        resources.foreach(resource => {
        	val name = (resource \ "name").text
        	val code = (resource \ "code").text
        	val website = (resource \ "relatedInformation").text
        	val citation = (resource \ "citation").text
        	val rights = (resource \ "useRestrictions").text
        	
        	println(code +" : " + name)
        })
        
            
            
        //retrieve a list of resource names
            
        //check to see if all the names of resources are currently in the collectory
            
            
//        val request = createSearchRequest("1.0", "search", digirEndpoint, "trawl_fibex", "A%", null)
//        println(request.toString)
//
//        val http = new HttpClient
//        val encodedRequest = URLEncoder.encode(request.toString)
//
//        val get = new GetMethod(digirEndpoint + "?request=" + encodedRequest)
//        val status = http.executeMethod(get)
//        println("response:" + get.getResponseBodyAsString)

        println("done")
    }

    def createMetadataRequest(version: String, destination: String) = {
        <request xmlns='http://digir.net/schema/protocol/2003/1.0' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:digir='http://digir.net/schema/protocol/2003/1.0'>
          <header>
             <version>{ version }</version>
             <sendTime>20030421T170441.431Z</sendTime>
             <source>127.0.0.1</source>
             <destination>{ destination }</destination>
             <type>metadata</type>
          </header>
        </request>
    }

    def createInventoryRequest(version: String, destination: String, resource: String) = {
        <request xmlns='http://digir.net/schema/protocol/2003/1.0' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:digir='http://digir.net/schema/protocol/2003/1.0'>
          <header>
            <version>{ version }</version>
            <sendTime>2011-07-03T19:14:58-05:00</sendTime>
            <source>127.0.0.1</source>
            <destination resource={ resource }>{ destination }</destination>
            <type>inventory</type>
          </header>
          <inventory xmlns:dwc='http://digir.net/schema/conceptual/darwin/2003/1.0'>
            <dwc:ScientificName/>
            <count>false</count>
          </inventory>
        </request>
    }

    def createSearchRequest(version: String, requestType: String, destination: String, resource: String, lower: String, upper: String, limit: Int = 10, startAt: Int = 0, count: Int = 10) = {

        def greaterThan(lower: String) = {
          <greaterThanOrEquals>
            <dwc:ScientificName>{ lower }</dwc:ScientificName>
          </greaterThanOrEquals>
        }

        def lessThan(upper: String) = {
          <lessThanOrEquals>
            <dwc:ScientificName>{ upper }</dwc:ScientificName>
          </lessThanOrEquals>
        }

        <request xmlns='http://digir.net/schema/protocol/2003/1.0' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:digir='http://digir.net/schema/protocol/2003/1.0' xmlns:darwin='http://digir.net/schema/conceptual/darwin/2003/1.0' xmlns:dwc='http://digir.net/schema/conceptual/darwin/2003/1.0' xsi:schemaLocation='http://digir.net/schema/protocol/2003/1.0 http://digir.sourceforge.net/schema/protocol/2003/1.0/digir.xsd http://digir.net/schema/conceptual/darwin/2003/1.0 http://digir.sourceforge.net/schema/conceptual/darwin/2003/1.0/darwin2.xsd'>
          <header>
            <version>{ version }</version>
            <sendTime>2011-07-03T19:14:58-05:00</sendTime>
            <source>127.0.0.1</source>
            <destination resource={ resource }>{ destination }</destination>
            <type>{ requestType }</type>
          </header>
          <search>
              <filter>
               {
                 if (lower != null && upper != null) {
                   <and>
                     { greaterThan(lower) }
                     { lessThan(upper) }
                   </and>
                 } else if (lower != null) {
                    { greaterThan(lower) }
                 } else if (upper != null) {
                    { lessThan(upper) }
                 }
                }
              </filter>
              <records limit={ limit.toString } startAt={ startAt.toString }>
                 <structure schemaLocation="http://digir.sourceforge.net/schema/conceptual/darwin/full/2003/1.0/darwin2full.xsd"/>
              </records>
              <count>true</count>
          </search>
        </request>
    }
}