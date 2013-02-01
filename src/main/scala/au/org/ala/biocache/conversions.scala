package au.org.ala.biocache

import au.org.ala.checklist.lucene.model.NameSearchResult
import java.util.Date
import org.apache.commons.lang.time.DateFormatUtils
import org.slf4j.LoggerFactory

object BiocacheConversions {
  val logger = LoggerFactory.getLogger("BiocacheConversions")

  implicit def asClassification(nsr: NameSearchResult): au.org.ala.biocache.Classification = {
    val cl = new au.org.ala.biocache.Classification
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
    if(cl.subspecies != null && cl.subspecies.size > 0)
      cl.subspeciesID = nsr.getLsid()
    cl.taxonConceptID = nsr.getLsid
    cl.left = nsr.getLeft
    cl.right = nsr.getRight
    if(nsr.getRank != null){
    	cl.taxonRank = nsr.getRank.getRank
    	cl.taxonRankID = nsr.getRank.getId.toString
    }
    else
      logger.warn("ERROR : " + nsr.getLsid + " doesn't have a rank " + rankClassification.getScientificName)
    
    //put the match reason
    cl.nameMatchMetric = nsr.getMatchType.toString
    cl
  }
  
  implicit def dateToString(date:Date):String = DateFormatUtils.format(date, "yyyy-MM-dd'T'HH:mm:ss'Z'")
}
