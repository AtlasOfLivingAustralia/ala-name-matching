package au.org.ala.biocache
/**
 * These classes represent caches of data sourced from other components
 * maintained within the biocache for performance reasons. These
 * components
 */
import au.org.ala.checklist.lucene.{HomonymException,SearchResultException}
import au.org.ala.data.model.LinnaeanRankClassification
import au.org.ala.util.ReflectBean
import org.wyki.cassandra.pelops.{Pelops,Selector}
import org.apache.cassandra.thrift.{Column,ConsistencyLevel}

/**
 * A DAO for accessing classification information in the cache. If the
 * value does not exist in the cache the name matching API is called.
 *
 * The cache will store a classification object for names that match. If the
 * name causes a homonym exeception or is not found the ErrorCode is stored.
 *
 * NOT BEING USED YET - some perfomance issue with this...
 * @author Natasha Carter
 *
 */
object ClassificationDAO{
  import ReflectBean._
  private val columnFamily ="namecl"
  private val cachedValues = new java.util.Hashtable[LinnaeanRankClassification, Classification]

  def getByHash(cl: LinnaeanRankClassification, classification:Classification):Array[QualityAssertion]={
    if(cl== null) return Array()

    if(!cachedValues.containsKey(cl)){

      //lookup the name in the name matching index
      val errors = lookUpName(cl, classification)
      if(errors.size == 0)
        cachedValues.put(cl, classification)
      errors

    }
    else{

      val scl =cachedValues.get(cl)
      //update the classification to include the items that are processed. This is necessary because the FullRecord stores the
      //classification in an objectArray
      classification.kingdom = scl.kingdom
      classification.kingdomID = scl.kingdomID
      classification.phylum = scl.phylum
      classification.phylumID = scl.phylumID
      classification.classs = scl.classs
      classification.classID = scl.classID
      classification.order = scl.order
      classification.orderID = scl.orderID
      classification.family = scl.family
      classification.familyID = scl.familyID
      classification.genus = scl.genus
      classification.genusID = scl.genusID
      classification.species = scl.species
      classification.speciesID = scl.speciesID
      classification.specificEpithet = scl.specificEpithet
      classification.scientificName = scl.scientificName
      classification.taxonConceptID = scl.taxonConceptID
      classification.left = scl.left
      classification.right = scl.right
      classification.taxonRank = scl.taxonRank
      classification.taxonRankID = scl.taxonRankID
      classification.vernacularName = scl.vernacularName
      classification.speciesGroups = scl.speciesGroups

      Array()
      //cachedValue
    }

    
  }
  def getByHashUsingMap(cl: LinnaeanRankClassification, classification:Classification):Array[QualityAssertion]={
    val mapValue = DAO.persistentManager.get(cl.hashCode.toString, columnFamily)
     if(mapValue.isEmpty || mapValue.get.size <1){
       val errors = lookUpName(cl, classification)
        if(errors.size == 0)
          DAO.persistentManager.put(cl.hashCode.toString, columnFamily, classification.getMap)
        errors
     }
     else{
       classification.kingdom = mapValue.get("kingdom")
        classification.kingdomID = mapValue.get("kingdomID")
        classification.phylum = mapValue.get("phylum")
        classification.phylumID = mapValue.get("phylumID")
        classification.classs = mapValue.get("classs")
        classification.classID = mapValue.get("classID")
        classification.order = mapValue.get("order")
        classification.orderID = mapValue.get("prderID")
        classification.family = mapValue.get("family")
        classification.familyID = mapValue.get("familyID")
        classification.genus = mapValue.get("genus")
        classification.genusID = mapValue.get("genusID")
        classification.species = mapValue.get("species")
        classification.speciesID = mapValue.get("speciesID")
        classification.specificEpithet = mapValue.get("specificEpithet")
        classification.scientificName = mapValue.get("scientificName")
        classification.taxonConceptID = mapValue.get("taxonConceptID")
        classification.left = mapValue.get("left")
        classification.right = mapValue.get("right")
        classification.taxonRank = mapValue.get("taxonRank")
        classification.taxonRankID = mapValue.get("taxonRankID")
        classification.vernacularName = mapValue.get("vernacularName")
        classification.speciesGroups = Json.toArray(mapValue.get("speciesGroup"), classOf[String].asInstanceOf[java.lang.Class[AnyRef]]).asInstanceOf[Array[String]]
        
      Array()
     }
    
  }
  def lookUpName(cl:LinnaeanRankClassification, classification:Classification):Array[QualityAssertion]={
    try {

      val nsr = DAO.nameIndex.searchForRecord(cl, true)
      //store the matched classification
      if (nsr != null) {
        val matchedcl = nsr.getRankClassification
        //val classification = new Classification
        //store ".p" values
        classification.kingdom = matchedcl.getKingdom
        classification.kingdomID = matchedcl.getKid
        classification.phylum = matchedcl.getPhylum
        classification.phylumID = matchedcl.getPid
        classification.classs = matchedcl.getKlass
        classification.classID = matchedcl.getCid
        classification.order = matchedcl.getOrder
        classification.orderID = matchedcl.getOid
        classification.family = matchedcl.getFamily
        classification.familyID = matchedcl.getFid
        classification.genus = matchedcl.getGenus
        classification.genusID = matchedcl.getGid
        classification.species = matchedcl.getSpecies
        classification.speciesID = matchedcl.getSid
        classification.specificEpithet = matchedcl.getSpecificEpithet
        classification.scientificName = matchedcl.getScientificName
        classification.taxonConceptID = nsr.getLsid
        classification.left = nsr.getLeft
        classification.right = nsr.getRight
        classification.taxonRank = nsr.getRank.getRank
        classification.taxonRankID = nsr.getRank.getId.toString
        //try to apply the vernacular name
        val taxonProfile = TaxonProfileDAO.getByGuid(nsr.getLsid)
        if(!taxonProfile.isEmpty && taxonProfile.get.commonName!=null){
          classification.vernacularName = taxonProfile.get.commonName
        }

        //Add the species group information - I think that it is better to store this value than calculate it at index time
        val speciesGroups = SpeciesGroups.getSpeciesGroups(classification)
        //logger.debug("Species Groups: " + speciesGroups)
        if(!speciesGroups.isEmpty && speciesGroups.get.length>0){
          classification.speciesGroups = speciesGroups.get.toArray[String]
        }
        
        Array()
      }
      else
        Array()
      }
      catch {
      case he: HomonymException => Array(QualityAssertion(AssertionCodes.HOMONYM_ISSUE, true, "Homonym issue resolving the classification"))
      //case he: HomonymException => logger.debug(he.getMessage,he);  Option(Array(QualityAssertion(AssertionCodes.HOMONYM_ISSUE, true, "Homonym issue resolving the classification")))
      case se: SearchResultException => Array()
        //case se: SearchResultException => logger.debug(se.getMessage,se); None)
    }
    Array()

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
              case "left" => taxonProfile.left = keyValue._2
              case "right" => taxonProfile.right = keyValue._2
              case "sensitive" => {
                  if(keyValue._2 !=null && keyValue._2.size>0){
                    taxonProfile.sensitive = Json.toArray(keyValue._2, classOf[SensitiveSpecies].asInstanceOf[java.lang.Class[AnyRef]]).asInstanceOf[Array[SensitiveSpecies]]
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
      properties.put("left", taxonProfile.left)
      properties.put("right", taxonProfile.right)
      if(taxonProfile.sensitive != null && taxonProfile.sensitive.size >0){
        properties.put("sensitive", Json.toJSON(taxonProfile.sensitive.asInstanceOf[Array[AnyRef]]))
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
      if(!map.isEmpty){
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
