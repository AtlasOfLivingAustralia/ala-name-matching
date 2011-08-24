package au.org.ala.biocache

import org.apache.commons.lang.time.DateFormatUtils
import au.org.ala.checklist.lucene.HomonymException
import collection.mutable.{HashMap, ArrayBuffer}
import java.util.GregorianCalendar
import au.org.ala.checklist.lucene.SearchResultException
import org.slf4j.LoggerFactory
import au.org.ala.data.model.LinnaeanRankClassification
import au.org.ala.sds.validation.FactCollection
import au.org.ala.sds.validation.ServiceFactory
import au.org.ala.sds.validation.ConservationOutcome
import au.org.ala.sds.validation.MessageFactory
import org.apache.commons.lang.StringUtils

/**
 * Trait to be implemented by all processors. 
 * This is a simple Command Pattern.
 */
trait Processor {
  def process(uuid:String, raw:FullRecord, processed:FullRecord) : Array[QualityAssertion]
  def getName:String
}

/**
 * Singleton that maintains the workflow
 */
object Processors {

  def foreach(proc: Processor => Unit) = processorMap.values.foreach(proc)

  val processorMap = Map(
      "IMAGE"-> new ImageProcessor,
      "ATTR" -> new AttributionProcessor,
      "CLASS"-> new ClassificationProcessor,
      "BOR" -> new BasisOfRecordProcessor,
      "EVENT"-> new EventProcessor,
      "LOC"-> new LocationProcessor,
      "TS" -> new TypeStatusProcessor
  )
  //TODO A better way to do this. Maybe need to group QA failures by issue type instead of phase. 
  //Can't change until we are able to reprocess the complete set records.
  def getProcessorForError(code:Int):String={
	  code match{
	    case c if c >= AssertionCodes.geospatialBounds._1 && c<AssertionCodes.geospatialBounds._2 => "loc"
	    case c if c >= AssertionCodes.taxonomicBounds._1 && c< AssertionCodes.taxonomicBounds._2 => "class"
	    case c if c == AssertionCodes.MISSING_BASIS_OF_RECORD.code || c ==AssertionCodes.BADLY_FORMED_BASIS_OF_RECORD.code => "bor"
	    case c if c == AssertionCodes.UNRECOGNISED_TYPESTATUS.code =>"type"
	    case c if c == AssertionCodes.UNRECOGNISED_COLLECTIONCODE.code ||c== AssertionCodes.UNRECOGNISED_INSTITUTIONCODE.code => "attr"	    
	    case c if c == AssertionCodes.INVALID_IMAGE_URL.code => "image"	    
	    case c if c >= AssertionCodes.temporalBounds._1 && c<AssertionCodes.temporalBounds._2 =>"event"
	    case _ => ""
	  }
	 
  }
}

class ImageProcessor extends Processor {

  //Regular expression used to parse an image URL - adapted from 
  //http://stackoverflow.com/questions/169625/regex-to-check-if-valid-url-that-ends-in-jpg-png-or-gif#169656
  lazy val imageParser = """^(https?://(?:[a-zA-Z0-9\-]+\.)+[a-zA-Z]{2,6}(?:/[^/#]+)+\.(?:jpg|gif|png|jpeg))$""".r
  
  /**
   * validates that the associated media is a valid image url
   */
  def process(guid:String, raw:FullRecord, processed:FullRecord) :Array[QualityAssertion] ={
    val urls = raw.occurrence.associatedMedia
    // val matchedGroups = groups.collect{case sg: SpeciesGroup if sg.values.contains(cl.getter(sg.rank)) => sg.name}
    if(urls != null){      
      val aurls = urls.split(";").map(url=> url.trim)
      processed.occurrence.images = aurls.filter(isValidImageURL(_))
      if(aurls.length != processed.occurrence.images.length)
          return Array(QualityAssertion(AssertionCodes.INVALID_IMAGE_URL, "URL can not be an image"))
    }
    Array()
  }

  def getName = "image"

  private def isValidImageURL(url:String) : Boolean = {
    !imageParser.unapplySeq(url.trim).isEmpty || url.startsWith(MediaStore.rootDir)
  }
}

class AttributionProcessor extends Processor {
  
  val logger = LoggerFactory.getLogger("AttributionProcessor")

  /**
   * Retrieve attribution infromation from collectory and tag the occurrence record.
   */
  def process(guid: String, raw: FullRecord, processed: FullRecord): Array[QualityAssertion] = {
    var assertions = new ArrayBuffer[QualityAssertion]

    //get the data resource information to check if it has mapped collections
    if (raw.attribution.dataResourceUid != null) {
      val dataResource = AttributionDAO.getDataResourceByUid(raw.attribution.dataResourceUid)
      if (!dataResource.isEmpty) {

        if (dataResource.get.hasMappedCollections && raw.occurrence.collectionCode != null) {
          val collCode = raw.occurrence.collectionCode
          //use the collection code as the institution code when one does not exist
          val instCode = if (raw.occurrence.institutionCode != null) raw.occurrence.institutionCode else collCode
          val attribution = AttributionDAO.getByCodes(instCode, collCode)
          if (!attribution.isEmpty) {
            processed.attribution = attribution.get
            //need to reinitialise the object array - DM switched to def, that
            //way objectArray created each time its accessed
            //processed.reinitObjectArray
          } else {
            assertions ++= Array(QualityAssertion(AssertionCodes.UNRECOGNISED_COLLECTIONCODE, "Unrecognised collection code institution code combination"))
          }
        }
        //update the details that come from the data resource
        processed.attribution.dataResourceName = dataResource.get.dataResourceName
        processed.attribution.dataProviderUid = dataResource.get.dataProviderUid
        processed.attribution.dataProviderName = dataResource.get.dataProviderName
        processed.attribution.dataHubUid = dataResource.get.dataHubUid
        processed.attribution.dataResourceUid = dataResource.get.dataResourceUid
        //only add the taxonomic hints if they were not populated by the collection
        if (processed.attribution.taxonomicHints == null)
          processed.attribution.taxonomicHints = dataResource.get.taxonomicHints
      }
    }
    
    assertions.toArray
  }

  def getName = "attr"
}

class EventProcessor extends Processor {

  import au.org.ala.util.StringHelper._

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
      case e: NumberFormatException => (-1, false)
    }
  }

  /**
   * Date parsing - this is pretty much copied from GBIF source code and needs
   * splitting into several methods
   */
  def process(guid:String, raw:FullRecord, processed:FullRecord) : Array[QualityAssertion] = {

    if( (raw.event.day == null || raw.event.day.isEmpty)
      && (raw.event.month == null || raw.event.month.isEmpty)
      && (raw.event.year == null || raw.event.year.isEmpty)
      && (raw.event.eventDate == null || raw.event.eventDate.isEmpty)
      && (raw.event.verbatimEventDate == null || raw.event.verbatimEventDate.isEmpty)
    )
      return Array(QualityAssertion(AssertionCodes.MISSING_COLLECTION_DATE,"No date information supplied"))

    var assertions = new ArrayBuffer[QualityAssertion]
    var date: Option[java.util.Date] = None
    val currentYear = DateUtil.getCurrentYear
    var comment = ""

    var (year,validYear) = validateNumber(raw.event.year,{year => year > 0 && year <= currentYear})
    var (month,validMonth) = validateNumber(raw.event.month,{month => month >= 1 && month <= 12})
    var (day,validDay) = validateNumber(raw.event.day,{day => day >= 1 && day <= 31})

    if(!validMonth && raw.event.month.isInt && raw.event.day.isInt){
      //are day and month transposed?
      val monthValue = raw.event.month.toInt
      val dayValue = raw.event.day.toInt
      if(monthValue > 12 && dayValue < 12){
        month = dayValue
        day = monthValue
        assertions + QualityAssertion(AssertionCodes.DAY_MONTH_TRANSPOSED,"Assume day and month transposed")
        validMonth = true
      }
    }

    //check for sensible year value
    if (year > 0) {
      if (year < 100) {
      //parse 89 for 1989
        if (year > currentYear % 100) {
          // Must be in last century
          year += ((currentYear / 100) - 1) * 100
        } else {
          // Must be in this century
          year += (currentYear / 100) * 100
        }
      } else if (year >= 100 && year < 1700) {
        year = -1
        validYear = false
        comment = "Year out of range"
      }
    }

    var validDate = validYear && validDay && validMonth

    //construct
    if (validDate) {
      try {
         
       val calendar = new GregorianCalendar(
          year.toInt,
          month.toInt - 1,
          day.toInt
       );
       //don't allow the calendar to be lenient we want exceptions with incorrect dates
       calendar.setLenient(false);
       date = Some(calendar.getTime)
      } catch {
        case e: Exception => {
          validDate = false
          comment = "Invalid year, day, month"
        }
      }
    }

    //set the processed values
    if (validYear) processed.event.year = year.toString
    if (validMonth) processed.event.month = String.format("%02d",int2Integer(month)) //NC ensure that a month is 2 characters long
    if (validDay) processed.event.day = day.toString
    if (!date.isEmpty) processed.event.eventDate = DateFormatUtils.format(date.get, "yyyy-MM-dd")

    //deal with event date if we dont have separate day, month, year fields
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
    if (!validDate) {
      assertions + QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE,comment)
    }

    assertions.toArray
  }

  def getName = "event"
}

class TypeStatusProcessor extends Processor {
  /**
   * Process the type status
   */
  def process(guid:String,raw:FullRecord,processed:FullRecord) : Array[QualityAssertion] = {

    if (raw.identification.typeStatus != null && !raw.identification.typeStatus.isEmpty) {
      val term = TypeStatus.matchTerm(raw.identification.typeStatus)
      if (term.isEmpty) {
        //add a quality assertion
        Array(QualityAssertion(AssertionCodes.UNRECOGNISED_TYPESTATUS,"Unrecognised type status"))
      } else {
        processed.identification.typeStatus = term.get.canonical
        Array()
      }
    } else {
      Array()
    }
  }
  def getName = "type"
}

class BasisOfRecordProcessor extends Processor {

  val logger = LoggerFactory.getLogger("BasisOfRecordProcessor")
  /**
   * Process basis of record
   */
  def process(guid:String, raw:FullRecord, processed:FullRecord) : Array[QualityAssertion] = {

    if (raw.occurrence.basisOfRecord == null || raw.occurrence.basisOfRecord.isEmpty) {
      //add a quality assertion
      //check to see if there is a default value for this
      val dr = AttributionDAO.getDataResourceByUid(raw.attribution.dataResourceUid)
      if(!dr.isEmpty && dr.get.getDefaultDwcValues != null && dr.get.getDefaultDwcValues().contains("basisOfRecord")){
        //the default balue will be one of the vocab
        processed.occurrence.basisOfRecord = dr.get.getDefaultDwcValues()("basisOfRecord")
        //TODO set the flag that default values have been used
        processed.setDefaultValuesUsed(true)
        Array[QualityAssertion]()
      }
      else
          Array(QualityAssertion(AssertionCodes.MISSING_BASIS_OF_RECORD,"Missing basis of record"))
    } else {
      val term = BasisOfRecord.matchTerm(raw.occurrence.basisOfRecord)
      if (term.isEmpty) {
        //add a quality assertion
        logger.debug("[QualityAssertion] " + guid + ", unrecognised BoR: " + guid + ", BoR:" + raw.occurrence.basisOfRecord)
        Array(QualityAssertion(AssertionCodes.BADLY_FORMED_BASIS_OF_RECORD,"Unrecognised basis of record"))
      } else {
        processed.occurrence.basisOfRecord = term.get.canonical
        Array[QualityAssertion]()
      }
    }
  }
  def getName() = "bor"
}

class LocationProcessor extends Processor {

  val logger = LoggerFactory.getLogger("LocationProcessor")
  //This is being initialised here because it may take some time to load all the XML records...
  lazy val sdsFinder = Config.sdsFinder

  import au.org.ala.util.StringHelper._

  /**
   * Process geospatial details
   *
   * TODO: Handle latitude and longitude that is supplied in verbatim format
   * We will need to parse a variety of formats. Bryn was going to find some regular
   * expressions/test cases he has used previously...
   *
   */
  def process(guid: String, raw: FullRecord, processed: FullRecord): Array[QualityAssertion] = {

    //retrieve the point
    var assertions = new ArrayBuffer[QualityAssertion]

    //handle the situation where the coordinates have already been sensitised
    setProcessedCoordinates(raw, processed)

    //Continue processing location if a processed longitude and latitude exists
    if (processed.location.decimalLatitude != null && processed.location.decimalLongitude != null) {

      //validate the coordinate values
      validateCoordinatesValues(raw, processed, assertions)

      //validate coordinate accuracy (coordinateUncertaintyInMeters) and coordinatePrecision (precision - A. Chapman)
      checkCoordinateUncertainty(raw, processed, assertions)

      //generate coordinate accuracy if not supplied
      var point = LocationDAO.getByLatLon(processed.location.decimalLatitude, processed.location.decimalLongitude);

      if (!point.isEmpty) {
        val (location, environmentalLayers, contextualLayers) = point.get
        processed.locationDetermined = true;
        //add state information
        processed.location.stateProvince = location.stateProvince
        processed.location.ibra = location.ibra
        processed.location.imcra = location.imcra
        processed.location.lga = location.lga
        processed.location.country = location.country
        //add the layers that are associated with the point
        processed.environmentalLayers = environmentalLayers
        processed.contextualLayers = contextualLayers
        // TODO find out if the EEZ layer has been include so the value can be obtained for this.
        processed.location.habitat = {
          if (!StringUtils.isEmpty(location.ibra)) "Terrestrial"
          else if (!StringUtils.isEmpty(location.imcra)) "Marine"
          else null
        }

        //check matched stateProvince
        checkForStateMismatch(raw, processed, assertions)

        //retrieve the species profile
        val taxonProfile = TaxonProfileDAO.getByGuid(processed.classification.taxonConceptID)
        if (!taxonProfile.isEmpty) {
          //add the conservation status if necessary
          addConservationStatus(raw, processed, taxonProfile.get)
          //check marine/non-marine
          checkForHabitatMismatch(raw, processed, taxonProfile.get, assertions)
        }

        //check centre point of the state
        if (StateProvinceCentrePoints.coordinatesMatchCentre(location.stateProvince, raw.location.decimalLatitude, raw.location.decimalLongitude)) {
          assertions + QualityAssertion(AssertionCodes.COORDINATES_CENTRE_OF_STATEPROVINCE, "Coordinates are centre point of " + location.stateProvince)
        }

        //check centre point of the country
        if (CountryCentrePoints.coordinatesMatchCentre(location.country, raw.location.decimalLatitude, raw.location.decimalLongitude)) {
          assertions + QualityAssertion(AssertionCodes.COORDINATES_CENTRE_OF_COUNTRY, "Coordinates are centre point of " + location.country)
        }

        //sensitise the coordinates if necessary.  Do this last so that habitat checks etc are performed on originally supplied coordinates
        processSensitivity(raw, processed, location)
      }
    }

    //Only process the raw state value if no latitude and longitude is provided
    if (processed.location.stateProvince == null && raw.location.decimalLatitude == null && raw.location.decimalLongitude == null) {
      //process the supplied state
      val stateTerm = StateProvinces.matchTerm(raw.location.stateProvince)
      if (!stateTerm.isEmpty) {
        processed.location.stateProvince = stateTerm.get.canonical
      }
    }
    assertions.toArray
  }

  def setProcessedCoordinates(raw: FullRecord, processed: FullRecord) {
    //handle the situation where the coordinates have already been sensitised
    if (raw.location.originalDecimalLatitude != null && raw.location.originalDecimalLongitude != null) {
      processed.location.decimalLatitude = raw.location.originalDecimalLatitude
      processed.location.decimalLongitude = raw.location.originalDecimalLongitude
      processed.location.verbatimLatitude = raw.location.originalVerbatimLatitude
      processed.location.verbatimLongitude = raw.location.originalVerbatimLongitude

    } else if (raw.location.decimalLatitude != null && raw.location.decimalLongitude != null) {
      //check to see if we have coordinates specified
      processed.location.decimalLatitude = raw.location.decimalLatitude
      processed.location.decimalLongitude = raw.location.decimalLongitude

    } else if (raw.location.verbatimLatitude != null && raw.location.verbatimLongitude != null) {
      //parse the expressions into their decimal equivalents
      processed.location.decimalLatitude = VerbatimLatLongParser.parseToStringOrNull(raw.location.verbatimLatitude)
      processed.location.decimalLongitude = VerbatimLatLongParser.parseToStringOrNull(raw.location.verbatimLongitude)
    }
  }

  def checkCoordinateUncertainty(raw: FullRecord, processed: FullRecord, assertions: ArrayBuffer[QualityAssertion]) {
    //validate coordinate accuracy (coordinateUncertaintyInMeters) and coordinatePrecision (precision - A. Chapman)
    if (raw.location.coordinateUncertaintyInMeters != null && raw.location.coordinateUncertaintyInMeters.length > 0) {
      //parse it into a numeric number in metres
      //TODO should this be a whole number??
      val parsedValue = DistanceRangeParser.parse(raw.location.coordinateUncertaintyInMeters)
      if (!parsedValue.isEmpty) {
        processed.location.coordinateUncertaintyInMeters = parsedValue.get.toString
      } else {
        val comment = "Supplied uncertainty, " + raw.location.coordinateUncertaintyInMeters + ", is not a supported format"
        assertions + QualityAssertion(AssertionCodes.UNCERTAINTY_RANGE_MISMATCH, comment)
      }
    } else {
      //check to see if the uncertainty has incorrectly been put in the precision
      if (raw.location.coordinatePrecision != null) {
        //TODO work out what sort of custom parsing is necessary
        val value = raw.location.coordinatePrecision.toFloatWithOption
        if (!value.isEmpty && value.get > 1) {
          processed.location.coordinateUncertaintyInMeters = value.get.toInt.toString
          val comment = "Supplied precision, " + raw.location.coordinatePrecision + ", is assumed to be uncertainty in metres";
          assertions + QualityAssertion(AssertionCodes.UNCERTAINTY_IN_PRECISION, comment)
        }
      }
    }
    //if the coordinateUncertainty is still empty populate it with the default
    // value (we don't test until now because the SDS will sometime include coordinate uncertainty)
    if (processed.location.coordinateUncertaintyInMeters == null) {
      processed.location.coordinateUncertaintyInMeters = "1000"
      assertions + QualityAssertion(AssertionCodes.UNCERTAINTY_NOT_SPECIFIED, "Uncertainty was not supplied, using default value 1000")
    }
  }

  def checkForHabitatMismatch(raw: FullRecord, processed: FullRecord, taxonProfile: TaxonProfile, assertions: ArrayBuffer[QualityAssertion]) {
    if (processed.location.habitat != null && taxonProfile.habitats != null && !taxonProfile.habitats.isEmpty) {
      val habitatsAsString = taxonProfile.habitats.mkString(",")
      val habitatFromPoint = processed.location.habitat
      val habitatsForSpecies = taxonProfile.habitats
      //is "terrestrial" the same as "non-marine" ??
      val validHabitat = HabitatMap.areTermsCompatible(habitatFromPoint, habitatsForSpecies)
      if (!validHabitat.isEmpty) {
        if (!validHabitat.get) {
          if (habitatsAsString != "???") {
            //HACK FOR BAD DATA
            logger.debug("[QualityAssertion] ******** Habitats incompatible for ROWKEY: " + raw.rowKey + ", processed:"
              + processed.location.habitat + ", retrieved:" + habitatsAsString
              + ", http://maps.google.com/?ll=" + processed.location.decimalLatitude + ","
              + processed.location.decimalLongitude)
            val comment = "Recognised habitats for species: " + habitatsAsString +
              ", Value determined from coordinates: " + habitatFromPoint
            assertions + QualityAssertion(AssertionCodes.COORDINATE_HABITAT_MISMATCH, comment)
          }
        }
      }
    }
  }

  def addConservationStatus(raw: FullRecord, processed: FullRecord, taxonProfile: TaxonProfile) {
    //add the conservation status if necessary
    if (processed.location.country == "Australia"  && taxonProfile.conservation != null) {
      val aust = taxonProfile.retrieveConservationStatus(processed.location.country)
      val state = taxonProfile.retrieveConservationStatus(processed.location.stateProvince)
      processed.occurrence.austConservation = aust.getOrElse(null)
      processed.occurrence.stateConservation = state.getOrElse(null)
    }
  }

  def checkForStateMismatch(raw: FullRecord, processed: FullRecord, assertions: ArrayBuffer[QualityAssertion]) {
    //check matched stateProvince
    if (processed.location.stateProvince != null && raw.location.stateProvince != null) {
      //quality systemAssertions
      val stateTerm = StateProvinces.matchTerm(raw.location.stateProvince)
      if (!stateTerm.isEmpty && !processed.location.stateProvince.equalsIgnoreCase(stateTerm.get.canonical)) {
        logger.debug("[QualityAssertion] " + raw.rowKey + ", processed:" + processed.location.stateProvince
          + ", raw:" + raw.location.stateProvince)
        //add a quality assertion
        val comment = "Supplied: " + stateTerm.get.canonical + ", calculated: " + processed.location.stateProvince
        assertions + QualityAssertion(AssertionCodes.STATE_COORDINATE_MISMATCH, comment)
      }
    }
  }

  /**
   * Performs a bunch of the coordinate validations
   */
  def validateCoordinatesValues(raw: FullRecord, processed: FullRecord, assertions: ArrayBuffer[QualityAssertion]) = {
    //when the locality is Australia latitude needs to be negative and longitude needs to be positive
    //TO DO fix this so that it uses the gazetteer to determine whether or not coordinates
    val latWithOption = processed.location.decimalLatitude.toFloatWithOption
    val lonWithOption = processed.location.decimalLongitude.toFloatWithOption

    if (!latWithOption.isEmpty && !lonWithOption.isEmpty) {

      val lat = latWithOption.get
      val lon = lonWithOption.get

      //Test that coordinates are in range
      if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
        //test to see if they have been inverted  (TODO other tests for inversion...)
        if (lon >= -90 && lon <= 90 && lat >= -180 && lat <= 180) {
          assertions + QualityAssertion(AssertionCodes.INVERTED_COORDINATES, "Assume that coordinates have been inverted. Original values: " +
            processed.location.decimalLatitude + "," + processed.location.decimalLongitude)
          val tmp = processed.location.decimalLatitude
          processed.location.decimalLatitude = processed.location.decimalLongitude
          processed.location.decimalLongitude = tmp
        } else {
          assertions + QualityAssertion(AssertionCodes.COORDINATES_OUT_OF_RANGE, "Coordinates are out of range: " +
            processed.location.decimalLatitude + "," + processed.location.decimalLongitude)
        }
      }

      if (lat == 0.0f && lon == 0.0f) {
        assertions + QualityAssertion(AssertionCodes.ZERO_COORDINATES, "Coordinates 0,0")
        processed.location.decimalLatitude = null
        processed.location.decimalLongitude = null
      }

      if (raw.location.country != null && raw.location.country !="") {
        val country = Countries.matchTerm(raw.location.country)
        if(!country.isEmpty){

          val latlngBBoxOption = CountryCentrePoints.matchName(country.get.canonical)
          latlngBBoxOption match {
            case Some((latlng, bbox)) => {

              if(!bbox.containsPoint(lat,lon)){
                if(bbox.containsPoint(lat * -1,lon)){
                  //latitude is negated
                  assertions + QualityAssertion(AssertionCodes.NEGATED_LATITUDE,
                    "Latitude seems to be negated.  Original value:" + processed.location.decimalLatitude)
                  processed.location.decimalLatitude = (lat * -1).toString
                }
                if(bbox.containsPoint(lat,lon * -1)){
                  //point in wrong EW hemisphere - what do we do?
                  assertions + QualityAssertion(AssertionCodes.NEGATED_LONGITUDE,
                    "Longitude seems to be negated. Original value: " + processed.location.decimalLongitude)
                  processed.location.decimalLongitude = (lon * -1).toString
                }
              }
            }
            case _ => //do nothing
          }
        } else {
          assertions + QualityAssertion(AssertionCodes.UNKNOWN_COUNTRY_NAME, "Country name '"+raw.location.country+"' not recognised.")
        }
      }
    }
  }

  /**Performs all the sensitivity processing.  Returns the new point ot be working with */
  def processSensitivity(raw: FullRecord, processed: FullRecord, location: Location) = {

    //Perform sensitivity actions if the record was located in Australia
    //removed the check for Australia because some of the loc cache records have a state without country (-43.08333, 147.66670)
    if (location.stateProvince != null) {
      //location.country == "Australia"){
      val sensitiveTaxon = {
        //check to see if the rank of the matched taxon is above a speceies
        val rankID = au.org.ala.util.ReflectBean.any2Int(processed.classification.taxonRankID)
        if (rankID != null && (rankID > 6000 || !processed.classification.scientificName.contains(" "))) {
          //match is based on a known LSID
          sdsFinder.findSensitiveSpeciesByLsid(processed.classification.taxonConceptID)
        } else {
          // use the "exact match" name
          val exact = getExactSciName(raw)
          if (exact != null)
            sdsFinder.findSensitiveSpeciesByExactMatch(exact)
          else
            null
        }
      }

      //only proceed if the taxon has been identified as sensitive
      if (sensitiveTaxon != null) {
        //populate the Facts that we know
        val facts = new FactCollection
        facts.add(FactCollection.DECIMAL_LATITUDE_KEY, processed.location.decimalLatitude)
        facts.add(FactCollection.DECIMAL_LONGITUDE_KEY, processed.location.decimalLongitude)
        facts.add(FactCollection.STATE_PROVINCE_KEY, location.stateProvince)

        val service = ServiceFactory.createValidationService(sensitiveTaxon)
        //TODO fix for different types of outcomes...
        val voutcome = service.validate(facts)
        if (voutcome.isValid && voutcome.isInstanceOf[ConservationOutcome]) {
          val outcome = voutcome.asInstanceOf[ConservationOutcome]
          val gl = outcome.getGeneralisedLocation
          if (!gl.isSensitive) {
            //don't generalise since it is not part of the Location where it should be generalised
          } else if (!gl.isGeneralised) {
            //already been genaralised by data resource provider non need to do anything.
            processed.occurrence.dataGeneralizations = gl.getDescription
          } else {
            //store the generalised values as the raw.location.decimalLatitude/Longitude
            //store the orginal as a hidden value
            raw.location.originalDecimalLatitude = processed.location.decimalLatitude
            raw.location.originalDecimalLongitude = processed.location.decimalLongitude
            raw.location.decimalLatitude = gl.getGeneralisedLatitude
            raw.location.decimalLongitude = gl.getGeneralisedLongitude
            raw.location.originalLocationRemarks = raw.location.locationRemarks
            raw.location.originalVerbatimLatitude = raw.location.verbatimLatitude
            raw.location.originalVerbatimLongitude = raw.location.verbatimLongitude
            raw.location.locationRemarks = null

            processed.location.decimalLatitude = gl.getGeneralisedLatitude
            processed.location.decimalLongitude = gl.getGeneralisedLongitude
            
            //gather the information about the rules that were applied.
            val si = gl.getSensitivityInstances().toArray(Array[au.org.ala.sds.model.SensitivityInstance] ()).asInstanceOf[Array[au.org.ala.sds.model.SensitivityInstance]]
            val extraComment ="\n"+si.map(i=>"Sensitive in " + i.getZone + " [" + i.getCategory.getValue +", " + i.getAuthority +"]" ).reduceLeft(_ + "\t" +_)
                        
            //update the generalised text
            if (gl.getDescription == MessageFactory.getMessageText(MessageFactory.LOCATION_WITHHELD)) {
              processed.occurrence.informationWithheld = gl.getDescription +extraComment
            } else {
              processed.occurrence.dataGeneralizations = gl.getDescription + extraComment
              processed.location.coordinateUncertaintyInMeters = gl.getGeneralisationInMetres
            }

            //TODO may need to fix locality information... change ths so that the generalisation
            // is performed before the point matching to gazetteer..
            //We want to associate the ibra layers to the sensitised point
            //update the required locality information
            val newPoint = LocationDAO.getByLatLon(processed.location.decimalLatitude, processed.location.decimalLongitude);
            newPoint match {
              case Some((loc, el, cl)) => processed.location.lga = loc.lga
              case _ => processed.location.lga = null //unset the lga
            }
          }
        }
      }
    }
  }

  def getExactSciName(raw: FullRecord): String = {
    if (raw.classification.scientificName != null)
      raw.classification.scientificName
    else if (raw.classification.genus != null) {
      if (raw.classification.specificEpithet != null) {
        if (raw.classification.infraspecificEpithet != null)
          raw.classification.genus + " " + raw.classification.specificEpithet + " " + raw.classification.infraspecificEpithet
        else
          raw.classification.genus + " " + raw.classification.specificEpithet
      }
      else
        raw.classification.genus
    }
    else //return the name default name string which will be null
      raw.classification.scientificName
  }

  def getName = FullRecordMapper.geospatialQa
}

class ClassificationProcessor extends Processor {

  val logger = LoggerFactory.getLogger("ClassificationProcessor")
  val afdApniIdentifier = """(:afd.|:apni.)""".r
  import au.org.ala.biocache.BiocacheConversions._

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

  def hasConflict(rank:String,taxon:String,hintMap:Map[String,Set[String]]) : Boolean = {
    taxon != null && !hintMap.get(rank).get.contains(taxon.toLowerCase)
  }

  /**
   * Match the classification
   */
  def process(guid:String, raw:FullRecord, processed:FullRecord) : Array[QualityAssertion] = {

    try {
      //val nsr = DAO.nameIndex.searchForRecord(classification, true)
      val nsr = ClassificationDAO.getByHashLRU(raw.classification).getOrElse(null)

      //store the matched classification
      if (nsr != null) {
        val classification = nsr.getRankClassification
        //Check to see if the classification fits in with the supplied taxonomic hints
        if(raw.occurrence.institutionCode!=null && raw.occurrence.collectionCode!=null){
          //get the taxonomic hints from the collection or data resource
          var attribution = AttributionDAO.getByCodes(raw.occurrence.institutionCode, raw.occurrence.collectionCode)
          if(attribution.isEmpty)
            attribution = AttributionDAO.getDataResourceByUid(raw.attribution.dataResourceUid)

          if(!attribution.isEmpty){
            logger.debug("Checking taxonomic hints")
            val taxonHints = attribution.get.taxonomicHints

            if(taxonHints != null && !taxonHints.isEmpty){
              val (isValid, comment) = isMatchValid(classification, attribution.get.retrieveParseHints)
              if(!isValid){
                  logger.info("Conflict in matched classification. Matched: " + guid+ ", Matched: "+comment+", Taxonomic hints in use: " + taxonHints.toList)
                  //TODO think about logging this information to a separate column family
                  return Array()//QualityAssertion(AssertionCodes.TAXONOMIC_ISSUE, "Conflict in matched classification. Matched: "+ comment))
              }
            }
          }
        }
        //store ".p" values
        processed.classification = nsr

        //try to apply the vernacular name
        val taxonProfile = TaxonProfileDAO.getByGuid(nsr.getLsid)
        if(!taxonProfile.isEmpty && taxonProfile.get.commonName!=null){
          processed.classification.vernacularName = taxonProfile.get.commonName
        }

        //Add the species group information - I think that it is better to store this value than calculate it at index time
        val speciesGroups = SpeciesGroups.getSpeciesGroups(processed.classification)
        logger.debug("Species Groups: " + speciesGroups)
        if(!speciesGroups.isEmpty && !speciesGroups.get.isEmpty){
          processed.classification.speciesGroups = speciesGroups.get.toArray[String]
        }

        //is the name in the NSLs ???
        if(afdApniIdentifier.findFirstMatchIn(nsr.getLsid).isEmpty){
           Array(QualityAssertion(AssertionCodes.NAME_NOT_IN_NATIONAL_CHECKLISTS, "Record not attached to concept in national species lists"))
        } else {
           Array()
        }

      } else {
        logger.debug("[QualityAssertion] No match for record, classification for Kingdom: " +
            raw.classification.kingdom + ", Family:" + raw.classification.family + ", Genus:" + raw.classification.genus +
            ", Species: " + raw.classification.species + ", Epithet: " + raw.classification.specificEpithet)
        Array(QualityAssertion(AssertionCodes.NAME_NOTRECOGNISED, "Name not recognised"))
      }
    } catch {
      case he: HomonymException => {
        logger.debug(he.getMessage,he)
        Array(QualityAssertion(AssertionCodes.HOMONYM_ISSUE, "Homonym issue resolving the classification"))
      }
      case se: SearchResultException => logger.debug(se.getMessage,se); Array()
      case e: Exception => logger.error("Exception during classification match.",e);Array()
    }
  }
  def getName = FullRecordMapper.taxonomicalQa
}

object DAOLayerTests2 {
  val occurrenceDAO = Config.occurrenceDAO
  val persistenceManager = Config.persistenceManager
  val rowKey ="test-rowKey"
  val uuid = "35b3ff3-test-uuid"
    def main(args: Array[String]): Unit = {
	  var key = "dr344|QM|Fishes|I14740"
	  val qa = QualityAssertion(AssertionCodes.GEOSPATIAL_ISSUE)
	      qa.comment = "My comment"
    qa.userId = "Natasha.Carter@csiro.au"
    qa.userDisplayName = "Natasha Carter"
    occurrenceDAO.addUserAssertion("db827bc0-4053-4965-9900-f56651a8ebb7", qa)
     occurrenceDAO.deleteUserAssertion("db827bc0-4053-4965-9900-f56651a8ebb7", qa.uuid)
    
  }
}