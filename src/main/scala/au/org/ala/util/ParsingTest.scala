package au.org.ala.util
import au.org.ala.biocache._
import java.util.Date

object ParsingTest {
  def main(args : Array[String]) : Unit = {
      val values = List("32132", "Aus bus", "12.43", "100.0", "Australia", "0.0001", "100", "1999")
      val values1 = List("32132", "Aus bus", "41 05 54.03S", "41 05 54.03N", "Australia", "1999-01-01")
      val values2 = List("32132", "Aus bus", "wipper snapper", "12.43", "121d 10' 34\" W", "Australia", "12 Hush Street")
      val verbatimSRS = List("EPSG:4326", "WGS84", "NAD27", "Campo Inchauspe", "European 1950", "Clarke 1866")
      val geodeticDatum = List("EPSG:4326", "WGS84", "NAD27", "Campo Inchauspe", "European 1950", "Clarke 1866")
  }
}

object Parser {
    
    import au.org.ala.util.StringHelper._
    
    def main(args:Array[String]){
        
        //need a special case for the first column
        //if its an int, we can assume its a recordID
        //if its a string, could sci name or common name
        //if its a float, and column 2 is a float, assume lat/long

    }
    
    
    def guessColumnHeaders(values:List[String]) : List[String] = {
        //val values1 = List("32132", "Macropus rufus", "41 05 54.03S", "41 05 54.03N", "Australia", "red kangaroo", "WGS84", "1999-01-01")
        
        if(values.size >= 2){
           parseHead(values(0), values(1))
        }
        
        val matchedTerms = parse(values)
        
        println(matchedTerms)
        matchedTerms
    }
    
    def parseHead(column1:String, column2:String) : Option[(String,String)] = column1 match {
        case it if (column1.isLatitude && column2.isLongitude) => Some("decimalLatitude", "decimalLongitude")
        case it if (column1.isLatitude && column2.isLongitude) => Some("decimalLongitude", "decimalLatitude")
        case it if it.isInt => Some("recordNumber", "")
        case it if it.startsWith("urn") => Some("occurrenceID", "")
        case it if it.startsWith("http://") => Some("occurrenceID","")
        case _ => None 
    }
    
	def parse(values:List[String]) : List[String] = {
	    values.map(value => {
	        value match {
	          case DateExtractor(value) => "eventDate"
	          case DecimalLatitudeExtractor(value) =>  "decimalLatitude"
	          case DecimalLongitudeExtractor(value) =>  "decimalLongitude"
	          case VerbatimLatitudeExtractor(value) =>  "verbatimLatitude"
	          case VerbatimLongitudeExtractor(value) =>  "verbatimLongitude"
	          case GeodeticDatumExtractor(value) =>  "geodeticDatum"
	          case CountryExtractor(value) =>  "country"
	          case CoordinateUncertaintyExtractor(value) =>  "coordinateUncertaintyInMeters"
	          case ScientificNameExtractor(value) =>  "scientificName"
	          case CommonNameExtractor(value) =>  "commonName"
	          case _ => ""
	        }
	    })
	}
}

object ScientificNameExtractor {
    
  def unapply(str:String) : Option[String] = {
    val nsr = Config.nameIndex.searchForRecord(str, null, null)
    if(nsr!=null && nsr.getLsid!=null){
       Some(nsr.getLsid) 
    } else {
       None
    }
  }
}

object CommonNameExtractor {
  def unapply(str:String) : Option[String] = {
    val nsr = Config.nameIndex.searchForCommonName(str)
    if(nsr!=null && nsr.getLsid!=null){
       Some(nsr.getLsid) 
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

object GeodeticDatumExtractor {
    def unapply(str:String) : Option[Term] = GeodeticDatum.matchTerm(str)
}

object CountryExtractor {
  def unapply(str:String) : Option[Term] = Countries.matchTerm(str)
}

object CoordinateUncertaintyExtractor {
  def unapply(str:String) : Option[Float] = DistanceRangeParser.parse(str)
}
