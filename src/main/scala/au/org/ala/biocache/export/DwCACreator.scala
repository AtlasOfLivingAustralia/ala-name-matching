package au.org.ala.biocache.export

import java.util.zip.ZipOutputStream
import java.io.{FileOutputStream, OutputStreamWriter}
import java.util.zip.ZipEntry
import au.com.bytecode.opencsv.CSVWriter
import scala.io.Source
import org.apache.commons.io.FileUtils
import scala.util.parsing.json.JSON
import org.slf4j.LoggerFactory
import au.org.ala.biocache.Config
import au.org.ala.biocache.util.OptionParser
import util.matching.Regex

/**
 * Companion object for the DwCACreator class.
 */
object DwCACreator {

  val logger = LoggerFactory.getLogger("DwCACreator")

  def main(args: Array[String]): Unit = {

    var resourceUid = ""
    var directory = ""

    val parser = new OptionParser("Create Darwin Core Archive") {
      arg("<data resource UID>", "The UID of the data resource to load or 'all' to generate for all", {v: String => resourceUid = v})
      arg("<directory to dump>", "skip the download and use local file", {v:String => directory = v } )
    }
    if(parser.parse(args)){
      val dwcc = new DwCACreator
      if("all".equalsIgnoreCase(resourceUid)){
        try {
          getDataResourceUids.foreach( dwcc.create(directory, _) )
        } catch {
          case e:Exception => logger.error(e.getMessage(), e)
        }
      } else {
        dwcc.create(directory, resourceUid)
      }
    }
  }

  // pattern to extract a data resource uid from a filter query , because the label show i18n value
  val dataResourcePattern ="(?:[\"]*)?(?:[a-z_]*_uid:\")([a-z0-9]*)(?:[\"]*)?".r
  def getDataResourceUids : Seq[String] = {
    val url = Config.biocacheServiceUrl + "/occurrences/search?q=*:*&facets=data_resource_uid&pageSize=0&flimit=10000"
    val jsonString = Source.fromURL(url).getLines.mkString
    val json = JSON.parseFull(jsonString).get.asInstanceOf[Map[String, String]]
    val results = json.get("facetResults").get.asInstanceOf[List[Map[String, String]]].head.get("fieldResult").get.asInstanceOf[List[Map[String, String]]]
    results.map(facet => {
      val fq = facet.get("fq").get
      parseFq(fq)
    }).filterNot(_.equals("Unknown"))

  }
  def parseFq(fq: String): String = fq match {
    case dataResourcePattern(dr) => dr
    case _ => "Unknown"
  }
}

/**
 * Class for creating a Darwin Core Archive from data in the biocache.
 *
 * TODO support for dwc fields in registry metadata. When not available use the default fields.
 */
class DwCACreator {

  val logger = LoggerFactory.getLogger("DwCACreator")

  val defaultFields = List("uuid", "catalogNumber", "collectionCode", "institutionCode", "scientificName", "recordedBy",
      "taxonRank", "kingdom", "phylum", "class", "order", "family", "genus", "specificEpithet", "infraspecificEpithet",
      "decimalLatitude", "decimalLongitude", "coordinatePrecision", "coordinateUncertaintyInMeters", "maximumElevationInMeters", "minimumElevationInMeters",
      "minimumDepthInMeters", "maximumDepthInMeters", "continent", "country", "stateProvince", "county", "locality", "year", "month",
      "day", "basisOfRecord", "identifiedBy", "dateIdentified", "occurrenceRemarks", "locationRemarks", "recordNumber",
      "vernacularName", "identificationQualifier", "individualCount", "eventID", "geodeticDatum", "eventTime", "associatedSequences",
      "eventDate")
  //The compulsory mapping fields for GBIF. This indicates that the data resource name may need to be assigned at load time instead of processing
  val compulsoryFields = Map("catalogNumber"->"uuid", "collectionCode"->"dataResourceName.p", "institutionCode"->"dataResourceName.p")

  def create(directory:String, dataResource:String) {
    logger.info("Creating archive for " + dataResource)
    val zipFile = new java.io.File(directory+System.getProperty("file.separator")+dataResource + System.getProperty("file.separator")+dataResource +"_ror_dwca.zip")
    FileUtils.forceMkdir(zipFile.getParentFile)
    val zop = new ZipOutputStream(new FileOutputStream(zipFile))
    if(addEML(zop, dataResource)){
      addMeta(zop)
      addCSV(zop, dataResource)
      zop.close
    } else{
      //no EML implies that a DWCA should not be generated.
      zop.close()
      FileUtils.deleteQuietly(zipFile)
    }
  }

  def addEML(zop:ZipOutputStream, dr:String):Boolean ={
    //query from the collectory to get the EML file
    try {
      zop.putNextEntry(new ZipEntry("eml.xml"))
      val content = Source.fromURL(Config.registryUrl + "/eml/"+dr).mkString
      zop.write(content.getBytes)
      zop.flush
      zop.closeEntry
      true
    } catch{
      case e:Exception => e.printStackTrace();false
    }
  }

  def addMeta(zop:ZipOutputStream) ={
    zop.putNextEntry(new ZipEntry("meta.xml"))
    val metaXml = <archive xmlns="http://rs.tdwg.org/dwc/text/" metadata="eml.xml">
      <core encoding="UTF-8" linesTerminatedBy="\r\n" fieldsTerminatedBy="," fieldsEnclosedBy="&quot;" ignoreHeaderLines="0" rowType="http://rs.tdwg.org/dwc/terms/Occurrence">
      <files>
            <location>raw_occurrence_record.csv</location>
      </files>
            <id index="0"/>
            {defaultFields.tail.map(f =>   <field index={defaultFields.indexOf(f).toString} term={"http://rs.tdwg.org/dwc/terms/"+f}/>)}
      </core>
    </archive>
    //add the XML
    zop.write("""<?xml version="1.0"?>""".getBytes)
    zop.write("\n".getBytes)
    zop.write(metaXml.mkString("\n").getBytes)
    zop.flush
    zop.closeEntry
  }

  def addCSV(zop:ZipOutputStream, dr:String) ={
    zop.putNextEntry(new ZipEntry("raw_occurrence_record.csv"))
    val startUuid = dr+"|"
    val endUuid = startUuid+"~"
    ExportUtil.export(new CSVWriter(new OutputStreamWriter(zop)),"occ", defaultFields, List("uuid"),List("uuid"), Some(compulsoryFields), startUuid, endUuid, Integer.MAX_VALUE)
    zop.flush
    zop.closeEntry
  }
}
