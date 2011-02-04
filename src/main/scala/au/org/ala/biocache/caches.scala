package au.org.ala.biocache
/**
 * These classes represent caches of data sourced from other components
 * maintained within the biocache for performance reasons. These
 * components
 */
import au.org.ala.util.ReflectBean
import org.wyki.cassandra.pelops.{Pelops,Selector}
import org.apache.cassandra.thrift.{Column,ConsistencyLevel}
/**
 * A DAO for accessing taxon profile information by GUID.
 * 
 * This should provide an abstraction layer, that (eventually) handles
 * "timeToLive" style functionality that invalidates values in the cache
 * and retrieves the latest values.
 */
object TaxonProfileDAO {

  private val columnFamily = "taxon"

  /**
   * Retrieve the profile by the taxon concept's GUID
   */
  def getByGuid(guid:String) : Option[TaxonProfile] = {

    if(guid==null || guid.isEmpty) return None

    val map = DAO.persistentManager.get(guid,columnFamily)
    if(!map.isEmpty){
        var taxonProfile = new TaxonProfile
        map.get.foreach ( keyValue => {
            keyValue._1 match {
              case "guid" => taxonProfile.guid = keyValue._2
              case "scientificName" => taxonProfile.scientificName = keyValue._2
              case "commonName" => taxonProfile.commonName = keyValue._2
              case "habitats" => {
                if(keyValue._2!=null && keyValue._2.size>0){
                  taxonProfile.habitats = keyValue._2.split(",")
                 }
              }
              case _ =>
            }
        })
      Some(taxonProfile)
    } else {
      None
    }
  }

  /**
   * Persist the taxon profile.
   */
  def add(taxonProfile:TaxonProfile) {

      var properties = scala.collection.mutable.Map[String,String]()
      properties.put("guid", taxonProfile.guid)
      properties.put("scientificName", taxonProfile.scientificName)
      properties.put("commonName", taxonProfile.commonName)
      if(taxonProfile.habitats!=null && taxonProfile.habitats.size>0){
        val habitatString = taxonProfile.habitats.reduceLeft(_+","+_)
        properties.put("habitats", habitatString)
      }
      DAO.persistentManager.put(taxonProfile.guid, columnFamily, properties.toMap)
  }
}

/**
 * A DAO for attribution data. The source of this data should be
 */
object AttributionDAO {

  import ReflectBean._
  private val columnFamily = "attr"

  /**
   * Persist the attribution information.
   */
  def add(institutionCode:String, collectionCode:String, attribution:Attribution){
    val guid = institutionCode.toUpperCase +"|"+collectionCode.toUpperCase
    val map = DAO.mapObjectToProperties(attribution)
    DAO.persistentManager.put(guid,"attr",map)
  }

  /**
   * Retrieve attribution via institution/collection codes.
   */
  def getByCodes(institutionCode:String, collectionCode:String) : Option[Attribution] = {
    if(institutionCode!=null && collectionCode!=null){
      val uuid = institutionCode.toUpperCase+"|"+collectionCode.toUpperCase
      val map = DAO.persistentManager.get(uuid,"attr")
      if(map.isEmpty){
        val attribution = new Attribution
        DAO.mapPropertiesToObject(attribution,map.get)
        Some(attribution)
      } else {
        None
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

  /**
   * Add a tag to a location
   */
  def addTagToLocation (latitude:Float, longitude:Float, tagName:String, tagValue:String) {
    val guid = latitude +"|"+longitude
    DAO.persistentManager.put(guid, columnFamily, "decimalLatitude",latitude.toString)
    DAO.persistentManager.put(guid, columnFamily, "decimalLongitude",longitude.toString)
    DAO.persistentManager.put(guid, columnFamily, tagName,tagValue)
  }

  /**
   * Add a region mapping for this point.
   */
  def addRegionToPoint (latitude:Float, longitude:Float, mapping:Map[String,String]) {
    val guid = latitude +"|"+longitude
    var properties = scala.collection.mutable.Map[String,String]()
    properties ++= mapping
    properties.put("decimalLatitude", latitude.toString)
    properties.put("decimalLongitude", longitude.toString)
    DAO.persistentManager.put(guid, columnFamily, properties.toMap)
  }

  /**
   * Round coordinates to 4 decimal places.
   */
  protected def roundCoord(x:String) : String = {
    try {
      (((x * 10000).toInt).toFloat / 10000).toString
    } catch {
      case e:NumberFormatException => x
    }
  }

  /**
   * Get location information for point.
   */
  def getByLatLon(latitude:String, longitude:String) : Option[Location] = {
    val uuid =  roundCoord(latitude)+"|"+roundCoord(longitude)
    val map = DAO.persistentManager.get(uuid,"loc")
    if(!map.isEmpty){
      val location = new Location
      DAO.mapPropertiesToObject(location,map.get)
      Some(location)
    } else {
      None
    }
  }
}
