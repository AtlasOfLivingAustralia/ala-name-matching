package au.org.ala.biocache

import reflect.BeanProperty
import au.org.ala.util.Stemmer
import scala.collection.JavaConversions
import scala.io.Source
import scala.util.parsing.json.JSON
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory

/** Case class that encapsulates a canonical form and variants. */
class Term (@BeanProperty val canonical:String, @BeanProperty rawVariants:Array[String]){
  val variants = rawVariants.map(v => v.toLowerCase.trim) ++ rawVariants.map(v => Stemmer.stem(v)) :+ Stemmer.stem(canonical)
}

/** Factory for terms */
object Term {
  def apply(canonical: String): Term = new Term(canonical, Array[String]())
  def apply(canonical: String, variant: String): Term = new Term(canonical, Array(variant))
  def apply(canonical: String, variants: String*): Term = new Term(canonical, Array(variants:_*))
  def apply(canonical: String, variants: Array[String]): Term = new Term(canonical, variants)
}

/**
 * A trait for a vocabulary. A vocabulary consists of a set
 * of Terms, each with string variants.
 */
trait Vocab {
  
  import JavaConversions._

  val all:Set[Term]

  val regexNorm = """[ \\"\\'\\.\\,\\-\\?]*"""
  
  def getStringList : java.util.List[String] = all.map(t => t.canonical).toList.sorted
  
  /**
   * Match a term. Matches canonical form or variants in array
   * @param string2Match
   * @return
   */
  def matchTerm(string2Match:String) : Option[Term] = {
    if(string2Match!=null){
      //strip whitespace & strip quotes and fullstops & uppercase
      val stringToUse = string2Match.replaceAll(regexNorm, "").toLowerCase
      val stemmed = Stemmer.stem(stringToUse)
      
      //println("string to use: " + stringToUse)
      all.foreach(term => {
        //println("matching to term " + term.canonical)
        if(term.canonical.equalsIgnoreCase(stringToUse))
          return Some(term)
        if(term.variants.contains(stringToUse) || term.variants.contains(stemmed)){
          return Some(term)
        }
      })
    }
    None
  }
  
  def retrieveCanonicals(terms:Seq[String]) = {
    terms.map(ch => {
        DwC.matchTerm(ch) match {
            case Some(term) => term.canonical
            case None => ch
        }
    })
  }

  def retrieveCanonicalsOrNothing(terms:Seq[String]) = {
    terms.map(ch => {
        DwC.matchTerm(ch) match {
            case Some(term) => term.canonical
            case None => ""
        }
    })
  }
  
  def loadVocabFromVerticalFile(filePath:String) : Set[Term] = {
    val map = scala.io.Source.fromURL(getClass.getResource(filePath), "utf-8").getLines.toList.map({ row =>
        val values = row.split("\t")
        val variant = values.head.replaceAll(regexNorm,"").toLowerCase
        val canonical = values.last
        (variant, canonical)
    }).toMap

    val grouped = map.groupBy({ case(k,v) => v })

    grouped.map({ case(canonical, valueMap) => {
       val variants = valueMap.keys
       new Term(canonical, variants.toArray)
    }}).toSet
  }

  def loadVocabFromFile(filePath:String) : Set[Term] = {
    scala.io.Source.fromURL(getClass.getResource(filePath), "utf-8").getLines.toList.map({ row =>
        val values = row.split("\t")
        val variants = values.map(x => x.replaceAll("""[ \\"\\'\\.\\,\\-\\?]*""","").toLowerCase)
        new Term(values.head, variants)
    }).toSet
  }

  /**
   * Retrieve all the terms defined in this vocab.
   * @return
   */
  def retrieveAll : Set[Term] = {
    val methods = this.getClass.getMethods
    (for{
      method<-methods
      if(method.getReturnType.getName == "au.org.ala.biocache.Term")
    } yield (method.invoke(this).asInstanceOf[Term])).toSet[Term]
  }
}

case class LatLng(latitude:Float, longitude:Float)

case class BBox(north:Float, east:Float, south:Float, west:Float){

  def containsPoint(latitude:Float, longitude:Float) = {

    if(east < west) {
      //ITS CROSSED THE DATE LINE
      north >= latitude && south <= latitude && ( (longitude >= -180 && longitude <= east)  || ( longitude >= west && longitude <= 180) )
    } else {
      north >= latitude && south <= latitude && east >= longitude && west <= longitude
    }
  }
}

trait CentrePoints {

  val north = 'N'
  val south = 'S'
  val east = 'E'
  val west = 'W'

  protected val map:Map[String, (LatLng, BBox)]
  protected val vocab:Vocab

  def matchName(str:String) = map.get(str.toLowerCase)

  /**
   * Returns true if the supplied coordinates are the centre point for the supplied
   * state or territory
   */
  def coordinatesMatchCentre(state:String, decimalLatitude:String, decimalLongitude:String) : Boolean = {

    val matchedState = vocab.matchTerm(state)
    if(!matchedState.isEmpty && decimalLatitude != null && decimalLongitude != null){

      val latlngBBox = map.get(matchedState.get.canonical.toLowerCase)
      if(!latlngBBox.isEmpty){

        val (latlng, bbox) = latlngBBox.get

        //how many decimal places are the supplied coordinates
        try {
          //convert supplied values to float
          val latitude = decimalLatitude.toFloat
          val longitude = decimalLongitude.toFloat

          val latDecPlaces = noOfDecimalPlace(latitude)
          val longDecPlaces = noOfDecimalPlace(longitude)

          //approximate the centre points appropriately
          val approximatedLat = round(latlng.latitude,latDecPlaces)
          val approximatedLong = round(latlng.longitude,longDecPlaces)

          //compare approximated centre point with supplied coordinates
          approximatedLat == latitude && approximatedLong == longitude

        } catch {
          case e:NumberFormatException => false
        }
      } else {
        false
      }
    } else {
      false
    }
  }

  def getHemispheresForPoint(lat:Double, lng:Double) : (Char,Char) = (
    if(lat >= 0) north else south,
    if(lng >= 0) east else west
  )

  def getHemispheres(region:String) : Option[Set[Char]] = {
    val matchedRegion = vocab.matchTerm(region)
    map.get(matchedRegion.get.canonical.toLowerCase) match {
      case Some((latlng, bbox)) => {
        Some(Set(
          if(bbox.north >= 0) north else south,
          if(bbox.south >  0) north else south,
          if(bbox.east  >= 0) east else west,
          if(bbox.west >  0) east else west
        ))
      }
      case _ => None
    }
  }

  /**
   * Round to the supplied no of decimal places.
   */
  def round(number:Float, decimalPlaces:Int) : Float = {
    if(decimalPlaces>0){
      var x = 1
      for (i <- 0 until decimalPlaces) x = x * 10
      (((number * x).toInt).toFloat) / x
    } else {
      number.round
    }
  }

  def noOfDecimalPlace(number:Float) : Int = {
    val numberString = number.toString
    val decimalPointLoc = numberString.indexOf(".")
    if(decimalPointLoc<0) {
      0
    } else {
       numberString.substring(decimalPointLoc+1).length
    }
  }

  def loadFromFile(filePath:String): Map[String, (LatLng,BBox)] = {
    scala.io.Source.fromURL(getClass.getResource(filePath), "utf-8").getLines.toList.map({ row =>
        val values = row.split("\t")
        val name = values.head.toLowerCase
        val coordinates = values.tail.map(x => x.toFloat)
        name -> (
          LatLng(coordinates(0), coordinates(1)),
          //12.630618	-69.8644638	12.406093	-70.0701141
          BBox(coordinates(2),coordinates(3),coordinates(4),coordinates(5))
        )
    }).toMap
  }
}

trait ValueMap {

  var map:Map[String,String] = _

  def loadFromFile(filePath:String): Map[String, String] = {
    scala.io.Source.fromURL(getClass.getResource(filePath), "utf-8").getLines.toList.map({ row =>
      val values = row.split("\t")
      values(0) -> values(1)
    }).toMap
  }
}


object StateProvinceToCountry extends ValueMap {
  map = loadFromFile("/stateProvince2Countries.txt")
}

/**
 * Matching of coordinates for centre points for states.
 * This is for detecting auto-generated coordinates at very low accuracy.
 */
object StateProvinceCentrePoints extends CentrePoints {
  val map = loadFromFile("/stateProvinceCentrePoints.txt")
  val vocab = StateProvinces
}

object CountryCentrePoints extends CentrePoints {
  val map = loadFromFile("/countryCentrePoints.txt")
  val vocab = Countries
}

object TagsToDwc extends ValueMap {
  map = loadFromFile("/tagsToDwc.txt")
}

object Kingdoms extends Vocab {
  val all = loadVocabFromFile("/kingdoms.txt")
}

object DwC extends Vocab {
  val junk = List("matched", "parsed", "processed", "-", "\\.","_")
  override def matchTerm(string2Match: String) = {
    val str = {
      var strx = string2Match.toLowerCase
      junk.foreach( j => { strx = strx.replaceAll(j,"") })
      strx.trim
    }
    super.matchTerm(str)
  }

  val all = loadVocabFromFile("/dwc.txt")
}

object OccurrenceStatus extends Vocab {
  val all = loadVocabFromFile("/occurrenceStatus.txt")
}

object GeodeticDatum extends Vocab {
  val all = loadVocabFromFile("/datums.txt")
}

object Countries extends Vocab {
  val all = loadVocabFromFile("/countries.txt")
}

object StateProvinces extends Vocab {
  val all = loadVocabFromFile("/stateProvinces.txt")
}

object LifeStage extends Vocab {
  val all = loadVocabFromFile("/lifeStage.txt")
}

object Sex extends Vocab {
  val all = loadVocabFromFile("/sex.txt")
}

object TaxonRanks extends Vocab {
  val all = loadVocabFromFile("/taxonRanks.txt")
}

/**
 * Vocabulary matcher for basis of record values.
 */
object BasisOfRecord extends Vocab {
  val all = loadVocabFromVerticalFile("/basisOfRecord.txt")
}

/**
 * Vocabulary matcher for type status values.
 */
object TypeStatus extends Vocab {
  val all = loadVocabFromFile("/typeStatus.txt")
}

object Interactions extends Vocab {
  val all = loadVocabFromFile("/interactions.txt")
}

object EstablishmentMeans extends Vocab {
  val all = loadVocabFromFile("/establishmentMeans.txt") 
}

/**
 * A vacabulary mapping trait. Supports tests for compatible terms.
 */
trait VocabMaps {

  import JavaConversions._
  
  /** The map of terms to query against */
  val termMap:Map[String, Array[String]]

  /** retrieve a java friendly string list of the canonicals */
  def getStringList : java.util.List[String] = termMap.keys.toList.sorted
  
  /**
   * Compares the supplied term to an array of options
   * for compatibility.
   *
   * @param term
   * @param terms
   * @return
   */
  def areTermsCompatible(term:String, terms:Array[String]) : Option[Boolean] = {
    var weTested:Option[Boolean] = None
    terms.foreach(matchingTerm => {
      val matches = isCompatible(term, matchingTerm)
      if(!matches.isEmpty){
        //term is recognised
        if(matches.get){
          //it matches
          return Some(true)
        } else {
          weTested = Some(false)
        }
      }
   })
    weTested
  }

  /**
   * Returns None if the term wasnt recognised. If it was recognised, then we can test it.
   *
   * @param term1
   * @param term2
   * @return returns None if terms not recognised, and a true if recognised and matched.
   */
  def isCompatible (term1:String, term2:String) : Option[Boolean] = {
    if(term1!=null && term2!=null){
      if(term1.toUpperCase == term2.toUpperCase){
        //same term, return true
        Some(true)
      } else {
        val mapped = termMap.get(term1.toUpperCase)
        if(mapped.isEmpty){
          // if the term isnt mapped, return no decision
          None
        } else {
          //it is mapped, so return if its compatible          
          Some(mapped.get.contains(term2.toUpperCase))
        }
      }
    } else {
      None
    }
  }
}

/**
 * A vocabulary mapping for habitats.
 */
object HabitatMap extends VocabMaps {
  //Assume that all habitats fit into a "MARINE AND NON-MARINE" environment.
  val termMap = Map(
    "MARINE" -> Array("MARINE","MARINE AND NON-MARINE"),
    "NON-MARINE" -> Array("NON-MARINE", "TERRESTRIAL", "LIMNETIC","MARINE AND NON-MARINE"),
    "TERRESTRIAL" -> Array("NON-MARINE", "TERRESTRIAL", "LIMNETIC","MARINE AND NON-MARINE"),
    "LIMNETIC" -> Array("NON-MARINE", "TERRESTRIAL", "LIMNETIC","MARINE AND NON-MARINE")
  )
}
 /**
  * Case class that stores the information required to map a species to its
  * associated groups
  */
  case class SpeciesGroup(name:String, rank:String, values:Array[String], excludedValues:Array[String], lftRgtValues:Array[(Int,Int, Boolean)], parent:String){
    /*
     * Determines whether the supplied lft value represents a species from this group/
     * Relies on the excluded values coming first in the lftRgtValues array
     */
    def isPartOfGroup(lft:Int):Boolean ={
      lftRgtValues.foreach{tuple =>
        val (l,r,include) = tuple
        if(lft >= l && lft < r) return include
        }
      false
    }
  }

  /**
   * The species groups to test classifications against
   */
  object SpeciesGroups {
    import au.org.ala.util.ReflectBean._
    
    import JavaConversions._
    val logger = LoggerFactory.getLogger("SpeciesGroups")
    
    val groups = List(
     createSpeciesGroup("Animals", "kingdom", Array("Animalia"), Array(), null),
     createSpeciesGroup("Mammals", "classs", Array("Mammalia"),Array(), "Animals"),
     createSpeciesGroup("Birds", "classs", Array("Aves"), Array(), "Animals"),
     createSpeciesGroup("Reptiles", "classs", Array("Reptilia"), Array(), "Animals"),
     createSpeciesGroup("Amphibians", "classs", Array("Amphibia"), Array(),"Animals"),
     createSpeciesGroup("Fish", "classs", Array("Agnatha", "Chondrichthyes", "Osteichthyes", "Actinopterygii", "Sarcopterygii"), Array(), "Animals"),
     createSpeciesGroup("Molluscs", "phylum", Array("Mollusca"), Array(), "Animals"),
     createSpeciesGroup("Arthropods", "phylum", Array("Arthropoda"), Array(), "Animals"),
     createSpeciesGroup("Crustaceans", "classs" , Array("Branchiopoda", "Remipedia", "Maxillopoda", "Ostracoda", "Malacostraca"), Array(), "Arthropods"),
     createSpeciesGroup("Insects",  "classs", Array("Insecta"), Array(), "Arthropods"),
     createSpeciesGroup("Plants", "kingdom", Array("Plantae"), Array(), null),
     createSpeciesGroup("Bryophytes","phylum",Array("Bryophyta","Marchantiophyta","Anthocerotophyta"),Array(),"Plants"), //new group for AVH
     createSpeciesGroup("Gymnosperms","subclass", Array("Pinidae", "Cycadidae"), Array(), "Plants"), //new group for AVH
     createSpeciesGroup("FernsAndAllies","subclass", Array("Equisetidae", "Lycopodiidae", "Marattiidae", "Ophioglossidae", "Polypodiidae","Psilotidae"), Array(), "Plants"),
     //new groups for AVH
     createSpeciesGroup("Angiosperms", "subclass",Array("Magnoliidae"), Array(), "Plants"),//new group for AVH
     createSpeciesGroup("Monocots", "superorder", Array("Lilianae"), Array(), "Angiosperms"), //new group for AVH
     createSpeciesGroup("Dicots", "subclass", Array("Magnoliidae"),  Array("Lilianae"), "Angiosperms"), //new group for AVH     
     createSpeciesGroup("Fungi", "kingdom", Array("Fungi"), Array(), null),
     createSpeciesGroup("Chromista","kingdom", Array("Chromista"), Array(), null),
     createSpeciesGroup("Protozoa", "kingdom", Array("Protozoa"), Array(), null),
     createSpeciesGroup("Bacteria", "kingdom", Array("Bacteria"), Array(), null),
     createSpeciesGroup("Algae","phylum", Array("Bacillariophyta","Chlorophyta","Cyanidiophyta","Prasinophyta","Rhodophyta",
                                                 "Cryptophyta","Ochrophyta","Sagenista","Cercozoa","Euglenozoa","Cyanobacteria"),Array(),null)
    )
    
    def main(args:Array[String]){
      println(subgroups)
    }
    
    val subgroups = {
      //look up the JSON String
      //FIXME this should be a URL from a webservice. This URL isnt stable!!!
      val json = Source.fromURL("https://ala-bie.googlecode.com/svn/trunk/bie-profile/src/main/resources/subgroups.json").getLines.mkString
      
      val list = JSON.parseFull(json).get.asInstanceOf[List[Map[String,Object]]]//.get(0).asInstanceOf[Map[String, String]]
      val subGroupBuffer = new scala.collection.mutable.ArrayBuffer[SpeciesGroup]
      //println(list)
      list.foreach{map =>{
        if(map.containsKey("taxonRank")){
          val rank = map.getOrElse("taxonRank","class").toString
          val taxaList = map.get("taxa").get.asInstanceOf[List[Map[String,String]]]
          taxaList.foreach{taxaMap =>
            taxaMap.foreach{case (key,value) => {
              subGroupBuffer += createSpeciesGroup(taxaMap.getOrElse("common","").trim, rank,Array(taxaMap.getOrElse("name","").trim), Array(), null)
            }}
          }
        }
      }}
      subGroupBuffer.toList
    }

    /*
     * Creates a species group by first determining the left right ranges for the values and excluded values.
     */
    def createSpeciesGroup(title:String, rank:String, values:Array[String], excludedValues:Array[String], parent:String):SpeciesGroup={
      val lftRgts = values.map((v:String) =>{
        var snr:au.org.ala.checklist.lucene.model.NameSearchResult ={ try{Config.nameIndex.searchForRecord(v, au.org.ala.data.util.RankType.getForName(rank))}
        catch{
          case e:au.org.ala.checklist.lucene.HomonymException => e.getResults()(0) 
          case _:Exception => null
        }}
        if(snr != null){
        if(snr.isSynonym)
          snr = Config.nameIndex.searchForRecordByLsid(snr.getAcceptedLsid)
        (Integer.parseInt(snr.getLeft()), Integer.parseInt(snr.getRight()),true)
      }
      else{
        logger.debug(v + " has no name " )
        (-1,-1,false)
      }
      })
      val lftRgtExcluded = excludedValues.map((v:String) =>{
        var snr = Config.nameIndex.searchForRecord(v, null)
        if(snr != null){
        if(snr.isSynonym)
          snr = Config.nameIndex.searchForRecordByLsid(snr.getAcceptedLsid)
        (Integer.parseInt(snr.getLeft()), Integer.parseInt(snr.getRight()),false)
        }
        else{
          logger.debug(v + " has no name")
          (-1,-1,false)
        }
      })
      SpeciesGroup(title, rank, values, excludedValues, lftRgtExcluded ++ lftRgts, parent) // Excluded values are first so that we can discount a species group if necessary
    }
    
    def getStringList : java.util.List[String] = groups.map(g => g.name).toList.sorted
    
    /**
     * Returns all the species groups to which supplied classification belongs
     * @deprecated It is better to use the left right values to determine species groups
     */
    @Deprecated
    def getSpeciesGroups(cl:Classification):Option[List[String]]={
      val matchedGroups = groups.collect{case sg: SpeciesGroup if sg.values.contains(StringUtils.capitalize(StringUtils.lowerCase(cl.getter(sg.rank).asInstanceOf[String]))) => sg.name}
      Some(matchedGroups)
    }

    /**
     * Returns all the species groups to which the supplied left right values belong
     */
    def getSpeciesGroups(lft:String, rgt:String):Option[List[String]]= getGenericGroups(lft,rgt, groups)

    def getSpeciesSubGroups(lft:String, rgt:String):Option[List[String]] = getGenericGroups(lft,rgt,subgroups)

    def getGenericGroups(lft:String, rgt:String, groupingList:List[SpeciesGroup]):Option[List[String]]={
      try{
        val ilft = Integer.parseInt(lft)
        //val irgt = Integer.parseInt(rgt)
        val matchedGroups = groupingList.collect{case sg:SpeciesGroup if(sg.isPartOfGroup(ilft)) => sg.name}
        Some(matchedGroups)
      }
      catch {
        case _:Exception => None
      }
    }
  }

object ErrorCodeCategory {
  val Error = "error"
  val Missing = "missing"
  val Warning = "warning"
  val Verified = "verified"
}

/**
 * Case class that represents an error code for a occurrence record.
 */
sealed case class ErrorCode(@BeanProperty name:String,
  @BeanProperty code:Int,
  @BeanProperty isFatal:Boolean,
  @BeanProperty description:String,
  @BeanProperty category:String = ErrorCodeCategory.Error
)

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
      if(method.getReturnType.getName == "au.org.ala.biocache.ErrorCode")
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

  def getMissingByCode(codes:List[Int]):Set[ErrorCode] ={
    (AssertionCodes.all &~ Set(VERIFIED, PROCESSING_ERROR)).filterNot(e =>codes.contains(e.code))
  }

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

object Layers {

  lazy val nameToIdMap: Map[String, String] = {
    //get the JSON string for the layers
    val nonDefaultFieldMap = Map[String, String]("aus1" -> "stateProvince", "aus2" -> "lga", "ibra_reg_shape" -> "ibra",
      "ibra_merged" -> "ibra", "imcra4_pb" -> "imcra", "ne_world" -> "country")
    try {
      val json = Source.fromURL("http://spatial.ala.org.au/layers.json").mkString
      val map = JSON.parseFull(json).get.asInstanceOf[Map[String, List[Map[String, AnyRef]]]]
      val layers = map("layerList")
      var idmap = new scala.collection.mutable.HashMap[String, String]
      layers.foreach(layer => {
        val name = layer.get("name")
        val layerType = layer.getOrElse("type", "")
        if (!name.isEmpty) {
          val sname = name.get.asInstanceOf[String].toLowerCase
          val id = nonDefaultFieldMap.getOrElse(sname, getPrefix(layerType.asInstanceOf[String]) + layer.get("id").get.asInstanceOf[Double].toInt)
          idmap += sname -> id
        }
      })
      idmap.toMap
    }
    catch {
      case e: Exception => e.printStackTrace; Map[String, String]()
    }
  }

  lazy val idToNameMap: Map[String, String] = nameToIdMap.map(_.swap)

  def getPrefix(value: String) = if (value == "Environmental") "el" else "cl"
}