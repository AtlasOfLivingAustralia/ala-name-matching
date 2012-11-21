package au.org.ala.biocache

import au.org.ala.checklist.lucene.HomonymException
import collection.mutable.{HashMap, ArrayBuffer}
import au.org.ala.checklist.lucene.SearchResultException
import org.slf4j.LoggerFactory
import au.org.ala.data.model.LinnaeanRankClassification
import au.org.ala.sds.validation.ServiceFactory
import collection.{mutable, JavaConversions}
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.time.{DateUtils, DateFormatUtils}
import java.util.{Date, GregorianCalendar}
import org.geotools.referencing.CRS
import org.geotools.referencing.operation.DefaultCoordinateOperationFactory
import org.geotools.geometry.GeneralDirectPosition
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.apache.commons.math3.util.{Precision, MathUtils}
import org.drools.lang.DRLParser.entry_point_key_return

/**
 * Trait to be implemented by all processors. 
 * This is a simple Command Pattern.
 */
trait Processor {
  def process(uuid: String, raw: FullRecord, processed: FullRecord): Array[QualityAssertion]

  def getName: String
}

/**
 * Singleton that maintains the workflow
 */
object Processors {

  def foreach(proc: Processor => Unit) = processorMap.values.foreach(proc)

  //need to preserve the ordering of the Processors so that the default values are populated first
  //also classification must be executed before location
  val processorMap = scala.collection.mutable.LinkedHashMap(
    "DEFAULT" -> new DefaultValuesProcessor,
    "IMAGE" -> new MiscellaneousProcessor,
    "ATTR" -> new AttributionProcessor,
    "CLASS" -> new ClassificationProcessor,
    "BOR" -> new BasisOfRecordProcessor,
    "EVENT" -> new EventProcessor,
    "LOC" -> new LocationProcessor,
    "TS" -> new TypeStatusProcessor
  )

  //TODO A better way to do this. Maybe need to group QA failures by issue type instead of phase. 
  //Can't change until we are able to reprocess the complete set records.
  def getProcessorForError(code: Int): String = code match {
    case c if c >= AssertionCodes.geospatialBounds._1 && c < AssertionCodes.geospatialBounds._2 => "loc"
    case c if c >= AssertionCodes.taxonomicBounds._1 && c < AssertionCodes.taxonomicBounds._2 => "class"
    case c if c == AssertionCodes.MISSING_BASIS_OF_RECORD.code || c == AssertionCodes.BADLY_FORMED_BASIS_OF_RECORD.code => "bor"
    case c if c == AssertionCodes.UNRECOGNISED_TYPESTATUS.code => "type"
    case c if c == AssertionCodes.UNRECOGNISED_COLLECTIONCODE.code || c == AssertionCodes.UNRECOGNISED_INSTITUTIONCODE.code => "attr"
    case c if c == AssertionCodes.INVALID_IMAGE_URL.code => "image"
    case c if c >= AssertionCodes.temporalBounds._1 && c < AssertionCodes.temporalBounds._2 => "event"
    case _ => ""
  }
}

/**
 * Maps the default values to the processed record when no raw value exists     
 * This processor should be run before the others so that the default values are populated before reporting missing values    
 *
 * This processor also restore the default values.  IMPLICATION is the LocationProcessor needs to be run to allow sensitive species to 
 *
 * TODO CHANGE this if we move to a phase based processing mechanism
 *
 */
class DefaultValuesProcessor extends Processor {
  def process(guid: String, raw: FullRecord, processed: FullRecord): Array[QualityAssertion] = {
    //add the default dwc fields if their is no raw value for them.
    val dr = AttributionDAO.getDataResourceByUid(raw.attribution.dataResourceUid)
    if (!dr.isEmpty) {
      if (dr.get.defaultDwcValues != null) {
        dr.get.defaultDwcValues.foreach({
          case (key, value) => {
            if (raw.getProperty(key).isEmpty) {
              //set the processed value to the default value
              processed.setProperty(key, value)
              if (!processed.getDefaultValuesUsed && !processed.getProperty(key).isEmpty)
                processed.setDefaultValuesUsed(true)
            }
          }
        })
      }
    }

    //reset the original sensitive values for use in subsequent processing.
    //covers all values that could have been change - thus allowing event dates to be processed correctly...
    if (raw.occurrence.originalSensitiveValues != null) {
      //TODO: Only apply the originalSensitiveValues if the last processed date occurs after the last load date
      FullRecordMapper.mapPropertiesToObject(raw, raw.occurrence.originalSensitiveValues)
    }

    Array()
  }

  def getName = "default"
}

class MiscellaneousProcessor extends Processor {
  val LIST_DELIM = ";".r;
  val interactionPattern = """([A-Za-z]*):([\x00-\x7F\s]*)""".r

  def process(guid: String, raw: FullRecord, processed: FullRecord): Array[QualityAssertion] = {
    var assertions = new ArrayBuffer[QualityAssertion]
    processImages(guid, raw, processed, assertions)
    processInteractions(guid, raw, processed)
    //now process the "establishmentMeans" values
    processEstablishmentMeans(raw, processed)
    //process the dates
    processDates(raw, processed)
    //TODO reenable identification processing after we have categorised issues better.
    //processIdentification(raw,processed,assertions)
    processCollectors(raw, processed, assertions)
    assertions.toArray
  }

  /**
   * parse the collector string to place in a consistent format
   */
  def processCollectors(raw: FullRecord, processed: FullRecord, assertions: ArrayBuffer[QualityAssertion]) = {
    if (StringUtils.isNotBlank(raw.occurrence.recordedBy)) {
      val parsedCollectors = CollectorNameParser.parseForList(raw.occurrence.recordedBy)
      if (parsedCollectors.isDefined) {
        processed.occurrence.recordedBy = parsedCollectors.get.mkString("|")
      }
      else {
        //println("Unable to parse: " + raw.occurrence.recordedBy)
        assertions + QualityAssertion(AssertionCodes.RECORDED_BY_UNPARSABLE, "Can not parse recordedBy")
      }
    }
  }

  def processDates(raw: FullRecord, processed: FullRecord) = {
    //process the "modified" date for the occurrence - we want all modified dates in the same format so that we can index on them...
    if (raw.occurrence.modified != null) {
      val parsedDate = DateParser.parseDate(raw.occurrence.modified)
      if (parsedDate.isDefined) {
        processed.occurrence.modified = parsedDate.get.startDate
      }
    }
    if (raw.identification.dateIdentified != null) {
      val parsedDate = DateParser.parseDate(raw.identification.dateIdentified)
      if (parsedDate.isDefined)
        processed.identification.dateIdentified = parsedDate.get.startDate
    }
  }

  def processEstablishmentMeans(raw: FullRecord, processed: FullRecord) = {
    //2012-0202: At this time AVH is the only data resource to support this. In the future it may be necessary for the value to be a list...
    //handle the "cultivated" type
    //2012-07-13: AVH has moved this to establishmentMeans and has also include nativeness
    if (StringUtils.isNotBlank(raw.occurrence.establishmentMeans)) {
      val ameans = LIST_DELIM.split(raw.occurrence.establishmentMeans)
      val newmeans = ameans.map(means => {
        val term = EstablishmentMeans.matchTerm(means)
        if (term.isDefined) term.get.getCanonical else ""
      }).filter(_.length > 0)
      if (newmeans.size > 0)
        processed.occurrence.establishmentMeans = newmeans.mkString("; ")

    }
  }

  def processIdentification(raw: FullRecord, processed: FullRecord, assertions: ArrayBuffer[QualityAssertion]) = {
    //check missing identification qualifier
    if (raw.identification.identificationQualifier == null)
      assertions + QualityAssertion(AssertionCodes.MISSING_IDENTIFICATIONQUALIFIER, "Missing identificationQualifier")
    //check missing identifiedBy
    if (raw.identification.identifiedBy == null)
      assertions + QualityAssertion(AssertionCodes.MISSING_IDENTIFIEDBY, "Missing identifiedBy")
    //check missing identification references
    if (raw.identification.identificationReferences == null)
      assertions + QualityAssertion(AssertionCodes.MISSING_IDENTIFICATIONREFERENCES, "Missing identificationReferences")
    //check missing date identified
    if (raw.identification.dateIdentified == null)
      assertions + QualityAssertion(AssertionCodes.MISSING_DATEIDENTIFIED, "Missing dateIdentified")
  }

  def processInteractions(guid: String, raw: FullRecord, processed: FullRecord) = {
    //interactions are supplied as part of the assciatedTaxa string
    //TODO more sophisticated parsing of the string. ATM we are only supporting the structure for dr642
    // TODO support multiple interactions
    if (raw.occurrence.associatedTaxa != null && !raw.occurrence.associatedTaxa.isEmpty) {
      val interaction = parseInteraction(raw.occurrence.associatedTaxa)
      if (!interaction.isEmpty) {
        val term = Interactions.matchTerm(interaction.get)
        if (!term.isEmpty) {
          processed.occurrence.interactions = Array(term.get.getCanonical)
        }
      }
    }
  }

  def parseInteraction(raw: String): Option[String] = raw match {
    case interactionPattern(interaction, taxa) => Some(interaction)
    case _ => None
  }

  /**
   * validates that the associated media is a valid image url
   */
  def processImages(guid: String, raw: FullRecord, processed: FullRecord, assertions: ArrayBuffer[QualityAssertion]) = {
    val urls = raw.occurrence.associatedMedia
    // val matchedGroups = groups.collect{case sg: SpeciesGroup if sg.values.contains(cl.getter(sg.rank)) => sg.name}
    if (urls != null) {
      val aurls = urls.split(";").map(url => url.trim)
      processed.occurrence.images = aurls.filter(url => MediaStore.isValidImageURL(url) && MediaStore.doesFileExist(url))
      processed.occurrence.sounds = aurls.filter(url => MediaStore.isValidSoundURL(url) && MediaStore.doesFileExist(url))
      processed.occurrence.videos = aurls.filter(url => MediaStore.isValidVideoURL(url) && MediaStore.doesFileExist(url))

      if (aurls.length != (processed.occurrence.images.length + processed.occurrence.sounds.length + processed.occurrence.videos.length))
        assertions + QualityAssertion(AssertionCodes.INVALID_IMAGE_URL, "URL refers to an invalid file.")
    }

  }

  def getName = "image"
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
        //the processed collection code is catering for the situation where the collection code is provided as a default in the collectory
        if (dataResource.get.hasMappedCollections && (raw.occurrence.collectionCode != null || processed.occurrence.collectionCode != null)) {
          val collCode = if (raw.occurrence.collectionCode != null) raw.occurrence.collectionCode else processed.occurrence.collectionCode
          //use the collection code as the institution code when one does not exist
          val instCode = if (raw.occurrence.institutionCode != null) raw.occurrence.institutionCode else if (processed.occurrence.institutionCode != null) processed.occurrence.institutionCode else collCode
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
        processed.attribution.provenance = dataResource.get.provenance
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
  def validateNumber(number: String, f: (Int => Boolean)): (Int, Boolean) = {
    try {
      if (number != null) {
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
  def process(guid: String, raw: FullRecord, processed: FullRecord): Array[QualityAssertion] = {

    if ((raw.event.day == null || raw.event.day.isEmpty)
      && (raw.event.month == null || raw.event.month.isEmpty)
      && (raw.event.year == null || raw.event.year.isEmpty)
      && (raw.event.eventDate == null || raw.event.eventDate.isEmpty)
      && (raw.event.verbatimEventDate == null || raw.event.verbatimEventDate.isEmpty)
    )
      return Array(QualityAssertion(AssertionCodes.MISSING_COLLECTION_DATE, "No date information supplied"))

    var assertions = new ArrayBuffer[QualityAssertion]
    var date: Option[java.util.Date] = None
    val currentYear = DateUtil.getCurrentYear
    var comment = ""

    var (year, validYear) = validateNumber(raw.event.year, {
      year => year > 0 && year <= currentYear
    })
    var (month, validMonth) = validateNumber(raw.event.month, {
      month => month >= 1 && month <= 12
    })
    var (day, validDay) = validateNumber(raw.event.day, {
      day => day >= 1 && day <= 31
    })
    //check month and day not transposed
    if (!validMonth && raw.event.month.isInt && raw.event.day.isInt) {
      //are day and month transposed?
      val monthValue = raw.event.month.toInt
      val dayValue = raw.event.day.toInt
      if (monthValue > 12 && dayValue < 12) {
        month = dayValue
        day = monthValue
        assertions + QualityAssertion(AssertionCodes.DAY_MONTH_TRANSPOSED, "Assume day and month transposed")
        validMonth = true
      } else {
        assertions + QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE, "Invalid month supplied")
      }
    }

    //TODO need to check for other months
    if (day == 0 || day > 31) {
      assertions + QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE, "Invalid day supplied")
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

          //although check that combined year-month-day isnt in the future
          if (day != 0 && month != 0) {
            val date = DateUtils.parseDate(year.toString + String.format("%02d", int2Integer(month)) + day.toString, Array("yyyyMMdd"))
            if (date.after(new Date())) {
              year -= 100
            }
          }
        }
      } else if (year >= 100 && year < 1600) {
        year = -1
        validYear = false
        comment = "Year out of range"
      } else if (year > DateUtil.getCurrentYear) {
        year = -1
        validYear = false
        comment = "Future year supplied"
        //assertions + QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE,comment)
      } else if (year == 1788 && month == 1 && day == 26) {
        //First fleet arrival date indicative of a null date.
        validYear = false
        comment = "First Fleet arrival implies a null date"
      }
      if (StringUtils.isNotEmpty(comment))
        assertions + QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE, comment)
    }

    var validDayMonthYear = validYear && validDay && validMonth

    //construct
    if (validDayMonthYear) {
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
          validDayMonthYear = false
          comment = "Invalid year, day, month"
          assertions + QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE, comment)
        }
      }
    }

    //set the processed values
    if (validYear) processed.event.year = year.toString
    if (validMonth) processed.event.month = String.format("%02d", int2Integer(month)) //NC ensure that a month is 2 characters long
    if (validDay) processed.event.day = day.toString
    if (!date.isEmpty) processed.event.eventDate = DateFormatUtils.format(date.get, "yyyy-MM-dd")

    //deal with event date if we dont have separate day, month, year fields
    if (date.isEmpty && raw.event.eventDate != null && !raw.event.eventDate.isEmpty) {
      val parsedDate = DateParser.parseDate(raw.event.eventDate)
      if (!parsedDate.isEmpty) {
        //set processed values
        processed.event.eventDate = parsedDate.get.startDate
        processed.event.day = parsedDate.get.startDay
        processed.event.month = parsedDate.get.startMonth
        processed.event.year = parsedDate.get.startYear

        if (DateUtil.isFutureDate(parsedDate.get)) {
          assertions + QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE, "Future date supplied")
        }
      }
    }

    //deal with verbatim date
    if (date.isEmpty && raw.event.verbatimEventDate != null && !raw.event.verbatimEventDate.isEmpty) {
      val parsedDate = DateParser.parseDate(raw.event.verbatimEventDate)
      if (!parsedDate.isEmpty) {
        //set processed values
        processed.event.eventDate = parsedDate.get.startDate
        processed.event.day = parsedDate.get.startDay
        processed.event.month = parsedDate.get.startMonth
        processed.event.year = parsedDate.get.startYear
        if (DateUtil.isFutureDate(parsedDate.get)) {
          assertions + QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE, "Future date supplied")
        }
      }
    }

    //if invalid date, add assertion
    if (!validYear && (processed.event.eventDate == null || processed.event.eventDate == "")) {
      assertions + QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE, comment)
    }

    //chec for future date
    if (!date.isEmpty && date.get.after(new Date())) {
      assertions + QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE, "Future date supplied")
    }

    assertions.toArray
  }

  def getName = "event"
}

class TypeStatusProcessor extends Processor {
  /**
   * Process the type status
   */
  def process(guid: String, raw: FullRecord, processed: FullRecord): Array[QualityAssertion] = {

    if (raw.identification.typeStatus != null && !raw.identification.typeStatus.isEmpty) {
      val term = TypeStatus.matchTerm(raw.identification.typeStatus)
      if (term.isEmpty) {
        //add a quality assertion
        Array(QualityAssertion(AssertionCodes.UNRECOGNISED_TYPESTATUS, "Unrecognised type status"))
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
  def process(guid: String, raw: FullRecord, processed: FullRecord): Array[QualityAssertion] = {

    if (raw.occurrence.basisOfRecord == null || raw.occurrence.basisOfRecord.isEmpty) {
      if (processed.occurrence.basisOfRecord != null && !processed.occurrence.basisOfRecord.isEmpty)
        Array[QualityAssertion]()
      else //add a quality assertion
        Array(QualityAssertion(AssertionCodes.MISSING_BASIS_OF_RECORD, "Missing basis of record"))
    } else {
      val term = BasisOfRecord.matchTerm(raw.occurrence.basisOfRecord)
      if (term.isEmpty) {
        //add a quality assertion
        logger.debug("[QualityAssertion] " + guid + ", unrecognised BoR: " + guid + ", BoR:" + raw.occurrence.basisOfRecord)
        Array(QualityAssertion(AssertionCodes.BADLY_FORMED_BASIS_OF_RECORD, "Unrecognised basis of record"))
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

  lazy val crsEpsgCodesMap = {
    var valuesMap = Map[String, String]()
    for (line <- scala.io.Source.fromURL(getClass.getResource("/crsEpsgCodes.txt"), "utf-8").getLines().toList) {
      val values = line.split('=')
      valuesMap += (values(0) -> values(1))
    }

    valuesMap
  }


  lazy val zoneEpsgCodesMap = {
    var valuesMap = Map[String, String]()
    for (line <- scala.io.Source.fromURL(getClass.getResource("/zoneEpsgCodes.txt"), "utf-8").getLines().toList) {
      val values = line.split('=')
      valuesMap += (values(0) -> values(1))
    }

    valuesMap
  }

  val WGS84_EPSG_Code = "EPSG:4326"

  import au.org.ala.util.StringHelper._
  import JavaConversions._

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
    setProcessedCoordinates(raw, processed, assertions)

    processAltitudeAndDepth(guid, raw, processed, assertions)

    //Continue processing location if a processed longitude and latitude exists
    if (processed.location.decimalLatitude != null && processed.location.decimalLongitude != null) {

      //validate the coordinate values
      validateCoordinatesValues(raw, processed, assertions)

      //validate coordinate accuracy (coordinateUncertaintyInMeters) and coordinatePrecision (precision - A. Chapman)
      checkCoordinateUncertainty(raw, processed, assertions)

      //generate coordinate accuracy if not supplied
      var point = LocationDAO.getByLatLon(processed.location.decimalLatitude, processed.location.decimalLongitude)

      if (!point.isEmpty) {
        val (location, environmentalLayers, contextualLayers) = point.get
        processed.locationDetermined = true;
        //add state information
        processed.location.stateProvince = location.stateProvince
        processed.location.ibra = location.ibra
        processed.location.imcra = location.imcra
        processed.location.lga = location.lga
        processed.location.country = location.country
        processed.el = environmentalLayers
        processed.cl = contextualLayers

        //add the layers that are associated with the point
        //processed.environmentalLayers = environmentalLayers
        //processed.contextualLayers = contextualLayers
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
        try {
          processSensitivity(raw, processed, location, contextualLayers)
        } catch {
          case e: Exception => println("Problem processing using the SDS for record " + guid); e.printStackTrace
        }
      }
    }

    //Only process the raw state value if no latitude and longitude is provided
    if (processed.location.stateProvince == null && raw.location.decimalLatitude == null && raw.location.decimalLongitude == null) {
      //process the supplied state
      val stateTerm = StateProvinces.matchTerm(raw.location.stateProvince)
      if (!stateTerm.isEmpty) {
        processed.location.stateProvince = stateTerm.get.canonical
        //now check for sensitivity based on state
        processSensitivity(raw, processed, processed.location, Map())
        processed.location.country = StateProvinceToCountry.map.getOrElse(processed.location.stateProvince, "")
      }
    }

    //Only process the raw country value if no latitude and longitude is provided
    if (processed.location.country == null && raw.location.decimalLatitude == null && raw.location.decimalLongitude == null) {
      //process the supplied state
      val countryTerm = Countries.matchTerm(raw.location.country)
      if (!countryTerm.isEmpty) {
        processed.location.country = countryTerm.get.canonical
      }
    }

    //validate the gereference values
    //TODO reenable georeferencing processing after we have categorised issues better.
    //validateGeoreferenceValues(raw,processed,assertions)

    assertions.toArray
  }

  /**
   * Performs the QAs associated with elevation and depth
   */
  def processAltitudeAndDepth(guid: String, raw: FullRecord, processed: FullRecord, assertions: ArrayBuffer[QualityAssertion]) {
    //check that the values are numeric
    if (raw.location.verbatimDepth != null) {
      try {
        val vdepth = raw.location.verbatimDepth.toFloat
        processed.location.verbatimDepth = vdepth.toString
        if (vdepth > 10000)
          assertions + QualityAssertion(AssertionCodes.DEPTH_OUT_OF_RANGE, "Depth " + vdepth + " is greater than 10,000 metres")
      }
      catch {
        case e: Exception => assertions + QualityAssertion(AssertionCodes.DEPTH_NON_NUMERIC, "Can't parse verbatimDepth " + raw.location.verbatimDepth)
      }
    }
    if (raw.location.verbatimElevation != null) {
      try {
        val velevation = raw.location.verbatimElevation.toFloat
        processed.location.verbatimElevation = velevation.toString
        if (velevation > 10000 || velevation < -100)
          assertions + QualityAssertion(AssertionCodes.ALTITUDE_OUT_OF_RANGE, "Elevation " + velevation + " is greater than 10,000 metres or less than -100 metres.")
      }
      catch {
        case e: Exception => assertions + QualityAssertion(AssertionCodes.ALTITUDE_NON_NUMERIC, "Can't parse verbatimElevation " + raw.location.verbatimElevation)
      }
    }
    //check for max and min reversals
    if (raw.location.minimumDepthInMeters != null && raw.location.maximumDepthInMeters != null) {
      try {
        val min = raw.location.minimumDepthInMeters.toFloat
        val max = raw.location.maximumDepthInMeters.toFloat
        if (min > max) {
          processed.location.minimumDepthInMeters = max.toString
          processed.location.maximumDepthInMeters = min.toString
          assertions + QualityAssertion(AssertionCodes.MIN_MAX_DEPTH_REVERSED, "The minimum, " + min + ", and maximum, " + max + ", depths have been transposed.")
        }
        else {
          processed.location.minimumDepthInMeters = min.toString
          processed.location.maximumDepthInMeters = max.toString
        }
      }
      catch {
        case _ =>
      }
    }
    if (raw.location.minimumElevationInMeters != null && raw.location.maximumElevationInMeters != null) {
      try {
        val min = raw.location.minimumElevationInMeters.toFloat
        val max = raw.location.maximumElevationInMeters.toFloat
        if (min > max) {
          processed.location.minimumElevationInMeters = max.toString
          processed.location.maximumElevationInMeters = min.toString
          assertions + QualityAssertion(AssertionCodes.MIN_MAX_ALTITUDE_REVERSED, "The minimum, " + min + ", and maximum, " + max + ", elevations have been transposed.")
        }
        else {
          processed.location.minimumElevationInMeters = min.toString
          processed.location.maximumElevationInMeters = max.toString
        }
      }
      catch {
        case _ =>
      }
    }
  }

  def setProcessedCoordinates(raw: FullRecord, processed: FullRecord, assertions: ArrayBuffer[QualityAssertion]) {

    //handle the situation where the coordinates have already been sensitised
    // (LEGACY format - as of 2011-10-01 there we are storing original values in a map...)
    if (raw.location.originalDecimalLatitude != null && raw.location.originalDecimalLongitude != null) {
      processed.location.decimalLatitude = raw.location.originalDecimalLatitude
      processed.location.decimalLongitude = raw.location.originalDecimalLongitude
      processed.location.verbatimLatitude = raw.location.originalVerbatimLatitude
      processed.location.verbatimLongitude = raw.location.originalVerbatimLongitude
      //set the raw values too
      raw.location.decimalLatitude = raw.location.originalDecimalLatitude
      raw.location.decimalLongitude = raw.location.originalDecimalLongitude

    } else {
      //use raw values
      val (y, x, geodeticDatum) = processLatLong(raw.location.decimalLatitude,
        raw.location.decimalLongitude, raw.location.geodeticDatum, raw.location.verbatimLatitude, raw.location.verbatimLongitude, raw.location.verbatimSRS, raw.location.easting, raw.location.northing, raw.location.zone, assertions).getOrElse((null, null, null))

      processed.location.decimalLatitude = y
      processed.location.decimalLongitude = x
      processed.location.geodeticDatum = geodeticDatum

    }
  }

  def processLatLong(rawLatitude: String, rawLongitude: String, rawGeodeticDatum: String, verbatimLatitude: String, verbatimLongitude: String, verbatimSRS: String, easting: String, northing: String, zone: String, assertions: ArrayBuffer[QualityAssertion]): Option[(String, String, String)] = {
    //check to see if we have coordinates specified
    if (rawLatitude != null && rawLongitude != null && !rawLatitude.toFloatWithOption.isEmpty && !rawLongitude.toFloatWithOption.isEmpty) {
      // if decimal lat/long is provided in a CRS other than WGS84, then we need to reproject

      if (rawGeodeticDatum != null) {
        val sourceEpsgCode = lookupEpsgCode(rawGeodeticDatum)
        if (!sourceEpsgCode.isEmpty) {
          if (sourceEpsgCode.get == WGS84_EPSG_Code) {
            //already in WGS84, no need to reproject
            Some((rawLatitude, rawLongitude, WGS84_EPSG_Code))
          } else {
            // Reproject decimal lat/long to WGS84
            val desiredNoDecimalPlaces = math.min(getNumberOfDecimalPlacesInDouble(rawLatitude), getNumberOfDecimalPlacesInDouble(rawLongitude))

            val reprojectedCoords = reprojectCoordinatesToWGS84(rawLatitude.toDouble, rawLongitude.toDouble, sourceEpsgCode.get, desiredNoDecimalPlaces)
            if (reprojectedCoords.isEmpty) {
              assertions + QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CONVERSION_FAILED, "Transformation of decimal latiude and longitude to WGS84 failed")
              None
            } else {
              assertions + QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CONVERTED, "Decimal latitude and longitude were converted to WGS84 (EPSG:4326)")
              val (reprojectedLatitude, reprojectedLongitude) = reprojectedCoords.get
              Some(reprojectedLatitude, reprojectedLongitude, WGS84_EPSG_Code)
            }
          }
        } else {
          assertions + QualityAssertion(AssertionCodes.UNRECOGNIZED_GEODETIC_DATUM, rawGeodeticDatum + "not recognized.")
          Some((rawLatitude, rawLongitude, rawGeodeticDatum))
        }
      } else {
        //assume coordinates already in WGS84
        assertions + QualityAssertion(AssertionCodes.GEODETIC_DATUM_ASSUMED_WGS84, "Geodetic datum assumed to be WGS84 (EPSG:4326)")
        Some((rawLatitude, rawLongitude, WGS84_EPSG_Code))
      }

      // Attempt to infer the decimal latitude and longitude from the verbatim latitude and longitude
    } else if (verbatimLatitude != null && verbatimLongitude != null) {
      var decimalVerbatimLat = verbatimLatitude.toFloatWithOption
      var decimalVerbatimLong = verbatimLongitude.toFloatWithOption

      if (decimalVerbatimLat.isEmpty || decimalVerbatimLong.isEmpty) {
        //parse the expressions into their decimal equivalents
        decimalVerbatimLat = VerbatimLatLongParser.parse(verbatimLatitude)
        decimalVerbatimLong = VerbatimLatLongParser.parse(verbatimLongitude)
      }

      if (!decimalVerbatimLat.isEmpty && !decimalVerbatimLong.isEmpty) {
        if (decimalVerbatimLat.get.toString.isLatitude && decimalVerbatimLong.get.toString.isLongitude) {

          // If a verbatim SRS is supplied, reproject coordinates to WGS 84
          if (verbatimSRS != null) {
            val sourceEpsgCode = lookupEpsgCode(verbatimSRS)
            if (!sourceEpsgCode.isEmpty) {
              if (sourceEpsgCode.get == WGS84_EPSG_Code) {
                //already in WGS84, no need to reproject
                assertions + QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATED_FROM_VERBATIM, "Decimal latitude and longitude were calculated using verbatimLatitude, verbatimLongitude and verbatimSRS")
                Some((decimalVerbatimLat.get.toString, decimalVerbatimLong.get.toString, WGS84_EPSG_Code))
              } else {
                val desiredNoDecimalPlaces = math.min(getNumberOfDecimalPlacesInDouble(decimalVerbatimLat.get.toString), getNumberOfDecimalPlacesInDouble(decimalVerbatimLong.get.toString))

                val reprojectedCoords = reprojectCoordinatesToWGS84(decimalVerbatimLat.get, decimalVerbatimLong.get, sourceEpsgCode.get, desiredNoDecimalPlaces)
                if (reprojectedCoords.isEmpty) {
                  assertions + QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATION_FROM_VERBATIM_FAILED, "Transformation of verbatim latiude and longitude to WGS84 failed")
                  None
                } else {
                  assertions + QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATED_FROM_VERBATIM, "Decimal latitude and longitude were calculated using verbatimLatitude, verbatimLongitude and verbatimSRS")
                  val (reprojectedLatitude, reprojectedLongitude) = reprojectedCoords.get
                  Some(reprojectedLatitude, reprojectedLongitude, WGS84_EPSG_Code)
                }
              }
            } else {
              assertions + QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATION_FROM_VERBATIM_FAILED, "Unrecognized verbatimSRS " + verbatimSRS)
              None
            }
            // Otherwise, assume latitude and longitude are already in WGS 84
          } else if (decimalVerbatimLat.get.toString.isLatitude && decimalVerbatimLong.get.toString.isLongitude) {
            assertions + QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATED_FROM_VERBATIM, "Decimal latitude and longitude were calculated using verbatimLatitude, verbatimLongitude and verbatimSRS")
            Some((decimalVerbatimLat.get.toString, decimalVerbatimLong.get.toString, WGS84_EPSG_Code))
          } else {
            // Invalid latitude, longitude
            assertions + QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATION_FROM_VERBATIM_FAILED, "Could not parse verbatim latitude and longitude")
            None
          }
        } else {
          None
        }
      } else {
        None
      }
    } else if (easting != null && northing != null && zone != null) {
      // Need a datum and a zone to get an epsg code for transforming easting/northing values
      var epsgCodeKey = {
        if (verbatimSRS != null) {
          verbatimSRS.toUpperCase + "|" + zone
        } else {
          // Assume GDA94 / MGA zone
          "GDA94|" + zone
        }
      }

      if (zoneEpsgCodesMap.contains(epsgCodeKey)) {
        val crsEpsgCode = zoneEpsgCodesMap(epsgCodeKey)
        val eastingAsDouble = easting.toDoubleWithOption;
        val northingAsDouble = northing.toDoubleWithOption;

        if (!eastingAsDouble.isEmpty && !northingAsDouble.isEmpty) {
          // Always round to 5 decimal places as easting/northing values are in metres and 0.00001 degree is approximately equal to 1m.
          val reprojectedCoords = reprojectCoordinatesToWGS84(eastingAsDouble.get, northingAsDouble.get, crsEpsgCode, 5)
          if (reprojectedCoords.isEmpty) {
            assertions + QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATION_FROM_EASTING_NORTHING_FAILED, "Transformation of verbatim easting and northing to WGS84 failed")
            None
          } else {
            assertions + QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATED_FROM_EASTING_NORTHING, "Decimal latitude and longitude were calculated using easting, northing and zone.")
            val (reprojectedLongitude, reprojectedLatitude) = reprojectedCoords.get
            Some(reprojectedLatitude, reprojectedLongitude, WGS84_EPSG_Code)
          }
        } else {
          None
        }
      } else {
        if (verbatimSRS == null) {
          assertions + QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATION_FROM_EASTING_NORTHING_FAILED, "Unrecognized zone GDA94 / MGA zone " + zone)
        } else {
          assertions + QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATION_FROM_EASTING_NORTHING_FAILED, "Unrecognized zone " + verbatimSRS + " / zone " + zone)
        }
        None
      }
    } else {
      None
    }
  }

  /**
   * Reprojects coordinates into WGS 84
   * @param coordinate1 first coordinate. If source value is easting/northing, then this should be the easting value. Otherwise it should be the latitude
   * @param coordinate2 first coordinate. If source value is easting/northing, then this should be the northing value. Otherwise it should be the longitude
   * @param sourceCrsEpsgCode epsg code for the source CRS, e.g. EPSG:4202 for AGD66
   * @param decimalPlacesToRoundTo number of decimal places to round the reprojected coordinates to
   * @return Reprojected coordinates, or None if the operation failed. NOTE: when converting degree values, return value will be latitude, longitude. However when converting easting/northing values, return value will be longitude, latitude.
   */
  def reprojectCoordinatesToWGS84(coordinate1: Double, coordinate2: Double, sourceCrsEpsgCode: String, decimalPlacesToRoundTo: Int): Option[(String, String)] = {
    try {
      val wgs84CRS = DefaultGeographicCRS.WGS84
      val sourceCRS = CRS.decode(sourceCrsEpsgCode)
      val transformOp = new DefaultCoordinateOperationFactory().createOperation(sourceCRS, wgs84CRS)
      val directPosition = new GeneralDirectPosition(coordinate1, coordinate2)
      val wgs84LatLong = transformOp.getMathTransform().transform(directPosition, null)

      val reprojectedCoordinate1 = wgs84LatLong.getOrdinate(0)
      val reprojectedCoordinate2 = wgs84LatLong.getOrdinate(1)

      val roundedCoordinate1 = Precision.round(reprojectedCoordinate1, decimalPlacesToRoundTo)
      val roundedCoordinate2 = Precision.round(reprojectedCoordinate2, decimalPlacesToRoundTo)

      Some(roundedCoordinate1.toString, roundedCoordinate2.toString)
    } catch {
      case ex: Exception => None
    }
  }

  /**
   * Get the number of decimal places in a double value in string form
   * @param decimalAsString
   * @return
   */
  def getNumberOfDecimalPlacesInDouble(decimalAsString: String): Int = {

    val tokens = decimalAsString.split('.')
    if (tokens.length == 2) {
      return tokens(1).length
    } else {
      return 0
    }
  }

  /**
   * Get the EPSG code associated with a coordinate reference system string e.g. "WGS84" or "AGD66".
   * @param crs The coordinate reference system string.
   * @return The EPSG code associated with the CRS, or None if no matching code could be found. If the supplied string is already a valid EPSG code, it will simply be returned.
   */
  def lookupEpsgCode(crs: String): Option[String] = {
    if (StringUtils.startsWithIgnoreCase(crs, "EPSG:")) {
      // Do a lookup with the EPSG code to ensure that it is valid
      try {
        CRS.decode(crs.toUpperCase)
        // lookup was successful so just return the EPSG code
        Some(crs.toUpperCase)
      } catch {
        case ex: Exception => None
      }
    } else if (crsEpsgCodesMap.contains(crs.toUpperCase)) {
      Some(crsEpsgCodesMap(crs.toUpperCase()))
    } else {
      None
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
    if (raw.location.coordinatePrecision == null)
      assertions + QualityAssertion(AssertionCodes.MISSING_COORDINATEPRECISION, "Missing coordinatePrecision")

    // If the coordinateUncertainty is still empty populate it with the default
    // value (we don't test until now because the SDS will sometime include coordinate uncertainty)
    // This step will pick up on default values because processed.location.coordinateUncertaintyInMeters
    // will already be populated if a default value exists
    if (processed.location.coordinateUncertaintyInMeters == null) {
      assertions + QualityAssertion(AssertionCodes.UNCERTAINTY_NOT_SPECIFIED, "Uncertainty was not supplied")
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
    if (processed.location.country == "Australia" && taxonProfile.conservation != null) {
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

  def validateGeoreferenceValues(raw: FullRecord, processed: FullRecord, assertions: ArrayBuffer[QualityAssertion]) = {
    //check for missing geodeticDatum
    if (raw.location.geodeticDatum == null && processed.location.geodeticDatum == null)
      assertions + QualityAssertion(AssertionCodes.MISSING_GEODETICDATUM, "Missing geodeticDatum")
    //check for missing georeferencedBy
    if (raw.location.georeferencedBy == null && processed.location.georeferencedBy == null)
      assertions + QualityAssertion(AssertionCodes.MISSING_GEOREFERNCEDBY, "Missing georeferencedBy")
    //check for missing georeferencedProtocol
    if (raw.location.georeferenceProtocol == null && processed.location.georeferenceProtocol == null)
      assertions + QualityAssertion(AssertionCodes.MISSING_GEOREFERENCEPROTOCOL, "Missing georeferenceProtocol")
    //check for missing georeferenceSources
    if (raw.location.georeferenceSources == null && processed.location.georeferenceSources == null)
      assertions + QualityAssertion(AssertionCodes.MISSING_GEOREFERENCESOURCES, "Missing georeferenceSources")
    //check for missing georeferenceVerificationStatus
    if (raw.location.georeferenceVerificationStatus == null && processed.location.georeferenceVerificationStatus == null)
      assertions + QualityAssertion(AssertionCodes.MISSING_GEOREFERENCEVERIFICATIONSTATUS, "Missing georeferenceVerificationStatus")
    //check for missing georeferenceDate
    //there is no georeferenceDate in out model ???
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

      if (raw.location.country != null && raw.location.country != "") {
        val country = Countries.matchTerm(raw.location.country)
        if (!country.isEmpty) {

          val latlngBBoxOption = CountryCentrePoints.matchName(country.get.canonical)
          latlngBBoxOption match {
            case Some((latlng, bbox)) => {

              if (!bbox.containsPoint(lat, lon)) {
                if (bbox.containsPoint(lat * -1, lon)) {
                  //latitude is negated
                  assertions + QualityAssertion(AssertionCodes.NEGATED_LATITUDE,
                    "Latitude seems to be negated.  Original value:" + processed.location.decimalLatitude)
                  processed.location.decimalLatitude = (lat * -1).toString
                }
                if (bbox.containsPoint(lat, lon * -1)) {
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
          assertions + QualityAssertion(AssertionCodes.UNKNOWN_COUNTRY_NAME, "Country name '" + raw.location.country + "' not recognised.")
        }
      }
    }
  }

  /** Performs all the sensitivity processing.  Returns the new point ot be working with */
  def processSensitivity(raw: FullRecord, processed: FullRecord, location: Location, contextualLayers: Map[String, String]) = {

    //Perform sensitivity actions if the record was located in Australia
    //removed the check for Australia because some of the loc cache records have a state without country (-43.08333, 147.66670)
    if (location.stateProvince != null) {
      //location.country == "Australia"){
      val sensitiveTaxon = {
        //check to see if the rank of the matched taxon is above a species
        val exact = getExactSciName(raw)
        if (processed.classification.taxonConceptID != null && exact != null) {
          val lsidVersion = sdsFinder.findSensitiveSpeciesByLsid(processed.classification.taxonConceptID)
          if (lsidVersion != null)
            lsidVersion
          else
            sdsFinder.findSensitiveSpeciesByExactMatch(exact)
        }
        else if (exact != null)
          sdsFinder.findSensitiveSpeciesByExactMatch(exact)
        else
          null

      }

      //only proceed if the taxon has been identified as sensitive
      if (sensitiveTaxon != null) {
        //get a map representation of the raw record...
        var rawMap = scala.collection.mutable.Map[String, String]()
        raw.objectArray.foreach(poso => {
          val map = FullRecordMapper.mapObjectToProperties(poso, Versions.RAW)
          rawMap.putAll(map)
        })
        //put the state information that we have from the point
        rawMap.put("stateProvince", location.stateProvince)

        //put the required contexual layers in the map
        au.org.ala.sds.util.GeoLocationHelper.getGeospatialLayers.foreach(key => {
          rawMap.put(key, contextualLayers.getOrElse(key, null))
        })


        val service = ServiceFactory.createValidationService(sensitiveTaxon)
        //TODO fix for different types of outcomes...
        val voutcome = service.validate(rawMap)
        if (voutcome.isValid && voutcome.isSensitive) {

          if (voutcome.getResult != null) {
            //conservation sensitive species will have a map of new values in the result
            //the map that is returned needs to be used to update the raw record
            val map: scala.collection.mutable.Map[java.lang.String, Object] = voutcome.getResult
            //logger.debug("SDS return map: "+map)
            //convert it to a string string map
            val stringMap = map.collect({
              case (k, v) if v != null => if (k == "originalSensitiveValues") {
                val osv = v.asInstanceOf[java.util.HashMap[String, String]]
                //add the original "processed" coordinate uncertainty to the sensitive values so that it can be available if necessary
                if (processed.location.coordinateUncertaintyInMeters != null)
                  osv.put("coordinateUncertaintyInMeters.p", processed.location.coordinateUncertaintyInMeters)
                val newv = Json.toJSON(osv)
                (k -> newv)
              } else (k -> v.toString)

            })
            //logger.debug("AFTER : " + stringMap)
            //take away the values that need to be added to the processed record NOT the raw record
            val uncertainty = map.get("generalisationInMetres")
            if (!uncertainty.isEmpty) {
              //we know that we have sensitised
              //add the uncertainty to the currently processed uncertainty
              if (StringUtils.isNotEmpty(uncertainty.get.toString)) {
                val currentUncertainty = if (StringUtils.isNotEmpty(processed.location.coordinateUncertaintyInMeters)) java.lang.Float.parseFloat(processed.location.coordinateUncertaintyInMeters) else 0
                val newuncertainty = currentUncertainty + java.lang.Integer.parseInt(uncertainty.get.toString)
                processed.location.coordinateUncertaintyInMeters = newuncertainty.toString
              }
              processed.location.decimalLatitude = stringMap.getOrElse("decimalLatitude", "")
              processed.location.decimalLongitude = stringMap.getOrElse("decimalLongitude", "")
              stringMap -= "generalisationInMetres"
            }
            processed.occurrence.informationWithheld = stringMap.getOrElse("informationWithheld", "")
            processed.occurrence.dataGeneralizations = stringMap.getOrElse("dataGeneralizations", "")
            stringMap -= "informationWithheld"
            stringMap -= "dataGeneralizations"

            if (stringMap.contains("day") || stringMap.contains("eventDate")) {
              //remove the day from the values
              raw.event.day = ""
              processed.event.day = ""
              processed.event.eventDate = ""
            }

            //update the raw record with whatever is left in the stringMap
            Config.persistenceManager.put(raw.rowKey, "occ", stringMap.toMap)

            //TODO may need to fix locality information... change ths so that the generalisation
            // is performed before the point matching to gazetteer..
            //We want to associate the ibra layers to the sensitised point
            //update the required locality information
            logger.debug("**************** Performing lookup for new point ['" + raw.rowKey
              + "'," + processed.location.decimalLongitude + "," + processed.location.decimalLatitude + "]")
            val newPoint = LocationDAO.getByLatLon(processed.location.decimalLatitude, processed.location.decimalLongitude);
            newPoint match {
              case Some((loc, el, cl)) => processed.location.lga = loc.lga
              case _ => processed.location.lga = null //unset the lga
            }
          }
          else {
            //TO do something with the PEST
            if (voutcome.getReport() != null)
              processed.occurrence.informationWithheld = "PEST: " + voutcome.getReport().toString()
          }
        }
      }
      else {
        //Species is NOT sensitive
        //if the raw record has originalSensitive values we need to re-initialise the value
        if (raw.occurrence.originalSensitiveValues != null && !raw.occurrence.originalSensitiveValues.isEmpty) {
          Config.persistenceManager.put(raw.rowKey, "occ", raw.occurrence.originalSensitiveValues + ("originalSensitiveValues" -> ""))
        }
      }
    }
  }

  def getExactSciName(raw: FullRecord): String = {
    if (raw.classification.scientificName != null)
      raw.classification.scientificName
    else if (raw.classification.subspecies != null)
      raw.classification.subspecies
    else if (raw.classification.species != null)
      raw.classification.species
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
    else if (raw.classification.vernacularName != null) // handle the case where only a common name is provided.
      raw.classification.vernacularName
    else //return the name default name string which will be null
      raw.classification.scientificName
  }

  def getName = FullRecordMapper.geospatialQa
}

class ClassificationProcessor extends Processor {

  val logger = LoggerFactory.getLogger("ClassificationProcessor")
  val afdApniIdentifier = """(:afd.|:apni.)""".r
  val questionPattern = """([\x00-\x7F\s]*)\?([\x00-\x7F\s]*)""".r
  val affPattern = """([\x00-\x7F\s]*) aff[#!?\\.]?([\x00-\x7F\s]*)""".r
  val cfPattern = """([\x00-\x7F\s]*) cf[#!?\\.]?([\x00-\x7F\s]*)""".r

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

  def hasConflict(rank: String, taxon: String, hintMap: Map[String, Set[String]]): Boolean = {
    taxon != null && !hintMap.get(rank).get.contains(taxon.toLowerCase)
  }

  def hasMatchToDefault(rank: String, taxon: String, classification: Classification): Boolean = {
    def term = DwC.matchTerm(rank)
    def field = if (term.isDefined) term.get.canonical else rank
    taxon != null && taxon.equalsIgnoreCase(classification.getProperty(field).getOrElse(""))
  }

  /**
   * Match the classification
   */
  def process(guid: String, raw: FullRecord, processed: FullRecord): Array[QualityAssertion] = {

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
      val nsr = ClassificationDAO.getByHashLRU(raw.classification).getOrElse(null)

      //store the matched classification
      if (nsr != null) {
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
              //TODO think about logging this information to a separate column family
              return Array() //QualityAssertion(AssertionCodes.TAXONOMIC_ISSUE, "Conflict in matched classification. Matched: "+ comment))
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
        processed.classification.taxonomicIssue = scientificName match {
          case questionPattern(a, b) => "questionSpecies"
          case affPattern(a, b) => "affinitySpecies"
          case cfPattern(a, b) => "conferSpecies"
          case _ => "noIssue"
        }

        //is the name in the NSLs ???
        if (afdApniIdentifier.findFirstMatchIn(nsr.getLsid).isEmpty) {
          Array(QualityAssertion(AssertionCodes.NAME_NOT_IN_NATIONAL_CHECKLISTS, "Record not attached to concept in national species lists"))
        } else {
          Array()
        }

      } else {
        logger.debug("[QualityAssertion] No match for record, classification for Kingdom: " +
          raw.classification.kingdom + ", Family:" + raw.classification.family + ", Genus:" + raw.classification.genus +
          ", Species: " + raw.classification.species + ", Epithet: " + raw.classification.specificEpithet)
        processed.classification.nameMatchMetric = "noMatch"
        Array(QualityAssertion(AssertionCodes.NAME_NOTRECOGNISED, "Name not recognised"))
      }
    } catch {
      case he: HomonymException => {
        logger.debug(he.getMessage, he)
        //need to remove any default values from the processed classification
        processed.classification.kingdom = null
        processed.classification.phylum = null
        processed.classification.classs = null
        processed.classification.order = null
        processed.classification.family = null
        Array(QualityAssertion(AssertionCodes.HOMONYM_ISSUE, "Homonym issue resolving the classification"))
      }
      case se: SearchResultException => logger.debug(se.getMessage, se); Array()
      case e: Exception => logger.error("Exception during classification match for record " + guid, e); Array()
    }
  }

  def getName = FullRecordMapper.taxonomicalQa

}