package au.org.ala.biocache
import scala.io.Source
import scala.util.parsing.json.JSON
import org.gbif.dwc.terms.TermFactory
import org.gbif.dwc.terms.ConceptTerm
import java.io.File
import org.apache.commons.io.FileUtils
import java.io.FileOutputStream
import au.org.ala.util.FileHelper
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import scalaj.http.Http


/**
 * A trait with utility code for loading data
 */
trait DataLoader {
    
    import FileHelper._
    val user ="harvest services"
    val api_key="Venezuela"
    val logger = LoggerFactory.getLogger("DataLoader")
    val temporaryFileStore = "/data/biocache-load/"
    val registryUrl = "http://collections.ala.org.au/ws/dataResource/"
    val pm = Config.persistenceManager
    val loadTime = org.apache.commons.lang.time.DateFormatUtils.format(new java.util.Date, "yyyy-MM-dd'T'HH:mm:ss'Z'")
    
    def retrieveConnectionParameters(resourceUid: String) : (String, String, List[String], Map[String,String]) = {

      //full document
      val json = Source.fromURL(registryUrl + resourceUid + ".json").getLines.mkString
      val map = JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
      
      //connection details
      val connectionParameters = map("connectionParameters").asInstanceOf[Map[String,String]]//JSON.parseFull(map("connectionParameters")).asInstanceOf[Map[String,String]]//map("connectionParameters").asInstanceOf[String]).get.asInstanceOf[Map[String, String]]
      val protocol = connectionParameters("protocol")
      val url = connectionParameters("url")
      val uniqueTerms = connectionParameters.getOrElse("termsForUniqueKey", List[String]()).asInstanceOf[List[String]]
      
      //optional config params for custom services
      val params = protocol.toLowerCase match {
          case "customwebservice" => JSON.parseFull(connectionParameters.getOrElse("params", "")).getOrElse(Map[String,String]()).asInstanceOf[Map[String, String]]
          case _ => Map[String,String]()
      }
      (protocol, url, uniqueTerms, params)
    }
    
    def mapConceptTerms(terms: List[String]): List[org.gbif.dwc.terms.ConceptTerm] = {
      val termFactory = new TermFactory  
      terms.map(term => termFactory.findTerm(term))
    }
    
    def load(dataResourceUid:String, fr:FullRecord, identifyingTerms:List[String]) : Boolean = {
        
        //the details of how to construct the UniqueID belong in the Collectory
        val uniqueID = identifyingTerms.isEmpty match {
        	case true => None
        	case false => Some((List(dataResourceUid) ::: identifyingTerms).mkString("|"))
        }

        //lookup the column
        val recordUuid = uniqueID match {
            case Some(value) => Config.occurrenceDAO.createOrRetrieveUuid(value)
            case None => Config.occurrenceDAO.createUuid
        }
        
        //add the full record
        fr.uuid = recordUuid
        //The row key is the uniqueID for the record. This will always start with the dataResourceUid
        fr.rowKey = if(uniqueID.isEmpty) dataResourceUid +"|"+recordUuid else uniqueID.get
        //The last load time
        fr.lastLoadTime = loadTime
        fr.attribution.dataResourceUid = dataResourceUid
        Config.occurrenceDAO.addRawOccurrenceBatch(Array(fr))
        true
    }
    
    def downloadArchive(url:String, resourceUid:String) : String = {
        val tmpStore = new File(temporaryFileStore)
        if(!tmpStore.exists){
        	FileUtils.forceMkdir(tmpStore)
        }

        print("Downloading zip file.....")
        val in = (new java.net.URL(url)).openStream
        val file = new File(temporaryFileStore + resourceUid + ".zip")
        val out = new FileOutputStream(file)
        val buffer: Array[Byte] = new Array[Byte](1024)
        var numRead = 0
        while ({ numRead = in.read(buffer); numRead != -1 }) {
            out.write(buffer, 0, numRead)
            out.flush
        }
        printf("Downloaded. File size: %skB\n", file.length / 1024)

        out.flush
        in.close
        out.close

        //extract the file
        file.extractZip

        val fileName = FilenameUtils.removeExtension(file.getAbsolutePath)
        println("Archive extracted to directory: " + fileName)
        fileName
    }

    /**
     * Calls the collectory webservice to update the last loaded time for a data resource
     */
    def updateLastChecked(resourceUid:String) :Boolean ={
        try{
          //set the last check time for the supplied resourceUid
          
          val map = Map("user"->user, "api_key"->api_key, "lastChecked" ->loadTime)
          //turn the map of values into JSON representation
          val data = map.map(pair => "\""+pair._1 +"\":\"" +pair._2 +"\"").mkString("{",",", "}")
          //"http://woodfired.ala.org.au:8080/Collectory/ws/dataResource/"
          println(Http.postData(registryUrl+resourceUid,data).header("content-type", "application/json").responseCode)
          
          true
          
        }
        catch{
            case e:Exception => e.printStackTrace();false
        }
    }
}