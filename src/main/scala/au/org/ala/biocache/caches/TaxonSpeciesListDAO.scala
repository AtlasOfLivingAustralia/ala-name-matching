package au.org.ala.biocache.caches

import org.slf4j.LoggerFactory
import au.org.ala.biocache.Config
import scala.collection.mutable.ArrayBuffer
import java.text.MessageFormat
import scala.util.parsing.json.JSON
import au.org.ala.biocache.persistence.PersistenceManager
import au.org.ala.biocache.util.Json
import au.org.ala.biocache.vocab.SpeciesListAcronyms

/**
 * A cache that stores the species lists for a taxon lsid.
 *
 * It will only store the lists that are configured via the specieslists property.
 */
object TaxonSpeciesListDAO {

  val logger = LoggerFactory.getLogger("TaxonSpeciesListDAO")

  def listToolUrl = "http://lists.ala.org.au/ws/species/"

  val guidUrl = "http://lists.ala.org.au/ws/speciesList/{0}/taxa"

  private val lru = new org.apache.commons.collections.map.LRUMap(100000)
  private val lock : AnyRef = new Object()
  private val validLists = Config.getProperty("specieslists","").split(",")
  private val prefixFields = Config.getProperty("slPrefix","stateProvince").split(",").toSet
  private val indexValues = Config.getProperty("slIndexKeys","category").split(",").toSet
  private val loadSpeciesLists = Config.getProperty("includeSpeciesLists","false").equals("true")

  private val columnFamily = "taxon"
  private val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]
  private val LIST = "lists"
  private val LIST_PROPERTIES="listProperties"

  private var speciesListMap:Map[String,(List[String], Map[String,String])] = _

  /**
   * Get the lists for the supplied taxon guid from the persistence manager
   * @param guid
   * @return
   */
  def getListsFromPM(guid:String): (List[String], Map[String,String]) ={
    //get itesm from PM
    val map = persistenceManager.getSelected(guid,columnFamily, Array(LIST,LIST_PROPERTIES))
    if(map.isDefined){
      (Json.toListWithGeneric( map.get.getOrElse(LIST,"[]") , classOf[String]), Json.toStringMap(map.get.getOrElse(LIST_PROPERTIES,"{}")))
    }

    (List[String](), Map[String,String]())
  }

  /**
   * Add the supplied lists details for taxon to the persistence manager
   *
   * @param guid
   * @param lists
   * @param listProperties
   */
  def addToPM(guid:String,lists:List[String], listProperties:Map[String,String]){
    persistenceManager.put(guid, columnFamily, Map(LIST->Json.toJSON(lists), LIST_PROPERTIES -> Json.toJSON(listProperties)))
  }

  /**
   * Updates the lists as configured to indicate which lists and properties are applicable for each guid
   * @return
   */
  def updateLists():Map[String,(List[String], Map[String,String])]={
    logger.info("Updating the lists")
    val newMap = new scala.collection.mutable.HashMap[String ,(List[String], Map[String,String])]()
    val guidsArray = new ArrayBuffer[String]()

    validLists.foreach(v =>{
      //get the guids on the list
      val url = MessageFormat.format(guidUrl, v)
      val response = WebServiceLoader.getWSStringContent(url)
      if(response != ""){
        val list =JSON.parseFull(response).get.asInstanceOf[List[String]]
        guidsArray ++= list.filter(_ != null)
      }
    })
    val guids = guidsArray.toSet
    //now load all the details
    logger.info("The number of distinct species " + guids.size)
    guids.foreach(g =>{
      //get the values from the cache
      newMap(g) = TaxonSpeciesListDAO.getListsForTaxon(g, true)
      //now add the values to the DB
      //TaxonSpeciesListDAO.addToPM(g, lists, props)
    })
    logger.info("Finished creating the map")
    newMap.toMap
  }

  /**
   * Get the list details for the suppplied guid
   * @param conceptLsid
   * @return
   */
  def getCachedListsForTaxon(conceptLsid:String):(List[String], Map[String,String])={
    //check to see if the species list map has been intialised
    if(speciesListMap == null){
      lock.synchronized{
        //only le the first thread actually perform the init by rechecking the null
        if(speciesListMap == null){
          speciesListMap = updateLists()
        }
      }
    }
    //now return the the values
    speciesListMap.getOrElse(conceptLsid, (List(),Map()))
  }

  //retrieves the species list information from the WS
  def getListsForTaxon(conceptLsid:String, forceLoad:Boolean):(List[String], Map[String,String])={
    val response = WebServiceLoader.getWSStringContent(listToolUrl+conceptLsid)
    if(response != ""){
      val list = JSON.parseFull(response).get.asInstanceOf[List[Map[String, AnyRef]]]
      //get a distinct list of lists that the supplied concept belongs to
      val newObject = list.map(_.getOrElse("dataResourceUid","").toString).filter(v => validLists.contains(v)).toSet.toList
      val newMap = collection.mutable.Map[String, String]()
      list.foreach(map => {
        val dr = map.getOrElse("dataResourceUid","").toString
        //only add the KVP items if they come from valid list
        if(validLists.contains(dr)){
          val kvparrayopt = map.get("kvpValues")

          if(kvparrayopt.isDefined){
            val kvparray = kvparrayopt.get.asInstanceOf[List[Map[String,String]]]
            //get the prefix information
            val prefix = kvparray.find(map => prefixFields.contains(map.getOrElse("key","")))
            val stringPrefix = {
              if(prefix.isDefined){
                val value = getValueBasedOnKVP(prefix.get)
                val matchedPrefix = SpeciesListAcronyms.matchTerm(value)
                if(matchedPrefix.isDefined) matchedPrefix.get.canonical.toLowerCase() else value.replaceAll(" " , "_").toLowerCase()
              } else {
                ""
              }
            }

            val filteredList = kvparray.filter(_.values.toSet.intersect(indexValues).size > 0)

            filteredList.foreach( item => {

              //now grab the indexItems
              if(indexValues.contains(item.getOrElse("key",""))){
                val value = getValueBasedOnKVP(item)
                newMap(dr + getEscapedValue(stringPrefix) + getEscapedValue(item.getOrElse("key",""))) = value
              }

            })
          }
        }
      })
      lock.synchronized{lru.put(conceptLsid, (newObject,newMap.toMap))}
      (newObject,newMap.toMap)
    } else{
      (List(),Map())
    }
  }

  def getEscapedValue(value:String) = if(value.trim.size>0) "_" + value else value.trim

  def getValueBasedOnKVP(item:Map[String,String]):String =  if (item.getOrElse("vocabValue",null) != null) item.getOrElse("vocabValue",null) else item.getOrElse("value","")

  def refreshCache = lock.synchronized {
    val tmpMap = updateLists()
    speciesListMap = tmpMap
  }
}
