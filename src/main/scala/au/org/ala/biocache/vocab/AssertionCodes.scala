package au.org.ala.biocache.vocab

import au.org.ala.biocache.model.QualityAssertion

/**
 * Assertion codes for records. These codes are a reflection of http://bit.ly/evMJv5
 */
object AssertionCodes {
  import ErrorCodeCategory._
  //geospatial issues
  val GEOSPATIAL_ISSUE = ErrorCode("geospatialIssue",0,true,"Geospatial issue")  // general purpose option
  val NEGATED_LATITUDE = ErrorCode("negatedLatitude",1,false,"Latitude is negated", Warning)
  val NEGATED_LONGITUDE = ErrorCode("negatedLongitude",2,false,"Longitude is negated", Warning)
  val INVERTED_COORDINATES = ErrorCode("invertedCoordinates",3,false,"Coordinates are transposed", Warning)
  val ZERO_COORDINATES = ErrorCode("zeroCoordinates",4,true,"Supplied coordinates are zero", Warning)
  val COORDINATES_OUT_OF_RANGE = ErrorCode("coordinatesOutOfRange",5,true,"Coordinates are out of range", Error)
  val UNKNOWN_COUNTRY_NAME = ErrorCode("unknownCountry",6,false,"Supplied country not recognised", Error)
  val ALTITUDE_OUT_OF_RANGE = ErrorCode("altitudeOutOfRange",7,false,"Altitude out of range", Error)
  val BADLY_FORMED_ALTITUDE = ErrorCode("erroneousAltitude",8,false, "Badly formed altitude", Error)
  val MIN_MAX_ALTITUDE_REVERSED = ErrorCode("minMaxAltitudeReversed",9,false, "Min and max altitude reversed", Warning)
  val DEPTH_IN_FEET = ErrorCode("depthInFeet",10,false,"Depth value supplied in feet", Warning)
  val DEPTH_OUT_OF_RANGE = ErrorCode("depthOutOfRange",11,false,"Depth out of range", Error)
  val MIN_MAX_DEPTH_REVERSED = ErrorCode("minMaxDepthReversed",12,false,"Min and max depth reversed", Warning)
  val ALTITUDE_IN_FEET = ErrorCode("altitudeInFeet",13,false,"Altitude value supplied in feet", Warning)
  val ALTITUDE_NON_NUMERIC = ErrorCode("altitudeNonNumeric",14,false,"Altitude value non-numeric", Error)
  val DEPTH_NON_NUMERIC = ErrorCode("depthNonNumeric",15,false,"Depth value non-numeric", Error)
  val COUNTRY_COORDINATE_MISMATCH = ErrorCode("countryCoordinateMismatch",16,false,"Coordinates dont match supplied country", Error)
  val STATE_COORDINATE_MISMATCH = ErrorCode("stateCoordinateMismatch",18,false,"Coordinates dont match supplied state", Error)
  val COORDINATE_HABITAT_MISMATCH = ErrorCode("habitatMismatch",19,true,"Habitat incorrect for species", Error)
  val DETECTED_OUTLIER = ErrorCode("detectedOutlier",20,true,"Suspected outlier", Error)
  val COUNTRY_INFERRED_FROM_COORDINATES = ErrorCode("countryInferredByCoordinates",21,false,"Country inferred from coordinates", Warning)
  val COORDINATES_CENTRE_OF_STATEPROVINCE = ErrorCode("coordinatesCentreOfStateProvince",22,true,"Supplied coordinates centre of state", Warning)
  val COORDINATE_PRECISION_MISMATCH = ErrorCode("coordinatePrecisionMismatch",23,false,"Coordinate precision not valid", Error)
  val PRECISION_RANGE_MISMATCH = ErrorCode("precisionRangeMismatch", 17, false, "The precision value should be between 0 and 1.", Error)
  val UNCERTAINTY_RANGE_MISMATCH = ErrorCode("uncertaintyRangeMismatch",24,false,"Coordinate accuracy not valid", Error)
  val UNCERTAINTY_IN_PRECISION = ErrorCode("uncertaintyInPrecision",25,false,"Coordinate precision and accuracy transposed", Error)
  val SPECIES_OUTSIDE_EXPERT_RANGE = ErrorCode("speciesOutsideExpertRange",26,true,"Geographic coordinates are outside the range as defined by 'expert/s' for the taxa", Error)
  val UNCERTAINTY_NOT_SPECIFIED = ErrorCode("uncertaintyNotSpecified", 27, false, "Coordinate uncertainty was not supplied", Missing)
  val COORDINATES_CENTRE_OF_COUNTRY = ErrorCode("coordinatesCentreOfCountry",28,true,"Supplied coordinates centre of country", Warning)
  val MISSING_COORDINATEPRECISION = ErrorCode("missingCoordinatePrecision", 29, false, "coordinatePrecision not supplied with the record", Missing)
  val MISSING_GEODETICDATUM = ErrorCode("missingGeodeticDatum",30, false, "geodeticDatum not supplied for coordinates", Missing)
  val MISSING_GEOREFERNCEDBY = ErrorCode("missingGeorefencedBy", 31, false, "GeoreferencedBy not supplied with the record", Missing)
  val MISSING_GEOREFERENCEPROTOCOL = ErrorCode("missingGeoreferenceProtocol",32, false, "GeoreferenceProtocol not supplied with the record", Missing)
  val MISSING_GEOREFERENCESOURCES = ErrorCode("missingGeoreferenceSources",33,false,"GeoreferenceSources not supplied with the record", Missing)
  val MISSING_GEOREFERENCEVERIFICATIONSTATUS = ErrorCode("missingGeoreferenceVerificationStatus",34, false,"GeoreferenceVerificationStatus not supplied with the record", Missing)
  val INVALID_GEODETICDATUM = ErrorCode("invalidGeodeticDatum", 35, false,"The geodetic datum is not valid", Error)

  val MISSING_GEOREFERENCE_DATE = ErrorCode("missingGeoreferenceDate",42,false, "GeoreferenceDate not supplied with the record", Missing)
  val LOCATION_NOT_SUPPLIED = ErrorCode("locationNotSupplied", 43, false, "No location information has been provided with the record", Missing)
  val DECIMAL_COORDINATES_NOT_SUPPLIED = ErrorCode("decimalCoordinatesNotSupplied", 44, false, "No decimal longitude and latitude provided", Missing)
  val DECIMAL_LAT_LONG_CONVERTED = ErrorCode("decimalLatLongConverted", 45, false, "Decimal latitude and longitude were converted to WGS84", Warning)
  val DECIMAL_LAT_LONG_CONVERSION_FAILED = ErrorCode("decimalLatLongConverionFailed", 46, true, "Conversion of decimal latitude and longitude to WGS84 failed", Error)
  val DECIMAL_LAT_LONG_CALCULATED_FROM_VERBATIM = ErrorCode("decimalLatLongCalculatedFromVerbatim", 47, false, "Decimal latitude and longitude were calculated using verbatimLatitude, verbatimLongitude and verbatimSRS", Warning )
  val DECIMAL_LAT_LONG_CALCULATION_FROM_VERBATIM_FAILED = ErrorCode("decimalLatLongCalculationFromVerbatimFailed", 48, true, "Failed to calculate decimal latitude and longitude from verbatimLatitude, verbatimLongitude and verbatimSRS", Error)
  val DECIMAL_LAT_LONG_CALCULATED_FROM_EASTING_NORTHING = ErrorCode("decimalLatLongCalculatedFromEastingNorthing", 49, false, "Decimal latitude and longitude were calculated using easting, nothing and zone", Warning)
  val DECIMAL_LAT_LONG_CALCULATION_FROM_EASTING_NORTHING_FAILED = ErrorCode("decimalLatLongCalculationFromEastingNorthingFailed", 50, true, "Failed to calculate decimal latitude and longitude using easting, northing and zone", Error)
  val GEODETIC_DATUM_ASSUMED_WGS84 = ErrorCode("geodeticDatumAssumedWgs84", 51, false, "Geodetic datum assumed to be WGS84 (EPSG:4326)", Warning)
  val UNRECOGNIZED_GEODETIC_DATUM = ErrorCode("unrecognizedGeodeticDatum", 52, false, "Geodetic datum not recognized", Error)

  //taxonomy issues
  val TAXONOMIC_ISSUE = ErrorCode("taxonomicIssue",10000,false,"Taxonomic issue", Error)  // general purpose option
  val INVALID_SCIENTIFIC_NAME = ErrorCode("invalidScientificName",10001,false,"Invalid scientific name", Error)
  val UNKNOWN_KINGDOM = ErrorCode("unknownKingdom",10002,false,"Kingdom not recognised", Error)
  val AMBIGUOUS_NAME = ErrorCode("ambiguousName",10003,false,"Higher taxonomy missing", Error)
  val NAME_NOTRECOGNISED = ErrorCode("nameNotRecognised",10004,false,"Name not recognised", Error)
  val NAME_NOT_IN_NATIONAL_CHECKLISTS = ErrorCode("nameNotInNationalChecklists",10005,false,"Name not in national checklists", Warning)
  val HOMONYM_ISSUE = ErrorCode("homonymIssue",10006,false,"Homonym issues with supplied name", Error)
  val IDENTIFICATION_INCORRECT = ErrorCode("identificationIncorrect",10007,false,"Taxon misidentified", Error)
  val MISSING_TAXONRANK = ErrorCode("missingTaxonRank",10008,false,"taxonRank not supplied with the record", Missing)
  val MISSING_IDENTIFICATIONQUALIFIER = ErrorCode("missingIdentificationQualifier",10009, false,"identificationQualifier not supplied with the record", Missing)
  val MISSING_IDENTIFIEDBY = ErrorCode("missingIdentifiedBy",10010,false,"identifiedBy not supplied with the record", Missing)
  val MISSING_IDENTIFICATIONREFERENCES = ErrorCode("missingIdentificationReferences",10011,false,"identificationReferences not supplied with the record", Missing)
  val MISSING_DATEIDENTIFIED = ErrorCode("missingDateIdentified", 10012,false,"identificationDate not supplied with the record", Missing)
  val NAME_NOT_SUPPLIED = ErrorCode("nameNotSupplied", 10015,false,"No scientific name or vernacular name was supplied", Missing)

  //miscellaneous issues
  val MISSING_BASIS_OF_RECORD = ErrorCode("missingBasisOfRecord",20001,true,"Basis of record not supplied", Missing)
  val BADLY_FORMED_BASIS_OF_RECORD = ErrorCode("badlyFormedBasisOfRecord",20002,true,"Basis of record badly formed", Error)
  val UNRECOGNISED_TYPESTATUS = ErrorCode("unrecognisedTypeStatus",20004,false,"Type status not recognised", Error)
  val UNRECOGNISED_COLLECTIONCODE = ErrorCode("unrecognisedCollectionCode",20005,false,"Collection code not recognised", Error)
  val UNRECOGNISED_INSTITUTIONCODE = ErrorCode("unrecognisedInstitutionCode",20006,false,"Institution code not recognised", Error)
  val INVALID_IMAGE_URL = ErrorCode("invalidImageUrl", 20007, false,"Image URL invalid", Error)
  val RESOURCE_TAXONOMIC_SCOPE_MISMATCH = ErrorCode("resourceTaxonomicScopeMismatch", 20008, false, "", Error)
  val DATA_ARE_GENERALISED = ErrorCode("dataAreGeneralised", 20009, false, "The data has been supplied generalised", Warning)
  val OCCURRENCE_IS_CULTIVATED_OR_ESCAPEE = ErrorCode("occCultivatedEscapee", 20010, false, "The occurrence is cultivated or escaped.", Warning)
  val INFERRED_DUPLICATE_RECORD = ErrorCode("inferredDuplicateRecord",20014,false,"The occurrence appears to be a duplicate", Error)
  val MISSING_CATALOGUENUMBER = ErrorCode("missingCatalogueNumber", 20015, false,"No catalogue number has been supplied", Missing)
  val RECORDED_BY_UNPARSABLE = ErrorCode("recordedByUnparsable", 20016, false,"", Warning)

  //temporal issues
  val TEMPORAL_ISSUE = ErrorCode("temporalIssue",30000,false,"Temporal issue", Error)  // general purpose option
  val ID_PRE_OCCURRENCE = ErrorCode("idPreOccurrence",30001,false,"Identification date before occurrence date", Error)
  val GEOREFERENCE_POST_OCCURRENCE = ErrorCode("georefPostDate",30002,false,"Georeferenced after occurrence date", Error)
  val FIRST_OF_MONTH = ErrorCode("firstOfMonth",30003,false,"First of the month", Warning)
  val FIRST_OF_YEAR = ErrorCode("firstOfYear",30004,false,"First of the year", Warning)
  val FIRST_OF_CENTURY = ErrorCode("firstOfCentury",30005,false,"First of the century", Warning)
  val DATE_PRECISION_MISMATCH = ErrorCode("datePrecisionMismatch",30006,false,"Date precision invalid", Error)
  val INVALID_COLLECTION_DATE = ErrorCode("invalidCollectionDate",30007,false,"Invalid collection date", Error)
  val MISSING_COLLECTION_DATE = ErrorCode("missingCollectionDate",30008,false,"Missing collection date",Missing)
  val DAY_MONTH_TRANSPOSED = ErrorCode("dayMonthTransposed",30009,false,"Day and month transposed", Warning)

  //verified type - this is a special code
  val VERIFIED = ErrorCode("userVerified", 50000, true, "Record Verified by collection manager", Verified)

  //this is a code user can use to flag a issue with processing
  val PROCESSING_ERROR = ErrorCode("processingError", 60000, true, "The system has incorrectly processed a record", Error)

  /**
   * Retrieve all the terms defined in this vocab.
   * @return
   */
  val retrieveAll : Set[ErrorCode] = {
    val methods = this.getClass.getMethods
    (for {
      method<-methods
      if(method.getReturnType.getName == "au.org.ala.biocache.vocab.ErrorCode")
    } yield (method.invoke(this).asInstanceOf[ErrorCode])).toSet[ErrorCode]
  }

  //all the codes
  val all = retrieveAll

  //ranges for error codes
  val geospatialBounds = (0, 10000)
  val taxonomicBounds = (10000, 20000)
  val miscellanousBounds = (20000, 30000)
  val temporalBounds = (30000, 40000)
  val importantCodes = Array(4,5,18,19,26)

  val geospatialCodes = all.filter(errorCode => {errorCode.code >= geospatialBounds._1 && errorCode.code < geospatialBounds._2})
  val taxonomicCodes = all.filter(errorCode => {errorCode.code>=10000 && errorCode.code<20000})
  val miscellaneousCodes = all.filter(errorCode => {errorCode.code>=20000 && errorCode.code<30000})
  val temporalCodes = all.filter(errorCode => {errorCode.code>=30000 && errorCode.code<40000})

  val userAssertionCodes = Array(GEOSPATIAL_ISSUE,COORDINATE_HABITAT_MISMATCH,DETECTED_OUTLIER,TAXONOMIC_ISSUE,IDENTIFICATION_INCORRECT,TEMPORAL_ISSUE)
  //the assertions that are NOT performed during the processing phase
  val offlineAssertionCodes = Array(INFERRED_DUPLICATE_RECORD, SPECIES_OUTSIDE_EXPERT_RANGE, DETECTED_OUTLIER)

  /** Retrieve an error code by the numeric code */
  def getByCode(code:Int) : Option[ErrorCode] = all.find(errorCode => errorCode.code == code)

  def getByName(name:String) :Option[ErrorCode] = all.find(errorCode => errorCode.name == name)

  def getAllByCode(codes:Array[String]):Set[ErrorCode] = all.filter(ec => codes.contains(ec.code.toString))

  def getMissingByCode(codes:List[Int]):Set[ErrorCode] =
    (AssertionCodes.all &~ Set(VERIFIED, PROCESSING_ERROR)).filterNot(e => codes.contains(e.code))

  def getMissingCodes(code:Set[ErrorCode]) : Set[ErrorCode] =
    AssertionCodes.all &~ (code ++ userAssertionCodes.toSet ++ Set(VERIFIED, PROCESSING_ERROR))

  def isVerified(assertion:QualityAssertion) :Boolean = assertion.code == VERIFIED.code

  /** Is it geospatially kosher */
  def isGeospatiallyKosher (assertions:Array[QualityAssertion]) : Boolean = assertions.filter(ass => {
     val errorCode = AssertionCodes.all.find(errorCode => errorCode.code == ass.code )
     if(!errorCode.isEmpty){
       ass.code >= AssertionCodes.geospatialBounds._1 &&
              ass.code < AssertionCodes.geospatialBounds._2 &&
              errorCode.get.isFatal
     } else {
        false
     }
  }).isEmpty

   /** Is it geospatially kosher based on a list of codes that have been asserted */
  def isGeospatiallyKosher(assertions:Array[Int]):Boolean = assertions.filter(qa=> {
      val code = AssertionCodes.geospatialCodes.find(c => c.code == qa)
      !code.isEmpty && code.get.isFatal
  }).isEmpty

  /** Is it taxonomically kosher */
  def isTaxonomicallyKosher (assertions:Array[QualityAssertion]) : Boolean = assertions.filter(ass => {
    val errorCode = AssertionCodes.all.find(errorCode => errorCode.code == ass.code )
    if(!errorCode.isEmpty){
      ass.code >= AssertionCodes.taxonomicBounds._1 &&
      ass.code < AssertionCodes.taxonomicBounds._2 &&
      errorCode.get.isFatal
    } else {
      false //we cant find the code, so ignore
    }
  }).isEmpty

  /** Is it taxonomically kosher based on a list of codes that have been asserted */
  def isTaxonomicallyKosher(assertions:Array[Int]):Boolean = assertions.filter(qa=> {
    val code = AssertionCodes.taxonomicCodes.find(c => c.code == qa)
    !code.isEmpty && code.get.isFatal
  }).isEmpty
}
