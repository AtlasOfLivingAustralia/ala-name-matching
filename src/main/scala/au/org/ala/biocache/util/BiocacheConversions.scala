package au.org.ala.biocache.util

import au.org.ala.names.model.NameSearchResult
import java.util.Date
import org.apache.commons.lang.time.DateFormatUtils
import org.slf4j.LoggerFactory
import au.org.ala.biocache.model.Classification

object BiocacheConversions {
  val logger = LoggerFactory.getLogger("BiocacheConversions")

  implicit def asClassification(nsr: NameSearchResult): Classification = {
    val cl = new Classification
    val rankClassification = nsr.getRankClassification
    cl.kingdom = rankClassification.getKingdom
    cl.kingdomID = rankClassification.getKid
    cl.phylum = rankClassification.getPhylum
    cl.phylumID = rankClassification.getPid
    cl.classs = rankClassification.getKlass
    cl.classID = rankClassification.getCid
    cl.order = rankClassification.getOrder
    cl.orderID = rankClassification.getOid
    cl.family = rankClassification.getFamily
    cl.familyID = rankClassification.getFid
    cl.genus = rankClassification.getGenus
    cl.genusID = rankClassification.getGid
    cl.species = rankClassification.getSpecies
    cl.speciesID = rankClassification.getSid
    cl.specificEpithet = rankClassification.getSpecificEpithet
    cl.scientificName = rankClassification.getScientificName
    cl.subspecies = rankClassification.getSubspecies
    if(cl.subspecies != null && cl.subspecies.size > 0){
      cl.subspeciesID = nsr.getLsid()
    }
    cl.taxonConceptID = nsr.getLsid
    cl.left = nsr.getLeft
    cl.right = nsr.getRight
    if(nsr.getRank != null){
    	cl.taxonRank = nsr.getRank.getRank
    	cl.taxonRankID = nsr.getRank.getId.toString
    } else {
      logger.debug(nsr.getLsid + " doesn't have a rank " + rankClassification.getScientificName)
    }
    
    //put the match reason
    cl.nameMatchMetric = nsr.getMatchType.toString
    cl
  }
  
  implicit def dateToString(date:Date):String = DateFormatUtils.format(date, "yyyy-MM-dd'T'HH:mm:ss'Z'")
}
