package au.org.ala.biocache

import reflect.BeanProperty
import au.org.ala.util.Stemmer
import scala.collection.JavaConversions

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
  
  def getStringList : java.util.List[String] = all.map(t => t.canonical).toList.sort((x,y) => x.compare(y) < 0)
  
  /**
   * Match a term. Matches canonical form or variants in array
   * @param string2Match
   * @return
   */
  def matchTerm(string2Match:String) : Option[Term] = {
    if(string2Match!=null){
      //strip whitespace & strip quotes and fullstops & uppercase
      val stringToUse = string2Match.replaceAll("""[ \\"\\'\\.\\,\\-\\?]*""", "").toLowerCase
      
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
  
  def retrieveCanonicals(terms:List[String]) = {
    terms.map(ch => {
        DwC.matchTerm(ch) match {
            case Some(term) => term.canonical
            case None => ch
        }
    })
  }

  def retrieveCanonicalsOrNothing(terms:List[String]) = {
    terms.map(ch => {
        DwC.matchTerm(ch) match {
            case Some(term) => term.canonical
            case None => ""
        }
    })
  }
  
  def loadVocabFromFile(filePath:String) : Set[Term] = {
    scala.io.Source.fromURL(getClass.getResource(filePath), "utf-8").getLines.toList.map({ row =>
        val values = row.split("\t")
        new Term(values.head, values.tail)
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

/**
 * Quick state string matching implementation.
 */
object States extends Vocab {
  val act = Term("Australian Capital Territory", Array("AustCapitalTerritory","AustCapitalTerrit","AusCap","AusCapTerrit","ACT"))
  val nsw = Term("New South Wales", Array("nswales","nsw"))
  val nt = Term("Northern Territory", Array("nterritory","nterrit","nt"))
  val qld = Term("Queensland", Array("qland","qld"))
  val sa = Term("South Australia", Array("sthaustralia","saustralia","saust","sa"))
  val tas = Term("Tasmania", Array("tassie","tas"))
  val vic = Term("Victoria", Array("vic","vict"))
  val wa = Term("Western Australia", Array("waustralia","westaustralia","westaust","wa"))
  val all = retrieveAll
}

/**
 * Matching of coordinates for centre points for states.
 * This is for detecting auto-generated coordinates at very low accuracy.
 */
object StateCentrePoints {
  val map = Map(
    States.act -> (-35.4734679f, 149.0123679f),
    States.nsw -> (-31.2532183f, 146.921099f),
    States.nt -> (-19.4914108f, 132.5509603f),
    States.qld -> (-20.9175738f, 142.7027956f),
    States.sa -> (-30.0002315f, 136.2091547f),
    States.tas -> (-41.3650419f, 146.6284905f),
    States.vic -> (-37.4713077f, 144.7851531f),
    States.wa -> (-27.6728168f, 121.6283098f)
  )

  /**
   * Returns true if the supplied coordinates are the centre point for the supplied
   * state or territory
   */
  def coordinatesMatchCentre(state:String, decimalLatitude:String, decimalLongitude:String) : Boolean = {

    val matchedState = States.matchTerm(state)
    if(!matchedState.isEmpty && decimalLatitude != null && decimalLongitude != null){

      val coordinates = map.get(matchedState.get)

      //how many decimal places are the supplied coordinates
      try {
          val latitude = decimalLatitude.toFloat
          val longitude = decimalLongitude.toFloat

          val latDecPlaces = noOfDecimalPlace(latitude)
          val longDecPlaces = noOfDecimalPlace(longitude)

          //println("Decimal places: "+latDecPlaces +", "+longDecPlaces)
          //approximate the centre points appropriately
          val approximatedLat = round(coordinates.get._1,latDecPlaces)
          val approximatedLong = round(coordinates.get._2,longDecPlaces)

          //println("Rounded values: "+approximatedLat +", "+approximatedLong)
          if(approximatedLat == latitude && approximatedLong == longitude){
            true
          } else {
            false
          }
      } catch {
        case e:NumberFormatException => false
      }
    } else {
      false
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
}

object DwC extends Vocab {
    val all = loadVocabFromFile("/dwc.txt")
}

object GeodeticDatum extends Vocab {
    val all = loadVocabFromFile("/datums.txt")
}

object Countries extends Vocab {
    val all = loadVocabFromFile("/countries.txt")
}

/**
 * Vocabulary matcher for basis of record values.
 */
object BasisOfRecord extends Vocab {
  val all = loadVocabFromFile("/basisOfRecord.txt")
}

/**
 * Vocabulary matcher for type status values.
 */
object TypeStatus extends Vocab {
  val all = loadVocabFromFile("/typeStatus.txt")
}

/**
 * A vacabulary mapping trait. Supports tests for compatible terms.
 */
trait VocabMaps {

  import JavaConversions._
  
  /** The map of terms to query against */
  val termMap:Map[String, Array[String]]

  /** retrieve a java friendly string list of the canonicals */
  def getStringList : java.util.List[String] = termMap.keys.toList.sort((x,y) => x.compare(y) < 0)
  
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
  val termMap = Map(
    "MARINE" -> Array("MARINE"),
    "NON-MARINE" -> Array("NON-MARINE", "TERRESTRIAL", "LIMNETIC"),
    "TERRESTRIAL" -> Array("NON-MARINE", "TERRESTRIAL", "LIMNETIC"),
    "LIMNETIC" -> Array("NON-MARINE", "TERRESTRIAL", "LIMNETIC")
  )
}
 /**
  * Case class that stores the information required to map a species to its
  * associated groups
  */
  case class SpeciesGroup(name:String, rank:String, values:Array[String], parent:String)

  /**
   * The species groups to test classifications against
   */
  object SpeciesGroups {
    import au.org.ala.util.ReflectBean._
    
    import JavaConversions._
    
    val groups = List(
     SpeciesGroup("Animals", "kingdom", Array("Animalia"), null),
     SpeciesGroup("Mammals", "classs", Array("Mammalia"), "Animals"),
     SpeciesGroup("Birds", "classs", Array("Aves"), "Animals"),
     SpeciesGroup("Reptiles", "classs", Array("Reptilia"), "Animals"),
     SpeciesGroup("Amphibians", "classs", Array("Amphibia"),"Animals"),
     SpeciesGroup("Fish", "classs", Array("Agnatha", "Chondrichthyes", "Osteichthyes", "Actinopterygii", "Sarcopterygii"), "Animals"),
     SpeciesGroup("Molluscs", "phylum", Array("Mollusca"), "Animals"),
     SpeciesGroup("Arthropods", "phylum", Array("Arthropoda"), "Animals"),
     SpeciesGroup("Crustaceans", "classs" , Array("Branchiopoda", "Remipedia", "Maxillopoda", "Ostracoda", "Malacostraca"), "Arthropods"),
     SpeciesGroup("Insects",  "classs", Array("Insecta"), "Arthropods"),
     SpeciesGroup("Plants", "kingdom", Array("Plantae"), null),
     SpeciesGroup("Fungi", "kingdom", Array("Fungi"), null),
     SpeciesGroup("Chromista","kingdom", Array("Chromista"), null),
     SpeciesGroup("Protozoa", "kingdom", Array("Protozoa"), null),
     SpeciesGroup("Bacteria", "kingdom", Array("Bacteria"), null)
    )
    
    def getStringList : java.util.List[String] = groups.map(g => g.name).toList.sort((x,y) => x.compare(y) < 0)
    
    /**
     * Returns all the species groups to which supplied classification belongs
     */
    def getSpeciesGroups(cl:Classification):Option[List[String]]={
      val matchedGroups = groups.collect{case sg: SpeciesGroup if sg.values.contains(cl.getter(sg.rank)) => sg.name}
      Some(matchedGroups)
    }
  }

/**
 * Case class that represents an error code for a occurrence record.
 */
sealed case class ErrorCode(@BeanProperty name:String, @BeanProperty code:Int, @BeanProperty isFatal:Boolean,
                               @BeanProperty description:String)

/**
 * Assertion codes for records. These codes are a reflection of http://bit.ly/evMJv5
 */
object AssertionCodes {

  //geospatial issues
  val GEOSPATIAL_ISSUE = ErrorCode("geospatialIssue",0,true,"Geospatial issue")  // general purpose option
  val NEGATED_LATITUDE = ErrorCode("negatedLatitude",1,false,"Latitude is negated")
  val NEGATED_LONGITUDE = ErrorCode("negatedLongitude",2,false,"Longitude is negated")
  val INVERTED_COORDINATES = ErrorCode("invertedCoordinates",3,false,"Coordinates are transposed")
  val ZERO_COORDINATES = ErrorCode("zeroCoordinates",4,true,"Supplied coordinates are zero")
  val COORDINATES_OUT_OF_RANGE = ErrorCode("coordinatesOutOfRange",5,true,"Coordinates are out of range for species")
  val UNKNOWN_COUNTRY_NAME = ErrorCode("unknownCountry",6,false,"Supplied country not recognised")
  val ALTITUDE_OUT_OF_RANGE = ErrorCode("altitudeOutOfRange",7,false,"Altitude out of range")
  val BADLY_FORMED_ALTITUDE = ErrorCode("erroneousAltitude",8,false, "Badly formed altitude")
  val MIN_MAX_ALTITUDE_REVERSED = ErrorCode("minMaxAltitudeReversed",9,false, "Min and max altitude reversed")
  val DEPTH_IN_FEET = ErrorCode("depthInFeet",10,false,"Depth value supplied in feet")
  val DEPTH_OUT_OF_RANGE = ErrorCode("depthOutOfRange",11,false,"Depth out of range")
  val MIN_MAX_DEPTH_REVERSED = ErrorCode("minMaxDepthReversed",12,false,"Min and max depth reversed")
  val ALTITUDE_IN_FEET = ErrorCode("altitudeInFeet",13,false,"Altitude value supplied in feet")
  val ALTITUDE_NON_NUMERIC = ErrorCode("altitudeNonNumeric",14,false,"Altitude value non-numeric")
  val DEPTH_NON_NUMERIC = ErrorCode("depthNonNumeric",15,false,"Depth value non-numeric")
  val COUNTRY_COORDINATE_MISMATCH = ErrorCode("countryCoordinateMismatch",16,false,"Coordinates dont match supplied country")
  val STATE_COORDINATE_MISMATCH = ErrorCode("stateCoordinateMismatch",18,false,"Coordinates dont match supplied state")
  val COORDINATE_HABITAT_MISMATCH = ErrorCode("habitatMismatch",19,true,"Habitat incorrect for species")
  val DETECTED_OUTLIER = ErrorCode("detectedOutlier",20,true,"Suspected outlier")
  val COUNTRY_INFERRED_FROM_COORDINATES = ErrorCode("countryInferredByCoordinates",21,false,"Country inferred from coordinates")
  val COORDINATES_CENTRE_OF_STATEPROVINCE = ErrorCode("coordinatesCentreOfStateProvince",22,true,"Supplied coordinates centre of state")
  val COORDINATE_PRECISION_MISMATCH = ErrorCode("coordinatePrecisionMismatch",23,false,"Coordinate precision not valid")
  val UNCERTAINTY_RANGE_MISMATCH = ErrorCode("uncertaintyRangeMismatch",24,false,"Coordinate accuracy not valid")
  val UNCERTAINTY_IN_PRECISION = ErrorCode("uncertaintyInPrecision",25,false,"Coordinate precision and accuracy transposed")
  val UNCERTAINTY_NOT_SPECIFIED = ErrorCode("uncertaintyNotSpecified", 27, false, "Coordinate uncertainty was not supplied")

  //taxonomy issues
  val TAXONOMIC_ISSUE = ErrorCode("taxonomicIssue",10000,false,"Taxonomic issue")  // general purpose option
  val INVALID_SCIENTIFIC_NAME = ErrorCode("invalidScientificName",10001,false,"Invalid scientific name")
  val UNKNOWN_KINGDOM = ErrorCode("unknownKingdom",10002,false,"Kingdom not recognised")
  val AMBIGUOUS_NAME = ErrorCode("ambiguousName",10003,false,"Higher taxonomy missing")
  val NAME_NOTRECOGNISED = ErrorCode("nameNotRecognised",10004,false,"Name not recognised")
  val NAME_NOT_IN_NATIONAL_CHECKLISTS = ErrorCode("nameNotInNationalChecklists",10005,false,"Name not in national checklists")
  val HOMONYM_ISSUE = ErrorCode("homonymIssue",10006,false,"Homonym issues with supplied name")
  val IDENTIFICATION_INCORRECT = ErrorCode("identificationIncorrect",10007,false,"Taxon misidentified")

  //miscellanous
  val MISSING_BASIS_OF_RECORD = ErrorCode("missingBasisOfRecord",20001,true,"Basis of record not supplied")
  val BADLY_FORMED_BASIS_OF_RECORD = ErrorCode("badlyFormedBasisOfRecord",20002,true,"Basis of record badly formed")
  val UNRECOGNISED_TYPESTATUS = ErrorCode("unrecognisedTypeStatus",20004,false,"Type status not recognised")
  val UNRECOGNISED_COLLECTIONCODE = ErrorCode("unrecognisedCollectionCode",20005,false,"Collection code not recognised")
  val UNRECOGNISED_INSTITUTIONCODE = ErrorCode("unrecognisedInstitutionCode",20006,false,"Institution code not recognised")
  val INVALID_IMAGE_URL = ErrorCode("invalidImageUrl", 20007, false,"Image URL invalid")
  val RESOURCE_TAXONOMIC_SCOPE_MISMATCH = ErrorCode("resourceTaxonomicScopeMismatch", 20008, false, "")

  //temporal
  val TEMPORAL_ISSUE = ErrorCode("temporalIssue",30000,false,"Temporal issue")  // general purpose option
  val ID_PRE_OCCURRENCE = ErrorCode("idPreOccurrence",30001,false,"Identification date before occurrence date")
  val GEOREFERENCE_POST_OCCURRENCE = ErrorCode("georefPostDate",30002,false,"Georeferenced after occurrence date")
  val FIRST_OF_MONTH = ErrorCode("firstOfMonth",30003,false,"First of the month")
  val FIRST_OF_YEAR = ErrorCode("firstOfYear",30004,false,"First of the year")
  val FIRST_OF_CENTURY = ErrorCode("firstOfCentury",30005,false,"First of the century")
  val DATE_PRECISION_MISMATCH = ErrorCode("datePrecisionMismatch",30006,false,"Date precision invalid")
  val INVALID_COLLECTION_DATE = ErrorCode("invalidCollectionDate",30007,false,"Invalid collection date")

  //all the codes
  val all = retrieveAll

  //ranges for error codes
  val geospatialBounds = (0, 10000)
  val taxonomicBounds = (10000, 20000)
  val miscellanousBounds = (20000, 30000)
  val temporalBounds = (30000, 40000)

  val geospatialCodes = all.filter(errorCode => {errorCode.code >= geospatialBounds._1 && errorCode.code < geospatialBounds._2})
  val taxonomicCodes = all.filter(errorCode => {errorCode.code>=10000 && errorCode.code<20000})
  val miscellaneousCodes = all.filter(errorCode => {errorCode.code>=20000 && errorCode.code<30000})
  val temporalCodes = all.filter(errorCode => {errorCode.code>=30000 && errorCode.code<40000})

  val userAssertionCodes = Array(GEOSPATIAL_ISSUE,COORDINATE_HABITAT_MISMATCH,DETECTED_OUTLIER,
      TAXONOMIC_ISSUE,IDENTIFICATION_INCORRECT,TEMPORAL_ISSUE)

  /**
   * Retrieve all the terms defined in this vocab.
   * @return
   */
  def retrieveAll : Set[ErrorCode] = {
    val methods = this.getClass.getMethods
    (for{
      method<-methods
      if(method.getReturnType.getName == "au.org.ala.biocache.ErrorCode")
    } yield (method.invoke(this).asInstanceOf[ErrorCode])).toSet[ErrorCode]
  }

  /** Retrieve an error code by the numeric code */
  def getByCode(code:Int) : Option[ErrorCode] = all.find(errorCode => errorCode.code == code)
  
  def getByName(name:String) :Option[ErrorCode] = all.find(errorCode => errorCode.name == name)

  /** Is it geospatially kosher */
  def isGeospatiallyKosher (assertions:Array[QualityAssertion]) : Boolean = {
    assertions.filter(ass => {
       val errorCode = AssertionCodes.all.find(errorCode => errorCode.code == ass.code )
       if(!errorCode.isEmpty){
         ass.code >= AssertionCodes.geospatialBounds._1 &&
                ass.code < AssertionCodes.geospatialBounds._2 &&
                errorCode.get.isFatal
       } else {
          false
       }
    }).isEmpty
  }

  /** Is it taxonomically kosher */
  def isTaxonomicallyKosher (assertions:Array[QualityAssertion]) : Boolean = {
    assertions.filter(ass => {
       val errorCode = AssertionCodes.all.find(errorCode => errorCode.code == ass.code )
       if(!errorCode.isEmpty){
         ass.code >= AssertionCodes.taxonomicBounds._1 &&
                ass.code < AssertionCodes.taxonomicBounds._2 &&
                errorCode.get.isFatal
       } else {
         false //we cant find the code, so ignore
       }
    }).isEmpty
  }
}