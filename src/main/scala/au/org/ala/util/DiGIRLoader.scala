package au.org.ala.util
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.PostMethod
import java.net.URLEncoder
import org.apache.commons.httpclient.methods.GetMethod
import scala.xml.XML
import au.org.ala.biocache._

/**
 * A work in progress... not actually supporting paging ATM
 */
object DiGIRLoader {

  def main(args: Array[String]) {
    val l = new DiGIRLoader
    l.load("dr95")
  }
}

/**
 * A work in progress...
 */
class DiGIRLoader extends DataLoader {

  def load(dataResourceUid: String) {

    val (protocol, url, uniqueTerms, params, customParams) = retrieveConnectionParameters(dataResourceUid)
    val firstchars = (0 to 25).map(x => (x + 'A').toChar).toList
    val secondchars = (0 to 25).map(x => (x + 'a').toChar).toList

    val queryList = for {
      firstChar <- firstchars
      secondChar <- secondchars
      if (secondChar < 'z')
    } yield (firstChar.toString + secondChar.toString, firstChar.toString + (secondChar + 1).toChar.toString)

    val retries = 3

    queryList.foreach(range => {

      val (start, end) = range
      //retrieve all records for this name - requires paging
      var startAt = 0
      val pageSize = 10
      var endOfRecord = doSearchRequest(dataResourceUid, uniqueTerms, url, params("resource"), start, end, startAt, pageSize)

      while (!endOfRecord) {
        startAt += pageSize
        endOfRecord = {
          doSearchRequest(dataResourceUid, uniqueTerms, url, params("resource"), start, end, startAt, pageSize)
        }
      }
    })

    println("done")
  }

  def doSearchRequest(dataResourceUid: String, uniqueTerms: List[String], url: String, resource: String, start: String, end: String, startAt: Int, pageSize: Int): Boolean = {
    val request = createSearchRequest("1.0", "search", url, resource, start, end, startAt, pageSize)
    //println(request.toString)
    println(start + ", " + end)

    val http = new HttpClient
    val encodedRequest = URLEncoder.encode(request.toString)

    val response = {
      val get = new GetMethod(url + "?request=" + encodedRequest)

      var counter = 0
      var hasResponse = false
      var response = ""
      while (!hasResponse && counter < 3) {
        try {
          val status = http.executeMethod(get)
          hasResponse = true
          response = get.getResponseBodyAsString
        } catch {
          case _ => println("Error in request: retrying.....")
        }
      }
      response
    }

    if (response == "") return true

    println("response:" + response)

    try {
      val xml = XML.loadString(response)

      val records = xml \\ "record"

      records.foreach(r => {

        //map each element to new dwc terms
        val map = r.child.map(el => {
          if (!el.text.isEmpty && el.text != null && el.text.trim != "") {
            if (!DwC.matchTerm(el.label).isEmpty) {
              Some(DwC.matchTerm(el.label).get.canonical -> el.text)
            } else {
              println("cant match term: " + el.label)
              None
            }
          } else {
            None
          }
        }).filter(x => !x.isEmpty).map(y => y.get).toMap[String, String]

        //retrieve the unique terms
        val uniqueTermsValues = uniqueTerms.map(t => map.getOrElse(t, "")) //for (t <-uniqueTerms) yield map.getOrElse(t,"")
        val fr = FullRecordMapper.createFullRecord("", map, Versions.RAW)
        load(dataResourceUid, fr, uniqueTermsValues)
      })

      //end of records
      (xml \\ "diagnostic").filter(x => (x \ "@code").text == "END_OF_RECORDS").text.toBoolean
    } catch {
      case _ => println("Error processing response")
    }
    true
  }

  def performInventoryRequest(digirEndpoint: String, resourceName: String): List[String] = {
    val metadataRequest = createInventoryRequest("1.0", digirEndpoint, resourceName)
    val http = new HttpClient
    val encodedRequest = URLEncoder.encode(metadataRequest.toString)
    val url = digirEndpoint + "?request=" + encodedRequest
    println(url)
    val get = new GetMethod(url)
    val status = http.executeMethod(get)
    val response = get.getResponseBodyAsString
    val xml = XML.loadString(response)
    val scientificNames = xml \\ "ScientificName"
    scientificNames.map(scientificName => scientificName.text).toList
  }

  def performMetadataRequest(digirEndpoint: String) {
    val metadataRequest = createMetadataRequest("1.0", digirEndpoint)
    val http = new HttpClient
    val encodedRequest = URLEncoder.encode(metadataRequest.toString)
    val get = new GetMethod(digirEndpoint + "?request=" + encodedRequest)
    val status = http.executeMethod(get)
    val response = get.getResponseBodyAsString
    val xml = XML.loadString(response)
    val resources = xml \\ "resource"
    resources.foreach(resource => {
      val name = (resource \ "name").text
      val code = (resource \ "code").text
      val website = (resource \ "relatedInformation").text
      val citation = (resource \ "citation").text
      val rights = (resource \ "useRestrictions").text
      println(code + " : " + name)
    })
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