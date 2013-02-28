package au.org.ala.util

import au.org.ala.biocache.DataLoader
import com.gargoylesoftware.htmlunit.{Page, WebRequestSettings, BrowserVersion, WebClient}
import java.net.URL
import com.gargoylesoftware.htmlunit.html.{HtmlHead, HtmlPage}
import xml.{Node, Elem, XML}
import java.text.{SimpleDateFormat, MessageFormat}
import java.util.Calendar

/**
 * Created with IntelliJ IDEA.
 * User: ChrisF
 * Date: 27/02/13
 * Time: 4:13 PM
 * To change this template use File | Settings | File Templates.
 */
object NatureShareLoader extends DataLoader {

  val INDEX_PAGE = "http://natureshare.org.au/observation/browse_by_created/?mode=basic"
  val OBSERVATION_PAGE_TEMPLATE = "http://natureshare.org.au/observation/{0}/"

  val EMPTY_DATETIME_STRING = "(No date/time)"
  val EMPTY_DESCRIPTION_STRING = "(No description.)"
  val EMPTY_TAGS_STRING = "(No tags.)"
  val EMPTY_LOCATION_STRING = "(No location.)"
  val EMPTY_COMMENTS_STRING = "(No comments.)"

  def main(args: Array[String]) {
    new NatureShareLoader().load("foo")
  }

  def getHTMLPageAsXML(url: String): Elem = {
    val webClient = new WebClient(BrowserVersion.FIREFOX_3);

    // Disable Javascript as basically we don't need it.
    webClient.setJavaScriptEnabled(false);

    val targetPageUrl = new URL(url);
    val reqSettings = new WebRequestSettings(targetPageUrl);
    reqSettings.setCharset("UTF-8");

    try {
      val currentPage: HtmlPage = webClient.getPage(reqSettings);

      webClient.closeAllWindows();
      currentPage.cleanUp();
      val idMetaTagElement = currentPage.createElement("meta");
      idMetaTagElement.setAttribute("name", "ALA.Guid");
      idMetaTagElement.setAttribute("scheme", "URL");
      idMetaTagElement.setAttribute("content", url);

      // this.currentPage.appendChild(idMetaTagElement);
      val rootElement = currentPage.getDocumentElement();
      val headSection = rootElement.getByXPath("//html/head").asInstanceOf[java.util.List[HtmlHead]];
      val currentHtmlHead: HtmlHead = headSection.get(0);
      currentHtmlHead.appendChild(idMetaTagElement);

      XML.loadString(currentPage.asXml());
    } catch {
      case ex: Exception => {
        logger.error(ex.getMessage(), ex);
      }

      return null;
    }
  }
}


class NatureShareLoader extends CustomWebserviceLoader {
  def load(dataResourceUid: String) {

    val xml = NatureShareLoader.getHTMLPageAsXML(NatureShareLoader.INDEX_PAGE)
    val pageLinks = xml \\ "@href"
    val firstLink = pageLinks.find(_.toString().startsWith("/observation/")).get

    val latestObservationNumber = firstLink.toString().split("/")(2)
    processObservation(latestObservationNumber)
  }

  def processObservation(observationNumber: String) {
    val xml = NatureShareLoader.getHTMLPageAsXML(MessageFormat.format(NatureShareLoader.OBSERVATION_PAGE_TEMPLATE, "122"))
    val divs = xml \\ "div"

    // scrape contributor
    val documentDiv = divs.find(nodeContainsAttributeWithValue(_, "class", "document")).get
    val contributor = (documentDiv \\ "a").head.text.trim
    println(contributor)

    // scrape photos
    val photoDivs = divs.filter(nodeContainsAttributeWithValue(_, "class", "observation_page_photo"))
    val imageUrls = photoDivs.map(n => (n \\ "a").last.attribute("href").get.text)
    println(imageUrls)

    // scrape species
    val speciesDiv = divs.find(nodeContainsAttributeWithValue(_, "id", "species")).get
    val spans = speciesDiv \\ "span"
    val nonDeletedSpeciesSpans = spans.filter(!nodeContainsAttributeWithValue(_, "class", "deleted"))
    val nonDeletedSpecies = nonDeletedSpeciesSpans.map(n => (n \\ "a").head.text.trim)
    println(nonDeletedSpecies)

    // scrape tags
    var tagTuples: Seq[(java.lang.String, java.lang.String)] = null
    val tagsDiv = divs.find(nodeContainsAttributeWithValue(_, "id", "tags")).get
    if (!tagsDiv.text.contains(NatureShareLoader.EMPTY_TAGS_STRING)) {
      // ignore the first table row as this is the "header" for the table
      val tableRows = (tagsDiv \\ "tr").tail

      tagTuples = tableRows.map(row => {
        // Some tags are marked as deleted. Return a tuple with two null strings in this situation
        if (!nodeContainsAttributeWithValue((row \\ "td").head, "class", "deleted")) {
          ((row \\ "td").head.text.trim(), (row \\ "td").last.text.trim())
        } else {
          (null, null)
        }
      })
    }
    println(tagTuples)

    // scrape date/time
    var parsedDateTime: java.util.Date = null
    val dateTimeDiv = divs.find(nodeContainsAttributeWithValue(_, "id", "datetime")).get
    if (!dateTimeDiv.text.contains(NatureShareLoader.EMPTY_DATETIME_STRING)) {
      val dateTime = (dateTimeDiv \\ "p").head.text.trim
      val dateTimeParts = dateTime.split("\n")
      var date = dateTimeParts(0).trim()
      // Month names used are inconsistent. Use 3 letter month names followed by a dot, so that we can parse dates using
      // a single instance of SimpleDateFormatter
      date = date.replace("Sept", "Sep")
      date = date.replace("March", "Mar.")
      date = date.replace("April", "Apr.")
      date = date.replace("May", "May.")
      date = date.replace("June", "Jun.")
      date = date.replace("July", "Jul.")

      // Some records only contain a date
      if (dateTimeParts.length >= 4) {
        val time = dateTimeParts(4).replace("(", "").replace(")", "").trim
        val natureServeDateTimeFormat = new SimpleDateFormat("MMM. dd, yyyy hh:mm a")
        parsedDateTime = natureServeDateTimeFormat.parse(date + " " + time)
      } else {
        val natureServeDateFormat = new SimpleDateFormat("MMM. dd, yyyy")
        parsedDateTime = natureServeDateFormat.parse(date)
      }
    }
    println(parsedDateTime)

    // scrape description
    var description: String = null
    val descriptionDiv = divs.find(nodeContainsAttributeWithValue(_, "id", "description")).get
    if (!descriptionDiv.text.contains(NatureShareLoader.EMPTY_DESCRIPTION_STRING)) {
      description = (descriptionDiv \\ "p").head.text.trim.replace("\n", " ")
    }

    println(description)

    // scrape collections
    // (ignore)

    // scrape location
    var latitude: String = null
    var longitude: String = null
    val locationDiv = divs.find(nodeContainsAttributeWithValue(_, "id", "location")).get
    if (!locationDiv.text.contains(NatureShareLoader.EMPTY_LOCATION_STRING)) {
      val locationString = (locationDiv \\ "p").head.text.trim.replace("\n", " ")
      val latitudeLongitude = locationString.split(":")(1)
      latitude = latitudeLongitude.split(",")(0).trim
      longitude = latitudeLongitude.split(",")(1).trim
    }
    println(latitude)
    println(longitude)

    // scrape comments
    var comments: Seq[String] = null
    val commentsDiv = divs.find(nodeContainsAttributeWithValue(_, "id", "comments")).get
    if (!commentsDiv.contains(NatureShareLoader.EMPTY_COMMENTS_STRING)) {
      val commentItems = (commentsDiv \\ "li")
      //The text that we want for the comment comes in 3 pieces. Strip out the newlines and spaces that separate them.
      comments = commentItems.map(node => node.text.trim.replace("\n", "").replaceAll( """\s{2,}""", " "))
    }
    println(comments)

    // scrape parts of the metadata
    val metadataDiv = divs.find(nodeContainsAttributeWithValue(_, "id", "meta")).get
    val metadataLines = (metadataDiv \\ "p").text.split("\n")
    val photoDateTimeUsed = (metadataLines(11).trim == "Photo datetime used: yes")
    val photoGeotagUsed = (metadataLines(14).trim == "Photo geotag used: yes")
    println(photoDateTimeUsed)
    println(photoGeotagUsed)

    // All records have licence CC BY 2.5 AU
  }

  def nodeContainsAttributeWithValue(node: Node, key: String, value: String): Boolean = {
    val attributeNodes = node.attribute(key).getOrElse(null)
    if (attributeNodes != null) {
      if (!attributeNodes.filter(_.text == value).isEmpty) {
        true
      } else {
        false
      }
    } else {
      false
    }
  }
}