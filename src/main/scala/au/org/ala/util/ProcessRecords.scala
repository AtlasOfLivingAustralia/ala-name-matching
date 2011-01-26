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
import java.util.GregorianCalendar
import scala.collection.mutable.ArrayBuffer
import au.org.ala.checklist.lucene.SearchResultException
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

  def main(args: Array[String]): Unit = {

    var counter = 0
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis

    //page over all records and process
    OccurrenceDAO.pageOverAll(Raw, record => {
      counter += 1
      if (!record.isEmpty) {

    	val raw = record.get
    	val guid = raw.o.uuid
    	
    	var processed = raw.clone
    	
    	var assertions = new ArrayBuffer[QualityAssertion]
    	
        //find a classification in NSLs
        assertions ++ processClassification(guid, raw, processed)

        //perform gazetteer lookups - just using point hash for now
        assertions ++ processLocation(guid, raw, processed)

        //temporal processing
        assertions ++ processEvent(guid, raw, processed)

        //basis of record parsing
        assertions ++ processBasisOfRecord(guid, raw, processed)

        //type status normalisation
        assertions ++ processTypeStatus(guid, raw, processed)

        //process the attribution - call out to the Collectory...
        assertions ++ processAttribution(guid, raw, processed)

        //perform SDS lookups - retrieve from BIE for now....
        
        // processImages
        // processLinkRecord
        // processIdentifierRecords 
        // 
        
        processed.assertions = assertions.toArray

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
  def processAttribution(guid:String, raw:FullRecord, processed:FullRecord) : Array[QualityAssertion] = {
	if(raw.o.institutionCode!=null && raw.o.collectionCode!=null){
	    val attribution = AttributionDAO.getByCodes(raw.o.institutionCode, raw.o.collectionCode)
	    if (!attribution.isEmpty) {
	      OccurrenceDAO.updateOccurrence(guid, attribution.get, Processed)
	      Array()
	    } else {
	      Array(QualityAssertion(AssertionCodes.OTHER_UNRECOGNISED_COLLECTIONCODE, false, "Unrecognised collection code"))
	    }
	} else {
		Array()
	}
  }

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
 
    var (year,invalidYear) = validateNumber(raw.e.year,{year => year < 0 || year > currentYear})
    var (month,invalidMonth) = validateNumber(raw.e.month,{month => month < 1 || month > 12})
    var (day,invalidDay) = validateNumber(raw.e.day,{day => day < 0 || day > 31})
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
    if (year != -1) processed.e.year = year.toString
    if (month != -1) processed.e.month = month.toString
    if (day != -1) processed.e.day = day.toString
    if (!date.isEmpty) processed.e.eventDate = DateFormatUtils.format(date.get, "yyyy-MM-dd")

    //deal with event date
    if (date.isEmpty && raw.e.eventDate != null && !raw.e.eventDate.isEmpty) {
      //TODO handle these formats
    	//	"1963-03-08T14:07-0600" is 8 Mar 1963 2:07pm in the time zone six hours earlier than UTC, 
    	//	"2009-02-20T08:40Z" is 20 Feb 2009 8:40am UTC, "1809-02-12" is 12 Feb 1809, 
    	//	"1906-06" is Jun 1906, "1971" is just that year, 
    	//	"2007-03-01T13:00:00Z/2008-05-11T15:30:00Z" is the interval between 1 Mar 2007 1pm UTC and 
    	//	11 May 2008 3:30pm UTC, "2007-11-13/15" is the interval between 13 Nov 2007 and 15 Nov 2007
      try {
        val eventDateParsed = DateUtils.parseDate(raw.e.eventDate,
          Array("yyyy-MM-dd", "yyyy-MM-dd'T'hh:mm:ss'Z'", "yyyy-MM-dd'T'hh:mm'Z'"))
        date = Some(eventDateParsed)
        processed.e.eventDate = DateFormatUtils.format(eventDateParsed, "yyyy-MM-dd")
        processed.e.day = DateFormatUtils.format(eventDateParsed, "dd")
        processed.e.month = DateFormatUtils.format(eventDateParsed, "MM")
        processed.e.year = DateFormatUtils.format(eventDateParsed, "yyyy")
      } catch {
        case e: Exception => {
          invalidDate = true
          comment = "Invalid eventDate"
        }
      }
      
      if (date.isEmpty && raw.e.eventDate != null && !raw.e.eventDate.isEmpty) {
	      try {
	        val eventDate = raw.e.eventDate.split("/").first
	        val eventDateParsed = DateUtils.parseDate(eventDate,
	          Array("yyyy-MM", "yyyy-MM-"))
//	        println(raw.e.eventDate)
	        processed.e.month = DateFormatUtils.format(eventDateParsed, "MM")
	        processed.e.year = DateFormatUtils.format(eventDateParsed, "yyyy")
//	        println("year: "+ processed.e.year)
	      } catch {
	        case e: Exception => {
	          comment = "Unable to parse event date"
	        }
	      }
      }
    }

    //deal with verbatim date
    if (date.isEmpty && raw.e.verbatimEventDate != null && !raw.e.verbatimEventDate.isEmpty) {
      try {
        val eventDate = raw.e.verbatimEventDate.split("/").first
        val eventDateParsed = DateUtils.parseDate(eventDate,
          Array("yyyy-MM-dd", "yyyy-MM-ddThh:mm-ss", "yyyy-MM-ddThh:mmZ"))
        processed.e.eventDate = DateFormatUtils.format(eventDateParsed, "yyyy-MM-dd")
        processed.e.day = DateFormatUtils.format(eventDateParsed, "dd")
        processed.e.month = DateFormatUtils.format(eventDateParsed, "MM")
        processed.e.year = DateFormatUtils.format(eventDateParsed, "yyyy")
      } catch {
        case e: Exception => {
          comment = "Unable to parse verbatim date"
        }
      }
    }
    
    //if invalid date, add assertion
    if (invalidDate) {
      assertions + QualityAssertion(AssertionCodes.OTHER_INVALID_DATE,false,comment)
//      OccurrenceDAO.addQualityAssertion(guid, qa, AssertionCodes.OTHER_INVALID_DATE)
    }
    
    
    assertions.toArray
  }

  /**
   * Process the type status
   */
  def processTypeStatus(guid:String,raw:FullRecord,processed:FullRecord) : Array[QualityAssertion] = {

    if (raw.o.typeStatus != null && raw.o.typeStatus.isEmpty) {
      val term = TypeStatus.matchTerm(raw.o.typeStatus)
      if (term.isEmpty) {
        //add a quality assertion
        Array(QualityAssertion(AssertionCodes.OTHER_UNRECOGNISED_TYPESTATUS,false,"Unrecognised type status"))
//        OccurrenceDAO.addQualityAssertion(guid, qa, AssertionCodes.OTHER_UNRECOGNISED_TYPESTATUS)
      } else {
        processed.o.basisOfRecord = term.get.canonical
        Array()
      }
    } else {
    	Array(QualityAssertion(AssertionCodes.OTHER_MISSING_BASIS_OF_RECORD,false,"Missing basis of record"))
    }
  }

  /**
   * Process basis of record
   */
  def processBasisOfRecord(guid:String, raw:FullRecord, processed:FullRecord) : Array[QualityAssertion] = {

    if (raw.o.basisOfRecord == null || raw.o.basisOfRecord.isEmpty) {
      //add a quality assertion
      Array(QualityAssertion(AssertionCodes.OTHER_MISSING_BASIS_OF_RECORD,false,"Missing basis of record"))
    } else {
      val term = BasisOfRecord.matchTerm(raw.o.basisOfRecord)
      if (term.isEmpty) {
        //add a quality assertion
        println("[QualityAssertion] " + guid + ", unrecognised BoR: " + guid + ", BoR:" + raw.o.basisOfRecord)
        Array(QualityAssertion(AssertionCodes.OTHER_MISSING_BASIS_OF_RECORD,false,"Unrecognised basis of record"))
//        Array(QualityAssertion(OccurrenceDAO.addQualityAssertion(guid, qa, AssertionCodes.OTHER_UNRECOGNISED_TYPESTATUS)))
      } else {
        processed.o.basisOfRecord = term.get.canonical
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
            val comment = "Supplied: " + stateTerm.get.canonical + ", calculated: " + processed.l.stateProvince
            assertions + QualityAssertion(AssertionCodes.GEOSPATIAL_STATE_COORDINATE_MISMATCH,false,comment)
            //store the assertion
//            OccurrenceDAO.addQualityAssertion(guid, qa, AssertionCodes.GEOSPATIAL_STATE_COORDINATE_MISMATCH)
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
        				if(habitatsAsString != "???"){ //HACK FOR BAD DATA
	        				println("[QualityAssertion] ******** Habitats incompatible for UUID: " + guid + ", processed:" + processed.l.habitat + ", retrieved:" + habitatsAsString
	        						+ ", http://maps.google.com/?ll="+processed.l.decimalLatitude+","+processed.l.decimalLongitude)
	        				val comment = "Recognised habitats for species: " + habitatsAsString+", Value determined from coordinates: "+habitatFromPoint
	        				assertions + QualityAssertion(AssertionCodes.COORDINATE_HABITAT_MISMATCH,false,comment)
	        				//OccurrenceDAO.addQualityAssertion(guid, qa, AssertionCodes.COORDINATE_HABITAT_MISMATCH)
        				}
        			}
        		}
        	}
        }
        
        //TODO check centre point of the state


      }
    }
	assertions.toArray
  }

  /**
   * Match the classification
   */
  def processClassification(guid:String, raw:FullRecord, processed:FullRecord) : Array[QualityAssertion] = {
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
        Array()
      } else {
        println("[QualityAssertion] No match for record, classification for Kingdom: " + raw.c.kingdom + ", Family:" + raw.c.family + ", Genus:" + raw.c.genus + ", Species: " + raw.c.species 
        		+ ", Epithet: " + raw.c.specificEpithet)
        Array(QualityAssertion(AssertionCodes.TAXONOMIC_NAME_NOTRECOGNISED, false, "Name not recognised"))
      }
    } catch {
      case e: HomonymException => Array(QualityAssertion(AssertionCodes.TAXONOMIC_HOMONYM_ISSUE, false, "Homonym issue resolving the classification"))
      case e: SearchResultException => Array()
    }
  }
}