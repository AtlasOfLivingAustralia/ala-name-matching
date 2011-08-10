package au.org.ala.biocache
/**
 * These classes represent caches of data sourced from other components
 * maintained within the biocache for performance reasons. These
 * components
 */
import au.org.ala.data.model.LinnaeanRankClassification
import au.org.ala.util.ReflectBean
import au.org.ala.checklist.lucene.model.NameSearchResult
//import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import au.org.ala.checklist.lucene.{CBIndexSearch, HomonymException, SearchResultException}
import scala.io.Source
import org.slf4j.LoggerFactory
import java.net.URLEncoder
//import scalaj.http.Http
import java.util.zip.GZIPInputStream
import java.io.BufferedReader
import scala.xml.XML
import java.io.InputStreamReader
import java.net.URL
import scalaj.http.Http
import scala.collection.JavaConversions


/**
 * A DAO for accessing classification information in the cache. If the
 * value does not exist in the cache the name matching API is called.
 *
 * The cache will store a classification object for names that match. If the
 * name causes a homonym exeception or is not found the ErrorCode is stored.
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

  /**
   * Uses a LRU cache
   */
  def getByHashLRU(cl:Classification) : Option[NameSearchResult] = {
    //use the vernacular name to lookup if there if there is no scientifica name, infra/specific epithet or genus

    val hash = { if(cl.vernacularName==null || cl.scientificName != null || cl.specificEpithet != null || cl.infraspecificEpithet != null || cl.genus!=null)
        Array(cl.kingdom,cl.phylum,cl.classs,cl.order,cl.family,cl.genus,cl.species,cl.specificEpithet,
            cl.subspecies,cl.infraspecificEpithet,cl.scientificName).reduceLeft(_+"|"+_)
              else cl.vernacularName
    }
    
    val cachedObject = lock.synchronized { lru.get(hash) }

    if(cachedObject!=null){
       cachedObject.asInstanceOf[Option[NameSearchResult]]
    } else {
        //use the lucene indexes

         
        //search for a scientific name if values for the classification were provided otherwise search for a common name
        val nsr = if(hash.contains("|")) nameIndex.searchForRecord(new LinnaeanRankClassification(
          cl.kingdom,
          cl.phylum,
          cl.classs,
          cl.order,
          cl.family,
          cl.genus,
          cl.species,
          cl.specificEpithet,
          cl.subspecies,
          cl.infraspecificEpithet,
          cl.scientificName), true) else nameIndex.searchForCommonName(cl.getVernacularName)

        if(nsr!=null){
            val result = Some(nsr)
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
    persistenceManager.put(guid,"attr",map)
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

   def getDataResourceFromWS(value:String):Option[Attribution]={
    try{
    val attribution = new Attribution
    println("Calling web service for " + value)

    val wscontent = WebServiceLoader.getWSStringContent(AttributionDAO.collectoryURL+"/ws/dataResource/"+value+".json")

    val wsmap = Json.toMap(wscontent)

    val name = wsmap.getOrElse("name","").toString

    val hints =wsmap.getOrElse("taxonomyCoverageHints",null)
    val ahints ={
                  if(hints != null){
                  hints.asInstanceOf[java.util.ArrayList[Object]].toArray.map((o:Object)=> o.toString().replace("=",":").replace("{","").replace("}",""));
                  
                  }
                  else null
              }
    //the hubMembership
     val hub = wsmap.getOrElse("hubMembership", null)
     val ahub ={
                if(hub !=  null){
                  hub.asInstanceOf[java.util.ArrayList[Object]].toArray.map((o:Object)=> (o.asInstanceOf[java.util.LinkedHashMap[Object,Object]]).get("uid").toString)
                  
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
    

    attribution.setDataResourceName(name)
    attribution.setDataProviderUid(dpuid)
    attribution.setDataProviderName(dpname)
    attribution.setDataHubUid(ahub)
    attribution.setTaxonomicHints(ahints)
    attribution.hasMappedCollections=hasColl
    if(defaultDwc!= null){
        attribution.setDefaultDwcValues(defaultDwc.asInstanceOf[java.util.LinkedHashMap[String,String]].toMap)
    }
    Some(attribution)
    }
    catch{
      case e:Exception => None
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
                attribution.setTaxonomicHints(ahint);
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

  private val columnFamily = "loc"
  private val lock : AnyRef = new Object()
  private val lru = new org.apache.commons.collections.map.LRUMap(10000)
  private val persistenceManager = Config.persistenceManager
//  private val lru = new ConcurrentLinkedHashMap.Builder[String, Option[Location]]()
//      .maximumWeightedCapacity(10000)
//      .build();
  /**
   * Add a tag to a location
   */
  def addTagToLocation (latitude:Float, longitude:Float, tagName:String, tagValue:String) {
    val guid = latitude +"|"+longitude
    persistenceManager.put(guid, columnFamily, "decimalLatitude",latitude.toString)
    persistenceManager.put(guid, columnFamily, "decimalLongitude",longitude.toString)
    persistenceManager.put(guid, columnFamily, tagName,tagValue)
  }

  /**
   * Add a region mapping for this point.
   */
  def addRegionToPoint (latitude:Double, longitude:Double, mapping:Map[String,String]) {
    val guid = latitude +"|"+longitude
    var properties = scala.collection.mutable.Map[String,String]()
    properties ++= mapping
    properties.put("decimalLatitude", latitude.toString)
    properties.put("decimalLongitude", longitude.toString)
    persistenceManager.put(guid, columnFamily, properties.toMap)
  }

  /**
   * Add a region mapping for this point.
   */
  def addRegionToPoint (latitude:String, longitude:String, mapping:Map[String,String]) {
    val guid = latitude +"|"+longitude
    var properties = scala.collection.mutable.Map[String,String]()
    properties ++= mapping
    properties.put("decimalLatitude", latitude)
    properties.put("decimalLongitude", longitude)
    persistenceManager.put(guid, columnFamily, properties.toMap)
  }  
  
  /**
   * Round coordinates to 4 decimal places.
   */
  protected def roundCoord(x:String) : String = {
    try {
      (((x.toFloat * 10000).toInt).toFloat / 10000).toString
    } catch {
      case e:NumberFormatException => x
    }
  }
  /**
   * Returns the persistence storage primary key for the supplied coordinates
   */
  def getLatLongKey(latitude:String, longitude:String) :Option[String]={
    if(latitude != null && longitude != null)
      return Some(roundCoord(latitude)+"|"+roundCoord(longitude))
    None
  }

  /**
   * Get location information for point.
   * For geo spatial requirements we don't want to round the latitude , longitudes 
   */
  def getByLatLon(latitude:String, longitude:String) : Option[(Location, EnvironmentalLayers, ContextualLayers)] = {
    val uuid =  latitude+"|"+longitude //roundCoord(latitude)+"|"+roundCoord(longitude)

    val cachedObject = lock.synchronized { lru.get(uuid) }
    //val cachedObject = lru.get(uuid)

    if(cachedObject!=null){
        cachedObject.asInstanceOf[Option[(Location, EnvironmentalLayers, ContextualLayers)]]
    } else {
        val map = persistenceManager.get(uuid,"loc")
        if(!map.isEmpty){
          val location = new Location
          val environmentalLayers = new EnvironmentalLayers
          val contextualLayers = new ContextualLayers
          FullRecordMapper.mapPropertiesToObject(location,map.get)
          FullRecordMapper.mapPropertiesToObject(environmentalLayers, map.get)
          FullRecordMapper.mapPropertiesToObject(contextualLayers, map.get)
          val returnValue = Some((location, environmentalLayers, contextualLayers))
          lock.synchronized {lru.put(uuid,returnValue)}
          returnValue
        } else {
          //do a gazetteer lookup???
          if(Config.allowWebserviceLookup){
            val map = gazetteerLookup(latitude, longitude)
            if (!map.isEmpty) {
              //cache it in cassandra
              addRegionToPoint(latitude, longitude, map)
              val location = new Location
              val environmentalLayers = new EnvironmentalLayers
              val contextualLayers = new ContextualLayers
              FullRecordMapper.mapPropertiesToObject(location, map)
              FullRecordMapper.mapPropertiesToObject(environmentalLayers, map)
              FullRecordMapper.mapPropertiesToObject(contextualLayers, map)
              val returnValue = Some((location, environmentalLayers, contextualLayers))
              lock.synchronized {
                lru.put(uuid, returnValue)
              }
              returnValue
            } else {
              lock.synchronized {
                lru.put(uuid, None)
              }
              None
            }
          } else {
            lock.synchronized {
              lru.put(uuid, None)
            }
            None
          }
        }
    }
  }

  def gazetteerLookup(latitude: String, longitude: String): Map[String, String] = {
    try {
      val url = new URL("http://spatial-dev.ala.org.au/gazetteer/latlon/" + latitude + "," + longitude)
      val connection = url.openConnection();
      connection.setRequestProperty("Accept", "application/xml");
      connection.setRequestProperty("Accept-Encoding", "gzip,deflate");
      connection.setDoInput(true);

      //println("content length: " + connection.getContentLength)

      if(connection.getContentLength > 0){
        val in = connection.getInputStream();
        val gis = new GZIPInputStream(in);
        val responseBuffer = new StringBuffer()
        val input = new BufferedReader(new InputStreamReader(gis));
        var line = input.readLine
        while (line != null) {
          responseBuffer.append(line)
          line = input.readLine
        }

        gis.close
        val xml = XML.loadString(responseBuffer.toString)
        val results = xml \\ "result"

        results.map(result => {
          val layerName = (result \ "layerName").text
          val name = (result \ "name").text

          DwC.matchTerm(layerName) match {
            case Some(t) => Map(t.canonical -> name)
            case _ => Map(layerName -> name)
          }
        }).flatten.toMap
      } else {
        Map()
      }
    } catch {
      case ex:Exception => ex.printStackTrace(); Map()
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