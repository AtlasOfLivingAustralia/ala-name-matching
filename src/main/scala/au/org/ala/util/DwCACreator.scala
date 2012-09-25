package au.org.ala.util
import java.util.zip.ZipOutputStream
import java.io.{FileOutputStream, OutputStreamWriter}
import java.util.zip.ZipEntry
import au.com.bytecode.opencsv.CSVWriter
import scala.io.Source
import org.apache.commons.io.FileUtils
import scala.util.parsing.json.JSON

object DwCACreator {

    def main(args: Array[String]): Unit = {

        var resourceUid = ""
        var directory =""

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
                case e:Exception => e.printStackTrace()
              }
            } else {
              dwcc.create(directory, resourceUid)
            }
        }
    }
    
    def getDataResourceUids : Seq[String] = {
      val url = "http://biocache.ala.org.au/ws/occurrences/search?q=*:*&facets=data_resource_uid&pageSize=0&flimit=10000"
      val jsonString = Source.fromURL(url).getLines.mkString
      val json = JSON.parseFull(jsonString).get.asInstanceOf[Map[String, String]]
      val results = json.get("facetResults").get.asInstanceOf[List[Map[String, String]]].head.get("fieldResult").get.asInstanceOf[List[Map[String, String]]]
      results.map(facet => {
        val uid = facet.get("label").get
        println(uid)
        uid
      })
    }    
}

/**
 * TODO support for dwc fields in collectory metadata. When not available use the default fields
 */
class DwCACreator {
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
        val zipFile = new java.io.File(directory+System.getProperty("file.separator")+dataResource + System.getProperty("file.separator")+dataResource +"_ror_dwca.zip")
        FileUtils.forceMkdir(zipFile.getParentFile)
        val zop = new ZipOutputStream(new FileOutputStream(zipFile))
        addEML(zop, dataResource)
        addMeta(zop)
        addCSV(zop, dataResource)
        zop.close
    }
    
    def addEML(zop:ZipOutputStream, dr:String) ={
        //query from the collectory to get the EML file
        zop.putNextEntry(new ZipEntry("eml.xml"))
        
        val content=Source.fromURL("http://collections.ala.org.au/eml/"+dr).mkString
        zop.write(content.getBytes)
        zop.flush
        zop.closeEntry
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
        ExportUtil.export(new CSVWriter(new OutputStreamWriter(zop)),"occ", defaultFields, List(), Some(compulsoryFields), startUuid, endUuid, Integer.MAX_VALUE)
        zop.flush
        zop.closeEntry
    }
}

/*
 * 
 * <?xml version="1.0"?>
<archive xmlns="http://rs.tdwg.org/dwc/text/" metadata="eml.xml">
        <core encoding="UTF-8" linesTerminatedBy="\r\n" fieldsTerminatedBy="," fieldsEnclosedBy="&quot;" ignoreHeaderLines="0" rowType="http://rs.tdwg.org/dwc/terms/Occurrence">
                <files>
                        <location>raw_occurrence_record.csv</location>
                </files>
                <id index="0"/>
                <field  index="1" term="http://rs.tdwg.org/dwc/terms/catalogNumber"/>
                <field  index="2" term="http://rs.tdwg.org/dwc/terms/collectionCode"/>
                <field  index="3" term="http://rs.tdwg.org/dwc/terms/institutionCode"/>
                <field  index="4" term="http://rs.tdwg.org/dwc/terms/scientificName"/>
                <field  index="5" term="http://rs.tdwg.org/dwc/terms/recordedBy"/>
                <field  index="6" term="http://rs.tdwg.org/dwc/terms/taxonRank"/>
                <field  index="7" term="http://rs.tdwg.org/dwc/terms/kingdom"/>
                <field  index="8" term="http://rs.tdwg.org/dwc/terms/phylum"/>
                <field  index="9" term="http://rs.tdwg.org/dwc/terms/class"/>
                <field  index="10" term="http://rs.tdwg.org/dwc/terms/order"/>
                <field  index="11" term="http://rs.tdwg.org/dwc/terms/family"/>
                <field  index="12" term="http://rs.tdwg.org/dwc/terms/genus"/>
                <field  index="13" term="http://rs.tdwg.org/dwc/terms/specificEpithet"/>
                <field  index="14" term="http://rs.tdwg.org/dwc/terms/infraspecificEpithet"/>
                <field  index="15" term="http://rs.tdwg.org/dwc/terms/decimalLatitude"/>
                <field  index="16" term="http://rs.tdwg.org/dwc/terms/decimalLongitude"/>
                <field  index="17" term="http://rs.tdwg.org/dwc/terms/coordinatePrecision"/>
                <field  index="18" term="http://rs.tdwg.org/dwc/terms/maximumElevationInMeters"/>
                <field  index="19" term="http://rs.tdwg.org/dwc/terms/minimumElevationInMeters"/>
                <field  index="20" term="http://rs.tdwg.org/dwc/terms/minimumDepthInMeters"/>
                <field  index="21" term="http://rs.tdwg.org/dwc/terms/maximumDepthInMeters"/>
                <field  index="22" term="http://rs.tdwg.org/dwc/terms/continent"/>
                <field  index="23" term="http://rs.tdwg.org/dwc/terms/country"/>
                <field  index="24" term="http://rs.tdwg.org/dwc/terms/stateProvince"/>
                <field  index="25" term="http://rs.tdwg.org/dwc/terms/county"/>
                <field  index="26" term="http://rs.tdwg.org/dwc/terms/locality"/>
                <field  index="27" term="http://rs.tdwg.org/dwc/terms/year"/>
                <field  index="28" term="http://rs.tdwg.org/dwc/terms/month"/>
                <field  index="29" term="http://rs.tdwg.org/dwc/terms/day"/>
                <field  index="30" term="http://rs.tdwg.org/dwc/terms/basisOfRecord"/>
                <field  index="31" term="http://rs.tdwg.org/dwc/terms/identifiedBy"/>
                <field  index="32" term="http://rs.tdwg.org/dwc/terms/dateIdentified"/>
                <field  index="33" term="http://purl.org/dc/terms/bibliographicCitation"/>
                <field  index="34" term="http://rs.tdwg.org/dwc/terms/occurrenceRemarks"/>
                <field  index="35" term="http://rs.tdwg.org/dwc/terms/locationRemarks"/>
                <field  index="36" term="http://rs.tdwg.org/dwc/terms/recordNumber"/>
                <field  index="37" term="http://rs.tdwg.org/dwc/terms/vernacularName"/>
                <field  index="38" term="http://rs.tdwg.org/dwc/terms/identificationQualifier"/>
                <field  index="39" term="http://rs.tdwg.org/dwc/terms/individualCount"/>
                <field  index="40" term="http://rs.tdwg.org/dwc/terms/eventID"/>
                <field  index="41" term="http://rs.tdwg.org/dwc/terms/geodeticDatum"/>
                <field  index="42" term="http://rs.tdwg.org/dwc/terms/eventTime"/>
                <field  index="43" term="http://rs.tdwg.org/dwc/terms/associatedSequences"/>
                <field  index="44" term="http://rs.tdwg.org/dwc/terms/eventDate"/>
        </core>
</archive>

 * 
 * 
 */
