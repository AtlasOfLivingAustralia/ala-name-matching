package au.org.ala.biocache
import scala.io.Source
import scala.util.parsing.json.JSON
import org.gbif.dwc.terms.TermFactory
import org.gbif.dwc.terms.ConceptTerm

trait DataLoader {
    
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
        Config.occurrenceDAO.addRawOccurrenceBatch(Array(fr))
        true
    }
}