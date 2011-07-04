package au.org.ala.util
import org.apache.commons.lang.time.DateUtils
import java.util.Date
import java.text.SimpleDateFormat
import scala.xml.XML
import au.org.ala.biocache.FullRecord

object FlickrLoader {
    
    def main(args:Array[String]){
        
       val connectParams = Map(
               "base_url" -> "http://api.flickr.com/services/rest/",
               "group_id" ->"806927@N20",
               "api_key" -> "08f5318120189e9d12669465c0113351",
               "start_date" -> "2010-01-01",
               "content_type" -> "1",
               "privacy_filter" -> "1",
               "per_page" -> "50");
       
        var totalIndexed = 0;
        var endDate = new Date
        val format = Array("yyyy-MM-dd")
        var finalStartDate = DateUtils.parseDate(connectParams("start_date"), format)

        if(System.getProperty("startDate")!=null){
            endDate = DateUtils.parseDate(System.getProperty("startDate"), format)
        }
        if(System.getProperty("endDate")!=null){
            finalStartDate = DateUtils.parseDate(System.getProperty("endDate"), format)
        }
        
        var startDate = DateUtils.addDays(endDate, -1);
        val df = new SimpleDateFormat("yyyy-MM-dd");
        
        // page through the images month-by-month
        while(startDate.after(finalStartDate)){
            println("Harvesting time period: "+df.format(startDate)+" to "+df.format(endDate));
            val photoIds = getPhotoIdsForDataRange(connectParams, startDate, endDate);
            photoIds.foreach(photoId => {
                val fr = processPhoto(connectParams, photoId)
                //persist the occurrence with image metadata
            })
            println("first id: " + photoIds.head+", number harvested: " + photoIds.size)
            endDate = startDate
            startDate = DateUtils.addDays(endDate, -1)
        }
        println("Total harvested: "+totalIndexed);
    }

    def processPhoto(connectParams:Map[String,String], photoId:String) : FullRecord = {
        
        //create an occurrence record
        val fr = new FullRecord
        fr.classification.scientificName = ""
        
        val url = makeGetInfoUrl(connectParams, photoId)
        println(url)
        
        val xml = XML.loadString(scala.io.Source.fromURL(url).mkString)
        
        //get the tags
        val tags = (xml \\ "tag").foreach(el => {
            val raw = el.attribute("raw")
            //println(raw.get.text)
            if(!raw.isEmpty && raw.get.text.contains("=")){
                //is it a machine tag - remove namespace
                val (tagName, tagValue) = parseMachineTag(raw.get.text.trim)
                
                tagName match {
                    case Some("scientificName") => fr.classification.scientificName = tagValue
                    case Some("author") => fr.classification.scientificNameAuthorship = tagValue
                    case Some("common") => fr.classification.vernacularName = tagValue
                    case Some("commonname") => fr.classification.vernacularName = tagValue
                    case Some("trinomial") => fr.classification.subspecies = tagValue; fr.classification.scientificName = tagValue
                    case Some("binomial") => fr.classification.species = tagValue; fr.classification.scientificName = tagValue
                    case Some("binomial name") => fr.classification.species = tagValue; fr.classification.scientificName = tagValue
                    case Some("species") => fr.classification.species = tagValue
                    case Some("genus") => fr.classification.genus = tagValue
                    case Some("subgenus") => fr.classification.subgenus = tagValue
                    case Some("family") => fr.classification.family = tagValue
                    case Some("subfamily") => fr.classification.subfamily = tagValue
                    case Some("order") => fr.classification.order = tagValue
                    case Some("bioorder") => fr.classification.order = tagValue
                    case Some("class") => fr.classification.classs = tagValue
                    case Some("phylum") => fr.classification.phylum = tagValue
                    case Some("kingdom") => fr.classification.kingdom = tagValue
                    case Some("country") => fr.location.country = tagValue
                    case Some("region") => fr.location.stateProvince = tagValue
                    case Some("stateProvince") => fr.location.stateProvince = tagValue
                    case Some("state") => fr.location.stateProvince = tagValue
                    case Some("province") => fr.location.stateProvince = tagValue
                    case Some("district") => fr.location.locality = tagValue
                    case Some("locality") => fr.location.locality = tagValue
                    case Some("lat") => fr.location.decimalLatitude = tagValue
                    case Some("latitude") => fr.location.decimalLatitude = tagValue
                    case Some("decimalLatitude") => fr.location.decimalLatitude = tagValue
                    case Some("lon") => fr.location.decimalLongitude = tagValue
                    case Some("long") => fr.location.decimalLongitude = tagValue
                    case Some("longitude") => fr.location.decimalLongitude = tagValue
                    case Some("decimalLongitude") => fr.location.decimalLongitude = tagValue
                    case Some("alt") => fr.location.maximumElevationInMeters = tagValue
                    case Some("altitude") => fr.location.maximumElevationInMeters = tagValue
                    case Some("accuracy") => fr.location.coordinateUncertaintyInMeters = tagValue
                    case Some("datum") => fr.location.geodeticDatum = tagValue
                    case Some("source") => fr.location.georeferenceSources = tagValue
                    case _ => println("unmatched : " + raw.get.text.trim)
                }
            }
        })
        
        fr.occurrence.occurrenceID = (xml \\ "url")(0).text.toString   //val photoPageUrl
        
        val title = (xml \\ "title")(0).text.toString
        val description = (xml \\ "description")(0).text.toString
        val (username,realname,location) = {
            val ownerElem = (xml \\ "owner")(0)
            (ownerElem.attribute("username"), ownerElem.attribute("realname"), ownerElem.attribute("location") )
        }
        
        fr
    }
    
    def parseMachineTag(tag:String): (Option[String], String) = {
        
        if(tag.contains("=")){
            val (name, value) = {
                val parts = tag.split("=")
                (parts(0).trim, parts(1).trim)
            }
            //if the tag has a name-space, remove it
            if(name.contains(":")){
                val split = name.split(':')
                split.length match {
                    case 2 => (Some(split(1).toLowerCase.trim), value)
                    case _ => (None, value)
                }
            } else {
                (Some(name), value)
            }
        } else {
        	(None, tag)
        }
    }
    
    def makeSearchUrl(connectParams:Map[String,String], minUpdateDate:String, maxUpdateDate:String, pageNumber:Int) =  connectParams("base_url") + 
         "?method=flickr.photos.search" + 
         "&content_type=" +  connectParams("content_type") + 
         "&group_id=" + connectParams("group_id") +
         "&privacy_filter=" +  connectParams("privacy_filter") + 
         "&min_upload_date=" + minUpdateDate +    //startDate
         "&max_upload_date=" + maxUpdateDate +    //endDate
         "&api_key=" + connectParams("api_key") + 
         "&per_page=" + connectParams("per_page") +
         "&page=" + pageNumber 
    
     def makeGetInfoUrl(connectParams:Map[String,String], photoId:String) =  connectParams("base_url") + 
         "?method=flickr.photos.getInfo" +  
         "&api_key=" + connectParams("api_key") +
         "&photo_id=" + photoId
         
    def getPhotoIdsForDataRange(connectParams:Map[String,String], startDate:Date, endDate:Date) : List[String] = {

        val mysqlDateTime = new SimpleDateFormat("yyyy-MM-dd");
        val minUpdateDate = mysqlDateTime.format(startDate);
        val maxUpdateDate = mysqlDateTime.format(endDate);
        val firstUrl = makeSearchUrl(connectParams, minUpdateDate,maxUpdateDate,0) 
        val xml = XML.loadString(scala.io.Source.fromURL(firstUrl).mkString)
        val pages = ((xml \\ "photos")(0) \ "@pages").toString.toInt
        
        val photoIds = {
        	val firstBatch = (xml \\ "photo").toList.map(photo => photo.attribute("id").get.toString)
        	val theRest = for (pageNo <- 2 until pages + 1) yield retrieveBatch(connectParams, minUpdateDate,maxUpdateDate,pageNo) 
        	(firstBatch ::: theRest.toList.flatten)
        }
        photoIds
    }
   
    def retrieveBatch(connectParams:Map[String,String], minUpdateDate:String, maxUpdateDate:String, pageNo:Int) : List[String] = {
        val urlToSearch = makeSearchUrl(connectParams, minUpdateDate,maxUpdateDate,pageNo)
        val xmlPage = XML.loadString(scala.io.Source.fromURL(urlToSearch).mkString)
        retrievePhotoIds(xmlPage)
    }
    
    def retrievePhotoIds(xml:scala.xml.Elem) = (xml \\ "photo").toList.map(photo => photo.attribute("id").get.toString)
}
