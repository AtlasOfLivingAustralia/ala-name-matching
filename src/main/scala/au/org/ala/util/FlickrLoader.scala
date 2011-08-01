package au.org.ala.util
import org.apache.commons.lang.time.DateUtils
import java.util.Date
import java.text.SimpleDateFormat
import scala.xml.XML
import au.org.ala.biocache.FullRecord
import au.org.ala.biocache.Config
import au.org.ala.biocache.DataLoader
import scala.collection.mutable.ListBuffer
import au.org.ala.biocache._

object FlickrLoader extends DataLoader{
    
    def main(args:Array[String]){
        
        var dataResourceUid = ""
        var startDate:Option[Date] = None
        var endDate:Option[Date] = None
        var overwriteImages = false
        
        val parser = new OptionParser("load flickr resource") {
            arg("<data resource UID>", "The UID of the data resource to load", {v: String => dataResourceUid = v})
            opt("s", "startDate", "start date to harvest from in yyyy-MM-dd format", {v:String => startDate = Some(DateUtils.parseDate(v, Array("yyyy-MM-dd"))) } )
            opt("e", "endDate", "end date in yyyy-MM-dd format", {v:String => endDate = Some(DateUtils.parseDate(v, Array("yyyy-MM-dd"))) } )
            booleanOpt("o", "overwrite", "overwrite images", {v:Boolean => overwriteImages = v } )
        }
        if(parser.parse(args)){
            val l = new FlickrLoader
        	l.load(dataResourceUid,startDate,endDate,overwriteImages)
        }
    }
}

class FlickrLoader extends DataLoader {
    
    def load(dataResourceUid:String){
        load(dataResourceUid, None, None)
    }
    
    def load(dataResourceUid:String, suppliedStartDate:Option[Date], suppliedEndDate:Option[Date], overwriteImages:Boolean = false){
        
        val (protocol, url, uniqueTerms, params, customParams) = retrieveConnectionParameters(dataResourceUid)
        val keywords = params.getOrElse("keywords", "").split(",").map(keyword => keyword.trim.replaceAll(" ","").toLowerCase).toList
        
        val endDate = suppliedEndDate.getOrElse( {
            params.get("end_date") match {
                case Some(v) => DateUtils.parseDate(v, Array("yyyy-MM-dd"))
                case None => new Date()
            }
        })
        val startDate = suppliedStartDate.getOrElse( {
            params.get("start_date") match {
                case Some(v) => DateUtils.parseDate(v, Array("yyyy-MM-dd"))
                case None => DateUtils.parseDate("2004-01-01", Array("yyyy-MM-dd"))
            }
        })

        var currentStartDate = DateUtils.addDays(endDate, -1)
        var currentEndDate = endDate
        
        val df =new SimpleDateFormat("yyyy-MM-dd")
        
        // page through the images month-by-month
        while (currentStartDate.after(startDate)){
            
            println("Harvesting time period: "+df.format(currentStartDate)+" to "+df.format(currentEndDate));
            try{
	            val photoIds = getPhotoIdsForDateRange(params, currentStartDate, currentEndDate);
	            photoIds.foreach(photoId => {
	                
	                try {
		                //persist the occurrence with image metadata
		                val (photoPageUrl, imageUrl, fr, tags) = processPhoto(params, photoId)
		                if(isOfInterest(tags, keywords)){
		                    load(dataResourceUid, fr, List(photoPageUrl) )
		                    val (filePath, exists) = MediaStore.exists(fr.uuid, dataResourceUid, imageUrl)
		                    if(overwriteImages || !exists){
		                    	MediaStore.save(fr.uuid, dataResourceUid, imageUrl)
		                    } else {
		                        logger.info("Image URL: " + imageUrl + " already saved to: " + filePath)
		                    }
		                    
		                    fr.occurrence.associatedMedia = filePath
		                    Config.occurrenceDAO.updateOccurrence(fr.rowKey, fr, Versions.RAW)
		                    println(fr.rowKey)
		                }
	                } catch {
	                    case e:Exception => e.printStackTrace
	                }
	            })
            } catch {
            	case e:Exception => e.printStackTrace
            }
            currentEndDate = currentStartDate
            currentStartDate = DateUtils.addDays(currentEndDate, -1)
        }
    }
    
    def isOfInterest(tags:List[String], keywords:List[String]) : Boolean = {
        //match on keywords
        val index = tags.indexWhere(
           tag => {
               val indexOfKeyword = keywords.indexWhere(keyword => tag.equalsIgnoreCase(keyword))
               indexOfKeyword >= 0
           }
        )
        index >= 0
    }
    
    def processPhoto(connectParams:Map[String,String], photoId:String) : (String,String,FullRecord,List[String]) = {
        
        //create an occurrence record
        val fr = new FullRecord     
        val infoPage:String = makeGetInfoUrl(connectParams, photoId)
        //println(url)
        
        val xml = XML.loadString(scala.io.Source.fromURL(infoPage).mkString)
        val listBuffer = new ListBuffer[String]
        //get the tags
        val tags = (xml \\ "tag").foreach(el => {
            val raw = el.attribute("raw")
            //println(raw.get.text)
            if(!raw.isEmpty && raw.get.text.contains("=")){
                //is it a machine tag - remove namespace
                val (tagName, tagValue) = parseMachineTag(raw.get.text.trim)
                listBuffer += tagValue
                
                tagName match {
                    case Some(term) => fr.setNestedProperty(term, tagValue)
                    case None => logger.debug("unmatched : " + raw.get.text.trim)
                }
            }
        })
        
        val htmlPhotoPage = (xml \\ "url")(0).text.toString   //val photoPageUrl
        fr.occurrence.occurrenceID = htmlPhotoPage
        //use occurrenceDetails to store URI back to source - http://rs.tdwg.org/dwc/terms/#occurrenceDetails
        fr.occurrence.occurrenceDetails = htmlPhotoPage
        
        val title = (xml \\ "title")(0).text.toString
        val description = (xml \\ "description")(0).text.toString
        val (username,realname,location) = {
            val ownerElem = (xml \\ "owner")(0)
            (ownerElem.attribute("username"), ownerElem.attribute("realname"), ownerElem.attribute("location") )
        }
        
        val photoElem = (xml \\ "photo")(0)
        val farmId = photoElem.attribute("farm").get
        val serverId = photoElem.attribute("server").get
        val photoSecret = photoElem.attribute("secret").get
        val originalformat = photoElem.attribute("originalformat").getOrElse("jpg")
        val photoImageUrl = "http://farm" + farmId + ".static.flickr.com/"+ serverId + "/" + photoId + "_" + photoSecret + "." + originalformat

        (fr.occurrence.occurrenceID, photoImageUrl, fr,listBuffer.toList)
    }
    
    def parseMachineTag(tag:String): (Option[String], String) = {
        //println(tag)
        if(tag.contains("=")){
            val (name, value) = {
                val parts = tag.split("=")
                parts.length match {
                    case 2 => (parts(0).replaceAll(" ","").trim.toLowerCase, parts(1).trim)
                    case _ => ("", parts.last)
                }
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
    
    def makeSearchUrl(connectParams:Map[String,String], minUpdateDate:String, maxUpdateDate:String, pageNumber:Int) =  connectParams("url") + 
         "?method=flickr.photos.search" + 
         "&content_type=" +  connectParams("content_type") + 
         "&group_id=" + connectParams("group_id") +
         "&privacy_filter=" +  connectParams("privacy_filter") + 
         "&min_upload_date=" + minUpdateDate +    //startDate
         "&max_upload_date=" + maxUpdateDate +    //endDate
         "&api_key=" + connectParams("api_key") + 
         "&per_page=" + connectParams("per_page") +
         "&page=" + pageNumber 
    
    def makeGetInfoUrl(connectParams:Map[String,String], photoId:String) : String =  connectParams("url") + 
         "?method=flickr.photos.getInfo" +  
         "&api_key=" + connectParams("api_key") +
         "&photo_id=" + photoId
         
    def getPhotoIdsForDateRange(connectParams:Map[String,String], startDate:Date, endDate:Date) : List[String] = {

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
