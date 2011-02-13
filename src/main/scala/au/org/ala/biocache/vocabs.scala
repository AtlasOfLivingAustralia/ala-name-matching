package au.org.ala.biocache

import reflect.BeanProperty

/**
 * Case class that encapsulates a canonical form and variants.
 */
case class Term (@BeanProperty canonical:String, @BeanProperty variants:Array[String])

/**
 * A trait for a vocabulary. A vocabulary consists of a set
 * of Terms, each with string variants.
 */
trait Vocab {

  val all:Set[Term]
  /**
   * Match a term. Matches canonical form or variants in array
   * @param string2Match
   * @return
   */
  def matchTerm(string2Match:String) : Option[Term] = {
    if(string2Match!=null){
      //strip whitespace & strip quotes and fullstops & uppercase
      val stringToUse = string2Match.replaceAll("([.,-]*)?([\\s]*)?", "").toLowerCase
      for(term<-all){
        if(term.canonical.equalsIgnoreCase(stringToUse))
          return Some(term)
        if(term.variants.contains(stringToUse)){
          return Some(term)
        }
      }
    }
    None
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
  val act = new Term("Australian Capital Territory", Array("AustCapitalTerritory","AustCapitalTerrit","AusCap","AusCapTerrit","ACT"))
  val nsw = new Term("New South Wales", Array("nswales","nsw"))
  val nt = new Term("Northern Territory", Array("nterritory","nterrit","nt"))
  val qld = new Term("Queensland", Array("qland","qld"))
  val sa = new Term("South Australia", Array("sthaustralia","saustralia","saust","sa"))
  val tas = new Term("Tasmania", Array("tassie","tas"))
  val vic = new Term("Victoria", Array("vic","vict"))
  val wa = new Term("Western Australia", Array("waustralia","westaustralia","westaust","wa"))
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
    if(!matchedState.isEmpty){

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

/**
 * Vocabulary matcher for basis of record values.
 */
object BasisOfRecord extends Vocab {
  val specimen = new Term("PreservedSpecimen", Array("specimen","s", "spec", "sp"))
  val observation = new Term("HumanObservation", Array("observation","o","obs"))
  val fossil = new Term("FossilSpecimen", Array("fossil","f", "fos"))
  val living = new Term("LivingSpecimen", Array("living","l"))
  val all = retrieveAll
}

/**
 * Vocabulary matcher for type status values.
 */
object TypeStatus extends Vocab {
  val allolectotype = new Term("allolectotype", Array[String]())
  val alloneotype = new Term("alloneotype", Array[String]())
  val allotype = new Term("allotype", Array[String]())
  val cotype = new Term("cotype", Array[String]())
  val epitype = new Term("epitype", Array[String]())
  val exepitype = new Term("exepitype", Array[String]())
  val exholotype = new Term("exholotype", Array("ex holotype"))
  val exisotype = new Term("exisotype", Array[String]())
  val exlectotype = new Term("exlectotype", Array[String]())
  val exneotype = new Term("exneotype", Array[String]())
  val exparatype = new Term("exparatype", Array[String]())
  val exsyntype = new Term("exsyntype", Array[String]())
  val extype = new Term("extype", Array[String]())
  val hapantotype = new Term("hapantotype", Array[String]())
  val holotype = new Term("holotype", Array("holo type"))
  val iconotype = new Term("iconotype", Array[String]())
  val isolectotype = new Term("isolectotype", Array[String]())
  val isoneotype = new Term("isoneotype", Array[String]())
  val isosyntype = new Term("isosyntype", Array[String]())
  val isotype = new Term("isotype", Array[String]("iso type"))
  val lectotype = new Term("lectotype", Array[String]())
  val neotype = new Term("neotype", Array[String]("neo type"))
  val notatype = new Term("notatype", Array("not a type"))  //should this be removed??
  val paralectotype = new Term("paralectotype", Array[String]())
  val paraneotype = new Term("paraneotype", Array[String]())
  val paratype = new Term("paratype", Array[String]())
  val plastoholotype = new Term("plastoholotype", Array[String]())
  val plastoisotype = new Term("plastoisotype", Array[String]())
  val plastolectotype = new Term("plastolectotype", Array[String]())
  val plastoneotype = new Term("plastoneotype", Array[String]())
  val plastoparatype = new Term("plastoparatype", Array[String]())
  val plastosyntype = new Term("plastosyntype", Array[String]())
  val plastotype = new Term("plastotype", Array[String]())
  val secondarytype = new Term("secondarytype", Array[String]())
  val supplementarytype = new Term("supplementarytype", Array[String]())
  val syntype = new Term("syntype", Array[String]())
  val topotype = new Term("topotype", Array[String]())
  val typee = new Term("type", Array[String]())
  val all = retrieveAll
}

/**
 * A vacabulary mapping trait. Supports tests for compatible terms.
 */
trait VocabMaps {

  /** The map of terms to query against */
  val termMap:Map[String, Array[String]]

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
    for(matchingTerm<-terms){
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
    }
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
 * Case class that represents an error code for a occurrence record.
 */
sealed case class ErrorCode(@BeanProperty name:String, @BeanProperty code:Int, @BeanProperty isFatal:Boolean)

/**
 * Assertion codes for records. These codes are a reflection of http://bit.ly/evMJv5
 */
object AssertionCodes {

  //geospatial issues
  val GEOSPATIAL_ISSUE = ErrorCode("geospatialIssue",0,true)  // general purpose option
  val NEGATED_LATITUDE = ErrorCode("negatedLatitude",1,false)
  val NEGATED_LONGITUDE = ErrorCode("negatedLongitude",2,false)
  val INVERTED_COORDINATES = ErrorCode("invertedCoordinates",3,false)
  val ZERO_COORDINATES = ErrorCode("zeroCoordinates",4,false)
  val COORDINATES_OUT_OF_RANGE = ErrorCode("coordinatesOutOfRange",5,false)
  val UNKNOWN_COUNTRY_NAME = ErrorCode("unknownCountry",6,false)
  val ALTITUDE_OUT_OF_RANGE = ErrorCode("altitudeOutOfRange",7,false)
  val BADLY_FORMED_ALTITUDE = ErrorCode("erroneousAltitude",8,false)
  val MIN_MAX_ALTITUDE_REVERSED = ErrorCode("minMaxAltitudeReversed",9,false)
  val DEPTH_IN_FEET = ErrorCode("depthInFeet",10,false)
  val DEPTH_OUT_OF_RANGE = ErrorCode("depthOutOfRange",11,false)
  val MIN_MAX_DEPTH_REVERSED = ErrorCode("minMaxDepthReversed",12,false)
  val ALTITUDE_IN_FEET = ErrorCode("altitudeInFeet",13,false)
  val ALTITUDE_NON_NUMERIC = ErrorCode("altitudeNonNumeric",14,false)
  val DEPTH_NON_NUMERIC = ErrorCode("depthNonNumeric",15,false)
  val COUNTRY_COORDINATE_MISMATCH = ErrorCode("countryCoordinateMismatch",16,false)
  val STATE_COORDINATE_MISMATCH = ErrorCode("stateCoordinateMismatch",18,false)
  val COORDINATE_HABITAT_MISMATCH = ErrorCode("habitatMismatch",19,false)
  val DETECTED_OUTLIER = ErrorCode("detectedOutlier",20,false)
  val COUNTRY_INFERRED_FROM_COORDINATES = ErrorCode("countryInferredByCoordinates",21,false)
  val COORDINATES_CENTRE_OF_STATEPROVINCE = ErrorCode("coordinatesCentreOfStateProvince",22,false)
  val COORDINATE_PRECISION_MISMATCH = ErrorCode("coordinatePrecisionMismatch",23,false)
  val UNCERTAINTY_RANGE_MISMATCH = ErrorCode("uncertaintyRangeMismatch",24,false)
  val UNCERTAINTY_IN_PRECISION = ErrorCode("uncertaintyInPrecision",25,false)

  //taxonomy issues
  val TAXONOMIC_ISSUE = ErrorCode("taxonomicIssue",10000,false)  // general purpose option
  val INVALID_SCIENTIFIC_NAME = ErrorCode("invalidScientificName",10001,false)
  val UNKNOWN_KINGDOM = ErrorCode("unknownKingdom",10002,false)
  val AMBIGUOUS_NAME = ErrorCode("ambiguousName",10003,false)
  val NAME_NOTRECOGNISED = ErrorCode("nameNotRecognised",10004,false)
  val NAME_NOT_IN_NATIONAL_CHECKLISTS = ErrorCode("nameNotRecognised",10005,false)
  val HOMONYM_ISSUE = ErrorCode("homonymIssue",10006,false)

  //miscellanous
  val MISSING_BASIS_OF_RECORD = ErrorCode("missingBasisOfRecord",20001,false)
  val BADLY_FORMED_BASIS_OF_RECORD = ErrorCode("badlyFormedBasisOfRecord",20002,false)
  val UNRECOGNISED_TYPESTATUS = ErrorCode("unrecognisedTypeStatus",20004,false)
  val UNRECOGNISED_COLLECTIONCODE = ErrorCode("unrecognisedCollectionCode",20005,false)
  val UNRECOGNISED_INSTITUTIONCODE = ErrorCode("unrecognisedInstitutionCode",20006,false)

  //temporal
  val ID_PRE_OCCURRENCE = ErrorCode("idPreOccurrence",30001,false)
  val GEOREFERENCE_POST_OCCURRENCE = ErrorCode("georefPostDate",30002,false)
  val FIRST_OF_MONTH = ErrorCode("firstOfMonth",30003,false)
  val FIRST_OF_YEAR = ErrorCode("firstOfYear",30004,false)
  val FIRST_OF_CENTURY = ErrorCode("firstOfCentury",30005,false)
  val DATE_PRECISION_MISMATCH = ErrorCode("datePrecisionMismatch",30006,false)
  val INVALID_COLLECTION_DATE = ErrorCode("invalidCollectionDate",30007,false)

  //all the codes
  val all = retrieveAll

  val geospatialBounds = (0, 10000)
  val taxonomicBounds = (10000, 20000)
  val geospatialCodes = all.filter(errorCode => {errorCode.code>=0 && errorCode.code<10000})
  val taxonomicCodes = all.filter(errorCode => {errorCode.code>=10000 && errorCode.code<20000})

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

  def getByCode(code:Int) : Option[ErrorCode] = all.find(errorCode => errorCode.code == code)

  /**
   * Is it geospatially kosher
   */
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
    }).size == 0
  }

  /**
   * is it taxonomically kosher
   */
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
    }).size == 0
  }
}