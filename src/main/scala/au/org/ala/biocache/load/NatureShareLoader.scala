package au.org.ala.biocache.load

import com.gargoylesoftware.htmlunit.{WebRequestSettings, BrowserVersion, WebClient}
import java.net.URL
import com.gargoylesoftware.htmlunit.html.{HtmlHead, HtmlPage}
import xml.{Node, Elem, XML}
import java.text.{SimpleDateFormat, MessageFormat}
import org.apache.commons.lang3.StringUtils
import au.org.ala.biocache.util.OptionParser
import au.org.ala.biocache.model.Versions

/**
 * Created with IntelliJ IDEA.
 * User: ChrisF
 * Date: 27/02/13
 * Time: 4:13 PM
 * To change this template use File | Settings | File Templates.
 */
object NatureShareLoader extends DataLoader {

  val INDEX_PAGE = "http://natureshare.org.au/observation/browse_by_created/?model=basic"
  val OBSERVATION_PAGE_TEMPLATE = "http://natureshare.org.au/observation/{0}/"

  val EMPTY_DATETIME_STRING = "(No date/time)"
  val EMPTY_DESCRIPTION_STRING = "(No description.)"
  val EMPTY_TAGS_STRING = "(No tags.)"
  val EMPTY_LOCATION_STRING = "(No location.)"
  val EMPTY_COMMENTS_STRING = "(No comments.)"

  val OVERALL_TAG_SPECIES = "(overall)"

  val ASSOCIATED_MEDIA_DWC_KEY = "associatedMedia"
  val ASSOCIATED_OCCURRENCES_DWC_KEY = "associatedOccurrences"
  val CATALOG_NUMBER_DWC_KEY = "catalogNumber"
  val OCCURRENCE_DETAILS_DWC_KEY = "occurrenceDetails"
  val OCCURRENCE_REMARKS_DWC_KEY = "occurrenceRemarks"
  val RECORDED_BY_DWC_KEY = "recordedBy"
  val EVENT_DATE_DWC_KEY = "eventDate"
  val RIGHTS_DWC_KEY = "rights"
  val PHOTOGRAPHER_FULLRECORD_KEY = "photographer"
  val VERBATIM_LATITUDE_DWC_KEY = "verbatimLatitude"
  val VERBATIM_LONGITUDE_DWC_KEY = "verbatimLongitude"
  val EVENT_REMARKS_DWC_KEY = "eventRemarks"
  val GEOREFERENCE_PROTOCOL_DWC_KEY = "georeferenceProtocol"
  val SCIENTIFIC_NAME_DWC_KEY = "scientificName"
  val RIGHTS_HOLDER_DWC_KEY = "rightsholder"

  def main(args: Array[String]) {
    val loader = new TasNvaDataLoader
    var dataResourceUid: String = null

    val parser = new OptionParser("Import Natureshare data") {
      arg("<data-resource-uid>", "the data resource to import", {
        v: String => dataResourceUid = v
      })
    }

    new NatureShareLoader().load(dataResourceUid)
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

    val latestObservationNumberAsString = firstLink.toString().split("/")(2)

    val totalObservations = Integer.parseInt(latestObservationNumberAsString)
    for (i <- 1 to totalObservations) {
      println("Processing observation " + i)

      try {
        processObservation(dataResourceUid, i.toString)
      } catch {
        case ex: Throwable => {
          println("ERROR: observation " + i.toString() + " failed to load:")
          ex.printStackTrace()
        };
      }
    }
  }

  def processObservation(dataResourceUid : String, observationNumber: String) {
    val observationUrl = MessageFormat.format(NatureShareLoader.OBSERVATION_PAGE_TEMPLATE, observationNumber)

    val xml = NatureShareLoader.getHTMLPageAsXML(observationUrl)

    val divs = xml \\ "div"

    // scrape contributor
    val documentDiv = divs.find(nodeContainsAttributeWithValue(_, "class", "document")).get
    val contributor = (documentDiv \\ "a").head.text.trim

    // scrape photos
    val photoDivs = divs.filter(nodeContainsAttributeWithValue(_, "class", "observation_page_photo"))
    val imageUrls = photoDivs.map(n => "http://natureshare.org.au" + (n \\ "a").last.attribute("href").get.text)

    // scrape species
    val speciesDiv = divs.find(nodeContainsAttributeWithValue(_, "id", "species")).get
    val spans = speciesDiv \\ "span"

    val nonDeletedSpeciesSpans = spans.filter(!nodeContainsAttributeWithValue(_, "class", "deleted"))
    val nonDeletedSpecies = nonDeletedSpeciesSpans.map(n => (n \\ "a").head.text.trim)

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
          // Deleted tag. Must return a value as using the map function here. Return a tuple with two empty strings as a placeholder.
          ("", "")
        }
      })
    }

    // scrape date/time
    var formattedDateTime: String = null
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
        val parsedDateTime = natureServeDateTimeFormat.parse(date + " " + time)

        val isoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        formattedDateTime = isoDateTimeFormat.format(parsedDateTime)
      } else {
        val natureServeDateFormat = new SimpleDateFormat("MMM. dd, yyyy")
        val parsedDateTime = natureServeDateFormat.parse(date)

        val isoDateFormat = new SimpleDateFormat("yyyy-MM-dd")
        formattedDateTime = isoDateFormat.format(parsedDateTime)
      }
    }

    // scrape description
    var description: String = null
    val descriptionDiv = divs.find(nodeContainsAttributeWithValue(_, "id", "description")).get
    if (!descriptionDiv.text.contains(NatureShareLoader.EMPTY_DESCRIPTION_STRING)) {
      description = (descriptionDiv \\ "p").head.text.trim.replace("\n", " ")
    }

    // ignore collections

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

    // scrape comments
    var comments: Seq[String] = null
    val commentsDiv = divs.find(nodeContainsAttributeWithValue(_, "id", "comments")).get
    if (!commentsDiv.contains(NatureShareLoader.EMPTY_COMMENTS_STRING)) {
      val commentItems = (commentsDiv \\ "li")
      //The text that we want for the comment comes in 3 pieces. Strip out the newlines and spaces that separate them.
      comments = commentItems.map(node => node.text.trim.replace("\n", "").replaceAll( """\s{2,}""", " "))
    }

    // scrape parts of the metadata
    val metadataDiv = divs.find(nodeContainsAttributeWithValue(_, "id", "meta")).get
    val metadataLines = (metadataDiv \\ "p").text.split("\n")
    val photoDateTimeUsed = (metadataLines(11).trim == "Photo datetime used: yes")
    val photoGeotagUsed = (metadataLines(14).trim == "Photo geotag used: yes")

    // To create an occurrence record we need a scientific name and a location at a minimum.
    if (!nonDeletedSpecies.isEmpty && latitude != null && longitude != null) {
      if (nonDeletedSpecies.size > 1) {
        for (scientificName <- nonDeletedSpecies) {
          val tagList = generateTagList(tagTuples, scientificName)
          val catalogNumber = observationNumber + "_" + scientificName.replace(" ", "_")
          val associatedOccurrences = nonDeletedSpecies.filter(species => species != scientificName).map(species => observationNumber + "_" + species.replace(" ", "_"))
          createOccurrenceRecord(dataResourceUid, catalogNumber, scientificName, contributor, formattedDateTime, latitude, longitude, imageUrls, description, comments, associatedOccurrences, tagList, observationUrl, photoDateTimeUsed, photoGeotagUsed)
        }
      } else {
        val scientificName = nonDeletedSpecies.head
        val catalogNumber = observationNumber + "_" + scientificName.replace(" ", "_")
        val tagList = generateTagList(tagTuples, scientificName)

        createOccurrenceRecord(dataResourceUid, catalogNumber, scientificName, contributor, formattedDateTime, latitude, longitude, imageUrls, description, comments, null, tagList, observationUrl, photoDateTimeUsed, photoGeotagUsed)
      }
    } else {
      println("Insufficient data - ignoring " + observationNumber)
    }
  }

  def generateTagList(tagTuples: Seq[(String, String)], scientificName: String) : Seq[String] = {
    var tagList: Seq[String] = Seq[String]()
    if (tagTuples != null) {
      for ((tag: String, speciesForTag: String) <- tagTuples) {
        // skip tuples where both values are null. These are placeholders for deleted tags.
        if (tag != "" && speciesForTag != "") {
          if ((speciesForTag == scientificName || speciesForTag == NatureShareLoader.OVERALL_TAG_SPECIES) && !tagList.contains(tag)) {
            tagList = tagList :+ tag
          }
        }
      }
    }
    tagList
  }

  def createOccurrenceRecord(dataResourceUid : String, catalogNumber: String, scientificName: String, contributor: String, date: String,
                             latitude: String, longitude: String, imageUrls: Seq[String], description: String,
                             comments: Seq[String], associatedOccurrences: Seq[String], tags: Seq[String], originalRecordUrl: String,
                             photoDateTimeUsed: Boolean, photoGeotagUsed: Boolean) {

    var mappedValues = Map[String, String]()
    mappedValues += (NatureShareLoader.CATALOG_NUMBER_DWC_KEY -> catalogNumber.trim())
    mappedValues += (NatureShareLoader.SCIENTIFIC_NAME_DWC_KEY -> scientificName.trim())
    mappedValues += (NatureShareLoader.RECORDED_BY_DWC_KEY -> contributor.trim())

    if (date != null) {
      mappedValues += (NatureShareLoader.EVENT_DATE_DWC_KEY -> date.trim())
    }

    mappedValues += (NatureShareLoader.VERBATIM_LATITUDE_DWC_KEY -> latitude.trim())
    mappedValues += (NatureShareLoader.VERBATIM_LONGITUDE_DWC_KEY -> longitude.trim())
    // All records have licence CC BY 2.5 AU
    mappedValues += (NatureShareLoader.RIGHTS_DWC_KEY -> "CC BY 2.5 AU")

    // Rights holder is always "NatureShare"
    val rightsHolderString = contributor + " via NatureShare"
    mappedValues += (NatureShareLoader.RIGHTS_HOLDER_DWC_KEY -> rightsHolderString)

    if (imageUrls != null && !imageUrls.isEmpty) {
      mappedValues += (NatureShareLoader.ASSOCIATED_MEDIA_DWC_KEY -> imageUrls.mkString(";"))

      mappedValues += (NatureShareLoader.PHOTOGRAPHER_FULLRECORD_KEY -> contributor.trim())
    }

    if (associatedOccurrences != null && !associatedOccurrences.isEmpty) {
      mappedValues += (NatureShareLoader.ASSOCIATED_OCCURRENCES_DWC_KEY -> associatedOccurrences.mkString(";"))
    }

    mappedValues += (NatureShareLoader.OCCURRENCE_DETAILS_DWC_KEY -> originalRecordUrl.trim())

    if (photoDateTimeUsed) {
      mappedValues += (NatureShareLoader.EVENT_REMARKS_DWC_KEY -> "Photo date/time used.")
    }

    if (photoGeotagUsed) {
      mappedValues += (NatureShareLoader.GEOREFERENCE_PROTOCOL_DWC_KEY -> "Photo geotag used.")
    }

    var occurrencesRemarksString: String = ""
    if (!StringUtils.isEmpty(description)) {
      occurrencesRemarksString += "Description: " + description + " "
    }

    if (tags != null && !tags.isEmpty) {
      occurrencesRemarksString += "Tags: " + tags.mkString(",") + " "
    }

    if (comments != null && !comments.isEmpty) {
      occurrencesRemarksString += "Comments: " + comments.mkString(",") + " "
    }

    if (!StringUtils.isEmpty(occurrencesRemarksString)) {
      mappedValues += (NatureShareLoader.OCCURRENCE_REMARKS_DWC_KEY -> occurrencesRemarksString.trim())
    }

    println(mappedValues)
    val fr = FullRecordMapper.createFullRecord("", mappedValues, Versions.RAW)
    val uniqueTermsValues = List(mappedValues(NatureShareLoader.CATALOG_NUMBER_DWC_KEY))
    load(dataResourceUid, fr, uniqueTermsValues)
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