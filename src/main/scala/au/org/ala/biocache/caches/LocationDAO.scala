package au.org.ala.biocache.caches

import org.slf4j.LoggerFactory
import au.org.ala.biocache.Config
import scala.collection.mutable.HashMap
import org.ala.layers.client.Client
import au.org.ala.biocache.model.Location
import au.org.ala.biocache.vocab.StateProvinces

/**
 * DAO for location lookups (lat, long -> locality).
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
