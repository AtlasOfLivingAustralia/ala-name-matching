package au.org.ala.biocache
/**
 * These classes represent caches of data sourced from other components
 * maintained within the biocache for performance reasons. These
 * components
 */
import au.org.ala.data.model.LinnaeanRankClassification
import au.org.ala.util.ReflectBean
import au.org.ala.checklist.lucene.model.{NameSearchResult,MetricsResultDTO}
import au.org.ala.checklist.lucene.{CBIndexSearch, HomonymException, SearchResultException}
import scala.io.Source
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.util.zip.GZIPInputStream
import java.io.BufferedReader
import scala.xml.XML
import java.io.InputStreamReader
import java.net.URL
import scalaj.http.Http
import collection.{mutable, JavaConversions}
import org.ala.layers.client.Client
import collection.mutable.{ArrayBuffer, HashMap}
import java.text.MessageFormat

//import org.apache.zookeeper.ZooKeeper.States
import scala.util.parsing.json.JSON

/**
 * A DAO for accessing classification information in the cache. If the
 * value does not exist in the cache the name matching API is called.
 *
 * The cache will store a classification object for names that match. If the
 * name causes a homonym exception or is not found the ErrorCode is stored.
 *
 * @author Natasha Carter
 */
object ClassificationDAO {

  val logger = LoggerFactory.getLogger("CLassificationDAO")
  private val lru = new org.apache.commons.collections.map.LRUMap(10000)
  private val lock : AnyRef = new Object()
  private val nameIndex = Config.nameIndex

  def stripStrayQuotes(str:String) : String  = {
    if (str == null){
      null
    } else {
      var normalised = str
      if(normalised.startsWith("'") || normalised.startsWith("\"")) normalised = normalised.drop(1)
      if(normalised.endsWith("'") || normalised.endsWith("\"")) normalised = normalised.dropRight(1)
      //if((normalised.endsWith("'") || normalised.endsWith("\"")) && (normalised.startsWith("'") || normalised.startsWith("\"") || normalised.)) normalised = normalised.dropRight(1)
      //if((normalised.endsWith("'") || normalised.endsWith("\"")) && (normalised.startsWith("'") || normalised.startsWith("\"") )) normalised = normalised.dropRight(1)
      normalised
    }
  }
  
  /**
   * Uses a LRU cache
   */
  def getByHashLRU(cl:Classification) : Option[MetricsResultDTO] = {
    //use the vernacular name to lookup if there if there is no scientific name or higher level classification
    //we don't really trust vernacular name matches thus only use as a last resort
    val hash = { if(cl.vernacularName==null || cl.scientificName != null || cl.specificEpithet != null || cl.infraspecificEpithet != null 
            || cl.kingdom != null || cl.phylum != null || cl.classs != null || cl.order != null || cl.family !=null  || cl.genus!=null)
        Array(cl.kingdom,cl.phylum,cl.classs,cl.order,cl.family,cl.genus,cl.species,cl.specificEpithet,
            cl.subspecies,cl.infraspecificEpithet,cl.scientificName).reduceLeft(_+"|"+_)
              else cl.vernacularName
    }

    if (cl.scientificName == null){
      if (cl.subspecies != null) cl.scientificName = cl.subspecies
      else if (cl.specificEpithet != null && cl.genus != null && cl.infraspecificEpithet!=null) cl.scientificName = cl.genus + " " + cl.specificEpithet + " " +cl.infraspecificEpithet
      else if (cl.specificEpithet != null && cl.genus != null) cl.scientificName = cl.genus + " " + cl.specificEpithet
      else if (cl.species != null) cl.scientificName = cl.species
      else if (cl.genus != null) cl.scientificName = cl.genus
      else if (cl.family != null) cl.scientificName = cl.family
      else if (cl.classs != null) cl.scientificName = cl.classs
      else if (cl.order != null) cl.scientificName = cl.order
      else if (cl.phylum != null) cl.scientificName = cl.phylum
      else if (cl.kingdom != null) cl.scientificName = cl.kingdom
    }

    val cachedObject = lock.synchronized { lru.get(hash) }

    if(cachedObject!=null){
       cachedObject.asInstanceOf[Option[MetricsResultDTO]]
    } else {
      //search for a scientific name if values for the classification were provided otherwise search for a common name
      val idnsr = if(cl.taxonConceptID != null)  nameIndex.searchForRecordByLsid(cl.taxonConceptID) else null
      
      var resultMetric = {
        try {
          if(idnsr != null){
              val metric = new MetricsResultDTO
              metric.setResult(idnsr)
              metric
          }
          //if (cl.taxonConceptID != null) nameIndex.searchForRecordByLsid(cl.taxonConceptID)
          else if(hash.contains("|")) nameIndex.searchForRecordMetrics(new LinnaeanRankClassification(
            stripStrayQuotes(cl.kingdom),
            stripStrayQuotes(cl.phylum),
            stripStrayQuotes(cl.classs),
            stripStrayQuotes(cl.order),
            stripStrayQuotes(cl.family),
            stripStrayQuotes(cl.genus),
            stripStrayQuotes(cl.species),
            stripStrayQuotes(cl.specificEpithet),
            stripStrayQuotes(cl.subspecies),
            stripStrayQuotes(cl.infraspecificEpithet),
            stripStrayQuotes(cl.scientificName)),
            true,
            true) //fuzzy matching is enabled because we have taxonomic hints to help prevent dodgy matches
          else null
        } catch {
          case e:Exception => {
            logger.debug(e.getMessage + ", hash =  " + hash, e)
          }; null
        }
      }

      if(resultMetric == null) {
        val cnsr = nameIndex.searchForCommonName(cl.getVernacularName)
        if(cnsr != null){
          resultMetric = new MetricsResultDTO
          resultMetric.setResult(cnsr);
        }
      }

      if(resultMetric != null && resultMetric.getResult() != null){
          
          //handle the case where the species is a synonym this is a temporary fix should probably go in ala-name-matching
          var result:Option[MetricsResultDTO] = if(resultMetric.getResult.isSynonym){
                val ansr =nameIndex.searchForRecordByLsid(resultMetric.getResult.getAcceptedLsid)
                if(ansr != null){
                   //change the name match metric for a synonym
                  ansr.setMatchType(resultMetric.getResult.getMatchType())
                  resultMetric.setResult(ansr)
                  Some(resultMetric)
                } else{ 
                  None
                }
              } else Some(resultMetric)
          //}
          if(result.isDefined){
              //change the name match metric for a synonym 
//              if(nsr.isSynonym())
//                result.get.setMatchType(nsr.getMatchType())
              //update the subspecies or below value if necessary
              val rank = result.get.getResult.getRank
              if(rank != null && rank.getId() >7000 && rank.getId <9999){
                result.get.getResult.getRankClassification.setSubspecies(result.get.getResult.getRankClassification.getScientificName())
              }
          } else {
            logger.debug("Unable to locate accepted concept for synonym " + resultMetric.getResult + ". Attempting a higher level match")
            if((cl.kingdom != null || cl.phylum != null || cl.classs != null || cl.order != null || cl.family != null || cl.genus != null) && (cl.getScientificName() != null || cl.getSpecies() != null || cl.getSpecificEpithet() != null || cl.getInfraspecificEpithet() != null)){
                val newcl = cl.clone()
                newcl.setScientificName(null)
                newcl.setInfraspecificEpithet(null)
                newcl.setSpecificEpithet(null)
                newcl.setSpecies(null)
                updateClassificationRemovingMissingSynonym(newcl, resultMetric.getResult())
                result = getByHashLRU(newcl)
            } else {
              logger.warn("Recursively unable to locate a synonym for " + cl)
            }
        }
        lock.synchronized { lru.put(hash, result) }
        result
      } else {
        val result = if(resultMetric != null) Some(resultMetric) else None
        lock.synchronized { lru.put(hash, result) }
        result
      }
    }
  }

  def updateClassificationRemovingMissingSynonym(newcl:Classification, result:NameSearchResult){
    val sciName = result.getRankClassification().getScientificName()
    if(newcl.genus == sciName)
      newcl.genus = null
    if(newcl.family == sciName)
      newcl.family = null
    if(newcl.order == sciName)
      newcl.order = null
    if(newcl.classs == sciName)
      newcl.classs = null
    if(newcl.phylum == sciName)
      newcl.phylum = null
  }
}

/**
 * A cache that stores the species lists for a taxon lsid.
 *
 * It will only store the lists that are configured via the specieslists property.
 *
 */
object TaxonSpeciesListDAO {
  val logger = LoggerFactory.getLogger("TaxonSpeciesListDAO")
  def listToolUrl = "http://lists.ala.org.au/ws/species/"
  val guidUrl="http://lists.ala.org.au/ws/speciesList/{0}/taxa"
  private val lru = new org.apache.commons.collections.map.LRUMap(100000)


  private val lock : AnyRef = new Object()
  private val validLists = Config.getProperty("specieslists","").split(",")
  private val prefixFields = Config.getProperty("slPrefix","stateProvince").split(",").toSet
  private val indexValues = Config.getProperty("slIndexKeys","category").split(",").toSet
  private val loadSpeciesLists=Config.getProperty("includeSpeciesLists","false").equals("true")

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

/**
 * A DAO for accessing taxon profile information by GUID.
 * 
 * This should provide an abstraction layer, that (eventually) handles
 * "timeToLive" style functionality that invalidates values in the cache
 * and retrieves the latest values.
 */
object TaxonProfileDAO {

  private val columnFamily = "taxon"
  private val lru = new org.apache.commons.collections.map.LRUMap(10000)
  private val lock : AnyRef = new Object()
  private val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]

  /**
   * Retrieve the profile by the taxon concept's GUID
   */
  def createTaxonProfile(map: Option[Map[String, String]]): TaxonProfile = {
      var taxonProfile = new TaxonProfile
      map.get.foreach(keyValue => {
          keyValue._1 match {
              case "guid" => taxonProfile.guid = keyValue._2
              case "scientificName" => taxonProfile.scientificName = keyValue._2
              case "commonName" => taxonProfile.commonName = keyValue._2
              case "rankString" => taxonProfile.rankString = keyValue._2
              case "habitats" => {
                  if (keyValue._2 != null && keyValue._2.size > 0) {
                      taxonProfile.habitats = keyValue._2.split(",")
                  }
              }
              case "left" => taxonProfile.left = keyValue._2
              case "right" => taxonProfile.right = keyValue._2
              case "sensitive" => {
                  if (keyValue._2 != null && keyValue._2.size > 0) {
                      taxonProfile.sensitive = Json.toArray(keyValue._2, classOf[SensitiveSpecies].asInstanceOf[java.lang.Class[AnyRef]]).asInstanceOf[Array[SensitiveSpecies]]
                  }
              }
              case "conservation" => {
                  if(keyValue._2 != null && keyValue._2.size >0){
                      taxonProfile.conservation = Json.toArray(keyValue._2, classOf[ConservationSpecies].asInstanceOf[java.lang.Class[AnyRef]]).asInstanceOf[Array[ConservationSpecies]]
                     
                  }
              }
              case _ => //ignore
          }
      })
      taxonProfile
  }

  def getByGuid(guid:String) : Option[TaxonProfile] = {

    if(guid==null || guid.isEmpty) return None

    val taxonProfile = {

        val cachedObject = lock.synchronized { lru.get(guid) }
        if(cachedObject==null){
            val map = persistenceManager.get(guid,columnFamily)
            if(!map.isEmpty){
              val result = Some(createTaxonProfile(map))
              lock.synchronized { lru.put(guid,result) }
              result
            } else {
              lock.synchronized { lru.put(guid,None) }
              None
            }
        } else {
          cachedObject
        }
    }
    taxonProfile.asInstanceOf[Option[TaxonProfile]]
  }

  /**
   * Persist the taxon profile.
   */
  def add(taxonProfile:TaxonProfile) {

      var properties = scala.collection.mutable.Map[String,String]()
      properties.put("guid", taxonProfile.guid)
      properties.put("scientificName", taxonProfile.scientificName)
      properties.put("commonName", taxonProfile.commonName)
      properties.put("rankString", taxonProfile.rankString)
      if(taxonProfile.habitats!=null && taxonProfile.habitats.size>0){
        val habitatString = taxonProfile.habitats.reduceLeft(_+","+_)
        properties.put("habitats", habitatString)
      }
      properties.put("left", taxonProfile.left)
      properties.put("right", taxonProfile.right)
      if(taxonProfile.sensitive != null && taxonProfile.sensitive.size >0){
        properties.put("sensitive", Json.toJSON(taxonProfile.sensitive.asInstanceOf[Array[AnyRef]]))
      }
      if(taxonProfile.conservation != null && taxonProfile.conservation.size >0){
          properties.put("conservation", Json.toJSON(taxonProfile.conservation.asInstanceOf[Array[AnyRef]]))
      }
      persistenceManager.put(taxonProfile.guid, columnFamily, properties.toMap)
  }
}

/**
 * A DAO for attribution data. The source of this data should be the collectory.
 *
 * There is probably only a couple of hundred
 */
object AttributionDAO {

  import ReflectBean._
  import JavaConversions._
  private val columnFamily = "attr"
  //can't use a scala hashmap because missing keys return None not null...
  private val lru = new org.apache.commons.collections.map.LRUMap(10000)//new HashMap[String, Option[Attribution]]
  private val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]
  //A mapping of the ws json properties to attribution properties
  private val wsPropertyMap = Map("name"->"collectionName", "uid"->"collectionUid", "taxonomyCoverageHints"->"taxonomicHints", "institutionUid"->"institutionUid", "institution"->"institutionName")
  private val lock : AnyRef = new Object()
  val logger = LoggerFactory.getLogger("AttributionDAO")

  /**
   * Persist the attribution information.
   */
  def add(institutionCode:String, collectionCode:String, attribution:Attribution){
    val guid = institutionCode.toUpperCase +"|"+collectionCode.toUpperCase
    val map = FullRecordMapper.mapObjectToProperties(attribution)
    persistenceManager.put(guid,columnFamily,map)
  }

  /**
   * Obtain the data resource attribution information from the cache
   *
   * TODO: Probably should cache these in persistence manager so that they are available if the WS is down
   *
   */
  def getDataResourceByUid(uid:String) : Option[Attribution] ={
    
    val cachedObject = lru.get(uid)
      if(cachedObject!=null){
        cachedObject.asInstanceOf[Option[Attribution]]
      } else {
        
        val att =getDataResourceFromWS(uid)
        //cache the data resource info
        lru.put(uid, att)
        att
      }
  }
  
   def getDataProviderAsMap(value:String):Map[String,String]={
     val json = Source.fromURL(Config.registryURL+"/dataProvider/" + value + ".json").getLines.mkString
     JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
   }
  
   def getDataResourceAsMap(value:String):Map[String,String]={
     val json = Source.fromURL(Config.registryURL+"/dataResource/" + value + ".json").getLines.mkString
     JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
   }

   def getDataResourceFromWS(value:String):Option[Attribution]={
    try {
      if(value == null) return None

      val attribution = new Attribution
      logger.info("Calling web service for " + value)

      val wscontent = WebServiceLoader.getWSStringContent(Config.registryURL+"/dataResource/"+value+".json")

      val wsmap = Json.toMap(wscontent)

      val name = wsmap.getOrElse("name","").asInstanceOf[String]

      val provenance = wsmap.getOrElse("provenance","").asInstanceOf[String]

      val hints = wsmap.getOrElse("taxonomyCoverageHints",null)
      val ahints = {
        if(hints != null){
          hints.asInstanceOf[java.util.ArrayList[Object]].toArray.map((o:Object) => {
            o.toString().replace("=",":").replace("{","").replace("}","")
          })
        } else {
          null
        }
      }

      //the hub membership
      val hub = wsmap.getOrElse("hubMembership", null)
      val ahub = {
        if(hub !=  null){
          hub.asInstanceOf[java.util.ArrayList[Object]].toArray.map((o:Object) => {
            (o.asInstanceOf[java.util.LinkedHashMap[Object,Object]]).get("uid").toString
          })
        }
        else null
      }

      //data Provider
      val dp = wsmap.getOrElse("provider", null).asInstanceOf[java.util.Map[String,String]]
      val dpname = if(dp != null) dp.get("name") else null
      val dpuid = if(dp != null) dp.get("uid") else null
      val hasColl = wsmap.getOrElse("hasMappedCollections", false).asInstanceOf[Boolean]
      
      //the default DWC terms
      val defaultDwc = wsmap.getOrElse("defaultDarwinCoreValues", null)
      attribution.dataResourceName = name
      attribution.dataProviderUid = dpuid
      attribution.dataProviderName = dpname
      attribution.dataHubUid = ahub
      attribution.taxonomicHints = ahints
      attribution.hasMappedCollections = hasColl
      attribution.provenance = provenance
      if(defaultDwc != null){
        //retrieve the dwc values for the supplied values
          val map = defaultDwc.asInstanceOf[java.util.LinkedHashMap[String,String]]
          val newMap = new java.util.LinkedHashMap[String,String]()
          map.keys.foreach { key:String => {
            val value=map.get(key)
            //get the vocab value
            val v = DwC.matchTerm(key)
            if(v.isDefined && !v.get.canonical.equals(key)){

              //map.remove(key);
              newMap.put(v.get.canonical, value);
            } else {
              newMap.put(key, value)
            }
          }
          attribution.defaultDwcValues = newMap.toMap
        }
      }
      Some(attribution)
    } catch {
      case e:Exception => { logger.error(e.getMessage,e); None }
    }
  }

  /**
   * Retrieve attribution via institution/collection codes.
   * We need to ensure that the cache is large enough to hold all possible values
   * <ol>
   * <li> Check if it is in the local cache</li>
   * <li> Request update from collectory service (cache it locally and in cassandra)</li>
   * <li> If service can not be contacted get the value from cassandra (cache it) </li>
   * </ol>
   */
  def getByCodes(institutionCode:String, collectionCode:String) : Option[Attribution] = {

    if(institutionCode!=null && collectionCode!=null){
      val uuid = institutionCode.toUpperCase+"|"+collectionCode.toUpperCase
      val cachedObject = lru.get(uuid)
      
      if(cachedObject != null){
        cachedObject.asInstanceOf[Option[Attribution]]
      } else {
        
        //lookup the collectory against the WS
        logger.info("Looking up collectory web service for " + uuid)
        val wscontent = WebServiceLoader.getWSStringContent(Config.registryURL+"/lookup/inst/"+URLEncoder.encode(institutionCode)+"/coll/"+URLEncoder.encode(collectionCode)+".json")
        val wsmap = Json.toMap(wscontent)

        if(!wsmap.isEmpty && !wsmap.contains("error")){
          
          //attempt to map the attribution properties from the JSON objects
          val attribution = new Attribution
          //handle the non standard properties
          val hints = wsmap.getOrElse("taxonomyCoverageHints",null)
          
          if(hints != null){
            val ahint = hints.asInstanceOf[java.util.ArrayList[Object]].toArray.map((o:Object)=> o.toString().replace("=",":").replace("{","").replace("}",""));
            attribution.taxonomicHints = ahint
          }

          //update the properties
          FullRecordMapper.mapmapPropertiesToObject(attribution, wsmap - "taxonomyCoverageHints", wsPropertyMap)
          val result = Some(attribution)
          //add it to the caches
          lock.synchronized { lru.put(uuid,result) }
          add(institutionCode, collectionCode, attribution)
          result
          
        } else {
          
          // grab the value from the cache if it exists
          val map = persistenceManager.get(uuid,"attr")
          val result = {
              if(!map.isEmpty){
                val attribution = new Attribution
                FullRecordMapper.mapPropertiesToObject(attribution,map.get)
                Some(attribution)
              } else {
                None
              }
          }
          lock.synchronized { lru.put(uuid,result) }
      
          //lru.put(uuid,result)
          result
        }
      }
    } else {
      None
    }
  }
}

/**
 * DAO for location lookups.
 */
object LocationDAO {

  val logger = LoggerFactory.getLogger("LocationDAO")
  private val columnFamily = "loc"
  private val lock : AnyRef = new Object()
  private val lru = new org.apache.commons.collections.map.LRUMap(10000)
  private val persistenceManager = Config.persistenceManager
  private final val latitudeCol = "lat"
  private final val longitudeCol = "lon"

  /**
   * Add a tag to a location
   */
  def addTagToLocation (latitude:Float, longitude:Float, tagName:String, tagValue:String) {
    val guid = getLatLongKey(latitude, longitude)
    persistenceManager.put(guid, columnFamily, latitudeCol, latitude.toString)
    persistenceManager.put(guid, columnFamily, longitudeCol, longitude.toString)
    persistenceManager.put(guid, columnFamily, tagName, tagValue)
  }

  /**
   * Add a region mapping for this point.
   */
  def addRegionToPoint (latitude:Double, longitude:Double, mapping:Map[String,String]) {
    val guid = getLatLongKey(latitude, longitude)
    var properties = scala.collection.mutable.Map[String,String]()
    properties ++= mapping
    properties.put(latitudeCol, latitude.toString)
    properties.put(longitudeCol, longitude.toString)
    persistenceManager.put(guid, columnFamily, properties.toMap)
  }

  /**
   * Add a region mapping for this point.
   */
  def addRegionToPoint (latitude:String, longitude:String, mapping:Map[String,String]) {
    if (latitude!=null && latitude.trim.length>0 && longitude!=null && longitude.trim.length>0){
      val guid = getLatLongKey(latitude, longitude)
      persistenceManager.put(guid, columnFamily, Map(latitudeCol-> latitude, longitudeCol -> longitude) ++ mapping)
    }
  }

  /**
   * Add a region mapping for this point.
   */
  def addLayerIntersects (latitude:String, longitude:String, contextual:Map[String,String], environmental:Map[String,Float]) {
    if (latitude!=null && latitude.trim.length>0 && longitude!=null && longitude.trim.length>0){
      val guid = getLatLongKey(latitude, longitude)

      val mapBuffer = new HashMap[String, String]
      mapBuffer += (latitudeCol -> latitude)
      mapBuffer += (longitudeCol-> longitude)
      mapBuffer ++= contextual
      mapBuffer ++= environmental.map(x => x._1 -> x._2.toString)

      persistenceManager.put(guid, columnFamily, mapBuffer.toMap)
    }
  }

  private def getLatLongKey(latitude:String, longitude:String) : String = {
    latitude.toFloat.toString.trim + "|" + longitude.toFloat.toString
  }

  private def getLatLongKey(latitude:Float, longitude:Float) : String = {
    latitude.toString.trim + "|" + longitude.toString
  }

  private def getLatLongKey(latitude:Double, longitude:Double) : String = {
    latitude.toString.trim + "|" + longitude.toString
  }

  import JavaConversions._
  /**
   * Get location information for point.
   * For geo spatial requirements we don't want to round the latitude , longitudes 
   */
  def getByLatLon(latitude:String, longitude:String) : Option[(Location, Map[String,String], Map[String,String])] = {

    if (latitude == null || longitude == null || latitude.trim.length == 0 || longitude.trim.length == 0){
      return None
    }

    val uuid = getLatLongKey(latitude, longitude)

    val cachedObject = lock.synchronized { lru.get(uuid) }

    if(cachedObject!=null){
        cachedObject.asInstanceOf[Option[(Location, Map[String, String], Map[String, String])]]
    } else {
        val map = persistenceManager.get(uuid,columnFamily)
        map match {
          case Some(map) => {
            val location = new Location
            location.decimalLatitude = latitude
            location.decimalLongitude = longitude

            //map this to sensible values we are used to
            val stateProvinceValue = map.getOrElse("cl927", null)
            if (stateProvinceValue != null & stateProvinceValue != ""){
              StateProvinces.matchTerm(stateProvinceValue) match {
                case Some(term) => location.stateProvince = term.canonical
                case None => {
                  /*do nothing for now */
                  logger.warn("Unrecognised state province value retrieved from layer cl927: " + stateProvinceValue)
                }
              }
            }
            location.ibra = map.getOrElse("cl20", null)
            location.imcra = map.getOrElse("cl21", null)
            location.country = map.getOrElse("cl932", null) //NC 20130322 - this is the new layer that supersedes cl922
            location.lga = map.getOrElse("cl23", null)
            
            //if the country is null but the stateProvince has a value we can assume that it is an Australian point
            if(location.country == null && location.stateProvince != null)
                location.country = "Australia"

            val el = map.filter(x => x._1.startsWith("el"))
            val cl = map.filter(x => x._1.startsWith("cl"))

            val returnValue = Some((location, el, cl))

            lock.synchronized {lru.put(uuid,returnValue)}

            returnValue
          }
          case None => {
            //do a layer lookup???
            logger.debug("Performing a layer lookup for [" + latitude + "," + longitude +"]")
            if(Config.allowLayerLookup){
              val intersection = doLayerIntersectForPoint(latitude, longitude)
              lock.synchronized {
                lru.put(uuid, intersection)
              }
              intersection
            } else {
              None
            }
          }
        }
    }
  }

  def doLayerIntersectForPoint(latitude:String, longitude:String) : Option[(Location, Map[String,String], Map[String,String])] = {

    //do a layers-store lookup
    val layerIntersectDAO = Client.getLayerIntersectDao()
    val points = Array(Array[Double](longitude.toDouble, latitude.toDouble))
    val samples:java.util.ArrayList[String] = layerIntersectDAO.sampling(Config.fieldsToSample, points)

    if(!samples.isEmpty){
      val values:Array[String] = samples.toArray(Array[String]())
      //create a map to store in loc
      val mapBuffer = new HashMap[String, String]
      mapBuffer += (latitudeCol -> latitude)
      mapBuffer += (longitude-> longitude)
      mapBuffer ++= (Config.fieldsToSample zip values).filter(x => x._2.trim.length != 0 && x._2 != "n/a")
      val propertyMap = mapBuffer.toMap
      val guid = getLatLongKey(latitude, longitude)
      persistenceManager.put(guid, columnFamily, propertyMap)
      //now map fields to elements of the model object "Location" and return this
      val location = new Location
      location.decimalLatitude = latitude
      location.decimalLongitude = longitude
      val stateProvinceValue = propertyMap.getOrElse("cl927", null)
      //now do the state vocab substitution
      if (stateProvinceValue != null & stateProvinceValue != ""){
        StateProvinces.matchTerm(stateProvinceValue) match {
          case Some(term) => location.stateProvince = term.canonical
          case None => {
            /*do nothing for now */
            logger.warn("Unrecognised stateprovince value retrieved from layer cl927: " + stateProvinceValue)
          }
        }
      }
      location.ibra = propertyMap.getOrElse("cl20", null)
      location.imcra = propertyMap.getOrElse("cl21", null)
      location.country = propertyMap.getOrElse("cl922", null)

      val el = propertyMap.filter(x => x._1.startsWith("el"))
      val cl = propertyMap.filter(x => x._1.startsWith("cl"))

      Some((location, el, cl))
    } else {
      None
    }
  }
}

/**
 * Provides the tools needed to work with webservices
 */
  object WebServiceLoader{
    def getWSStringContent( url: String ) = {
       try {
        Source.fromURL( url ).mkString
      } catch {
        case e: Exception => ""
      }
    }
  }