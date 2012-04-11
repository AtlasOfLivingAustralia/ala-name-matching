package au.org.ala.biocache

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

import java.util.regex.Pattern
import au.org.ala.biocache.LatOrLong
/**
 * Parser for coordinates in deg, min sec format
 */
object VerbatimLatLongParser {

  import LatOrLong._
  //    16° 52' 37" S
  //    37º 10' 48" S
  val verbatimPattern = """(?:[\\-])?([0-9]{1,3})([d|deg|degree|degrees|°|º][ ]*)([0-9]{1,2})?([m|min|minutes|minute|'][ ]*)([0-9]{1,2}.?[0-9]{0,})?(["|']{1,2}[ ]*)?(s|south|n|north|w|west|e|east)""".r
  val verbatimPatternNoDenom = """(?:[\\-])?([0-9]{1,3})(?:[ ]*)?([0-9]{1,2})?(?:[ ]*)?([0-9]{1,2}.?[0-9]{0,})?(?:"[ ]*)?(s|south|n|north|w|west|e|east)""".r
  val negativePattern = "(s|south|w|west)".r

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
      case _ =>None
    }
  }
}

object DistanceRangeParser {

   val singleNumber = """([0-9]{1,})""".r
   val decimalNumber = """([0-9]{1,}[.]{1}[0-9]{1,})""".r
   val range = """([0-9.]{1,})([km|m|metres|meters]{0,})-([0-9.]{1,})([km|m|metres|meters]{0,})""".r
   val greaterOrLessThan = """(\>|\<)([0-9.]{1,})([km|m|metres|meters]{0,})""".r
   val metres = """(m|metres|meters)""".r
   val kilometres = """(km|kilometres|kilometers)""".r

   val singleNumberMetres = """([0-9]{1,})(m|metres|meters)""".r
   val singleNumberKilometres = """([0-9]{1,})(km|kilometres|kilometers)""".r
   
   
  /**
   * Handle these formats:
   * 2000
   * 1km-10km
   * 100m-1000m
   * >10km
   * >100m
   * 100-1000 m
   */
  def parse(stringValue:String) : Option[Float] = {
    try {
        val normalised =  stringValue.replaceAll("[ ,]", "").toLowerCase.trim
        //remove trailing chars
        normalised match {
            case singleNumber(number) =>  { Some(number.toFloat) }
            case singleNumberMetres(number,denom) =>  { Some(number.toFloat) }
            case singleNumberKilometres(number,denom) =>  { convertToMetres(denom,number) }
            case decimalNumber(number) =>  { Some(number.toFloat) }
            case range(firstNumber, denom1, secondNumber, denom2) =>  { convertToMetres(denom2, secondNumber) }
            case greaterOrLessThan(greaterThan, number, denom) =>  { convertToMetres(denom, number) }
            case _ => None
        }
    } catch {
        case _ => None
    }
  }

  def convertToMetres(denom:String, value:String) : Option[Float] = {
      try {
        denom match {
            case metres(demon) => { Some(value.toFloat)  }
            case kilometres(denom) => {Some(value.toFloat * 1000)  }
            case _ => {  Some(value.toFloat) }
        }
      } catch {
          case _ => None
      }
  }
}