package au.org.ala.util

import org.apache.commons.lang.time.DateFormatUtils
import org.wyki.cassandra.pelops.Pelops
import au.org.ala.checklist.lucene.HomonymException
import au.org.ala.data.model.LinnaeanRankClassification
import java.util.GregorianCalendar
import scala.collection.mutable.ArrayBuffer
import au.org.ala.checklist.lucene.SearchResultException
import org.slf4j.LoggerFactory
import au.org.ala.biocache._

/**
 * 1. Classification matching
 * 	- include a flag to indicate record hasnt been matched to NSLs
 * 
 * 2. Parse locality information
 * 	- "Vic" -> Victoria
 * 
 * 3. Point matching
 * 	- parse latitude/longitude
 * 	- retrieve associated point mapping
 * 	- check state supplied to state point lies in
 * 	- marine/non-marine/limnetic (need a webservice from BIE)
 * 
 * 4. Type status normalization
 * 	- use GBIF's vocabulary
 * 
 * 5. Date parsing
 * 	- date validation
 * 	- support for date ranges
 * 
 * 6. Collectory lookups for attribution chain
 * 
 * Tests to conform to: http://bit.ly/eqSiFs
 */
object ProcessRecords {

  val logger = LoggerFactory.getLogger("ProcessRecords")

  def main(args: Array[String]): Unit = {
    logger.info("Starting processing records....")
    processAll
    logger.info("Finished. Shutting down.")
    Pelops.shutdown
  }

  /**
   * Process all records in the store
   */
  def processAll {
    var counter = 0
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis

    //page over all records and process
    OccurrenceDAO.pageOverAll(Raw, record => {
      counter += 1
      if (!record.isEmpty) {
        val raw = record.get
        processRecord(raw)

        //debug counter
        if (counter % 1000 == 0) {
          finishTime = System.currentTimeMillis
          logger.info(counter + " >> Last key : " + raw.occurrence.uuid + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
          startTime = System.currentTimeMillis
        }
      }
      true
    })
  }

  /**
   * Process a record, adding metadata and records quality systemAssertions
   */
  def processRecord(raw:FullRecord){

    val guid = raw.occurrence.uuid
    //NC: Changed so that a processed record only contains values that have been processed.
    var processed = new FullRecord//raw.clone
    var assertions = new ArrayBuffer[QualityAssertion]

    //find a classification in NSLs
    assertions ++= processClassification(guid, raw, processed)

    //perform gazetteer lookups - just using point hash for now
    assertions ++= processLocation(guid, raw, processed)

    //temporal processing
    assertions ++= processEvent(guid, raw, processed)

    //basis of record parsing
    assertions ++= processBasisOfRecord(guid, raw, processed)

    //type status normalisation
    assertions ++= processTypeStatus(guid, raw, processed)

    //process the attribution - call out to the Collectory...
    assertions ++= processAttribution(guid, raw, processed)

    //perform SDS lookups - retrieve from BIE for now....
    // processImages
    // processLinkRecord
    // processIdentifierRecords 
    // 

    //store the occurrence
    OccurrenceDAO.updateOccurrence(guid, processed, assertions.toArray, Processed)
  }

  /**
   * select icm.institution_uid, icm.collection_uid,  ic.code, ic.name, ic.lsid, cc.code from inst_coll_mapping icm
   * inner join institution_code ic ON ic.id = icm.institution_code_id
   * inner join collection_code cc ON cc.id = icm.collection_code_id
   * limit 10;
   */
  def processAttribution(guid:String, raw:FullRecord, processed:FullRecord) : Array[QualityAssertion] = {
    if(raw.occurrence.institutionCode!=null && raw.occurrence.collectionCode!=null){
        val attribution = AttributionDAO.getByCodes(raw.occurrence.institutionCode, raw.occurrence.collectionCode)
        if (!attribution.isEmpty) {
          OccurrenceDAO.updateOccurrence(guid, attribution.get, Processed)
          Array()
        } else {
          Array(QualityAssertion(AssertionCodes.UNRECOGNISED_COLLECTIONCODE, false, "Unrecognised collection code"))
        }
    } else {
      Array()
    }
  }

  /**
   * Validate the supplied number using the supplied function.
   */
  def validateNumber(number:String, f:(Int=>Boolean) ) : (Int, Boolean) = {
    try {
      if(number != null) {
        val parsedNumber = number.toInt
        (parsedNumber, f(parsedNumber))
      } else {
        (-1, false)
      }
    } catch {
      case e: NumberFormatException => {
        (-1, false)
      }
    }
  }

  /**
   * Date parsing - this is pretty much copied from GBIF source code and needs
   * splitting into several methods
   */
  def processEvent(guid:String, raw:FullRecord, processed:FullRecord) : Array[QualityAssertion] = {

    var assertions = new ArrayBuffer[QualityAssertion]
    var date: Option[java.util.Date] = None
    val now = new java.util.Date
    val currentYear = DateFormatUtils.format(now, "yyyy").toInt
    var comment = ""

    var (year,invalidYear) = validateNumber(raw.event.year,{year => year < 0 || year > currentYear})
    var (month,invalidMonth) = validateNumber(raw.event.month,{month => month < 1 || month > 12})
    var (day,invalidDay) = validateNumber(raw.event.day,{day => day < 0 || day > 31})
    var invalidDate = invalidYear || invalidDay || invalidMonth

    //check for sensible year value
    if (year > 0) {
      if (year < 100) {
      //parse 89 for 1989
        if (year > currentYear % 100) {
          // Must be in last century
          year += ((currentYear / 100) - 1) * 100;
        } else {
          // Must be in this century
          year += (currentYear / 100) * 100;
        }
      } else if (year >= 100 && year < 1700) {
        year = -1
        invalidDate = true;
        comment = "Year out of range"
      }
    }

    //construct
    if (year != -1 && month != -1 && day != -1) {
      try {
       val calendar = new GregorianCalendar(
          year.toInt ,
          month.toInt - 1,
          day.toInt
       );
       date = Some(calendar.getTime)
      } catch {
        case e: Exception => {
          invalidDate = true
          comment = "Invalid year, day, month"
        }
      }
    }

    //set the processed values
    if (year != -1) processed.event.year = year.toString
    if (month != -1) processed.event.month = month.toString
    if (day != -1) processed.event.day = day.toString
    if (!date.isEmpty) processed.event.eventDate = DateFormatUtils.format(date.get, "yyyy-MM-dd")

    //deal with event date
    if (date.isEmpty && raw.event.eventDate != null && !raw.event.eventDate.isEmpty) {
      val parsedDate = DateParser.parseDate(raw.event.eventDate)
      if(!parsedDate.isEmpty){
        //set processed values
          processed.event.eventDate = parsedDate.get.startDate
          processed.event.day = parsedDate.get.startDay
          processed.event.month = parsedDate.get.startMonth
          processed.event.year = parsedDate.get.startYear
      }
    }

    //deal with verbatim date
    if (date.isEmpty && raw.event.verbatimEventDate != null && !raw.event.verbatimEventDate.isEmpty) {
      val parsedDate = DateParser.parseDate(raw.event.verbatimEventDate)
      if(!parsedDate.isEmpty){
        //set processed values
          processed.event.eventDate = parsedDate.get.startDate
          processed.event.day = parsedDate.get.startDay
          processed.event.month = parsedDate.get.startMonth
          processed.event.year = parsedDate.get.startYear
      }
    }

    //if invalid date, add assertion
    if (invalidDate) {
      assertions + QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE,false,comment)
    }

    assertions.toArray
  }

  /**
   * Process the type status
   */
  def processTypeStatus(guid:String,raw:FullRecord,processed:FullRecord) : Array[QualityAssertion] = {

    if (raw.occurrence.typeStatus != null && !raw.occurrence.typeStatus.isEmpty) {
      val term = TypeStatus.matchTerm(raw.occurrence.typeStatus)
      if (term.isEmpty) {
        //add a quality assertion
        Array(QualityAssertion(AssertionCodes.UNRECOGNISED_TYPESTATUS,false,"Unrecognised type status"))
      } else {
        processed.occurrence.typeStatus = term.get.canonical
        Array()
      }
    } else {
      //NC I don't think that a missing type status needs to be reported in a QA
      // It should only be reported if we know for sure that an occurrence record is a typification record with a missing type
      //Array(QualityAssertion(AssertionCodes.MISSING_BASIS_OF_RECORD,false,"Missing type status"))
      Array()
    }
  }

  /**
   * Process basis of record
   */
  def processBasisOfRecord(guid:String, raw:FullRecord, processed:FullRecord) : Array[QualityAssertion] = {

    if (raw.occurrence.basisOfRecord == null || raw.occurrence.basisOfRecord.isEmpty) {
      //add a quality assertion
      Array(QualityAssertion(AssertionCodes.MISSING_BASIS_OF_RECORD,false,"Missing basis of record"))
    } else {
      val term = BasisOfRecord.matchTerm(raw.occurrence.basisOfRecord)
      if (term.isEmpty) {
        //add a quality assertion
        logger.debug("[QualityAssertion] " + guid + ", unrecognised BoR: " + guid + ", BoR:" + raw.occurrence.basisOfRecord)
        Array(QualityAssertion(AssertionCodes.MISSING_BASIS_OF_RECORD,false,"Unrecognised basis of record"))
      } else {
        processed.occurrence.basisOfRecord = term.get.canonical
        Array[QualityAssertion]()
      }
    }
  }

  /**
   * Process geospatial details
   */
  def processLocation(guid:String,raw:FullRecord, processed:FullRecord) : Array[QualityAssertion] = {
    //retrieve the point
    var assertions = new ArrayBuffer[QualityAssertion]

    if (raw.location.decimalLatitude != null && raw.location.decimalLongitude != null) {

      //TODO validate decimal degrees and parse degrees, minutes, seconds format
      processed.location.decimalLatitude = raw.location.decimalLatitude
      processed.location.decimalLongitude = raw.location.decimalLongitude

      //validate coordinate accuracy (coordinateUncertaintyInMeters) and coordinatePrecision (precision - A. Chapman)
      if(raw.location.coordinateUncertaintyInMeters!=null && raw.location.coordinateUncertaintyInMeters.length>0){
          //parse it into a numeric number in metres
          val parsedValue = DistanceRangeParser.parse(raw.location.coordinateUncertaintyInMeters)
          if(!parsedValue.isEmpty)
            processed.location.coordinateUncertaintyInMeters = parsedValue.get.toString
      }

      //generate coordinate accuracy if not supplied
      val point = LocationDAO.getByLatLon(raw.location.decimalLatitude, raw.location.decimalLongitude);
      if (!point.isEmpty) {

        //add state information
        processed.location.stateProvince = point.get.stateProvince
        processed.location.ibra = point.get.ibra
        processed.location.imcra = point.get.imcra
        processed.location.lga = point.get.lga
        processed.location.habitat = point.get.habitat

        //TODO - replace with country association with points via the gazetteer
        if(processed.location.imcra!=null && !processed.location.imcra.isEmpty
            || processed.location.ibra!=null && !processed.location.ibra.isEmpty){
            processed.location.country = "Australia"
        }

        //check matched stateProvince
        if (processed.location.stateProvince != null && raw.location.stateProvince != null) {
          //quality systemAssertions
          val stateTerm = States.matchTerm(raw.location.stateProvince)

          if (!stateTerm.isEmpty && !processed.location.stateProvince.equalsIgnoreCase(stateTerm.get.canonical)) {
            logger.debug("[QualityAssertion] " + guid + ", processed:" + processed.location.stateProvince 
                + ", raw:" + raw.location.stateProvince)
            //add a quality assertion
            val comment = "Supplied: " + stateTerm.get.canonical + ", calculated: " + processed.location.stateProvince
            assertions + QualityAssertion(AssertionCodes.STATE_COORDINATE_MISMATCH,false,comment)
            //store the assertion
          }
        }

        //check marine/non-marine
        if(processed.location.habitat!=null){

          //retrieve the species profile
          val taxonProfile = TaxonProfileDAO.getByGuid(processed.classification.taxonConceptID)
          if(!taxonProfile.isEmpty && taxonProfile.get.habitats!=null && taxonProfile.get.habitats.size>0){
            val habitatsAsString =  taxonProfile.get.habitats.reduceLeft(_+","+_)
            val habitatFromPoint = processed.location.habitat
            val habitatsForSpecies = taxonProfile.get.habitats
            //is "terrestrial" the same as "non-marine" ??
            val validHabitat = HabitatMap.areTermsCompatible(habitatFromPoint, habitatsForSpecies)
            if(!validHabitat.isEmpty){
              if(!validHabitat.get){
                if(habitatsAsString != "???"){ //HACK FOR BAD DATA
                  logger.debug("[QualityAssertion] ******** Habitats incompatible for UUID: " + guid + ", processed:" 
                      + processed.location.habitat + ", retrieved:" + habitatsAsString
                      + ", http://maps.google.com/?ll="+processed.location.decimalLatitude+","
                      + processed.location.decimalLongitude)
                  val comment = "Recognised habitats for species: " + habitatsAsString +
                       ", Value determined from coordinates: " + habitatFromPoint
                  assertions + QualityAssertion(AssertionCodes.COORDINATE_HABITAT_MISMATCH,false,comment)
                }
              }
            }
          }
        }

        //TODO check centre point of the state
        if(StateCentrePoints.coordinatesMatchCentre(point.get.stateProvince, raw.location.decimalLatitude, raw.location.decimalLongitude)){
          assertions + QualityAssertion(AssertionCodes.COORDINATES_CENTRE_OF_STATEPROVINCE,false,"Coordinates are centre point of "+point.get.stateProvince)
        }
      }
    }

    if(processed.location.stateProvince ==null){
      //process the supplied state
      val stateTerm = States.matchTerm(raw.location.stateProvince)
      if(!stateTerm.isEmpty){
        processed.location.stateProvince = stateTerm.get.canonical
      }
    }
    assertions.toArray
  }

  /**
   * Match the classification
   */
  def processClassification(guid:String, raw:FullRecord, processed:FullRecord) : Array[QualityAssertion] = {
    val classification = new LinnaeanRankClassification(
      raw.classification.kingdom,
      raw.classification.phylum,
      raw.classification.classs,
      raw.classification.order,
      raw.classification.family,
      raw.classification.genus,
      raw.classification.species,
      raw.classification.specificEpithet,
      raw.classification.subspecies,
      raw.classification.infraspecificEpithet,
      raw.classification.scientificName)
    //logger.debug("Record: "+occ.uuid+", classification for Kingdom: "+occ.kingdom+", Family:"+  occ.family +", Genus:"+  occ.genus +", Species: " +occ.species+", Epithet: " +occ.specificEpithet)
    try {
      val nsr = DAO.nameIndex.searchForRecord(classification, true)
      //store the matched classification
      if (nsr != null) {
        val classification = nsr.getRankClassification
        //store ".p" values
        processed.classification.kingdom = classification.getKingdom
        processed.classification.kingdomID = classification.getKid
        processed.classification.phylum = classification.getPhylum
        processed.classification.phylumID = classification.getPid
        processed.classification.classs = classification.getKlass
        processed.classification.classID = classification.getCid
        processed.classification.order = classification.getOrder
        processed.classification.orderID = classification.getOid
        processed.classification.family = classification.getFamily
        processed.classification.familyID = classification.getFid
        processed.classification.genus = classification.getGenus
        processed.classification.genusID = classification.getGid
        processed.classification.species = classification.getSpecies
        processed.classification.speciesID = classification.getSid
        processed.classification.specificEpithet = classification.getSpecificEpithet
        processed.classification.scientificName = classification.getScientificName
        processed.classification.taxonConceptID = nsr.getLsid
        processed.classification.left = nsr.getLeft
        processed.classification.right = nsr.getRight
        processed.classification.taxonRank = nsr.getRank.getRank
        processed.classification.taxonRankID = nsr.getRank.getId.toString
        //try to apply the vernacular name
        val taxonProfile = TaxonProfileDAO.getByGuid(nsr.getLsid)
        if(!taxonProfile.isEmpty && taxonProfile.get.commonName!=null){
          processed.classification.vernacularName = taxonProfile.get.commonName
        }
        Array()
      } else {
        logger.debug("[QualityAssertion] No match for record, classification for Kingdom: " +
            raw.classification.kingdom + ", Family:" + raw.classification.family + ", Genus:" + raw.classification.genus +
            ", Species: " + raw.classification.species + ", Epithet: " + raw.classification.specificEpithet)
        Array(QualityAssertion(AssertionCodes.NAME_NOTRECOGNISED, false, "Name not recognised"))
      }
    } catch {
      case he: HomonymException => logger.debug(he.getMessage,he); Array(QualityAssertion(AssertionCodes.HOMONYM_ISSUE, false, "Homonym issue resolving the classification"))
      case se: SearchResultException => logger.debug(se.getMessage,se); Array()
    }
  }
}
