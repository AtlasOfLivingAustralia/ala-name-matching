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

trait DataLoader {
    
    import FileHelper._
    
    val temporaryFileStore = "/data/biocache-load/"
    val pm = Config.persistenceManager
    
    def retrieveConnectionParameters(resourceUid: String) : (String, String, List[String], Map[String,String]) = {
      
      val json = Source.fromURL("http://collections.ala.org.au/ws/dataResource/" + resourceUid + ".json").getLines.mkString

      //full document
      val map = JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
      
      //connection details
      val connectionParameters = JSON.parseFull(map("connectionParameters").asInstanceOf[String]).get.asInstanceOf[Map[String, String]]
      
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
        val uniqueID = {
            //create the unique ID
            if (!identifyingTerms.isEmpty) {
                Some((List(dataResourceUid) ::: identifyingTerms).mkString("|"))
            } else {
                None
            }
        }

        //lookup the column
        val recordUuid = {
            uniqueID match {
                case Some(value) => Config.occurrenceDAO.createOrRetrieveUuid(value)
                case None => Config.occurrenceDAO.createUuid
            }
        }
        
        //add the full record
        fr.uuid = recordUuid
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
}