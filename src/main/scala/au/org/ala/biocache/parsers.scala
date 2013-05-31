package au.org.ala.biocache
import java.util.regex.Pattern
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.text.WordUtils
import org.slf4j.LoggerFactory

object LatOrLong extends Enumeration {
  type LatOrLong = Value
  val Latitude, Longitude = Value
  val latDirString = "s|south|n|north".split("\\|").toSet
  val longDirString = "w|west|e|east".split("\\|").toSet

  def getDirection(raw:String): Option[LatOrLong] = {
    val normalised = raw.toLowerCase.trim
    if(latDirString.contains(normalised)) Some(Latitude)
    else if (longDirString.contains(normalised)) Some(Longitude)
    else None
  }
}

/**
 * Parser for coordinates in deg, min sec format
 */
object VerbatimLatLongParser {

  import LatOrLong._
  val verbatimPattern = """(?:[\\-])?([0-9]{1,3})([d|deg|degree|degrees|°|º][ ]*)([0-9]{1,2})?([m|min|minutes|minute|'][ ]*)([0-9]{1,2}.?[0-9]{0,})?(["|']{1,2}[ ]*)?(s|south|n|north|w|west|e|east)""".r
  val verbatimPatternNoDenom = """(?:[\\-])?([0-9]{1,3})(?:[ ]*)?([0-9]{1,2})?(?:[ ]*)?([0-9]{1,2}.?[0-9]{0,})?(?:"[ ]*)?(s|south|n|north|w|west|e|east)""".r
  val negativePattern = "(s|south|w|west)".r

  /**
   * Parse with a decimal number output and an indication of latitude or longitude
   *
   * @param stringValue
   * @return
   */
  def parseWithDirection(stringValue:String) : (Option[Float], Option[LatOrLong]) = {
    try{
      val normalised = {
        stringValue.toLowerCase.trim.replaceAll("''", "\"")
      }
      normalised match {
        case verbatimPattern(degree, dsign, minute, msign, second, ssign, direction) => {
          (convertToDecimal(degree, minute, second, direction), LatOrLong.getDirection(direction))
        }
        case verbatimPatternNoDenom(degree, minute, second, direction) => {
          (convertToDecimal(degree, minute, second, direction), LatOrLong.getDirection(direction))
        }
        case _ => (None,None)
      }
    } catch {
      case e:Exception => (None,None)
    }
  }

  /**
   * Parses the verbatim latitude or longitude
   * Formats handled:
   * 30° 01' S
   * 153° 12' E
   * 145° 44' 55.85" E
   * 16° 52' 37" S
   *
   * TODO: Enhance this parser to cater for more formats and dirty data
   */
  def parse(stringValue:String) : Option[Float] = {
    try{
      val normalised = {
        stringValue.toLowerCase.trim.replaceAll("''", "\"")
      }
      normalised match {
       case verbatimPattern(degree, dsign, minute, msign, second, ssign, direction) => {
         convertToDecimal(degree, minute, second, direction)
       }
       case verbatimPatternNoDenom(degree, minute, second, direction) => {
         convertToDecimal(degree, minute, second, direction)
       }
       case _ => None
      }
    } catch {
      case e:Exception => None
    }
  }

  /**
   * Parses to float and converts to string or null
   */
  def parseToStringOrNull(stringValue:String) : String = {
     parse(stringValue) match {
       case Some(v) => v.toString
       case None => null
     }
  }

  def convertToDecimal(degree:String, minute:String, second:String, direction:String) : Option[Float] ={
    var decimalValue = degree.toInt * 10000000
    //println("after degree: " + decimalValue)
    if(minute != null)
      decimalValue += ((minute.toInt*10000000)/60)
    //println("after minute: " + decimalValue)
    if(second != null){
      decimalValue += ((second.toFloat * 10000000).toInt/3600)
    //println("after second: "+ decimalValue + " - " + second + " = " + second.toFloat + " in degrees = " + (second.toFloat/3600))
    }
    try{
      direction match {
          case negativePattern(pat) => Some(-decimalValue.toFloat/10000000)
          case _ => Some(decimalValue.toFloat/10000000)
      }
    } catch {
      case _:Exception => None
    }
  }
}

object CollectorNameParser {
   val logger = LoggerFactory.getLogger("CollectorNameParser")
  val NAME_LETTERS = "A-ZÏËÖÜÄÉÈČÁÀÆŒ\\p{Lu}"
  val name_letters = "a-zïëöüäåéèčáàæœ\\p{Ll}"
  val na = "[nN]/[aA]|\\([\\x00-\\x7F\\s]*?\\)"
  val titles = """Dr|DR|dr|\(Professor\)|Mr|MR|mr|Mrs|mrs|MRS|Ms|ms|MS|Lieutenant"""
  val etAl = "[eE][tT][. ] ?[aA][Ll][. ]?"
  val initialsRegEx = """((?:[A-Z][-. ]? ?){0,4})"""  
  val ORGANISATION_WORDS="""collection|Entomology|University|Oceanographic|Indonesia|Division|American|Photographic|SERVICE|Section|Arachnology|Northern|Institute|Ichthyology|AUSTRALIA|Malacology|Institution|Department|Survey|DFO|Society|FNS-\(SA\)|Association|Government|COMMISSION|Department|Conservation|Expedition|NPWS-\(SA\)|Study Group|DIVISION|Melbourne|ATLAS|summer parties|Macquarie Island|NSW|Australian|Museum|Herpetology|ORNITHOLOGICAL|ASSOCIATION|SURVEY|Fisheries|Queensland|Griffith Npws|NCS-\(SA\)|UNIVERSITY|SCIENTIFIC|Ornithologists|Bird Observation|CMAR|Kangaroo Management Program"""
  //val SURNAME_PREFIXES ="(?:[vV](?:an)(?:[ -](?:den|der) )? ?|von[ -](?:den |der |dem )?|(?:del|Des|De|de|di|Di|da|N)[`' _]|le |d'|D'|de la |Mac|Mc|Le|St\\.? ?|Ou|O')"
  val surname_prefixes = Array ("ben", "da", "Da", "Dal", "de", "De", "del", "Del", "den", "der", "Di", "du", "e", "la", "La", "Le", "Mc", "San", "St", "Ste", "van", "Van", "Vander", "vel", "von", "Von")
  val SURNAME_PREFIX_REGEX="""((?:(?:"""+surname_prefixes.mkString(("|"))+""")(?:[. ]|$)){0,2})"""
  val INITIALS_Surname = ("(?:(?:"+titles+")(?:[. ]|$))?"+initialsRegEx+"[. ]([\\p{Lu}\\p{Ll}'-]*) ?(?:(?:"+titles+")(?:[. ]|$)?)?(?:" +etAl+")?").r  
  val SURNAMEFirstnamePattern = (""""?([\p{Lu}'-]*) ((?:[A-Z][-. ] ?){0,4}) ?([\p{Lu}\p{Ll}']*)(?: """+na+""")?"?""").r
  val SurnamePuncFirstnamePattern = ("\"?" +SURNAME_PREFIX_REGEX +"""([\p{Lu}\p{Ll}'-]*) ?[,] ?(?:(?:"""+titles+""")(?:[. ]|$))? ?((?:[A-Z][-. ] ?){0,4}) ?"""+SURNAME_PREFIX_REGEX+"""([\p{Lu}\p{Ll}']*)? ?([\p{Lu}\p{Ll}']{3,})? ?((?:[A-Z][. ]? ?){0,4})"""+SURNAME_PREFIX_REGEX+"""(?: """ +na+")?\"?").r
  val SINGLE_NAME_PATTERN = ("(?:(?:"+titles+")(?:[. ]|$))?([\\p{Lu}\\p{Ll}']*)").r
  val ORGANISATION_PATTERN = ("((?:.*?)?(?:"+ORGANISATION_WORDS+")(?:.*)?)").r
  val AND = "AND|and|And|&"  
  val COLLECTOR_DELIM = ";|\"\"| - ".r;
  val COMMA_LIST = ",|&".r
  val suffixes = "jr|Jr|JR"  
  val AND_NAME_LISTPattern = ("((?:[A-Z][. ] ?){0,3})(["+NAME_LETTERS+"][\\p{Ll}-']*)? ?(["+NAME_LETTERS+"][\\p{Ll}\\p{Lu}'-]*)? ?" +"(?:"+AND+") ?((?:[A-Z][. ] ?){0,3})(["+NAME_LETTERS+"][\\p{Ll}'-]*)? ?(["+NAME_LETTERS+"][\\p{Ll}\\p{Lu}'-]*)?").r
  val FirstnameSurnamePattern = ("(["+NAME_LETTERS+"][\\p{Ll}']*) ((?:[A-Z][. ] ?){0,4}) ?([\\p{Lu}\\p{Ll}'-]*)? ?(?:"+na+")?").r //"(["+NAME_LETTERS +"][" + name_letters + "?]{1,}" + " ) (["+NAME_LETTERS +"][" + name_letters + "?]{1,}" + " )"
  val unknown = List("\"?ANON  N/A\"?","\"NOT ENTERED[ ]*-[ ]*SEE ORIGINAL DATA[ ]*-[ ]*\"","\\[unknown\\]","Anon.","No data","Unknown","Anonymous","\\?")
  val unknownPattern = ("("+unknown.mkString("|")+")").r

  if(logger.isDebugEnabled){
    logger.debug(FirstnameSurnamePattern.toString)
    logger.debug( SurnamePuncFirstnamePattern.toString())
    logger.debug(unknownPattern.toString)
  }

  def parseForList(stringValue:String) : Option[List[String]] ={
    
    stringValue match {
      case AND_NAME_LISTPattern(initials1,firstName, secondName,initials2,thirdName,forthName) =>{
        if(StringUtils.isEmpty(secondName)){
          if(StringUtils.isEmpty(forthName) && StringUtils.isEmpty(initials1)){            
            //we have 2 surnames
            Some(List(generateName(null,firstName,initials1),generateName(null,thirdName,initials2)))
          }
          else{
            //we have 2 people who share the same surname
            if(StringUtils.isNotEmpty(initials1) && StringUtils.isNotEmpty(initials2))
              Some(List(generateName(null,thirdName,initials1),generateName(null,thirdName,initials2)))
            else
                Some(List(generateName(firstName, forthName, initials1), generateName(thirdName,forthName,initials2)))
          }
        }
        else{
          if(StringUtils.isEmpty(forthName)){
            //first person has 2 names second has a surname
            Some(List(generateName(firstName,secondName,initials1), generateName(null,thirdName,initials2)))
          }
          else
            Some(List(generateName(firstName,secondName,initials1), generateName(thirdName,forthName,initials2)))
        }
      }
      case unknownPattern(value) => Some(List("UNKNOWN OR ANONYMOUS"))
      case _ => {        
        
        //else{
          //check to see if it contains a "collector" delimitter
          var list = COLLECTOR_DELIM.split(stringValue).toList          
          if(list.size>1){
            list = list.map(value =>{
              
              val name = parse(value.trim)
              //println(value.trim + " : " + name)
              if(name.isDefined)
                name.get
               else
                null
            })
          }
          else{
            val res = parse(stringValue)
        if(res.isDefined)
          return Some(List(res.get))
          else{
            //check to see if it contains a comma delimited list - this needs to be done outside the other items due to mixed meaning of comma
            list = COMMA_LIST.split(stringValue).toList
            if(list.size>1){
              list = list.map(value =>{
              val name = parse(value.trim)
              if(name.isDefined)
                name.get
               else
                null
            })
            }
          }
          }
          
//          var list =SURNAMEFirstnamePattern.findAllIn(stringValue).matchData.map(value =>{
//            val Seq(surname,initials, firstname) = 1 to 3 map value.group
//            if(StringUtils.isNotBlank(surname))
//                generateName(firstname, surname, initials)
//            else null
//            }).toList.filter(s =>s != null)
//          if(list.size == 0){
//            list = FirstnameSurnamePattern.findAllIn(stringValue).matchData.map(value =>{
//              val Seq(firstname,initials,surname) = 1 to 3 map value.group
//              if(StringUtils.isNotBlank(surname))
//                  generateName(firstname, surname, initials)
//              else 
//                null
//            }).toList.filter(_ != null)
//          }
//          if(list.size==0)
//            list = SurnamePuncFirstnamePattern.findAllIn(stringValue).matchData.map(value=>{
//              val Seq(surname, initials, firstname,initials2) = 1 to 4 map value.group
//              if(StringUtils.isNotBlank(surname))
//                  generateName(firstname, surname, if(StringUtils.isEmpty(initials)) initials2 else initials)
//              else
//                null
//            }).toList.filter(_ != null)
//            if(list.size == 0)
//              list = INITIALS_Surname.findAllIn(stringValue).matchData.map(value =>{
//                val Seq(initials, surname) = 1 to 2 map value.group
//                if(surname != null)
//                  generateName(null,surname, initials)
//                else
//                  null
//              }).toList.filter( _ != null)
          if(list.size>0)
            Some(list)
          else
            None//Some(List(stringValue))
        //}
      }
    }
    
  }
  def parse(stringValue:String) : Option[String] = {    
    stringValue match{
      case unknownPattern(value) => logger.debug(stringValue + " UNKNOWN PATTERN");Some("UNKNOWN OR ANONYMOUS")
      case ORGANISATION_PATTERN(org) =>logger.debug(stringValue+ " ORGANISTION_PATTERN");Some(org)
      case INITIALS_Surname(initials, surname) => logger.debug(stringValue + " INTIALS SURNAME PATTERN"); Some(generateName(null,surname, initials))
      case SURNAMEFirstnamePattern(surname,initials, firstname) => logger.debug(stringValue + " SURNAME FIRSTNAME PATTERN");Some(generateName(firstname,surname,initials))
      case SurnamePuncFirstnamePattern(prefix,surname, initials, prefix2, firstname, middlename,initials2,prefix3) => logger.debug(stringValue + " SURNAME PUNCT PATTERN");Some(generateName(firstname, surname, if(StringUtils.isEmpty(initials)) initials2 else initials,middlename,if(StringUtils.isNotEmpty(prefix3)) prefix3 else if(StringUtils.isNotEmpty(prefix2))prefix2 else prefix))
      case FirstnameSurnamePattern(firstname,initials,surname) => logger.debug(stringValue + " FIRSTNAME SURNAME PATTERN");Some(generateName(firstname, surname, initials))
      case SINGLE_NAME_PATTERN(surname) =>logger.debug(stringValue +" SINGLENAME PATTERN");Some(generateName(null,surname,null))
      case _ => None
    }
  }
  
  def generateName(firstName:String,surname:String,initials:String, middlename:String=null, surnamePrefix:String = null):String ={
    var name = "";
    if(surnamePrefix != null)
      name += surnamePrefix.trim + " "
    if(surname != null)
      name += org.apache.commons.lang3.text.WordUtils.capitalize(surname.toLowerCase(),'-','\'')
    if(StringUtils.isNotBlank(initials)){
      name += ", " 
      val newinit = initials.trim.replaceAll("[^\\p{Lu}\\p{Ll}-]" ,"")
      //println(newinit)
      newinit.toCharArray().foreach(c=>
        name += c + "."
        )
       name = name.replaceAll("\\.-\\.","-")
      
//      if(!name.endsWith("."))
//        name +="."
    }
    if(StringUtils.isNotBlank(firstName)){
      
      if(StringUtils.isBlank(initials)){
        name += ", "+ firstName.charAt(0).toUpper +"."
        if(StringUtils.isNotBlank(middlename))
          name += middlename.charAt(0).toUpper + "."
      }
      name += " "+ org.apache.commons.lang3.StringUtils.capitalize(firstName.toLowerCase())
    }
    name.trim
  }
  
  
  /*protected static final String NAME_LETTERS = "A-ZÏËÖÜÄÉÈČÁÀÆŒ";
  protected static final String name_letters = "a-zïëöüäåéèčáàæœ";
  protected static final String AUTHOR_LETTERS = NAME_LETTERS + "\\p{Lu}"; // upper case unicode letter, not numerical
// (\W is alphanum)
  protected static final String author_letters = name_letters + "\\p{Ll}";*/
}

object DistanceRangeParser {

  val singleNumber = """(-?[0-9]{1,})""".r
  val decimalNumber = """(-?[0-9]{1,}[.]{1}[0-9]{1,})""".r
  val range = """(-?[0-9.]{1,})([km|m|metres|meters|ft|feet]{0,})-([0-9.]{1,})([km|m|metres|meters]{0,})""".r
  val greaterOrLessThan = """(\>|\<)(-?[0-9.]{1,})([km|m|metres|meters|ft|feet]{0,})""".r
  val metres = """(m|metres|meters)""".r
  val kilometres = """(km|kilometres|kilometers)""".r
  val feet = """(ft|feet|f)""".r
  val singleNumberMetres = """(-?[0-9]{1,})(m|metres|meters)""".r
  val singleNumberKilometres = """(-?[0-9]{1,})(km|kilometres|kilometers)""".r
  val singleNumberFeet = """(-?[0-9]{1,})(ft|feet|f)""".r

  /**
   * Handle these formats:
   * 2000
   * 1km-10km
   * 100m-1000m
   * >10km
   * >100m
   * 100-1000 m
   * @return the value in metres and the original units
   */
  def parse(stringValue:String) : Option[(Float, MeasurementUnit)] = {
    try {
      val normalised =  stringValue.replaceAll("[ ,]", "").toLowerCase.trim
      //remove trailing chars
      normalised match {
        case singleNumber(number) =>  { Some((number.toFloat, Metres)) }
        case singleNumberMetres(number,denom) =>  { Some((number.toFloat, Metres)) }
        case singleNumberKilometres(number,denom) =>  { convertToMetres(denom,number) }
        case singleNumberFeet(number, denom) => { convertToMetres(denom, number)}
        case decimalNumber(number) =>  { Some((number.toFloat, Metres)) }
        case range(firstNumber, denom1, secondNumber, denom2) =>  { convertToMetres(denom2, secondNumber) }
        case greaterOrLessThan(greaterThan, number, denom) =>  { convertToMetres(denom, number) }
        case _ => None
      }
    } catch {
      case _:Exception => None
    }
  }

  def convertToMetres(denom:String, value:String) : Option[(Float, MeasurementUnit)] = {
    try {
      denom match {
        case metres(demon) => { Some((value.toFloat, Metres))  }
        case kilometres(denom) => { Some((value.toFloat * 1000, Kilometres))  }
        case feet(denom) => { Some((value.toFloat * 0.3048f, Feet))}
        case _ => {  Some((value.toFloat, Metres)) }
      }
    } catch {
        case _ :Exception => None
    }
  }
}