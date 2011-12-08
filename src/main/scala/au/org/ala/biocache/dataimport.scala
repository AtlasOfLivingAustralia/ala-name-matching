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
    
    def retrieveConnectionParameters(resourceUid: String) : (String, List[String], List[String], Map[String,String], Map[String,String]) = {

      //full document
      val json = Source.fromURL(registryUrl + resourceUid + ".json").getLines.mkString
      val map = JSON.parseFull(json).get.asInstanceOf[Map[String, String]]

      //connection details
      val connectionParameters = map("connectionParameters").asInstanceOf[Map[String,AnyRef]]
      val protocol = connectionParameters("protocol").asInstanceOf[String]
      val urlsObject = connectionParameters.getOrElse("url", List[String]())
      val urls = {
        if(urlsObject.isInstanceOf[List[String]]){
          urlsObject
        } else {
          val singleValue = connectionParameters("url").asInstanceOf[String]
          List(singleValue)
        }
      }

      val uniqueTerms = connectionParameters.getOrElse("termsForUniqueKey", List[String]()).asInstanceOf[List[String]]
      
      //optional config params for custom services
      val customParams = protocol.asInstanceOf[String].toLowerCase match {
          case "customwebservice" => {
            val params = connectionParameters.getOrElse("params", "").asInstanceOf[String]
            JSON.parseFull(params).getOrElse(Map[String,String]()).asInstanceOf[Map[String, String]]
          }
          case _ => Map[String,String]()
      }
      (protocol, urls.asInstanceOf[List[String]], uniqueTerms, map("connectionParameters").asInstanceOf[Map[String,String]], customParams)
    }
    
    def mapConceptTerms(terms: List[String]): List[org.gbif.dwc.terms.ConceptTerm] = {
      val termFactory = new TermFactory  
      terms.map(term => termFactory.findTerm(term))
    }

    def exists(dataResourceUid:String, identifyingTerms:List[String]) : Boolean = {
      !Config.occurrenceDAO.getUUIDForUniqueID(createUniqueID(dataResourceUid, identifyingTerms)).isEmpty
    }

    private def createUniqueID(dataResourceUid:String,identifyingTerms:List[String]) : String = {
      (List(dataResourceUid) ::: identifyingTerms).mkString("|")
    }

    def load(dataResourceUid:String, fr:FullRecord, identifyingTerms:List[String]) : Boolean = {
       load(dataResourceUid:String, fr:FullRecord, identifyingTerms:List[String], true)
    }

    def load(dataResourceUid:String, fr:FullRecord, identifyingTerms:List[String], updateLastModified:Boolean) : Boolean = {
        
        //the details of how to construct the UniqueID belong in the Collectory
        val uniqueID = identifyingTerms.isEmpty match {
        	case true => None
        	case false => Some(createUniqueID(dataResourceUid,identifyingTerms))
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
        if(updateLastModified){
          fr.lastModifiedTime = loadTime
        }
        fr.attribution.dataResourceUid = dataResourceUid

        Config.occurrenceDAO.addRawOccurrenceBatch(Array(fr))
        true
    }
    
    def downloadArchive(url:String, resourceUid:String) : String = {
        val tmpStore = new File(temporaryFileStore)
        if(!tmpStore.exists){
        	FileUtils.forceMkdir(tmpStore)
        }

        print("Downloading zip file from "+ url)
        val in = (new java.net.URL(url)).openStream
        val (file, isZipped) = {
          if (url.endsWith(".zip")){
            val f = new File(temporaryFileStore + resourceUid + ".zip")
            f.createNewFile()
            (f, true)
          } else {
            val f = new File(temporaryFileStore + resourceUid + File.separator + resourceUid +".csv")
            println("creating file: " + f.getAbsolutePath)
            FileUtils.forceMkdir(f.getParentFile())
            f.createNewFile()
            (f, false)
          }
        }
        val out = new FileOutputStream(file)
        val buffer: Array[Byte] = new Array[Byte](1024)
        var numRead = 0
        while ({ numRead = in.read(buffer); numRead != -1 }) {
          out.write(buffer, 0, numRead)
          out.flush
        }
        printf("\nDownloaded. File size: ", file.length / 1024 +"kB, " + file.getAbsolutePath +", is zipped: " + isZipped+"\n")

        out.flush
        in.close
        out.close

        //extract the file
        if (isZipped){
          file.extractZip
          val fileName = FilenameUtils.removeExtension(file.getAbsolutePath)
          println("Archive extracted to directory: " + fileName)
          fileName
        } else {
          file.getParentFile.getAbsolutePath
        }
    }

    /**
     * Calls the collectory webservice to update the last loaded time for a data resource
     */
    def updateLastChecked(resourceUid:String) :Boolean ={
        try {
          //set the last check time for the supplied resourceUid
          val map = Map("user"-> user, "api_key"-> api_key, "lastChecked" ->loadTime)
          //turn the map of values into JSON representation
          val data = map.map(pair => "\""+pair._1 +"\":\"" +pair._2 +"\"").mkString("{",",", "}")
          //"http://woodfired.ala.org.au:8080/Collectory/ws/dataResource/"
          println(Http.postData(registryUrl+resourceUid,data).header("content-type", "application/json").responseCode)
          true
        } catch {
          case e:Exception => e.printStackTrace();false
        }
    }
}