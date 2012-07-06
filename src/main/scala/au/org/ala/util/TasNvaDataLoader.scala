package au.org.ala.util
import au.org.ala.biocache.DataLoader
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.protocol.Protocol
import scala.xml.XML
import au.org.ala.biocache.FullRecord
import scala.xml.Node
import au.org.ala.biocache.FullRecordMapper
import au.org.ala.biocache.Versions
import javax.xml.bind.JAXBContext
import com.vividsolutions.jts.io.WKTWriter
import java.io.StringReader
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.gml2.GMLReader
import org.geotools.factory.Hints
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.geotools.geometry.jts.JTSFactoryFinder
import com.vividsolutions.jts.geom.GeometryFactory
import org.geotools.gml3.GMLConfiguration
import org.geotools.xml.Parser
import org.apache.commons.lang.StringUtils
import org.apache.commons.io.IOUtils
import org.geotools.referencing.CRS
import org.geotools.referencing.ReferencingFactoryFinder
import org.geotools.referencing.operation.DefaultCoordinateOperationFactory
import org.geotools.geometry.GeneralDirectPosition
import java.text.MessageFormat
import org.apache.commons.io.FileUtils
import java.io.File
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory

/**
 * Loads data from the Tasmanian Natural Value Atlas
 */
object TasNvaDataLoader extends DataLoader {

    // Raw metadata keys    
    val OBSERVATION_KEY = "WFS_OBSERVATION"

    val GEOMETRY_KEY = "GEOMETRY"
    val KINGDOM_KEY = "KINGDOM"
    val PHYLUM_KEY = "PHYLUM"
    val CLASS1_KEY = "CLASS1"
    val ORDER1_KEY = "ORDER1"
    val FAMILY_KEY = "FAMILY"
    val GENUS_KEY = "GENUS"
    val SPECIES_CODE_KEY = "SPECIES_CODE"
    val NATIONAL_SCHEDULE_KEY = "NATIONAL_SCHEDULE"
    val STATE_SCHEDULE_KEY = "STATE_SCHEDULE"
    val SPECIES_NAME_KEY = "SPECIES_NAME"
    val INFRASPECIES_KEY = "INFRASPECIES"
    val INFRASPECIES_RANK_KEY = "INFRASPECIES_RANK"
    val SPECIES_AUTHORITY_KEY = "SPECIES_AUTHORITY"
    val SPECIES_PUBLICATION_KEY = "SPECIES_PUBLICATION"
    val INFRASPECIES_PUBLICATION_KEY = "INFRASPECIES_PUBLICATION"
    val INFRASPECIES_AUTHORITY_KEY = "INFRASPECIES_AUTHORITY"
    val SPECIES_NOTES_KEY = "SPECIES_NOTES"
    val PREFERRED_COMMON_NAMES_KEY = "PREFERRED_COMMON_NAMES"
    val CONSERVATION_SIGNIFICANCE_YN_KEY = "CONSERVATION_SIGNIFICANCE_YN"
    val SCIENTIFIC_SIGNIFICANCE_KEY = "SCIENTIFIC_SIGNIFICANCE"
    val RFA_PRIORITY_CODE_KEY = "RFA_PRIORITY_CODE"
    val CULTURAL_SIGNIFICANCE_KEY = "CULTURAL_SIGNIFICANCE"
    val UNCOMMON_KEY = "UNCOMMON"
    val PRIMITIVE_STATUS_KEY = "PRIMITIVE_STATUS"
    val SPECIES_SENSITIVITY_KEY = "SPECIES_SENSITIVITY"
    val RESERVATION_STATUS_KEY = "RESERVATION_STATUS"
    val INTRODUCED_WATCH_LIST_KEY = "INTRODUCED_WATCH_LIST"
    val NATIVE_WATCH_LIST_KEY = "NATIVE_WATCH_LIST"
    val THREATENED_YN_KEY = "THREATENED_YN"
    val CFEV_YN_KEY = "CFEV_YN"
    val FORESTRY_YN_KEY = "FORESTRY_YN"
    val CENSUS_YN_KEY = "CENSUS_YN"
    val DISCONTINUED_DATE_KEY = "DISCONTINUED_DATE"
    val SPECIES_OBSERVATION_ID_KEY = "SPECIES_OBSERVATION_ID"
    val OBSERVATION_TYPE_KEY = "OBSERVATION_TYPE"
    val FOREIGN_ID_KEY = "FOREIGN_ID"
    val PROJECT_CODE_KEY = "PROJECT_CODE"
    val ACTIVITY_ID_KEY = "ACTIVITY_ID"
    val EASTING_KEY = "EASTING"
    val NORTHING_KEY = "NORTHING"
    val POSITION_ACCURACY_KEY = "POSITION_ACCURACY"
    val MAPPING_METHOD_KEY = "MAPPING_METHOD"
    val OBSERVATION_DATE_KEY = "OBSERVATION_DATE"
    val DATE_ACCURACY_KEY = "DATE_ACCURACY"
    val SPECIES_OBSERVATION_NOTES_KEY = "SPECIES_OBSERVATION_NOTES"
    val OBSERVATION_STATE_KEY = "OBSERVATION_STATE"
    val ORIGINAL_SPECIES_NAME_KEY = "ORIGINAL_SPECIES_NAME"
    val CREATION_DATE_KEY = "CREATION_DATE"
    val REPORTED_DATE_KEY = "REPORTED_DATE"
    val BIRTH_DATE_ACCURACY_KEY = "BIRTH_DATE_ACCURACY"
    val HOST_SPECIES_KEY = "HOST_SPECIES"
    val COLLECTION_METHOD_KEY = "COLLECTION_METHOD"
    val OBSERVER_TEXT_KEY = "OBSERVER_TEXT"
    val REPORTER_TEXT_KEY = "REPORTER_TEXT"
    val REPRODUCTION_STATUS_KEY = "REPRODUCTION_STATUS"
    val INDIVIDUALS_COUNT_KEY = "INDIVIDUALS_COUNT"
    val INDIVIDUALS_COUNT_NOTES_KEY = "INDIVIDUALS_COUNT_NOTES"
    val INDIVIDUALS_COUNT_VARN_KEY = "INDIVIDUALS_COUNT_VARN"
    val COVERAGE_AREA_KEY = "COVERAGE_AREA"
    val COVERAGE_PCNT_KEY = "COVERAGE_PCNT"
    val AVERAGE_HEIGHT_KEY = "AVERAGE_HEIGHT"
    val ADULT_PCNT_KEY = "ADULT_PCNT"
    val JUVENILE_PCNT_KEY = "JUVENILE_PCNT"
    val RECRUITMENT_YN_KEY = "RECRUITMENT_YN"
    val AREA_OCCUPIED_NOTES_KEY = "AREA_OCCUPIED_NOTES"
    val DISTURBANCE_TYPE_KEY = "DISTURBANCE_TYPE"
    val DISTURBANCE_DATE_KEY = "DISTURBANCE_DATE"
    val DISTURBANCE_NOTES_KEY = "DISTURBANCE_NOTES"
    val CLOSEST_FEATURE_KEY = "CLOSEST_FEATURE"
    val ALTITUDE_KEY = "ALTITUDE"
    val THREAT_NOTES_KEY = "THREAT_NOTES"
    val LANDFORM_KEY = "LANDFORM"
    val ASPECT_KEY = "ASPECT"
    val TASVEG_COMMUNITY_KEY = "TASVEG_COMMUNITY"
    val VEG_NOTES_KEY = "VEG_NOTES"
    val GEOLOGY_KEY = "GEOLOGY"
    val SUBSTRATE_TYPE_KEY = "SUBSTRATE_TYPE"
    val ROCK_PCNT_KEY = "ROCK_PCNT"
    val BARE_GROUND_PCNT_KEY = "BARE_GROUND_PCNT"
    val SHADING_PCNT_KEY = "SHADING_PCNT"
    val HABITAT_EXTENT_AREA_KEY = "HABITAT_EXTENT_AREA"
    val LAND_USE_KEY = "LAND_USE"
    val SLOPE_KEY = "SLOPE"
    val DRAINAGE_KEY = "DRAINAGE"
    val DATE_SUBMITTED_KEY = "DATE_SUBMITTED"
    val DATE_APPROVED_KEY = "DATE_APPROVED"
    val LAST_UPDATE_USER_ID_KEY = "LAST_UPDATE_USER_ID"
    val LAST_UPDATE_DATE_TIME_KEY = "LAST_UPDATE_DATE_TIME"

    // Associated darwin core keys
    val footprintWKT_KEY = "footprintWKT"
    val footprintSRS_KEY = "footprintSRS"
    val kingdom_KEY = "kingdom"
    val phylum_KEY = "phylum"
    val class_KEY = "classs"
    val order_KEY = "order"
    val family_KEY = "family"
    val genus_KEY = "genus"
    val taxonID_KEY = "taxonID"
    val scientificName_KEY = "scientificName"
    val taxonRank_KEY = "taxonRank"
    val infraspecificEpithet_KEY = "infraspecificEpithet"
    val nameAccordingTo_KEY = "nameAccordingTo"
    val namePublishedIn_KEY = "namePublishedIn"
    val taxonRemarks_KEY = "taxonRemarks"
    val vernacularName_KEY = "vernacularName"
    val catalogNumber_KEY = "catalogNumber"
    val basisOfRecord_KEY = "basisOfRecord"
    val locationID_KEY = "locationID"
    val eventID_KEY = "eventID"
    val verbatimCoordinates_KEY = "verbatimCoordinates"
    val verbatimCoordinateSystem_KEY = "verbatimCoordinateSystem"
    val verbatimSRS_KEY = "verbatimSRS"
    val decimalLatitude_KEY = "decimalLatitude"
    val decimalLongitude_KEY = "decimalLongitude"
    val geodeticDatum_KEY = "geodeticDatum"
    val coordinateUncertaintyInMeters_KEY = "coordinateUncertaintyInMeters"
    val georeferenceProtocol_KEY = "georeferenceProtocol"
    val eventDate_KEY = "eventDate"
    val eventRemarks_KEY = "eventRemarks"
    val occurrenceStatus_KEY = "occurrenceStatus"
    val previousIdentifications_KEY = "previousIdentifications"
    val associatedTaxa_KEY = "associatedTaxa"
    val samplingProtocol_KEY = "samplingProtocol"
    val recordedBy_KEY = "recordedBy"
    val reproductiveCondition_KEY = "reproductiveCondition"
    val individualCount_KEY = "individualCount"
    val occurrenceRemarks_KEY = "occurrenceRemarks"
    val locationRemarks_KEY = "locationRemarks"
    val verbatimElevation_KEY = "verbatimElevation"
    val habitat_KEY = "habitat"
    val modified_KEY = "modified"

    val darwinCoreMapping = Map(
        (GEOMETRY_KEY -> footprintWKT_KEY),
        (KINGDOM_KEY -> kingdom_KEY),
        (PHYLUM_KEY -> phylum_KEY),
        (CLASS1_KEY -> class_KEY),
        (ORDER1_KEY -> order_KEY),
        (FAMILY_KEY -> family_KEY),
        (GENUS_KEY -> genus_KEY),
        (SPECIES_CODE_KEY -> taxonID_KEY),
        (SPECIES_NAME_KEY -> scientificName_KEY),
        (INFRASPECIES_KEY -> taxonRank_KEY),
        (INFRASPECIES_RANK_KEY -> infraspecificEpithet_KEY),
        (SPECIES_AUTHORITY_KEY -> nameAccordingTo_KEY),
        (SPECIES_PUBLICATION_KEY -> namePublishedIn_KEY),
        (INFRASPECIES_PUBLICATION_KEY -> namePublishedIn_KEY),
        (INFRASPECIES_AUTHORITY_KEY -> nameAccordingTo_KEY),
        (SPECIES_NOTES_KEY -> taxonRemarks_KEY),
        (PREFERRED_COMMON_NAMES_KEY -> vernacularName_KEY),
        (SPECIES_OBSERVATION_ID_KEY -> catalogNumber_KEY),
        (OBSERVATION_TYPE_KEY -> basisOfRecord_KEY),
        (PROJECT_CODE_KEY -> locationID_KEY),
        (ACTIVITY_ID_KEY -> eventID_KEY),
        (EASTING_KEY -> verbatimCoordinates_KEY),
        (NORTHING_KEY -> verbatimCoordinates_KEY),
        (POSITION_ACCURACY_KEY -> coordinateUncertaintyInMeters_KEY),
        (MAPPING_METHOD_KEY -> georeferenceProtocol_KEY),
        (OBSERVATION_DATE_KEY -> eventDate_KEY),
        (SPECIES_OBSERVATION_NOTES_KEY -> eventRemarks_KEY),
        (OBSERVATION_STATE_KEY -> occurrenceStatus_KEY),
        (ORIGINAL_SPECIES_NAME_KEY -> previousIdentifications_KEY),
        (HOST_SPECIES_KEY -> associatedTaxa_KEY),
        (COLLECTION_METHOD_KEY -> samplingProtocol_KEY),
        (OBSERVER_TEXT_KEY -> recordedBy_KEY),
        (REPRODUCTION_STATUS_KEY -> reproductiveCondition_KEY),
        (INDIVIDUALS_COUNT_KEY -> individualCount_KEY),
        (INDIVIDUALS_COUNT_NOTES_KEY -> occurrenceRemarks_KEY),
        (AREA_OCCUPIED_NOTES_KEY -> locationRemarks_KEY),
        (DISTURBANCE_NOTES_KEY -> locationRemarks_KEY),
        (ALTITUDE_KEY -> verbatimElevation_KEY),
        (TASVEG_COMMUNITY_KEY -> habitat_KEY),
        (LAST_UPDATE_DATE_TIME_KEY -> modified_KEY))

    val epsg4326Name = "EPSG:4326"
    val epsg28355Name = "EPSG:28355"
    val eastingAndNorthingCoordSysName = "Easting and northing"

    val cachedPageNamePrefix = "tas_nva_page"

    def main(args: Array[String]) {
        //Use EasySSLProtocolFactory to get around problem with the self-signed certificate used by the web service.
        Protocol.unregisterProtocol("https");
        Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));

        val loader = new TasNvaDataLoader
        var dataResourceUid: String = null
        var cacheDirectoryPath: String = null
        var loadFromCache = false
        var startAtPage = 0
        var pageLimit = -1

        val parser = new OptionParser("Import Tasmanian Natural Values Atlas data") {
            arg("<data-resource-uid>", "the data resource to import", { v: String => dataResourceUid = v })
            arg("<cache-directory-path>", "the location of the cache", { v: String => cacheDirectoryPath = v })
            booleanOpt("c", "loadFromCache", "load previously cached data instead of data from the web service", { v: Boolean => loadFromCache = v })
            opt("p", "startAtPage", "Start loading from the specified page index", { v: String => startAtPage = v.toInt })
            opt("l", "pageLimit", "Load up to but excluding the specified page number. For testing purposes only", { v: String => pageLimit = v.toInt })
        }

        if (parser.parse(args)) {
            loader.load(dataResourceUid, cacheDirectoryPath, loadFromCache, startAtPage, pageLimit)
        }
    }
}

class TasNvaDataLoader extends DataLoader {
    //For parsing GML
    val gmlConfiguration = new GMLConfiguration
    val gmlParser = new Parser(gmlConfiguration)

    //for converting easting and northing values into lat/long.
    val wgs84crs = CRS.decode(TasNvaDataLoader.epsg4326Name)
    val gda94MGAZone55crs = CRS.decode(TasNvaDataLoader.epsg28355Name)
    val transformOp = new DefaultCoordinateOperationFactory().createOperation(gda94MGAZone55crs, wgs84crs)

    def load(dataResourceUid: String, cacheDirectoryPath: String, loadFromCache: Boolean, startAtPage: Int, pageLimit: Int) {
        val (protocol, urls, uniqueTerms, params, customParams) = retrieveConnectionParameters(dataResourceUid)
        val username = customParams("username")
        val password = customParams("password")
        val pageSize = customParams("pagesize").toInt

        val urlTemplate = params("url")
        var processedRecords = 0

        if (loadFromCache) {
            // re-load previously cached data
            var cacheDirectory = new File(cacheDirectoryPath)
            for (cacheFileName <- cacheDirectory.list()) {
                val cacheFile = new File(cacheDirectoryPath, cacheFileName)
                val xml = XML.loadString(FileUtils.readFileToString(cacheFile, "UTF-8"))
                val recordNodes = (xml \\ TasNvaDataLoader.OBSERVATION_KEY)

                for (recordNode <- recordNodes) {
                    val mappedValues = processRecord(recordNode)
                    val fr = FullRecordMapper.createFullRecord("", mappedValues, Versions.RAW)
                    val uniqueTermsValues = uniqueTerms.map(t => mappedValues.getOrElse(t, ""))
                    load(dataResourceUid, fr, uniqueTermsValues)
                    processedRecords += 1
                }
            }
        } else {
            // load data using the web service
            var moreRecords = true
            var pageNumber = startAtPage
            var startIndex = pageNumber * pageSize

          while (moreRecords && (pageLimit == -1 || startIndex < pageSize * pageLimit)) {
                println("Loading page " + pageNumber)

                try {
                    val xml = XML.loadString(retrieveDataFromWebService(username, password, urlTemplate, pageSize, startIndex))
                    val recordNodes = (xml \\ TasNvaDataLoader.OBSERVATION_KEY)

                    if (recordNodes.isEmpty) {
                        moreRecords = false
                    }

                    for (recordNode <- recordNodes) {
                        val mappedValues = processRecord(recordNode)
                        val fr = FullRecordMapper.createFullRecord("", mappedValues, Versions.RAW)
                        val uniqueTermsValues = uniqueTerms.map(t => mappedValues.getOrElse(t, ""))
                        load(dataResourceUid, fr, uniqueTermsValues)

                        writePageToCache(cacheDirectoryPath, xml.toString(), pageNumber)
                        processedRecords += 1
                    }
                } catch {
                    case ex: Throwable => println("ERROR: page " + pageNumber.toString() + " failed to load:"); ex.printStackTrace()
                }

                startIndex += pageSize
                pageNumber += 1
            }
        }

        println(processedRecords + " records processed")
    }

    def retrieveDataFromWebService(username: String, password: String, urlTemplate: String, numRecords: Int, startIndex: Int): String = {
        var url = MessageFormat.format(urlTemplate, numRecords.toString(), startIndex.toString())
        println(url)

        var dataXML: String = null

        val credentials = new UsernamePasswordCredentials(username, password)
        val httpClient = new HttpClient()
        httpClient.getState().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM), credentials);
        val get = new GetMethod(url)
        try {
            val responseCode = httpClient.executeMethod(get)
            if (responseCode == 200) {
                dataXML = get.getResponseBodyAsString();
            } else {
                throw new Exception("Request failed (" + responseCode + ")")
            }
        } finally {
            get.releaseConnection()
        }

        dataXML
    }

    def writePageToCache(cacheDirectoryPath: String, xml: String, pageNumber: Int) = {
        val fileName = TasNvaDataLoader.cachedPageNamePrefix + pageNumber.toString() + ".xml"
        val file = new File(cacheDirectoryPath, fileName)
        FileUtils.writeStringToFile(file, xml, "UTF-8")
    }

    def processRecord(recordNode: Node): Map[String, String] = {
        var mappedValues = Map[String, String]()

        recordNode.child.foreach(node => (mappedValues = addValue(node, mappedValues)))

        // convert easting, northing into WGS84 decimal lat long
        if (mappedValues.contains(TasNvaDataLoader.verbatimCoordinates_KEY)) {
            val arrayEastingNorthing = (mappedValues(TasNvaDataLoader.verbatimCoordinates_KEY)).split(";")
            val easting = arrayEastingNorthing(0).toDouble
            val northing = arrayEastingNorthing(1).toDouble
            val (lat, long) = convertEastingNorthingToLatLong(easting, northing)

            mappedValues = mappedValues + ((TasNvaDataLoader.decimalLatitude_KEY) -> lat.toString())
            mappedValues = mappedValues + ((TasNvaDataLoader.decimalLongitude_KEY) -> long.toString())
            mappedValues = mappedValues + ((TasNvaDataLoader.geodeticDatum_KEY) -> TasNvaDataLoader.epsg4326Name)
            // update verbatim coordinates to place comma instead of semicolon between values
            mappedValues = mappedValues + ((TasNvaDataLoader.verbatimCoordinates_KEY) -> (easting.toString() + ", " + northing.toString()))
            // write verbatimCoordinateSystem value. This is always "easting and northing"
            mappedValues = mappedValues + ((TasNvaDataLoader.verbatimCoordinateSystem_KEY) -> TasNvaDataLoader.eastingAndNorthingCoordSysName)
            // write verbatim srs value. This is always EPSG:28355
            mappedValues = mappedValues + ((TasNvaDataLoader.verbatimSRS_KEY) -> TasNvaDataLoader.epsg28355Name)
        }

        // write WKT for SRS used for footprintWKT. This is always EPSG:28355
        if (mappedValues.contains(TasNvaDataLoader.footprintWKT_KEY)) {
            mappedValues = mappedValues + ((TasNvaDataLoader.footprintSRS_KEY) -> gda94MGAZone55crs.toWKT())
        }

        mappedValues
    }

    def addValue(node: Node, map: Map[String, String]): Map[String, String] = {
        // Convert geometry gml into wkt
        if (node.label == TasNvaDataLoader.GEOMETRY_KEY) {
            map + ((TasNvaDataLoader.footprintWKT_KEY) -> convertGMLToWKT(node.child(0).toString()))
        } else if (TasNvaDataLoader.darwinCoreMapping.contains(node.label)) {
            val dwcKey = TasNvaDataLoader.darwinCoreMapping(node.label)

            if (map.contains(dwcKey)) {
                map + (dwcKey -> (map(dwcKey) + ";" + node.text))
            } else {
                map + (dwcKey -> node.text)
            }

        } else {
            map + (node.label -> node.text)
        }
    }

    def convertGMLToWKT(gml: String): String = {
        val inputStream = IOUtils.toInputStream(gml)
        val featureCollection = gmlParser.parse(inputStream)
        featureCollection.toString()
    }

    def convertEastingNorthingToLatLong(easting: Double, northing: Double): (Double, Double) = {
        val eastingNorthing = new GeneralDirectPosition(easting, northing)
        val latLong = transformOp.getMathTransform().transform(eastingNorthing, eastingNorthing)
        (latLong.getOrdinate(0), latLong.getOrdinate(1))
    }

}