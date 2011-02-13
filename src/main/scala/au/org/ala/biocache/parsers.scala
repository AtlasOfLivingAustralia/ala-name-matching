package au.org.ala.biocache

import java.util.regex.Pattern

object DistanceRangeParser {

   val singleNumber = """([0-9]{1,})""".r
   val decimalNumber = """([0-9]{1,}[.]{1}[0-9]{1,})""".r
   val range = """([0-9.]{1,})([km|m|metres|meters]{0,})-([0-9.]{1,})([km|m|metres|meters]{0,})""".r
   val greaterOrLessThan = """(\>|\<)([0-9.]{1,})([km|m|metres|meters]{0,})""".r
   val metres = """(m|metres|meters)""".r
   val kilometres = """(km|kilometres|kilometers)""".r

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