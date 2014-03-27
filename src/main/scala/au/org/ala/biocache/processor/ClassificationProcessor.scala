package au.org.ala.biocache.processor

import org.slf4j.LoggerFactory
import scala.collection.JavaConversions
import scala.collection.mutable.{ArrayBuffer, HashMap}
import au.org.ala.names.model.LinnaeanRankClassification
import org.apache.commons.lang.StringUtils
import au.org.ala.biocache.caches.{TaxonProfileDAO, ClassificationDAO, AttributionDAO}
import au.org.ala.biocache.util.BiocacheConversions
import au.org.ala.biocache.model.{QualityAssertion, FullRecord, Classification}
import au.org.ala.biocache.load.FullRecordMapper
import au.org.ala.biocache.vocab.{Kingdoms, SpeciesGroups, DwC, AssertionCodes}

/**
 * A processor of taxonomic information.
 */
class ClassificationProcessor extends Processor {

  val logger = LoggerFactory.getLogger("ClassificationProcessor")
  val afdApniIdentifier = """(:afd.|:apni.)""".r
  val questionPattern = """([\x00-\x7F\s]*)\?([\x00-\x7F\s]*)""".r
  val affPattern = """([\x00-\x7F\s]*) aff[#!?\\.]?([\x00-\x7F\s]*)""".r
  val cfPattern = """([\x00-\x7F\s]*) cf[#!?\\.]?([\x00-\x7F\s]*)""".r

  import BiocacheConversions._
  import JavaConversions._
  /**
   * Parse the hints into a usable map with rank -> Set.
   */
  def parseHints(taxonHints: List[String]): Map[String, Set[String]] = {
    //parse taxon hints into rank : List of
    val rankSciNames = new HashMap[String, Set[String]]
    val pairs = taxonHints.map(x => x.split(":"))
    pairs.foreach(pair => {
      val values = rankSciNames.getOrElse(pair(0), Set())
      rankSciNames.put(pair(0), values + pair(1).trim.toLowerCase)
    })
    rankSciNames.toMap
  }

  /**
   * Returns false if the any of the taxonomic hints conflict with the classification
   */
  def isMatchValid(cl: LinnaeanRankClassification, hintMap: Map[String, Set[String]]): (Boolean, String) = {
    //are there any conflicts??
    for (rank <- hintMap.keys) {
      val (conflict, comment) = {
        rank match {
          case "kingdom" => (hasConflict(rank, cl.getKingdom, hintMap), "Kingdom:" + cl.getKingdom)
          case "phylum" => (hasConflict(rank, cl.getPhylum, hintMap), "Phylum:" + cl.getPhylum)
          case "class" => (hasConflict(rank, cl.getKlass, hintMap), "Class:" + cl.getKlass)
          case "order" => (hasConflict(rank, cl.getOrder, hintMap), "Order:" + cl.getOrder)
          case "family" => (hasConflict(rank, cl.getFamily, hintMap), "Family:" + cl.getFamily)
          case _ => (false, "")
        }
      }
      if (conflict) return (false, comment)
    }
    (true, "")
  }

  def hasConflict(rank: String, taxon: String, hintMap: Map[String, Set[String]]): Boolean = {
    taxon != null && !hintMap.get(rank).get.contains(taxon.toLowerCase)
  }

  def hasMatchToDefault(rank: String, taxon: String, classification: Classification): Boolean = {
    def term = DwC.matchTerm(rank)
    def field = if (term.isDefined) term.get.canonical else rank
    taxon != null && taxon.equalsIgnoreCase(classification.getProperty(field).getOrElse(""))
  }

  def setMatchStats(nameMetrics:au.org.ala.names.model.MetricsResultDTO, processed:FullRecord, assertions:ArrayBuffer[QualityAssertion]){
    //set the parse type and errors for all results before continuing
    processed.classification.nameParseType = if(nameMetrics.getNameType != null)nameMetrics.getNameType.toString else "UNKNOWN"
    //add the taxonomic issues for the match
    processed.classification.taxonomicIssue = if(nameMetrics.getErrors != null)nameMetrics.getErrors.toList.map(_.toString).toArray else Array("noIssue")
    //check the name parse tye to see if the scientific name was valid
    if (processed.classification.nameParseType == "blacklisted"){
      assertions += QualityAssertion(AssertionCodes.INVALID_SCIENTIFIC_NAME)
    } else {
      assertions += QualityAssertion(AssertionCodes.INVALID_SCIENTIFIC_NAME, 1)
    }
  }

  def testSuppliedValues(raw:FullRecord, processed:FullRecord, assertions:ArrayBuffer[QualityAssertion]){
    //test for the missing taxon rank
    if (StringUtils.isBlank(raw.classification.taxonRank)){
      assertions += QualityAssertion(AssertionCodes.MISSING_TAXONRANK, "Missing taxonRank")
    } else {
      assertions += QualityAssertion(AssertionCodes.MISSING_TAXONRANK, 1)
    }
    //test that a scientific name or vernacular name has been supplied
    if (StringUtils.isBlank(raw.classification.scientificName) && StringUtils.isBlank(raw.classification.vernacularName)){
      assertions += QualityAssertion(AssertionCodes.NAME_NOT_SUPPLIED, "No scientificName or vernacularName has been supplied. Name match will be based on a constructed name.")
    } else {
      assertions += QualityAssertion(AssertionCodes.NAME_NOT_SUPPLIED, 1)
    }

    //test for mismatch in kingdom
    if (StringUtils.isNotBlank(raw.classification.kingdom)){
      val matchedKingdom = Kingdoms.matchTerm(raw.classification.kingdom)
      if (matchedKingdom.isDefined){
        //the supplied kingdom is recognised
        assertions += QualityAssertion(AssertionCodes.UNKNOWN_KINGDOM, 1)
      } else {
        assertions += QualityAssertion(AssertionCodes.UNKNOWN_KINGDOM, "The supplied kingdom is not recognised")
      }
    }
  }

  /**
   * Match the classification
   */
  def process(guid: String, raw: FullRecord, processed: FullRecord,lastProcessed: Option[FullRecord] = None): Array[QualityAssertion] = {
    var assertions = new ArrayBuffer[QualityAssertion]

    testSuppliedValues(raw, processed, assertions)

    try {
      //update the raw with the "default" values if necessary
      if (processed.defaultValuesUsed) {
        if (raw.classification.kingdom == null && processed.classification.kingdom != null) raw.classification.kingdom = processed.classification.kingdom
        if (raw.classification.phylum == null && processed.classification.phylum != null) raw.classification.phylum = processed.classification.phylum
        if (raw.classification.classs == null && processed.classification.classs != null) raw.classification.classs = processed.classification.classs
        if (raw.classification.order == null && processed.classification.order != null) raw.classification.order = processed.classification.order
        if (raw.classification.family == null && processed.classification.family != null) raw.classification.family = processed.classification.family
      }

      //val nsr = DAO.nameIndex.searchForRecord(classification, true)
      val nameMetrics = ClassificationDAO.getByHashLRU(raw.classification).getOrElse(null)
      if(nameMetrics != null){

        val nsr = nameMetrics.getResult

        //store the matched classification
        if (nsr != null) {
          //The name is recognised:
          assertions += QualityAssertion(AssertionCodes.NAME_NOTRECOGNISED, 1)
          val classification = nsr.getRankClassification
          //Check to see if the classification fits in with the supplied taxonomic hints
          //get the taxonomic hints from the collection or data resource
          var attribution = AttributionDAO.getByCodes(raw.occurrence.institutionCode, raw.occurrence.collectionCode)
          if (attribution.isEmpty)
            attribution = AttributionDAO.getDataResourceByUid(raw.attribution.dataResourceUid)

          if (!attribution.isEmpty) {
            logger.debug("Checking taxonomic hints")
            val taxonHints = attribution.get.taxonomicHints

            if (taxonHints != null && !taxonHints.isEmpty) {
              val (isValid, comment) = isMatchValid(classification, attribution.get.retrieveParseHints)
              if (!isValid) {
                logger.info("Conflict in matched classification. Matched: " + guid + ", Matched: " + comment + ", Taxonomic hints in use: " + taxonHints.toList)
                processed.classification.nameMatchMetric = "matchFailedHint"
                assertions += QualityAssertion(AssertionCodes.RESOURCE_TAXONOMIC_SCOPE_MISMATCH, comment)
              } else if (attribution.get.retrieveParseHints.size >0){
                //the taxonomic hints passed
                assertions += QualityAssertion(AssertionCodes.RESOURCE_TAXONOMIC_SCOPE_MISMATCH, 1)
              }
            }
          }

          //check for default match before updating the classification.
          val hasDefaultMatch = processed.defaultValuesUsed && nsr.getRank() != null && hasMatchToDefault(nsr.getRank().getRank(), nsr.getRankClassification().getScientificName(), processed.classification)
          //store ".p" values
          processed.classification = nsr
          //check to see if the classification has been matched to a default value
          if (hasDefaultMatch)
            processed.classification.nameMatchMetric = "defaultHigherMatch" //indicates that a default value was used to make the higher level match

          //try to apply the vernacular name
          val taxonProfile = TaxonProfileDAO.getByGuid(nsr.getLsid)
          if (!taxonProfile.isEmpty) {
            if (taxonProfile.get.commonName != null)
              processed.classification.vernacularName = taxonProfile.get.commonName
            if (taxonProfile.get.habitats != null)
              processed.classification.speciesHabitats = taxonProfile.get.habitats
          }

          //Add the species group information - I think that it is better to store this value than calculate it at index time
          //val speciesGroups = SpeciesGroups.getSpeciesGroups(processed.classification)
          val speciesGroups = SpeciesGroups.getSpeciesGroups(processed.classification.getLeft(), processed.classification.getRight())
          logger.debug("Species Groups: " + speciesGroups)
          if (!speciesGroups.isEmpty && !speciesGroups.get.isEmpty) {
            processed.classification.speciesGroups = speciesGroups.get.toArray[String]
          }

          //add the taxonomic rating for the raw name
          val scientificName = {
            if (raw.classification.scientificName != null) raw.classification.scientificName
            else if (raw.classification.species != null) raw.classification.species
            else if (raw.classification.specificEpithet != null && raw.classification.genus != null) raw.classification.genus + " " + raw.classification.specificEpithet
            else null
          }
          //NC: 2013-02-15 This is handled from the name match as an "errorType"
          //        processed.classification.taxonomicIssue = scientificName match {
          //          case questionPattern(a, b) => "questionSpecies"
          //          case affPattern(a, b) => "affinitySpecies"
          //          case cfPattern(a, b) => "conferSpecies"
          //          case _ => "noIssue"
          //        }

          setMatchStats(nameMetrics,processed, assertions)

          //is the name in the NSLs ???
          if (afdApniIdentifier.findFirstMatchIn(nsr.getLsid).isEmpty) {
            assertions += QualityAssertion(AssertionCodes.NAME_NOT_IN_NATIONAL_CHECKLISTS, "Record not attached to concept in national species lists")
          } else {
            assertions += QualityAssertion(AssertionCodes.NAME_NOT_IN_NATIONAL_CHECKLISTS, 1)
          }

        } else if(nameMetrics.getErrors.contains(au.org.ala.names.model.ErrorType.HOMONYM)){
          logger.debug("[QualityAssertion] A homonym was detected (with  no higher level match), classification for Kingdom: " +
            raw.classification.kingdom + ", Family:" + raw.classification.family + ", Genus:" + raw.classification.genus +
            ", Species: " + raw.classification.species + ", Epithet: " + raw.classification.specificEpithet)
          processed.classification.nameMatchMetric = "noMatch"
          setMatchStats(nameMetrics,processed, assertions)
          assertions += QualityAssertion(AssertionCodes.HOMONYM_ISSUE, "A homonym was detected in supplied classificaiton.")
        } else {
          logger.debug("[QualityAssertion] No match for record, classification for Kingdom: " +
            raw.classification.kingdom + ", Family:" + raw.classification.family + ", Genus:" + raw.classification.genus +
            ", Species: " + raw.classification.species + ", Epithet: " + raw.classification.specificEpithet)
          processed.classification.nameMatchMetric = "noMatch"
          setMatchStats(nameMetrics,processed,assertions)
          assertions += QualityAssertion(AssertionCodes.NAME_NOTRECOGNISED, "Name not recognised")
        }
      } else {
        logger.debug("[QualityAssertion] No match for record, classification for Kingdom: " +
          raw.classification.kingdom + ", Family:" + raw.classification.family + ", Genus:" + raw.classification.genus +
          ", Species: " + raw.classification.species + ", Epithet: " + raw.classification.specificEpithet)
        processed.classification.nameMatchMetric = "noMatch"
        assertions += QualityAssertion(AssertionCodes.NAME_NOTRECOGNISED, "Name not recognised")
      }
    } catch {
      case e: Exception => logger.error("Exception during classification match for record " + guid, e)
    }
    assertions.toArray
  }

  def getName = FullRecordMapper.taxonomicalQa
}
