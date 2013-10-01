package au.org.ala.util
import org.apache.commons.lang.time.DateUtils
import java.text.SimpleDateFormat
import scala.xml.XML
import scala.collection.mutable.ListBuffer
import au.org.ala.biocache._
import java.util.Date
import org.slf4j.LoggerFactory
import scala.collection.mutable
import scala.util.parsing.json.JSON

object FlickrUserLoader extends DataLoader {
  def main(args: Array[String]) {

    var dataResourceUid = ""
    var flickrUserId = ""

    val parser = new OptionParser("Load flickr images for registered users") {
      arg("<data resource UID>", "The UID of the data resource to load", { v: String => dataResourceUid = v })
      opt("nsid", "The Flickr NSID for this user", "The Flickr NSID (internal flickr ID) for this user", { v: String => flickrUserId = v })
    }
    if(parser.parse(args)){
      val l = new FlickrLoader
      //get the connection details
      val (protocol, url, uniqueTerms, params, customParams, lastChecked) = retrieveConnectionParameters(dataResourceUid)
      if(flickrUserId != ""){
        l.loadWithoutDateRange(params ++ Map("user_id"  -> flickrUserId), dataResourceUid, false)
      } else {
        val userLookup = l.getUserLookup
        userLookup.keys.foreach(flickrUserId => {
          //get the connection details
          val (protocol, url, uniqueTerms, params, customParams, lastChecked) = retrieveConnectionParameters(dataResourceUid)
          l.loadWithoutDateRange(params ++ Map("user_id"  -> flickrUserId), dataResourceUid, false)
        })
      }
    }
  }
}

object FlickrLoader extends DataLoader {

  def main(args: Array[String]) {

    var dataResourceUid = ""
    var startDate: Option[Date] = None
    var endDate: Option[Date] = None
    var overwriteImages = false
    var lastMonth = false
    var lastDay = false
    var lastWeek = false
    var updateCollectory = false

    val parser = new OptionParser("Load flickr resource") {
      arg("<data resource UID>", "The UID of the data resource to load", { v: String => dataResourceUid = v })
      opt("s", "startDate", "start date to harvest from in yyyy-MM-dd format", { v: String => startDate = Some(DateUtils.parseDate(v, Array("yyyy-MM-dd"))) })
      opt("e", "endDate", "end date in yyyy-MM-dd format", { v: String => endDate = Some(DateUtils.parseDate(v, Array("yyyy-MM-dd"))) })
      booleanOpt("lm", "harvestLastMonth", "Harvest the last month of records", { v: Boolean => lastMonth = v })
      booleanOpt("ld", "harvestLastDay", "Harvest the last day of records", { v: Boolean => lastDay = v })
      booleanOpt("lw", "harvestLastWeek", "Harvest the last week of records", { v: Boolean => lastWeek = v })
      booleanOpt("o", "overwrite", "overwrite images", { v: Boolean => overwriteImages = v })
      booleanOpt("u", "updateCollectory", "Update the harvesting information in the collectory", { v: Boolean => updateCollectory = v })
    }
    if(parser.parse(args)){
      val l = new FlickrLoader
      if(lastMonth){
        val today = new Date
        val monthAgo = DateUtils.addMonths(today, -1)
        l.load(dataResourceUid, Some(monthAgo), Some(today), updateCollectory, overwriteImages)
      } else if(lastDay){
        val today = new Date
        val yesterday = DateUtils.addDays(today, -1)
        l.load(dataResourceUid, Some(yesterday), Some(today), updateCollectory, overwriteImages)
      } else if(lastWeek){
        val today = new Date
        val sevenDaysAgo = DateUtils.addWeeks(today, -1)
        l.load(dataResourceUid, Some(sevenDaysAgo), Some(today), updateCollectory, overwriteImages)
      } else {
        l.load(dataResourceUid, startDate, endDate, updateCollectory, overwriteImages)
      }
    }
  }
}

/** Class for holding details of the licences in Flickr */
case class FlickrLicence(id:String,name:String,url:String)

class FlickrLoader extends DataLoader {

  val BHLLinkInText = """([.*]*)?(biodiversitylibrary.org/page/)([0-9]*)([.*]*)?"""".r
  val BHLLink = """(biodiversitylibrary.org/page/)([0-9]*)"""".r

  def load(dataResourceUid: String, updateCollectory: Boolean):Unit = load(dataResourceUid, None, None, updateCollectory)

  /**
   * Retrieve a map of licences
   */
  def retrieveLicenceMap(connectParams:Map[String,String]) : Map[String,FlickrLicence] = {
    val infoPage = makeGetLicencesUrl(connectParams)
    val xml = XML.loadString(scala.io.Source.fromURL(infoPage).mkString)
    (xml \\ "license").map(el => {
      val id = el.attribute("id").get.text
      val name = el.attribute("name").get.text
      val url = el.attribute("url").get.text
      id -> FlickrLicence(id,name,url)
    }).toMap
  }

  /**
   * Load the resource between the supplied dates.
   */
  def load(dataResourceUid: String, suppliedStartDate: Option[Date], suppliedEndDate: Option[Date], updateCollectory: Boolean, overwriteImages: Boolean = false){
    val (protocol, url, uniqueTerms, params, customParams, lastChecked) = retrieveConnectionParameters(dataResourceUid)
    load(params, suppliedEndDate, suppliedStartDate, dataResourceUid, updateCollectory)
  }

  def getUserLookup : Map[String,String] = {
    if(Config.flickrUsersUrl == "")
      return Map()

    try {
      val mapBuff = new mutable.HashMap[String,String]
      //retrieve from properties
      val userListJson = scala.io.Source.fromURL(Config.flickrUsersUrl).getLines().mkString
      //convert to flickrId -> alaID map.....
      val userArray:Seq[Map[String,String]] =  JSON.parseFull(userListJson).get.asInstanceOf[Seq[Map[String, String]]]
      userArray.foreach(u => mapBuff.put(u.getOrElse("externalId",""), u.getOrElse("id","")))
      mapBuff.toMap
    } catch {
      case e:Exception => logger.error(e.getMessage, e); Map()
    }
  }

  /**
   * Load the supplied data source
   *
   * @param params
   * @param dataResourceUid
   * @param updateCollectory
   */
  def loadWithoutDateRange(params: Map[String, String], dataResourceUid: String, updateCollectory: Boolean) {
    val licences = retrieveLicenceMap(params)
    val keywords = params.getOrElse("keywords", "").split(",").map(keyword => keyword.trim.replaceAll(" ", "").toLowerCase).toList
    val userLookup = getUserLookup

    try {
      val photoIds = getPhotoIds(params)
      photoIds.foreach(photoId => {

        try {
          //persist the occurrence with image metadata
          val (photoPageUrl, imageUrl, fr, tags) = processPhoto(params, licences, userLookup, photoId)

          //have we already loaded this record?
          val alreadyLoaded = exists(dataResourceUid, List(photoPageUrl))

          //load it if its of interest and we havent loaded it
          if (isOfInterest(fr, tags, keywords)) {
            load(dataResourceUid, fr, List(photoPageUrl), !alreadyLoaded, true)
          }
        } catch {
          case e: Exception => logger.error(e.getMessage, e)
        }
      })
    } catch {
      case e: Exception => logger.error(e.getMessage, e)
    }

    if (updateCollectory) {
      updateLastChecked(dataResourceUid)
    }
  }

  /**
   * Load the supplied data source
   *
   * @param params
   * @param suppliedEndDate
   * @param suppliedStartDate
   * @param dataResourceUid
   * @param updateCollectory
   */
  def load(params: Map[String, String], suppliedEndDate: Option[Date], suppliedStartDate: Option[Date], dataResourceUid: String, updateCollectory: Boolean) {
    val licences = retrieveLicenceMap(params)
    val keywords = params.getOrElse("keywords", "").split(",").map(keyword => keyword.trim.replaceAll(" ", "").toLowerCase).toList
    val userLookup = getUserLookup

    val endDate = suppliedEndDate.getOrElse({
      params.get("end_date") match {
        case Some(v) => DateUtils.parseDate(v, Array("yyyy-MM-dd"))
        case None => new Date()
      }
    })
    val startDate = suppliedStartDate.getOrElse({
      params.get("start_date") match {
        case Some(v) => DateUtils.parseDate(v, Array("yyyy-MM-dd"))
        case None => DateUtils.parseDate("2004-01-01", Array("yyyy-MM-dd"))
      }
    })

    var currentStartDate = DateUtils.addDays(endDate, -1)
    var currentEndDate = endDate

    val df = new SimpleDateFormat("yyyy-MM-dd")

    logger.info("Starting with range: " + df.format(currentStartDate) + " to " + df.format(currentEndDate))

    // page through the images month-by-month
    while (currentStartDate.after(startDate) || currentStartDate.equals(startDate)) {

      logger.info("Harvesting time period: " + df.format(currentStartDate) + " to " + df.format(currentEndDate))
      try {
        val photoIds = getPhotoIdsForDateRange(params, currentStartDate, currentEndDate)
        photoIds.foreach(photoId => {

          try {
            //persist the occurrence with image metadata
            val (photoPageUrl, imageUrl, fr, tags) = processPhoto(params, licences, userLookup, photoId)

            //have we already loaded this record?
            val alreadyLoaded = exists(dataResourceUid, List(photoPageUrl))

            //load it if its of interest and we havent loaded it
            if (isOfInterest(fr, tags, keywords)) {
              load(dataResourceUid, fr, List(photoPageUrl), !alreadyLoaded, true)
            }
          } catch {
            case e: Exception => logger.error(e.getMessage, e)
          }
        })
      } catch {
        case e: Exception => logger.error(e.getMessage, e)
      }
      currentEndDate = currentStartDate
      currentStartDate = DateUtils.addDays(currentEndDate, -1)
    }

    if (updateCollectory) {
      updateLastChecked(dataResourceUid)
    }
  }

  /**
   * Is the image of interest
   */
  def isOfInterest(fr:FullRecord, tags: Seq[String], keywords: Seq[String]): Boolean = {

    val hasTaxon = {
      val nonEmptyTaxonProp = fr.classification.getPropertyNames.find(p => {
         !fr.classification.getProperty(p).isEmpty
      })
      !nonEmptyTaxonProp.isEmpty
    }
    if(!hasTaxon){
      return false
    }

    //match on keywords
    val index = tags.indexWhere(
      tag => {
        val indexOfKeyword = keywords.indexWhere(keyword => tag.equalsIgnoreCase(keyword))
        indexOfKeyword >= 0
      })
    index >= 0
  }

  /**
   * Process a single photo returning a FullRecord representing this photo.
   *
   * @param connectParams
   * @param licences
   * @param photoId
   * @return
   */
  def processPhoto(connectParams: Map[String, String], licences: Map[String,FlickrLicence],
                   userLookup:Map[String,String], photoId: String): (String, String, FullRecord, Seq[String]) = {

    //create an occurrence record
    val fr = new FullRecord
    val infoPage: String = makeGetInfoUrl(connectParams, photoId)

    logger.info(infoPage)

    val tagList = new ListBuffer[String]
    val xml = XML.loadString(scala.io.Source.fromURL(infoPage).mkString.trim)
    //val listBuffer = new ListBuffer[String]
    //get the tags
    val tags = (xml \\ "tag").foreach(el => {
      val raw = el.attribute("raw")
      //println(raw.get.text)
      if (!raw.isEmpty && raw.get.text.contains("=")) {
        //parse machine tage
        val (namespace, tagName, tagValue) = parseMachineTag(raw.get.text.trim)
        tagList += tagValue
        //match to darwin core terms
        if (!tagName.isEmpty){
          val dwcTerm = matchDwcTerm(namespace, tagName.get)
          dwcTerm match {
            case Some(term) => fr.setNestedProperty(term, tagValue)
            case None => logger.debug("unmatched : " + raw.get.text.trim)
          }
        }
      }
    })

    val htmlPhotoPage = (xml \\ "url")(0).text.toString //val photoPageUrl
    fr.occurrence.occurrenceID = htmlPhotoPage
    //use occurrenceDetails to store URI back to source - http://rs.tdwg.org/dwc/terms/#occurrenceDetails
    fr.occurrence.occurrenceDetails = htmlPhotoPage

    val licenseID = (xml \\ "photo")(0).attribute("license").get.text.toString
    val title = (xml \\ "title")(0).text.toString
    val description = (xml \\ "description")(0).text.toString
    val (username, realname, location, nsid) = {
      val ownerElem = (xml \\ "owner")(0)
      (ownerElem.attribute("username"), ownerElem.attribute("realname"), ownerElem.attribute("location"), ownerElem.attribute("nsid"))
    }

    //lookup the ID
    userLookup.get(nsid.get.text).exists(userId => {
      fr.occurrence.userId = userId
      true
    })

    val photoElem = (xml \\ "photo")(0)
    val farmId = photoElem.attribute("farm").get
    val serverId = photoElem.attribute("server").get
    val photoSecret = photoElem.attribute("secret").get
    val originalformat = photoElem.attribute("originalformat").getOrElse("jpg")
    val photoImageUrl = "http://farm" + farmId + ".static.flickr.com/" + serverId + "/" + photoId + "_" + photoSecret + "." + originalformat
    fr.occurrence.associatedMedia = photoImageUrl

    val datesElem = (xml \\ "dates")(0)
    fr.event.eventDate = datesElem.attribute("taken").get.text
    fr.occurrence.occurrenceRemarks = description

    if(fr.occurrence.occurrenceDetails == null){
      //check the description for BHL link
      checkForBHLUrl(description) match {
        case Some(url) =>  fr.occurrence.occurrenceDetails = url
        case None =>
      }
    }

    fr.occurrence.recordedBy = realname.head.text

    //get the licence and rights fields
    val licence = licences.get(licenseID).get
    fr.occurrence.rights = licence.name

    //check the location elem
    if(!(photoElem \\ "location").isEmpty){
      val locationElem =  (photoElem \\ "location")(0)
      fr.location.decimalLatitude = locationElem.attribute("latitude").get.text
      fr.location.decimalLongitude = locationElem.attribute("longitude").get.text
      fr.location.coordinateUncertaintyInMeters = locationElem.attribute("accuracy").get.text
      //println("Found lat, long: " + fr.location.decimalLatitude +", " +fr.location.decimalLongitude)
    }

    fr.occurrence.basisOfRecord = "Image"
    (fr.occurrence.occurrenceID, photoImageUrl, fr, tagList)
  }

  def matchDwcTerm(namespace:Option[String], tagName:String) : Option[String] = DwC.matchTerm(tagName) match {
    case Some(term) => Some(term.canonical)
    case None => TagsToDwc.map.get(namespace.getOrElse("") +":" + tagName)
  }

  /**
   * Splits the tags into namespace, name and value
   *
   */
  def parseMachineTag(tag: String): (Option[String], Option[String], String) = {
    //println(tag)
    if (tag.contains("=")) {
      val (name, value) = {
        val parts = tag.split("=")
        parts.length match {
          case 2 => (parts(0).replaceAll(" ", "").trim.toLowerCase, parts(1).trim)
          case _ => ("", parts.last)
        }
      }
      //if the tag has a name-space, remove it
      if (name.contains(":")) {
        val split = name.split(':')
        split.length match {
          case 2 => (Some(split(0).toLowerCase.trim), Some(split(1).toLowerCase.trim), value)
          case _ => (None, None, value)
        }
      } else if(name == "bhl:page") {
        //hackish - but no clean way of doing this to link to BHL AU
        (None,  Some("occurrenceDetails"), "http://bhl.ala.org.au/page/" + value)
      } else {
        (None, Some(name), value)
      }
    } else {
      (None, None, tag)
    }
  }

  def createParamIfNotNull(connectParams: Map[String, String], name:String, key:String) : String = {
    val value = connectParams.getOrElse(key, "")
    if(value != ""){
      "&" + name + "=" + value
    } else {
      ""
    }
  }

  def makeSearchUrl(connectParams: Map[String, String], pageNumber: Int) = connectParams("url") +
    "?method=flickr.photos.search" +
    createParamIfNotNull(connectParams, "content_type", "content_type") +
    createParamIfNotNull(connectParams, "user_id", "user_id") +
    createParamIfNotNull(connectParams, "group_id", "group_id") +
    createParamIfNotNull(connectParams, "privacy_filter", "privacy_filter") +
    createParamIfNotNull(connectParams, "api_key", "api_key") +
    createParamIfNotNull(connectParams, "per_page", "per_page") +
    "&page=" + pageNumber


  def makeSearchUrl(connectParams: Map[String, String], minUpdateDate: String, maxUpdateDate: String, pageNumber: Int) = connectParams("url") +
    "?method=flickr.photos.search" +
    createParamIfNotNull(connectParams, "content_type", "content_type") +
    createParamIfNotNull(connectParams, "user_id", "user_id") +
    createParamIfNotNull(connectParams, "group_id", "group_id") +
    createParamIfNotNull(connectParams, "privacy_filter", "privacy_filter") +
    createParamIfNotNull(connectParams, "api_key", "api_key") +
    createParamIfNotNull(connectParams, "per_page", "per_page") +
    "&min_upload_date=" + minUpdateDate + //startDate
    "&max_upload_date=" + maxUpdateDate + //endDate
    "&page=" + pageNumber

  def makeGetInfoUrl(connectParams: Map[String, String], photoId: String): String = connectParams("url") +
    "?method=flickr.photos.getInfo" +
    "&api_key=" + connectParams("api_key") +
    "&photo_id=" + photoId

  def makeGetLicencesUrl(connectParams: Map[String, String]): String = connectParams("url") +
    "?method=flickr.photos.licenses.getInfo" +
    "&api_key=" + connectParams("api_key")

  def getPhotoIdsForDateRange(connectParams: Map[String, String], startDate: Date, endDate: Date): List[String] = {

    val mysqlDateTime = new SimpleDateFormat("yyyy-MM-dd")
    val minUpdateDate = mysqlDateTime.format(startDate)
    val maxUpdateDate = mysqlDateTime.format(endDate)
    val firstUrl = makeSearchUrl(connectParams, minUpdateDate, maxUpdateDate, 0)
    println(firstUrl)
    println(scala.io.Source.fromURL(firstUrl).mkString)
    val xml = XML.loadString(scala.io.Source.fromURL(firstUrl).mkString)
    val pages = ((xml \\ "photos")(0) \ "@pages").toString.toInt

    val photoIds = {
      val firstBatch = (xml \\ "photo").toList.map(photo => photo.attribute("id").get.toString)
      val theRest = for (pageNo <- 2 until pages + 1) yield retrieveBatch(connectParams, minUpdateDate, maxUpdateDate, pageNo)
      (firstBatch ::: theRest.toList.flatten)
    }
    photoIds
  }

  def getPhotoIds(connectParams: Map[String, String]): List[String] = {

    val firstUrl = makeSearchUrl(connectParams, 0)
    println(firstUrl)
    println(scala.io.Source.fromURL(firstUrl).mkString)
    val xml = XML.loadString(scala.io.Source.fromURL(firstUrl).mkString)
    val pages = ((xml \\ "photos")(0) \ "@pages").toString.toInt

    val photoIds = {
      val firstBatch = (xml \\ "photo").toList.map(photo => photo.attribute("id").get.toString)
      val theRest = for (pageNo <- 2 until pages + 1) yield retrieveBatch(connectParams, pageNo)
      (firstBatch ::: theRest.toList.flatten)
    }
    photoIds
  }

  def retrieveBatch(connectParams: Map[String, String], pageNo: Int): List[String] = {
    val urlToSearch = makeSearchUrl(connectParams, pageNo)
    val xmlPage = XML.loadString(scala.io.Source.fromURL(urlToSearch).mkString)
    retrievePhotoIds(xmlPage)
  }

  def retrieveBatch(connectParams: Map[String, String], minUpdateDate: String, maxUpdateDate: String, pageNo: Int): List[String] = {
    val urlToSearch = makeSearchUrl(connectParams, minUpdateDate, maxUpdateDate, pageNo)
    val xmlPage = XML.loadString(scala.io.Source.fromURL(urlToSearch).mkString)
    retrievePhotoIds(xmlPage)
  }

  def retrievePhotoIds(xml: scala.xml.Elem) = (xml \\ "photo").toList.map(photo => photo.attribute("id").get.toString)

  def checkForBHLUrl(description:String):Option[String] = BHLLinkInText.findFirstIn(description) match {
    case Some(url) => {
      val BHLLink(domain, pageId) = url
      Some("http://www.biodiversitylibrary.org/page/" + pageId)
    }
    case None => None
  }
}