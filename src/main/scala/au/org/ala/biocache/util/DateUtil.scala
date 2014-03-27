package au.org.ala.biocache.util

import org.apache.commons.lang.time.{DateUtils, DateFormatUtils}
import java.util.Date
import au.org.ala.biocache.parser.EventDate

/**
 * Date util
 */
object DateUtil {

  def getCurrentYear = DateFormatUtils.format(new Date(), "yyyy").toInt

  def isFutureDate(date:EventDate) : Boolean = {
    val (str, format):(String, Array[String]) ={
      date match{
        case dt if dt.startDate != "" => (dt.startDate, Array("yyyy-MM-dd"))
        case dt if dt.startYear!= "" && dt.startMonth != "" => (dt.startYear +"-" + dt.startMonth, Array("yyyy-MM"))
        case dt if dt.startYear != "" => (dt.startYear, Array("yyyy"))
        case _ => (null, Array())
      }
    }
    //check for future date
    if(str != null){
      val date = DateUtils.parseDate(str, format)
      date != null && date.after(new Date())
    } else {
      false
    }
  }
}
