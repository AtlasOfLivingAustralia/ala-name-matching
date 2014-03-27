package au.org.ala.biocache.caches

import org.slf4j.LoggerFactory
import au.org.ala.biocache.Config
import au.org.ala.names.model.{NameSearchResult, MetricsResultDTO,LinnaeanRankClassification}
import au.org.ala.biocache.model.Classification


import au.org.ala.biocache.util.ReflectBean

import au.org.ala.names.search.{ALANameSearcher, HomonymException, SearchResultException}
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

  val logger = LoggerFactory.getLogger("ClassificationDAO")
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
  def getByHashLRU(cl:Classification, count:Int=0) : Option[MetricsResultDTO] = {
    //use the vernacular name to lookup if there if there is no scientific name or higher level classification
    //we don't really trust vernacular name matches thus only use as a last resort
    val hash = {
      if(cl.vernacularName == null || cl.scientificName != null || cl.specificEpithet != null
        || cl.infraspecificEpithet != null || cl.kingdom != null || cl.phylum != null
        || cl.classs != null || cl.order != null || cl.family !=null  || cl.genus!=null){
        Array(cl.kingdom,cl.phylum,cl.classs,cl.order,cl.family,cl.genus,cl.species,cl.specificEpithet,
          cl.subspecies,cl.infraspecificEpithet,cl.scientificName).reduceLeft(_+"|"+_)
      } else {
        cl.vernacularName
      }
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

    if(cachedObject != null){
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
        } else {
          Some(resultMetric)
        }

        if(result.isDefined){
          //update the subspecies or below value if necessary
          val rank = result.get.getResult.getRank
          if(rank != null && rank.getId() > 7000 && rank.getId < 9999){
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
            if(count < 4){
              result = getByHashLRU(newcl, count+1)
            } else {
              logger.warn("Potential recursive issue with " + cl.getKingdom()+" " + cl.getPhylum + " " + cl.getClasss + " " + cl.getOrder + " " + cl.getFamily)
            }
            //[Processor Thread 7] 710000 >> Last key : dr695|BAS|SOMBASE/TOTAL BIOCONSTRUCTORS|74175, records per sec: 772.2008 recursive Stack overflow
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