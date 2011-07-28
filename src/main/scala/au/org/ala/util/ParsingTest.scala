package au.org.ala.util
import au.org.ala.biocache._
import java.util.Date
import java.io.File
import au.org.ala.util._
import scala.collection.mutable.ListBuffer
import java.io.Reader
import au.com.bytecode.opencsv.CSVReader

object ParsingTest {
  def main(args : Array[String]) : Unit = {
      val values = List("32132", "Aus bus", "12.43", "100.0", "Australia", "0.0001", "100", "1999")
      val values1 = List("32132", "Aus bus", "41 05 54.03S", "41 05 54.03N", "Australia", "1999-01-01")
      val values2 = List("32132", "Aus bus", "wipper snapper", "12.43", "121d 10' 34\" W", "Australia", "12 Hush Street")
      val verbatimSRS = List("EPSG:4326", "WGS84", "NAD27", "Campo Inchauspe", "European 1950", "Clarke 1866")
      val geodeticDatum = List("EPSG:4326", "WGS84", "NAD27", "Campo Inchauspe", "European 1950", "Clarke 1866")
  }
}

case class ProcessedValue(name:String, raw:String, processed:String)
case class ParsedRecord(values:Array[ProcessedValue], assertions:Array[QualityAssertion])

//object HeaderTest {
//    
//    def main(args:Array[String]){
//        
//        val hdr = """basisOfRecord, occurrenceID, catalogNumber, recordedBy, individualCount, sex, lifeStage, year, month, day, higherGeography, country, countryCode, stateProvince, locality, decimalLatitude, decimalLongitude, scientificName, acceptedNameUsage, higherClassification, kingdom, phylum, _class, order, family, genus, subgenus, specificEpithet, infraspecificEpithet, taxonRank, verbatimTaxonRank, scientificNameAuthorship"""
//        val hdrs = hdr.split(",").map(x => x.trim)
//        val output = Parser.guessColumnHeaders(hdrs)
//        for(s <- output){
//            println(s)
//        }
//    }
//}


object Parser {
    
    import au.org.ala.util.StringHelper._
    import FileHelper._
    
    def main(args:Array[String]){
        
        //need a special case for the first column
        //if its an int, we can assume its a recordID
        //if its a string, could sci name or common name
        //if its a float, and column 2 is a float, assume lat/long
        processCSV(args(0))
    }

    def processColumnHeaders(list:Array[String]) : Array[String] ={
        //are these darwin core terms?
        val matchedCount = DwC.retrieveCanonicalsOrNothing(list.toList).count(x => x != "")
        //if not try and match them
        if(matchedCount>2){
            val t = DwC.retrieveCanonicals(list.toList)
            println("Matched terms: " + t)
            t.toArray
        } else {
            val t = guessColumnHeaders(list)
            println("Guessed terms: " + t)
            t
        }
    }
    
    def processLine(hdrs:Array[String], values:Array[String]) : ParsedRecord = { 
        val tuples = (hdrs zip values).toMap
        val raw = FullRecordMapper.createFullRecord("", tuples, Versions.RAW)
        //println(raw.classification.scientificName)
        val p  = new RecordProcessor
        val (processed, assertions) = p.processRecord(raw)
        //what values are processed???
        
        val rawAndProcessed = raw.objectArray zip processed.objectArray
        val listBuff = new ListBuffer[ProcessedValue]
        for((rawPoso,procPoso) <- rawAndProcessed){
            
            if(!rawPoso.isInstanceOf[ContextualLayers] && !rawPoso.isInstanceOf[EnvironmentalLayers] ){
                rawPoso.propertyNames.foreach (name => {
                    val rawValue = rawPoso.getProperty(name)
                    val procValue = procPoso.getProperty(name)
                    if( !rawValue.isEmpty || !procValue.isEmpty){
                        val term = ProcessedValue(name, rawValue.getOrElse(""), procValue.getOrElse(""))
                        listBuff += term
                    }
                })
            }
        }
        //assertions
//        assertions.values.foreach(assertionGroup => {
//            assertionGroup.foreach(assertion => {
//            	val term = ProcessedValue(assertion.name, "", assertion.comment)
//            	listBuff += term
//            })
//        })
        ParsedRecord( listBuff.toList.toArray, assertions.values.flatten.toArray)
    }
    
    def processCSV(filepath:String){
        (new File(filepath)).readAsCSV(',', '"', processColumnHeaders, (hdrs, values) =>{
            val result = processLine(hdrs,values)
            CommandLineTool.printTable(result.values.map(r => {
                Map("name"-> r.name, "raw"-> r.raw, "processed"-> r.processed)
            }).toList
        )})
    }

    def processReader(reader:java.io.Reader){
        val csvReader =  new CSVReader(reader, ',', '"')
        val rawColumnHdrs = csvReader.readNext
        val columnHdrs = processColumnHeaders(rawColumnHdrs)
        var currentLine = csvReader.readNext
        while(currentLine != null){
            val result = processLine(columnHdrs, currentLine)
            currentLine = csvReader.readNext
        }
    }
    
    def guessColumnHeaders(values:Array[String]) : Array[String] = {
        //assume we have darwin core terms
        val matchedDwc = DwC.retrieveCanonicals(values.toList)
        val nofMatched = matchedDwc.filter(x => x.size > 0).size
        if(nofMatched < 3){
	        parse(values)
        } else {
            matchedDwc.toArray
        }
    }
    
    def parseHead(column1:String, column2:String) : Option[(String,String)] = column1 match {
        case it if (column1.isLatitude && column2.isLongitude) => Some("decimalLatitude", "decimalLongitude")
        case it if (column1.isLatitude && column2.isLongitude) => Some("decimalLongitude", "decimalLatitude")
        case it if it.isInt => Some("recordNumber", "")
        case it if it.startsWith("urn") => Some("occurrenceID", "")
        case it if it.startsWith("http://") => Some("occurrenceID","")
        case _ => None 
    }
    
	def parse(values:Array[String]) : Array[String] = {
	    values.map(value => {
	        value match {
	          case BasisOfRecordExtractor(value) => "basisOfRecord"
	          case DateExtractor(value) => "eventDate"
	          case DecimalLatitudeExtractor(value) =>  "decimalLatitude"
	          case DecimalLongitudeExtractor(value) =>  "decimalLongitude"
	          case VerbatimLatitudeExtractor(value) =>  "verbatimLatitude"
	          case VerbatimLongitudeExtractor(value) =>  "verbatimLongitude"
	          case GeodeticDatumExtractor(value) =>  "geodeticDatum"
	          case CountryExtractor(value) =>  "country"
	          case CoordinateUncertaintyExtractor(value) =>  "coordinateUncertaintyInMeters"
	          case ScientificNameExtractor(value) =>  "scientificName"
	          case CommonNameExtractor(value) =>  "vernacularName"
	          case _ => ""
	        }
	    })
	}
}

object ScientificNameExtractor {
    
  def unapply(str:String) : Option[String] = {
    if(str !=""){
	    val nsr = Config.nameIndex.searchForRecord(str, null, null)
	    if(nsr!=null && nsr.getLsid!=null){
	       Some(nsr.getLsid) 
	    } else {
	       None
	    }
    } else{
        None
    }
  }
}

object CommonNameExtractor {
  def unapply(str:String) : Option[String] = {
    if(str !=""){  
	    val nsr = Config.nameIndex.searchForCommonName(str)
	    if(nsr!=null && nsr.getLsid!=null){
	       Some(nsr.getLsid) 
	    } else {
	       None
	    }
    } else {
        None
    }
  }
}

object DateExtractor {
  def unapply(str:String) : Option[EventDate] = {
      val parsed = DateParser.parseDate(str)
      parsed match {
          case it if !it.isEmpty && DateParser.isValid(it.get) => parsed
          case _ => None
      }
  }
}

object DecimalLatitudeExtractor {
  def unapply(str:String) : Option[Float] = {
    try{
        val latitude = str.toFloat
        if(latitude <=90.0 && latitude>= -90.0){
            Some(latitude)
        } else {
            None
        }
    } catch {
        case e:Exception => None
    }
  }
}

object DecimalLongitudeExtractor {
  def unapply(str:String) : Option[Float] = {
    try{
        val longitude = str.toFloat
        if(longitude <=180.0 && longitude>= -180.0){
            Some(longitude)
        } else {
            None
        }
    } catch {
        case e:Exception => None
    }
  }
}

object VerbatimLatitudeExtractor {
  def unapply(str:String) : Option[Float] = VerbatimLatLongParser.parse(str)
}

object VerbatimLongitudeExtractor {
    def unapply(str:String) : Option[Float] = VerbatimLatLongParser.parse(str)
}

object BasisOfRecordExtractor {
    def unapply(str:String) : Option[Term] = BasisOfRecord.matchTerm(str)
}

object GeodeticDatumExtractor {
    def unapply(str:String) : Option[Term] = GeodeticDatum.matchTerm(str)
}

object CountryExtractor {
  def unapply(str:String) : Option[Term] = Countries.matchTerm(str)
}

object CoordinateUncertaintyExtractor {
  def unapply(str:String) : Option[Float] = DistanceRangeParser.parse(str)
}
