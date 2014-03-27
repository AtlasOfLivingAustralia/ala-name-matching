package au.org.ala.biocache.processor

import org.slf4j.LoggerFactory
import au.org.ala.biocache._
import au.org.ala.sds.SensitiveDataService
import scala.collection.JavaConversions
import scala.collection.mutable.ArrayBuffer
import org.apache.commons.lang.StringUtils
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.geotools.referencing.CRS
import org.geotools.referencing.operation.DefaultCoordinateOperationFactory
import org.geotools.geometry.GeneralDirectPosition
import org.apache.commons.math3.util.Precision
import au.org.ala.sds.validation.ServiceFactory
import au.org.ala.biocache.caches.{TaxonProfileDAO, LocationDAO}
import au.org.ala.biocache.parser.{DistanceRangeParser, VerbatimLatLongParser}
import au.org.ala.biocache.model._
import au.org.ala.biocache.load.FullRecordMapper
import au.org.ala.biocache.vocab._
import scala.Some
import scala.Some
import au.org.ala.biocache.util.{Json, StringHelper}

/**
 * Processor of location information.
 */
class LocationProcessor extends Processor {

  import StringHelper._

  val logger = LoggerFactory.getLogger("LocationProcessor")
  //This is being initialised here because it may take some time to load all the XML records...
  lazy val sdsFinder = Config.sdsFinder
  val sds = new SensitiveDataService()

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

  import JavaConversions._

  /**
   * Process geospatial details
   *
   * TODO: Handle latitude and longitude that is supplied in verbatim format
   * We will need to parse a variety of formats. Bryn was going to find some regular
   * expressions/test cases he has used previously...
   *
   */
  def process(guid: String, raw: FullRecord, processed: FullRecord, lastProcessed: Option[FullRecord]=None): Array[QualityAssertion] = {

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
        //TODO find out if the EEZ layer has been include so the value can be obtained for this.
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

        //sensitise the coordinates if necessary.  Do this last so that habitat checks etc are performed on originally supplied coordinates
        try {
          processSensitivity(raw, processed, location, contextualLayers)
        } catch {
          case e: Exception => logger.error("Problem processing using the SDS for record " + guid, e)
        }
      }
      assertions += QualityAssertion(AssertionCodes.LOCATION_NOT_SUPPLIED,1)
    } else{
      //check to see if we have any location information at all for the record
      if (raw.location.footprintWKT == null && raw.location.locality == null && raw.location.locationID == null){
        assertions += QualityAssertion(AssertionCodes.LOCATION_NOT_SUPPLIED)
      } else {
        assertions += QualityAssertion(AssertionCodes.LOCATION_NOT_SUPPLIED, 1)
      }
    }

    processLocations(raw,processed,assertions)

    //validate the gereference values
    //TODO reenable georeferencing processing after we have categorised issues better.
    validateGeoreferenceValues(raw,processed,assertions)

    assertions.toArray
  }

  def processLocations(raw: FullRecord, processed: FullRecord, assertions: ArrayBuffer[QualityAssertion]){
    if(raw.location.country == null && processed.location.country != null){
      assertions += QualityAssertion(AssertionCodes.COUNTRY_INFERRED_FROM_COORDINATES,0)
    } else{
      assertions += QualityAssertion(AssertionCodes.COUNTRY_INFERRED_FROM_COORDINATES,1)
    }

    //check centre point of the state
    if (StateProvinceCentrePoints.coordinatesMatchCentre(processed.location.stateProvince, raw.location.decimalLatitude, raw.location.decimalLongitude)) {
      assertions += QualityAssertion(AssertionCodes.COORDINATES_CENTRE_OF_STATEPROVINCE, "Coordinates are centre point of " + processed.location.stateProvince)
    } else{
      assertions += QualityAssertion(AssertionCodes.COORDINATES_CENTRE_OF_STATEPROVINCE,1)
    }

    //check centre point of the country
    if (CountryCentrePoints.coordinatesMatchCentre(processed.location.country, raw.location.decimalLatitude, raw.location.decimalLongitude)) {
      assertions += QualityAssertion(AssertionCodes.COORDINATES_CENTRE_OF_COUNTRY, "Coordinates are centre point of " + processed.location.country)
    } else {
      assertions += QualityAssertion(AssertionCodes.COORDINATES_CENTRE_OF_COUNTRY, 1)
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
  }

  /**
   * Performs the QAs associated with elevation and depth
   */
  def processAltitudeAndDepth(guid: String, raw: FullRecord, processed: FullRecord, assertions: ArrayBuffer[QualityAssertion]) {
    //check that the values are numeric
    if (raw.location.verbatimDepth != null) {
      val parseDepthResult = DistanceRangeParser.parse(raw.location.verbatimDepth)
      if (parseDepthResult.isDefined){
        val (vdepth, sourceUnit) = parseDepthResult.get
        processed.location.verbatimDepth = vdepth.toString
        if (vdepth > 10000)
          assertions += QualityAssertion(AssertionCodes.DEPTH_OUT_OF_RANGE, "Depth " + vdepth + " is greater than 10,000 metres")
        else
          assertions += QualityAssertion(AssertionCodes.DEPTH_OUT_OF_RANGE, 1)
        assertions += QualityAssertion(AssertionCodes.DEPTH_NON_NUMERIC, 1)
        //check on the units
        if(sourceUnit == Feet){
          assertions += QualityAssertion(AssertionCodes.DEPTH_IN_FEET, "The supplied depth was in feet it has been converted to metres")
        } else {
          assertions += QualityAssertion(AssertionCodes.DEPTH_IN_FEET, 1)
        }
      } else{
        assertions += QualityAssertion(AssertionCodes.DEPTH_NON_NUMERIC, "Can't parse verbatimDepth " + raw.location.verbatimDepth)
      }
    }

    if (raw.location.verbatimElevation != null) {
      val parseElevationResult = DistanceRangeParser.parse(raw.location.verbatimElevation)
      if(parseElevationResult.isDefined){
        val (velevation, sourceUnit) = parseElevationResult.get
        processed.location.verbatimElevation = velevation.toString
        if (velevation > 10000 || velevation < -100){
          assertions += QualityAssertion(AssertionCodes.ALTITUDE_OUT_OF_RANGE, "Elevation " + velevation + " is greater than 10,000 metres or less than -100 metres.")
        } else {
          assertions += QualityAssertion(AssertionCodes.ALTITUDE_OUT_OF_RANGE, 1)
        }
        assertions += QualityAssertion(AssertionCodes.ALTITUDE_NON_NUMERIC,1)

        if(sourceUnit == Feet){
          assertions += QualityAssertion(AssertionCodes.ALTITUDE_IN_FEET, "The supplied altitude was in feet it has been converted to metres")
        } else {
          assertions += QualityAssertion(AssertionCodes.ALTITUDE_IN_FEET , 1)
        }
      } else {
        assertions += QualityAssertion(AssertionCodes.ALTITUDE_NON_NUMERIC, "Can't parse verbatimElevation " + raw.location.verbatimElevation)
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
          assertions += QualityAssertion(AssertionCodes.MIN_MAX_DEPTH_REVERSED, "The minimum, " + min + ", and maximum, " + max + ", depths have been transposed.")
        } else {
          processed.location.minimumDepthInMeters = min.toString
          processed.location.maximumDepthInMeters = max.toString
          assertions += QualityAssertion(AssertionCodes.MIN_MAX_DEPTH_REVERSED, 1)
        }
      }
      catch {
        case _:Exception =>
      }
    }

    if (raw.location.minimumElevationInMeters != null && raw.location.maximumElevationInMeters != null) {
      try {
        val min = raw.location.minimumElevationInMeters.toFloat
        val max = raw.location.maximumElevationInMeters.toFloat
        if (min > max) {
          processed.location.minimumElevationInMeters = max.toString
          processed.location.maximumElevationInMeters = min.toString
          assertions += QualityAssertion(AssertionCodes.MIN_MAX_ALTITUDE_REVERSED, "The minimum, " + min + ", and maximum, " + max + ", elevations have been transposed.")
        } else {
          processed.location.minimumElevationInMeters = min.toString
          processed.location.maximumElevationInMeters = max.toString
          assertions += QualityAssertion(AssertionCodes.MIN_MAX_ALTITUDE_REVERSED,1)
        }
      } catch {
        case e:Exception => logger.debug("Exception thrown processing elevation:" + e.getMessage())
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
      //coordinates were supplied so the test passed
      assertions += QualityAssertion(AssertionCodes.DECIMAL_COORDINATES_NOT_SUPPLIED, 1)
      // if decimal lat/long is provided in a CRS other than WGS84, then we need to reproject

      if (rawGeodeticDatum != null) {
        //no assumptions about the datum is being made:
        assertions += QualityAssertion(AssertionCodes.GEODETIC_DATUM_ASSUMED_WGS84, 1)
        val sourceEpsgCode = lookupEpsgCode(rawGeodeticDatum)
        if (!sourceEpsgCode.isEmpty) {
          //datum is recognised so pass the test:
          assertions += QualityAssertion(AssertionCodes.UNRECOGNIZED_GEODETIC_DATUM,1)
          if (sourceEpsgCode.get == WGS84_EPSG_Code) {
            //already in WGS84, no need to reproject
            Some((rawLatitude, rawLongitude, WGS84_EPSG_Code))
          } else {
            // Reproject decimal lat/long to WGS84
            val desiredNoDecimalPlaces = math.min(getNumberOfDecimalPlacesInDouble(rawLatitude), getNumberOfDecimalPlacesInDouble(rawLongitude))

            val reprojectedCoords = reprojectCoordinatesToWGS84(rawLatitude.toDouble, rawLongitude.toDouble, sourceEpsgCode.get, desiredNoDecimalPlaces)
            if (reprojectedCoords.isEmpty) {
              assertions += QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CONVERSION_FAILED, "Transformation of decimal latiude and longitude to WGS84 failed")
              None
            } else {
              //transformation of coordinates did not fail:
              assertions += QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CONVERSION_FAILED, 1)
              assertions += QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CONVERTED, "Decimal latitude and longitude were converted to WGS84 (EPSG:4326)")
              val (reprojectedLatitude, reprojectedLongitude) = reprojectedCoords.get
              Some(reprojectedLatitude, reprojectedLongitude, WGS84_EPSG_Code)
            }
          }
        } else {
          assertions += QualityAssertion(AssertionCodes.UNRECOGNIZED_GEODETIC_DATUM, "Geodetic datum \"" + rawGeodeticDatum + "\" not recognized.")
          Some((rawLatitude, rawLongitude, rawGeodeticDatum))
        }
      } else {
        //assume coordinates already in WGS84
        assertions += QualityAssertion(AssertionCodes.GEODETIC_DATUM_ASSUMED_WGS84, "Geodetic datum assumed to be WGS84 (EPSG:4326)")
        Some((rawLatitude, rawLongitude, WGS84_EPSG_Code))
      }

      // Attempt to infer the decimal latitude and longitude from the verbatim latitude and longitude
    } else {
      //no decimal latitude/longitude was provided
      assertions += QualityAssertion(AssertionCodes.DECIMAL_COORDINATES_NOT_SUPPLIED)
      if (verbatimLatitude != null && verbatimLongitude != null) {
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
                //calculation from verbatim did NOT fail:
                assertions += QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATION_FROM_VERBATIM_FAILED, 1)
                if (sourceEpsgCode.get == WGS84_EPSG_Code) {
                  //already in WGS84, no need to reproject
                  assertions += QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATED_FROM_VERBATIM, "Decimal latitude and longitude were calculated using verbatimLatitude, verbatimLongitude and verbatimSRS")
                  Some((decimalVerbatimLat.get.toString, decimalVerbatimLong.get.toString, WGS84_EPSG_Code))
                } else {
                  val desiredNoDecimalPlaces = math.min(getNumberOfDecimalPlacesInDouble(decimalVerbatimLat.get.toString), getNumberOfDecimalPlacesInDouble(decimalVerbatimLong.get.toString))

                  val reprojectedCoords = reprojectCoordinatesToWGS84(decimalVerbatimLat.get, decimalVerbatimLong.get, sourceEpsgCode.get, desiredNoDecimalPlaces)
                  if (reprojectedCoords.isEmpty) {
                    assertions += QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATION_FROM_VERBATIM_FAILED, "Transformation of verbatim latiude and longitude to WGS84 failed")
                    None
                  } else {
                    //reprojection did NOT fail:
                    assertions += QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATION_FROM_VERBATIM_FAILED, 1)
                    assertions += QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATED_FROM_VERBATIM, "Decimal latitude and longitude were calculated using verbatimLatitude, verbatimLongitude and verbatimSRS")
                    val (reprojectedLatitude, reprojectedLongitude) = reprojectedCoords.get
                    Some(reprojectedLatitude, reprojectedLongitude, WGS84_EPSG_Code)
                  }
                }
              } else {
                assertions += QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATION_FROM_VERBATIM_FAILED, "Unrecognized verbatimSRS " + verbatimSRS)
                None
              }
              // Otherwise, assume latitude and longitude are already in WGS 84
            } else if (decimalVerbatimLat.get.toString.isLatitude && decimalVerbatimLong.get.toString.isLongitude) {
              //conversion dod NOT fail
              assertions += QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATION_FROM_VERBATIM_FAILED,1)
              assertions += QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATED_FROM_VERBATIM, "Decimal latitude and longitude were calculated using verbatimLatitude, verbatimLongitude and verbatimSRS")
              Some((decimalVerbatimLat.get.toString, decimalVerbatimLong.get.toString, WGS84_EPSG_Code))
            } else {
              // Invalid latitude, longitude
              assertions += QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATION_FROM_VERBATIM_FAILED, "Could not parse verbatim latitude and longitude")
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
              assertions += QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATION_FROM_EASTING_NORTHING_FAILED, "Transformation of verbatim easting and northing to WGS84 failed")
              None
            } else {
              //lat and long from easting and northing did NOT fail:
              assertions += QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATION_FROM_EASTING_NORTHING_FAILED, 1)
              assertions += QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATED_FROM_EASTING_NORTHING, "Decimal latitude and longitude were calculated using easting, northing and zone.")
              val (reprojectedLatitude, reprojectedLongitude) = reprojectedCoords.get
              Some(reprojectedLatitude, reprojectedLongitude, WGS84_EPSG_Code)
            }
          } else {
            None
          }
        } else {
          if (verbatimSRS == null) {
            assertions += QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATION_FROM_EASTING_NORTHING_FAILED, "Unrecognized zone GDA94 / MGA zone " + zone)
          } else {
            assertions += QualityAssertion(AssertionCodes.DECIMAL_LAT_LONG_CALCULATION_FROM_EASTING_NORTHING_FAILED, "Unrecognized zone " + verbatimSRS + " / zone " + zone)
          }
          None
        }
      } else {
        None
      }
    }
  }

  /**
   * Reprojects coordinates into WGS 84
   * @param coordinate1 first coordinate. If source value is easting/northing, then this should be the easting value. Otherwise it should be the latitude
   * @param coordinate2 first coordinate. If source value is easting/northing, then this should be the northing value. Otherwise it should be the longitude
   * @param sourceCrsEpsgCode epsg code for the source CRS, e.g. EPSG:4202 for AGD66
   * @param decimalPlacesToRoundTo number of decimal places to round the reprojected coordinates to
   * @return Reprojected coordinates (latitude, longitude), or None if the operation failed.
   */
  def reprojectCoordinatesToWGS84(coordinate1: Double, coordinate2: Double, sourceCrsEpsgCode: String, decimalPlacesToRoundTo: Int): Option[(String, String)] = {
    try {
      val wgs84CRS = DefaultGeographicCRS.WGS84
      val sourceCRS = CRS.decode(sourceCrsEpsgCode)
      val transformOp = new DefaultCoordinateOperationFactory().createOperation(sourceCRS, wgs84CRS)
      val directPosition = new GeneralDirectPosition(coordinate1, coordinate2)
      val wgs84LatLong = transformOp.getMathTransform().transform(directPosition, null)

      //NOTE - returned coordinates are longitude, latitude, despite the fact that if coverting latitude and longitude values, they must be supplied as latitude, longitude.
      //No idea why this is the case.
      val longitude = wgs84LatLong.getOrdinate(0)
      val latitude = wgs84LatLong.getOrdinate(1)

      val roundedLongitude = Precision.round(longitude, decimalPlacesToRoundTo)
      val roundedLatitude = Precision.round(latitude, decimalPlacesToRoundTo)

      Some(roundedLatitude.toString, roundedLongitude.toString)
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
    var checkedPrecision =false
    if (raw.location.coordinateUncertaintyInMeters != null && raw.location.coordinateUncertaintyInMeters.length > 0) {
      //parse it into a numeric number in metres
      //TODO should this be a whole number??
      val parsedResult = DistanceRangeParser.parse(raw.location.coordinateUncertaintyInMeters)
      if (!parsedResult.isEmpty) {
        val (parsedValue, rawUnit) = parsedResult.get
        if(parsedValue > 0){
          //not an uncertainty mismatch
          assertions += QualityAssertion(AssertionCodes.UNCERTAINTY_RANGE_MISMATCH, 1)
        } else {
          val comment = "Supplied uncertainty, " + raw.location.coordinateUncertaintyInMeters + ", is not a supported format"
          assertions += QualityAssertion(AssertionCodes.UNCERTAINTY_RANGE_MISMATCH, comment)
        }
        processed.location.coordinateUncertaintyInMeters = parsedValue.toString
      } else {
        val comment = "Supplied uncertainty, " + raw.location.coordinateUncertaintyInMeters + ", is not a supported format"
        assertions += QualityAssertion(AssertionCodes.UNCERTAINTY_RANGE_MISMATCH, comment)
      }
    } else {
      //check to see if the uncertainty has incorrectly been put in the precision
      if (raw.location.coordinatePrecision != null) {
        //TODO work out what sort of custom parsing is necessary
        val value = raw.location.coordinatePrecision.toFloatWithOption
        if (!value.isEmpty && value.get > 1) {
          processed.location.coordinateUncertaintyInMeters = value.get.toInt.toString
          val comment = "Supplied precision, " + raw.location.coordinatePrecision + ", is assumed to be uncertainty in metres";
          assertions += QualityAssertion(AssertionCodes.UNCERTAINTY_IN_PRECISION, comment)
          checkedPrecision = true
        }
      }
    }
    if (raw.location.coordinatePrecision == null){
      assertions += QualityAssertion(AssertionCodes.MISSING_COORDINATEPRECISION, "Missing coordinatePrecision")
    } else {
      assertions += QualityAssertion(AssertionCodes.MISSING_COORDINATEPRECISION, 1)
      if(!checkedPrecision){
        val value = raw.location.coordinatePrecision.toFloatWithOption
        if(value.isDefined){
          //Ensure that the precision is within the required ranges
          if (value.get > 0 && value.get <= 1){
            assertions += QualityAssertion(AssertionCodes.PRECISION_RANGE_MISMATCH, 1)
            //now test for coordinate precision
            val pre = if (raw.location.coordinatePrecision.contains(".")) raw.location.coordinatePrecision.split("\\.")(1).length else 0
            val lat = processed.location.decimalLatitude
            val long = processed.location.decimalLongitude
            val latp = if(lat.contains("."))lat.split("\\.")(1).length else 0
            val lonp = if(long.contains("."))long.split("\\.")(1).length else 0
            if(pre == latp && pre == lonp){
              // no coordinate precision mismatch exists
              assertions += QualityAssertion(AssertionCodes.COORDINATE_PRECISION_MISMATCH, 1)
            } else {
              assertions += QualityAssertion(AssertionCodes.COORDINATE_PRECISION_MISMATCH)
            }
          }
          else{
            assertions += QualityAssertion(AssertionCodes.PRECISION_RANGE_MISMATCH, "Coordinate precision is not between 0 and 1" )
          }
        }
        else {
           assertions += QualityAssertion(AssertionCodes.PRECISION_RANGE_MISMATCH, "Unable to parse the coordinate precision")
        }
      }
    }

    // If the coordinateUncertainty is still empty populate it with the default
    // value (we don't test until now because the SDS will sometime include coordinate uncertainty)
    // This step will pick up on default values because processed.location.coordinateUncertaintyInMeters
    // will already be populated if a default value exists
    if (processed.location.coordinateUncertaintyInMeters == null) {
      assertions += QualityAssertion(AssertionCodes.UNCERTAINTY_NOT_SPECIFIED, "Uncertainty was not supplied")
    } else{
      assertions += QualityAssertion(AssertionCodes.UNCERTAINTY_NOT_SPECIFIED, 1)
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
          //HACK FOR BAD DATA
          if (habitatsAsString != "???") {
            logger.debug("[QualityAssertion] ******** Habitats incompatible for ROWKEY: " + raw.rowKey + ", processed:"
              + processed.location.habitat + ", retrieved:" + habitatsAsString
              + ", http://maps.google.com/?ll=" + processed.location.decimalLatitude + ","
              + processed.location.decimalLongitude)
            val comment = "Recognised habitats for species: " + habitatsAsString +
              ", Value determined from coordinates: " + habitatFromPoint
            assertions += QualityAssertion(AssertionCodes.COORDINATE_HABITAT_MISMATCH, comment)
          }
        } else{
          //habitats ARE compatible
          assertions += QualityAssertion(AssertionCodes.COORDINATE_HABITAT_MISMATCH, 1)
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
        assertions += QualityAssertion(AssertionCodes.STATE_COORDINATE_MISMATCH, comment)
      } else{
        //states are not in mismatch
        assertions += QualityAssertion(AssertionCodes.STATE_COORDINATE_MISMATCH, 1)
      }
    }
  }

  def validateGeoreferenceValues(raw: FullRecord, processed: FullRecord, assertions: ArrayBuffer[QualityAssertion]) = {
    //check for missing geodeticDatum
    if (raw.location.geodeticDatum == null && processed.location.geodeticDatum == null)
      assertions += QualityAssertion(AssertionCodes.MISSING_GEODETICDATUM, "Missing geodeticDatum")
    else
      assertions += QualityAssertion(AssertionCodes.MISSING_GEODETICDATUM,1)
    //check for missing georeferencedBy
    if (raw.location.georeferencedBy == null && processed.location.georeferencedBy == null)
      assertions += QualityAssertion(AssertionCodes.MISSING_GEOREFERNCEDBY, "Missing georeferencedBy")
    else
      assertions += QualityAssertion(AssertionCodes.MISSING_GEOREFERNCEDBY, 1)
    //check for missing georeferencedProtocol
    if (raw.location.georeferenceProtocol == null && processed.location.georeferenceProtocol == null)
      assertions += QualityAssertion(AssertionCodes.MISSING_GEOREFERENCEPROTOCOL, "Missing georeferenceProtocol")
    else
      assertions += QualityAssertion(AssertionCodes.MISSING_GEOREFERENCEPROTOCOL,1)
    //check for missing georeferenceSources
    if (raw.location.georeferenceSources == null && processed.location.georeferenceSources == null)
      assertions += QualityAssertion(AssertionCodes.MISSING_GEOREFERENCESOURCES, "Missing georeferenceSources")
    else
      assertions += QualityAssertion(AssertionCodes.MISSING_GEOREFERENCESOURCES,1)
    //check for missing georeferenceVerificationStatus
    if (raw.location.georeferenceVerificationStatus == null && processed.location.georeferenceVerificationStatus == null)
      assertions += QualityAssertion(AssertionCodes.MISSING_GEOREFERENCEVERIFICATIONSTATUS, "Missing georeferenceVerificationStatus")
    else
      assertions += QualityAssertion(AssertionCodes.MISSING_GEOREFERENCEVERIFICATIONSTATUS,1)
    //check for missing georeferenceDate
    if (StringUtils.isBlank(raw.location.georeferencedDate) && !raw.miscProperties.containsKey("georeferencedDate")){
      assertions += QualityAssertion(AssertionCodes.MISSING_GEOREFERENCE_DATE)
    } else {
      assertions += QualityAssertion(AssertionCodes.MISSING_GEOREFERENCE_DATE, 1)
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
          assertions += QualityAssertion(AssertionCodes.INVERTED_COORDINATES, "Assume that coordinates have been inverted. Original values: " +
            processed.location.decimalLatitude + "," + processed.location.decimalLongitude)
          val tmp = processed.location.decimalLatitude
          processed.location.decimalLatitude = processed.location.decimalLongitude
          processed.location.decimalLongitude = tmp
          //coordinates are not out of range:
          assertions += QualityAssertion(AssertionCodes.COORDINATES_OUT_OF_RANGE, 1)
        } else {
          assertions += QualityAssertion(AssertionCodes.COORDINATES_OUT_OF_RANGE, "Coordinates are out of range: " +
            processed.location.decimalLatitude + "," + processed.location.decimalLongitude)
          assertions += QualityAssertion(AssertionCodes.INVERTED_COORDINATES,1)
        }
      } else{
        assertions ++= Array(QualityAssertion(AssertionCodes.INVERTED_COORDINATES,1), QualityAssertion(AssertionCodes.COORDINATES_OUT_OF_RANGE, 1))
      }

      if (lat == 0.0f && lon == 0.0f) {
        assertions += QualityAssertion(AssertionCodes.ZERO_COORDINATES, "Coordinates 0,0")
        processed.location.decimalLatitude = null
        processed.location.decimalLongitude = null
      } else{
        assertions += QualityAssertion(AssertionCodes.ZERO_COORDINATES,1)
      }

      if (raw.location.country != null && raw.location.country != "") {

        val country = Countries.matchTerm(raw.location.country)

        if (!country.isEmpty) {

          assertions += QualityAssertion(AssertionCodes.UNKNOWN_COUNTRY_NAME, 1)

          CountryCentrePoints.matchName(country.get.canonical) match {

            case Some((latlng, bbox)) => {

              if (!bbox.containsPoint(lat, lon)) {

                var hasCoordinateMismatch = true

                if (bbox.containsPoint(lat * -1, lon)) {
                  //latitude is negated
                  assertions += QualityAssertion(AssertionCodes.NEGATED_LATITUDE,
                    "Latitude seems to be negated. Original value:" + processed.location.decimalLatitude)
                  processed.location.decimalLatitude = (lat * -1).toString
                  hasCoordinateMismatch = false
                }

                if (bbox.containsPoint(lat, lon * -1)) {
                  //point in wrong EW hemisphere - what do we do?
                  assertions += QualityAssertion(AssertionCodes.NEGATED_LONGITUDE,
                    "Longitude seems to be negated. Original value: " + processed.location.decimalLongitude)
                  processed.location.decimalLongitude = (lon * -1).toString
                  hasCoordinateMismatch = false
                }

                if(hasCoordinateMismatch){
                  assertions += QualityAssertion(AssertionCodes.COUNTRY_COORDINATE_MISMATCH)
                } else {
                  //there was no mismatch
                  assertions += QualityAssertion(AssertionCodes.COUNTRY_COORDINATE_MISMATCH, 1)
                }

              }
            }
            case _ => //do nothing
          }
        } else {
          assertions += QualityAssertion(AssertionCodes.UNKNOWN_COUNTRY_NAME, "Country name '" + raw.location.country + "' not recognised.")
        }
      }
    }
  }
  /**
   * New version to process the sensitivity.  It allows for Pest sensitivity to be reported in the "informationWithheld" field.
   * Rework will be necessary when we work out the best way to handle these.
   *
   */
  def processSensitivity(raw: FullRecord, processed: FullRecord, location: Location, contextualLayers: Map[String, String]) = {
    //needs to be performed for all records whether or not they are in Australia
    //get a map representation of the raw record...
    var rawMap = scala.collection.mutable.Map[String, String]()
    raw.objectArray.foreach(poso => {
      val map = FullRecordMapper.mapObjectToProperties(poso, Versions.RAW)
      rawMap.putAll(map)
    })

    //put the state information that we have from the point
    if(location.stateProvince != null){
      rawMap.put("stateProvince", location.stateProvince)
    }

    //put the required contexual layers in the map
    au.org.ala.sds.util.GeoLocationHelper.getGeospatialLayers.foreach(key => {
      rawMap.put(key, contextualLayers.getOrElse(key, "n/a"))
    })

    //put the processed event date components in to allow for correct date applications of the rules
    if(processed.event.day != null)
      rawMap("day") = processed.event.day
    if(processed.event.month != null)
      rawMap("month") = processed.event.month
    if(processed.event.year != null)
      rawMap("year") = processed.event.year

    val exact = getExactSciName(raw)
    //now get the ValidationOutcome from the Sensitive Data Service
    val outcome = sds.testMapDetails(sdsFinder,rawMap, exact, processed.classification.taxonConceptID)

    if (outcome != null && outcome.isValid && outcome.isSensitive) {

      if (outcome.getResult != null) {
        //conservation sensitive species will have a map of new values in the result
        //the map that is returned needs to be used to update the raw record
        val map: scala.collection.mutable.Map[java.lang.String, Object] = outcome.getResult
        //logger.debug("SDS return map: "+map)
        //convert it to a string string map
        val stringMap = map.collect({
          case (k, v) if v != null => if (k == "originalSensitiveValues") {
            val osv = v.asInstanceOf[java.util.HashMap[String, String]]
            //add the original "processed" coordinate uncertainty to the sensitive values so that it can be available if necessary
            if (processed.location.coordinateUncertaintyInMeters != null)
              osv.put("coordinateUncertaintyInMeters.p", processed.location.coordinateUncertaintyInMeters)
              //remove all the el/cl's from the original sensitive values
              au.org.ala.sds.util.GeoLocationHelper.getGeospatialLayers.foreach(key =>osv.remove(key))
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

      if(outcome.getReport().getMessages() != null){
        var infoMessage =""
        outcome.getReport().getMessages().foreach(message=>{
          infoMessage += message.getCategory() + "\t" + message.getMessageText() + "\n"
        })
        processed.occurrence.informationWithheld=infoMessage
      }
    } else {
      //Species is NOT sensitive
      //if the raw record has originalSensitive values we need to re-initialise the value
      if (raw.occurrence.originalSensitiveValues != null && !raw.occurrence.originalSensitiveValues.isEmpty) {
        Config.persistenceManager.put(raw.rowKey, "occ", raw.occurrence.originalSensitiveValues + ("originalSensitiveValues" -> ""))
      }
    }
  }

  /** Performs all the sensitivity processing.  Returns the new point ot be working with */
  def processSensitivityOldVersion(raw: FullRecord, processed: FullRecord, location: Location, contextualLayers: Map[String, String]) = {

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
        } else if (exact != null){
          sdsFinder.findSensitiveSpeciesByExactMatch(exact)
        } else {
          null
        }
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
          rawMap.put(key, contextualLayers.getOrElse(key, "n/a"))
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
          } else {
            //TO do something with the PEST
            if (voutcome.getReport() != null)
              processed.occurrence.informationWithheld = "PEST: " + voutcome.getReport().toString()
          }
        }
      } else {
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
