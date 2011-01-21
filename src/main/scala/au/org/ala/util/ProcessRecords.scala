package au.org.ala.util

import au.org.ala.biocache.HabitatMap
import au.org.ala.biocache.Raw
import au.org.ala.biocache.Processed
import au.org.ala.biocache.TaxonProfileDAO
import au.org.ala.biocache.AttributionDAO
import org.apache.commons.lang.time.DateUtils
import java.util.Calendar
import org.apache.commons.lang.time.DateFormatUtils
import org.wyki.cassandra.pelops.Pelops
import au.org.ala.biocache.DAO
import au.org.ala.biocache.TypeStatus
import au.org.ala.biocache.BasisOfRecord
import au.org.ala.biocache.AssertionCodes
import au.org.ala.biocache.QualityAssertion
import au.org.ala.biocache.States
import au.org.ala.biocache.Event
import au.org.ala.biocache.Classification
import au.org.ala.biocache.Location
import au.org.ala.biocache.LocationDAO
import au.org.ala.biocache.Version
import au.org.ala.checklist.lucene.HomonymException
import au.org.ala.data.util.RankType
import au.org.ala.biocache.Occurrence
import au.org.ala.data.model.LinnaeanRankClassification
import au.org.ala.checklist.lucene.CBIndexSearch
import au.org.ala.biocache.OccurrenceDAO
import au.org.ala.biocache.FullRecord
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
 */
object ProcessRecords {

  def main(args: Array[String]): Unit = {

    var counter = 0
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis

    //page over all records and process
    OccurrenceDAO.pageOverAll(Raw, record => {
      counter += 1
      if (!record.isEmpty) {

    	val raw = record.get
    	var processed =  raw.clone
    	val guid = raw.o.uuid
    	
        //find a classification in NSLs
        processClassification(guid, raw, processed)

        //perform gazetteer lookups - just using point hash for now
        processLocation(guid, raw, processed)

        //temporal processing
        processEvent(guid, raw, processed)

        //basis of record parsing
        processBasisOfRecord(guid, raw, processed)

        //type status normalisation
        processTypeStatus(guid, raw, processed)

        //process the attribution - call out to the Collectory...
        processAttribution(guid, raw, processed)

        //perform SDS lookups - retrieve from BIE for now....
        
        // processImages
        // processLinkRecord
        // processIdentifierRecords 
        // 
        

        //store the occurrence
        OccurrenceDAO.updateOccurrence(guid, processed, Processed)
        
        //debug counter
        if (counter % 1000 == 0) {
          finishTime = System.currentTimeMillis
          println(counter + " >> Last key : " + raw.o.uuid + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
          startTime = System.currentTimeMillis
        }
      }
    })
    Pelops.shutdown
  }

  /**
   * select icm.institution_uid, icm.collection_uid,  ic.code, ic.name, ic.lsid, cc.code from inst_coll_mapping icm
   * inner join institution_code ic ON ic.id = icm.institution_code_id
   * inner join collection_code cc ON cc.id = icm.collection_code_id
   * limit 10;
   */
  def processAttribution(guid:String, raw:FullRecord, processed:FullRecord) {
    val attribution = AttributionDAO.getByCodes(raw.o.institutionCode, raw.o.collectionCode)
    if (!attribution.isEmpty) {
      OccurrenceDAO.updateOccurrence(guid, attribution.get, Processed)
    }
  }

  /**
   * Date parsing - this is pretty much copied from GBIF source code and needs
   * splitting into several methods
   */
  def processEvent(guid:String, raw:FullRecord, processed:FullRecord) {

    var year = -1
    var month = -1
    var day = -1
    var date: Option[java.util.Date] = None

    var invalidDate = false;
    val now = new java.util.Date
    val currentYear = DateFormatUtils.format(now, "yyyy").toInt
    var comment = ""

    try {
      if (raw.e.year != null) {
        year = raw.e.year.toInt
        if (year < 0 || year > currentYear) {
          invalidDate = true
          year = -1
        }
      }
    } catch {
      case e: NumberFormatException => {
        invalidDate = true
        comment = "Invalid year supplied"
        year = -1
      }
    }

    try {
      if (raw.e.month != null)
        month = raw.e.month.toInt
      if (month < 1 || month > 12) {
        month = -1
        invalidDate = true
      }
    } catch {
      case e: NumberFormatException => {
        invalidDate = true
        comment = "Invalid month supplied"
        month = -1
      }
    }

    try {
      if (raw.e.day != null)
        day = raw.e.day.toInt
      if (day < 0 || day > 31) {
        day = -1
        invalidDate = true
        comment = "Invalid day supplied"
      }
    } catch {
      case e: NumberFormatException => {
        invalidDate = true
        comment = "Invalid day supplied"
        day = -1
      }
    }

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
        val calendar = Calendar.getInstance
        calendar.set(year, month - 1, day, 12, 0, 0);
        date = Some(new java.util.Date(calendar.getTimeInMillis()))
      } catch {
        case e: Exception => {
          invalidDate = true
          comment = "Invalid year, day, month"
        }
      }
    }

    if (year != -1) processed.e.year = year.toString
    if (month != -1) processed.e.month = month.toString
    if (day != -1) processed.e.day = day.toString
    if (!date.isEmpty) processed.e.eventDate = DateFormatUtils.format(date.get, "yyyy-MM-dd")

    if (date.isEmpty && raw.e.eventDate != null && !raw.e.eventDate.isEmpty) {
      //TODO handle these formats
      //			"1963-03-08T14:07-0600" is 8 Mar 1963 2:07pm in the time zone six hours earlier than UTC, 
      //			"2009-02-20T08:40Z" is 20 Feb 2009 8:40am UTC, "1809-02-12" is 12 Feb 1809, 
      //			"1906-06" is Jun 1906, "1971" is just that year, 
      //			"2007-03-01T13:00:00Z/2008-05-11T15:30:00Z" is the interval between 1 Mar 2007 1pm UTC and 
      //			11 May 2008 3:30pm UTC, "2007-11-13/15" is the interval between 13 Nov 2007 and 15 Nov 2007
      try {
        val eventDateParsed = DateUtils.parseDate(raw.e.eventDate,
          Array("yyyy-MM-dd", "yyyy-MM-ddThh:mm-ss", "yyyy-MM-ddThh:mmZ"))
        processed.e.eventDate = DateFormatUtils.format(date.get, "yyyy-MM-dd")
      } catch {
        case e: Exception => {
          //handle "1906-06"
          invalidDate = true
          comment = "Invalid eventDate"
        }
      }
    }

    //deal with verbatim date
    if (date.isEmpty && raw.e.verbatimEventDate != null && !raw.e.verbatimEventDate.isEmpty) {
      try {
        val eventDate = raw.e.verbatimEventDate.split("/").first
        val eventDateParsed = DateUtils.parseDate(eventDate,
          Array("yyyy-MM-dd", "yyyy-MM-ddThh:mm-ss", "yyyy-MM-ddThh:mmZ"))
      } catch {
        case e: Exception => {
          invalidDate = true
          comment = "Unable to parse verbatim date"
        }
      }
    }

    if (invalidDate) {
      var qa = new QualityAssertion
      qa.assertionCode = AssertionCodes.OTHER_INVALID_DATE.code
      qa.positive = false
      qa.comment = comment
      qa.userId = "system"
      OccurrenceDAO.addQualityAssertion(guid, qa, AssertionCodes.OTHER_INVALID_DATE)
    }
  }


  /**
   * Process the type status
   */
  def processTypeStatus(guid:String,raw:FullRecord,processed:FullRecord) {

    if (raw.o.typeStatus != null && raw.o.typeStatus.isEmpty) {
      val term = TypeStatus.matchTerm(raw.o.typeStatus)
      if (term.isEmpty) {
        //add a quality assertion
        val qa = new QualityAssertion
        qa.positive = false
        qa.assertionCode = AssertionCodes.OTHER_UNRECOGNISED_TYPESTATUS.code
        qa.comment = "Unrecognised type status"
        qa.userId = "system"
        OccurrenceDAO.addQualityAssertion(guid, qa,  AssertionCodes.OTHER_UNRECOGNISED_TYPESTATUS)
      } else {
        processed.o.basisOfRecord = term.get.canonical
      }
    }
  }


  /**
   * Process basis of record
   */
  def processBasisOfRecord(guid:String, raw:FullRecord, processed:FullRecord) {

    if (raw.o.basisOfRecord == null || raw.o.basisOfRecord.isEmpty) {
      //add a quality assertion
      val qa = new QualityAssertion
      qa.positive = false
      qa.assertionCode = AssertionCodes.OTHER_MISSING_BASIS_OF_RECORD.code
      qa.comment = "Missing basis of record"
      qa.userId = "system"
      OccurrenceDAO.addQualityAssertion(guid, qa,  AssertionCodes.OTHER_UNRECOGNISED_TYPESTATUS)
    } else {
      val term = BasisOfRecord.matchTerm(raw.o.basisOfRecord)
      if (term.isEmpty) {
        //add a quality assertion
        println("[QualityAssertion] " + guid + ", unrecognised BoR: " + guid + ", BoR:" + raw.o.basisOfRecord)
        val qa = new QualityAssertion
        qa.positive = false
        qa.assertionCode = AssertionCodes.OTHER_BADLY_FORMED_BASIS_OF_RECORD.code
        qa.comment = "Unrecognised basis of record"
        qa.userId = "system"
        OccurrenceDAO.addQualityAssertion(guid, qa,  AssertionCodes.OTHER_UNRECOGNISED_TYPESTATUS)
      } else {
        processed.o.basisOfRecord = term.get.canonical
      }
    }
  }

  /**
   * Process geospatial details
   */
  def processLocation(guid:String,raw:FullRecord, processed:FullRecord) {
    //retrieve the point
    if (raw.l.decimalLatitude != null && raw.l.decimalLongitude != null) {

      //TODO validate decimal degrees
      processed.l.decimalLatitude = raw.l.decimalLatitude
      processed.l.decimalLongitude = raw.l.decimalLongitude

      //validate coordinate accuracy (coordinateUncertaintyInMeters) and coordinatePrecision (precision - A. Chapman)
      
      //generate coordinate accuracy if not supplied
      val point = LocationDAO.getByLatLon(raw.l.decimalLatitude, raw.l.decimalLongitude);
      if (!point.isEmpty) {

        //add state information
        processed.l.stateProvince = point.get.stateProvince
        processed.l.ibra = point.get.ibra
        processed.l.imcra = point.get.imcra
        processed.l.lga = point.get.lga
        processed.l.habitat = point.get.habitat

        //check matched stateProvince
        if (processed.l.stateProvince != null && raw.l.stateProvince != null) {
          //quality assertions
          val stateTerm = States.matchTerm(raw.l.stateProvince)

          if (!stateTerm.isEmpty && !processed.l.stateProvince.equalsIgnoreCase(stateTerm.get.canonical)) {
            println("[QualityAssertion] " + guid + ", processed:" + processed.l.stateProvince + ", raw:" + raw.l.stateProvince)
            //add a quality assertion
            val qa = new QualityAssertion
            qa.positive = false
            qa.assertionCode = AssertionCodes.GEOSPATIAL_STATE_COORDINATE_MISMATCH.code
            qa.comment = "Supplied: " + stateTerm.get.canonical + ", calculated: " + processed.l.stateProvince
            qa.userId = "system"
            //store the assertion
            OccurrenceDAO.addQualityAssertion(guid, qa, AssertionCodes.GEOSPATIAL_STATE_COORDINATE_MISMATCH)
          }
        }

        //check marine/non-marine
        if(processed.l.habitat!=null){
        	
        	//retrieve the species profile
        	val taxonProfile = TaxonProfileDAO.getByGuid(processed.c.taxonConceptID)
        	if(!taxonProfile.isEmpty && taxonProfile.get.habitats!=null && taxonProfile.get.habitats.size>0){
        		val habitatsAsString =  taxonProfile.get.habitats.reduceLeft(_+","+_)
        		val habitatFromPoint = processed.l.habitat
        		val habitatsForSpecies = taxonProfile.get.habitats
        		//is "terrestrial" the same as "non-marine" ??
        		val validHabitat = HabitatMap.areTermsCompatible(habitatFromPoint, habitatsForSpecies)
        		if(!validHabitat.isEmpty){
        			if(!validHabitat.get){
        				println("[QualityAssertion] ******** Habitats incompatible for UUID: " + guid + ", processed:" + processed.l.habitat + ", retrieved:" + habitatsAsString)
        				var qa = new QualityAssertion
        				qa.userId = "system"
        				qa.assertionCode = AssertionCodes.COORDINATE_HABITAT_MISMATCH.code
        				qa.comment = "Recognised habitats for species: " + habitatsAsString+", Value determined from coordinates: "+habitatFromPoint
        				qa.positive = false
        				OccurrenceDAO.addQualityAssertion(guid, qa, AssertionCodes.COORDINATE_HABITAT_MISMATCH)
        			}
        		}
        	}
        }
        
        //check centre point of the state


      }
    }
  }

  /**
   * Match the classification
   */
  def processClassification(guid:String, raw:FullRecord, processed:FullRecord) {
    val classification = new LinnaeanRankClassification(
      raw.c.kingdom,
      raw.c.phylum,
      raw.c.classs,
      raw.c.order,
      raw.c.family,
      raw.c.genus,
      raw.c.species,
      raw.c.specificEpithet,
      raw.c.subspecies,
      raw.c.infraspecificEpithet,
      raw.c.scientificName)
    //println("Record: "+occ.uuid+", classification for Kingdom: "+occ.kingdom+", Family:"+  occ.family +", Genus:"+  occ.genus +", Species: " +occ.species+", Epithet: " +occ.specificEpithet)
    try {
      val nsr = DAO.nameIndex.searchForRecord(classification, true)
      //store the matched classification
      if (nsr != null) {
        val classification = nsr.getRankClassification
        //store ".p" values
        processed.c.kingdom = classification.getKingdom
        processed.c.phylum = classification.getPhylum
        processed.c.classs = classification.getKlass
        processed.c.order = classification.getOrder
        processed.c.family = classification.getFamily
        processed.c.genus = classification.getGenus
        processed.c.species = classification.getSpecies
        processed.c.specificEpithet = classification.getSpecificEpithet
        processed.c.scientificName = classification.getScientificName
        processed.c.taxonConceptID = nsr.getLsid
      } else {
        println("[QualityAssertion] No match for record, classification for Kingdom: " + raw.c.kingdom + ", Family:" + raw.c.family + ", Genus:" + raw.c.genus + ", Species: " + raw.c.species 
        		+ ", Epithet: " + raw.c.specificEpithet)
      }
    } catch {
      case e: HomonymException => //println("Homonym exception for record, classification for Kingdom: "+raw.kingdom+", Family:"+  raw.family +", Genus:"+  raw.genus +", Species: " +raw.species+", Epithet: " +raw.specificEpithet)
      case e: Exception => e.printStackTrace
    }
  }
}