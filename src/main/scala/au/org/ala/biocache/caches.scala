package au.org.ala.biocache
/**
 * These classes represent caches of data sourced from other components
 * maintained within the biocache for performance reasons. These
 * components
 */
import au.org.ala.checklist.lucene.CBIndexSearch
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
import au.org.ala.util.ReflectBean
import org.wyki.cassandra.pelops.{Mutator,Pelops,Selector}
import scala.collection.mutable.{LinkedList,ListBuffer}
import org.apache.cassandra.thrift.{Column,ConsistencyLevel,ColumnPath,SlicePredicate,SliceRange}
import java.util.ArrayList
import org.wyki.cassandra.pelops.Policy

/**
 * A DAO for accessing taxon profile information by GUID.
 * 
 * This should provide an abstraction layer, that (eventually) handles
 * "timeToLive" style functionality that invalidates values in the cache
 * and retrieves the latest values.
 */
object TaxonProfileDAO {
	
	val columnFamily = "taxon"
	
	/**
	 * Retrieve the profile by the taxon concept's GUID
	 */
	def getByGuid(guid:String) : Option[TaxonProfile] = {
		
		if(guid==null || guid.isEmpty) return None
		
        val selector = Pelops.createSelector(DAO.poolName, DAO.keyspace)
        val slicePredicate = Selector.newColumnsPredicateAll(true, 10000)
        try {
	        val columns = selector.getColumnsFromRow(guid, columnFamily, slicePredicate, ConsistencyLevel.ONE)
	        val columnList = List(columns.toArray : _*)
	        var taxonProfile = new TaxonProfile
	        
	        for(column<-columnList){
	        	
	          val field = new String(column.asInstanceOf[Column].name)
	          val value = new String(column.asInstanceOf[Column].value)
	           
	          field match {
	         	  case "guid" => taxonProfile.guid = value
	         	  case "scientificName" => taxonProfile.scientificName = value
	         	  case "commonName" => taxonProfile.commonName = value
	         	  case "habitats" => {
	         	 	  if(value!=null && value.size>0){
	         	 		  taxonProfile.habitats = value.split(",")
	         	 	   }
	         	  }
	         	  case _ =>
	          }
	        }
	        Some(taxonProfile)
        } catch {
        	case e:Exception => None
        }
	}
	
	/**
	 * Persist the taxon profile.
	 */
	def add(taxonProfile:TaxonProfile) {
	    val mutator = Pelops.createMutator(DAO.poolName, DAO.keyspace)
	    mutator.writeColumn(taxonProfile.guid, columnFamily, mutator.newColumn("guid", taxonProfile.guid))
	    mutator.writeColumn(taxonProfile.guid, columnFamily, mutator.newColumn("scientificName", taxonProfile.scientificName))
	    if(taxonProfile.commonName!=null)
	    	mutator.writeColumn(taxonProfile.guid, columnFamily, mutator.newColumn("commonName", taxonProfile.commonName))
	    if(taxonProfile.habitats!=null && taxonProfile.habitats.size>0){
	    	val habitatString = taxonProfile.habitats.reduceLeft(_+","+_)
	    	mutator.writeColumn(taxonProfile.guid, columnFamily, mutator.newColumn("habitats", habitatString))
	    }
	    mutator.execute(ConsistencyLevel.ONE)
	}
}

/**
 * A DAO for attribution data. The source of this data should be
 */
object AttributionDAO {

  import ReflectBean._
  val columnFamily = "attr"

  /**
   * Persist the attribution information.
   */
  def add(institutionCode:String, collectionCode:String, attribution:Attribution){
    val guid = institutionCode.toUpperCase +"|"+collectionCode.toUpperCase
      val mutator = Pelops.createMutator(DAO.poolName, DAO.keyspace)
      for(field<-DAO.attributionDefn){
        val fieldValue = attribution.getter(field).asInstanceOf[String]
        if(fieldValue!=null && !fieldValue.isEmpty){
          val fieldValue = attribution.getter(field).asInstanceOf[String].getBytes
          mutator.writeColumn(guid, columnFamily, mutator.newColumn(field, fieldValue))
        }
      }
    mutator.execute(ConsistencyLevel.ONE)
  }

  /**
   * Retrieve attribution via institution/collection codes.
   */
  def getByCodes(institutionCode:String, collectionCode:String) : Option[Attribution] = {
    try {
      if(institutionCode!=null && collectionCode!=null){
        val uuid = institutionCode.toUpperCase+"|"+collectionCode.toUpperCase
        //println(uuid)
        val selector = Pelops.createSelector(DAO.poolName, DAO.keyspace)
        val slicePredicate = Selector.newColumnsPredicateAll(true, 10000)
        val columns = selector.getColumnsFromRow(uuid, columnFamily, slicePredicate, ConsistencyLevel.ONE)
        val columnList = List(columns.toArray : _*)
        val attribution = new Attribution
        for(column<-columnList){
          val field = new String(column.asInstanceOf[Column].name)
          val value = new String(column.asInstanceOf[Column].value)
          val method = attribution.getClass.getMethods.find(_.getName == field + "_$eq")
          method.get.invoke(attribution, value.asInstanceOf[AnyRef])
        }
        Some(attribution)
      } else {
        None
      }
    } catch {
      case e:Exception => println(e.printStackTrace); None
    }
  }
}

/**
 * DAO for location lookups.
 */
object LocationDAO {

  val columnFamily = "loc"

  /**
   * Add a tag to a location
   */
  def addTagToLocation (latitude:Float, longitude:Float, tagName:String, tagValue:String) {
    val guid = latitude +"|"+longitude
    val mutator = Pelops.createMutator(DAO.poolName, DAO.keyspace)
    mutator.writeColumn(guid, columnFamily, mutator.newColumn("decimalLatitude", latitude.toString))
    mutator.writeColumn(guid, columnFamily, mutator.newColumn("decimalLongitude", longitude.toString))
    mutator.writeColumn(guid, columnFamily, mutator.newColumn(tagName, tagValue))
    mutator.execute(ConsistencyLevel.ONE)
  }

  
  def addRegionToPoint (latitude:Float, longitude:Float, mapping:Map[String,String]) {
    val guid = latitude +"|"+longitude
    val mutator = Pelops.createMutator(DAO.poolName, DAO.keyspace)
    mutator.writeColumn(guid,columnFamily, mutator.newColumn("decimalLatitude", latitude.toString))
    mutator.writeColumn(guid,columnFamily, mutator.newColumn("decimalLongitude", longitude.toString))
    for(map<-mapping){
      mutator.writeColumn(guid, columnFamily, mutator.newColumn(map._1, map._2))
    }
    mutator.execute(ConsistencyLevel.ONE)
  }

  def roundCoord(x:String) : String = {
	try {
		(((x * 10000).toInt).toFloat / 10000).toString
	} catch {
	  case e:NumberFormatException => x
	}
  }
  
  def getByLatLon(latitude:String, longitude:String) : Option[Location] = {
    try {
      val uuid =  roundCoord(latitude)+"|"+roundCoord(longitude)
      //println(uuid)
      val selector = Pelops.createSelector(DAO.poolName, DAO.keyspace)
      val slicePredicate = Selector.newColumnsPredicateAll(true, 10000)
      val columns = selector.getColumnsFromRow(uuid, columnFamily, slicePredicate, ConsistencyLevel.ONE)
      val columnList = List(columns.toArray : _*)
      val location = new Location
      for(column<-columnList){
        val field = new String(column.asInstanceOf[Column].name)
        val value = new String(column.asInstanceOf[Column].value)
        //println(new String(column.asInstanceOf[Column].name)+ " " +column.asInstanceOf[Column].value)
        //println("field name : " + field+", value : "+value)
        val method = location.getClass.getMethods.find(_.getName == field + "_$eq")
        method.get.invoke(location, value.asInstanceOf[AnyRef])
      }
      Some(location)
    } catch {
      case e:Exception => println(e.printStackTrace); None
    }
  }
}
