package au.org.ala.biocache
/**
 * These classes represent caches of data sourced from other components
 * maintained within the biocache for performance reasons. These
 * components
 */
import au.org.ala.data.model.LinnaeanRankClassification
import au.org.ala.util.ReflectBean
import au.org.ala.checklist.lucene.model.NameSearchResult
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
import scala.collection.JavaConversions
import org.ala.layers.client.Client
import scala.collection.mutable.HashMap
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

  private val columnFamily ="namecl"
 // private val cachedValues = new java.util.Hashtable[LinnaeanRankClassification, Classification]
  private val lru = new org.apache.commons.collections.map.LRUMap(10000)
//  private val lru = new ConcurrentLinkedHashMap.Builder[String, Option[NameSearchResult]]()
//    .maximumWeightedCapacity(10000)
//    .build();

  private val lock : AnyRef = new Object()

  private val nameIndex = Config.nameIndex

  def stripStrayQuotes(str:String) : String  = {
    if (str == null){
      null
    } else {
      var normalised = str
      if(normalised.startsWith("'") || normalised.startsWith("\"")) normalised = normalised.drop(1)
      if(normalised.endsWith("'") || normalised.endsWith("\"")) normalised = normalised.dropRight(1)
      normalised
    }
  }
  
  /**
   * Uses a LRU cache
   */
  def getByHashLRU(cl:Classification) : Option[NameSearchResult] = {
    //use the vernacular name to lookup if there if there is no scientific name or higher level classification
    //we don't really trust vernacular name matches thus only use as a last resort
    val hash = { if(cl.vernacularName==null || cl.scientificName != null || cl.specificEpithet != null || cl.infraspecificEpithet != null 
    				|| cl.kingdom != null || cl.phylum != null || cl.classs != null || cl.order != null || cl.family !=null  || cl.genus!=null)
        Array(cl.kingdom,cl.phylum,cl.classs,cl.order,cl.family,cl.genus,cl.species,cl.specificEpithet,
            cl.subspecies,cl.infraspecificEpithet,cl.scientificName).reduceLeft(_+"|"+_)
              else cl.vernacularName
    }
    
    val cachedObject = lock.synchronized { lru.get(hash) }

    if(cachedObject!=null){
       cachedObject.asInstanceOf[Option[NameSearchResult]]
    } else {
      //search for a scientific name if values for the classification were provided otherwise search for a common name
      val nsr = {
        if (cl.taxonConceptID != null) nameIndex.searchForRecordByLsid(cl.taxonConceptID)
        else if(hash.contains("|")) nameIndex.searchForRecord(new LinnaeanRankClassification(
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
        else nameIndex.searchForCommonName(cl.getVernacularName)
      }

      if(nsr!=null){
          //handle the case where the species is a synonym this is a temporary fix should probably go in ala-name-matching
          val result = if(nsr.isSynonym) Some(nameIndex.searchForRecordByLsid(nsr.getAcceptedLsid))  else Some(nsr)
          //change the name match metric for a synonym 
          if(nsr.isSynonym())
            result.get.setMatchType(nsr.getMatchType())
          lock.synchronized { lru.put(hash, result) }
          result
      } else {
          val result = None
          lock.synchronized { lru.put(hash, result) }
          result
      }
    }
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
//  private val lru = new ConcurrentLinkedHashMap.Builder[String, Option[TaxonProfile]]()
//      .maximumWeightedCapacity(10000)
//      .build();

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
  var collectoryURL ="http://collections.ala.org.au"
  private val columnFamily = "attr"
  //can't use a scala hashap because missing keys return None not null...
  private val lru = new org.apache.commons.collections.map.LRUMap(10000)//new HashMap[String, Option[Attribution]]
  private val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]
  //A mapping of the ws json properties to attribution properties
  private val wsPropertyMap = Map("name"->"collectionName", "uid"->"collectionUid", "taxonomyCoverageHints"->"taxonomicHints", "institutionUid"->"institutionUid", "institution"->"institutionName")
//  private val lru = new ConcurrentLinkedHashMap.Builder[String, Option[Attribution]]()
//      .maximumWeightedCapacity(1000)
//      .build();
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
     val json = Source.fromURL(AttributionDAO.collectoryURL+"/ws/dataProvider/" + value + ".json").getLines.mkString
     JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
   }
  
   def getDataResourceAsMap(value:String):Map[String,String]={
     val json = Source.fromURL(AttributionDAO.collectoryURL+"/ws/dataResource/" + value + ".json").getLines.mkString
     JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
   }

   def getDataResourceFromWS(value:String):Option[Attribution]={
    try{
    val attribution = new Attribution
    println("Calling web service for " + value)

    val wscontent = WebServiceLoader.getWSStringContent(AttributionDAO.collectoryURL+"/ws/dataResource/"+value+".json")

    val wsmap = Json.toMap(wscontent)

    val name = wsmap.getOrElse("name","").asInstanceOf[String]
    
    val provenance = wsmap.getOrElse("provenance","").asInstanceOf[String]

    val hints =wsmap.getOrElse("taxonomyCoverageHints",null)
    val ahints = {
      if(hints != null){
        hints.asInstanceOf[java.util.ArrayList[Object]].toArray.map((o:Object) => {
          o.toString().replace("=",":").replace("{","").replace("}","")
        })
      }
      else null
    }

    //the hubMembership
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
    if(defaultDwc!= null){
      //retrieve the dwc values for the supplied values      
        val map = defaultDwc.asInstanceOf[java.util.LinkedHashMap[String,String]]
        map.keys.foreach{key:String =>{
          //get the vocab value          
          val v = DwC.matchTerm(key)          
          if(v.isDefined && !v.get.canonical.equals(key)){            
            val value=attribution.defaultDwcValues.get(key)            
            map.remove(key);            
            map.put(v.get.canonical, value.get);
          }
          }
        attribution.defaultDwcValues = map.toMap
        }
        
    }
    Some(attribution)
    }
    catch{
      case e:Exception => {e.printStackTrace();None;}
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
      if(cachedObject!=null){
        cachedObject.asInstanceOf[Option[Attribution]]
      } else {
        //lookup the collectory against the WS
        logger.info("Looking up collectory web service for " + uuid)
          val wscontent = WebServiceLoader.getWSStringContent(collectoryURL+"/lookup/inst/"+URLEncoder.encode(institutionCode)+"/coll/"+URLEncoder.encode(collectionCode)+".json")
          val wsmap = Json.toMap(wscontent)

        if(!wsmap.isEmpty && !wsmap.contains("error")){
              //attempt to map the attribution proerties from the JSON objects
              val attribution = new Attribution
              //handle the non standard properties
              val hints =wsmap.getOrElse("taxonomyCoverageHints",null)
              if(hints != null){
                val ahint = hints.asInstanceOf[java.util.ArrayList[Object]].toArray.map((o:Object)=> o.toString().replace("=",":").replace("{","").replace("}",""));
                attribution.taxonomicHints = ahint
              }
              //the hubMembership no longer in collections obtain from the data resource instead
//              val hub = wsmap.getOrElse("hubMembership", null)
//              if(hub !=  null){
//                val ahub = hub.asInstanceOf[java.util.ArrayList[Object]].toArray.map((o:Object)=> (o.asInstanceOf[java.util.LinkedHashMap[Object,Object]]).get("uid").toString)
//                attribution.setDataHubUid(ahub)
//                //println("Hub membership: " +ahub)
//              }
              //update the properties
              FullRecordMapper.mapmapPropertiesToObject(attribution, wsmap - "taxonomyCoverageHints", wsPropertyMap)
              val result = Some(attribution)
              //add it to the caches
              lock.synchronized { lru.put(uuid,result) }
              add(institutionCode, collectionCode, attribution)
              result
          }
          else{
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
                  logger.warn("Unrecognised stateprovince value retrieved from layer cl927: " + stateProvinceValue)
                }
              }
            }
            location.ibra = map.getOrElse("cl20", null)
            location.imcra = map.getOrElse("cl21", null)
            location.country = map.getOrElse("cl922", null)
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