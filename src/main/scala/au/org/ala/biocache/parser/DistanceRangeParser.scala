package au.org.ala.biocache.parser

import au.org.ala.biocache.model.{Kilometres, Feet, Metres, MeasurementUnit}

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