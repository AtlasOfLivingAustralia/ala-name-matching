package au.org.ala.biocache.parser

import au.org.ala.biocache.parser.LatOrLong.LatOrLong

/**
 * Parser for coordinates in deg, min sec format
 */
object VerbatimLatLongParser {

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



