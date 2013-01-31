package au.org.ala.biocache

import org.apache.commons.lang.time.DateUtils
import org.apache.commons.lang.time.DateFormatUtils
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import java.util.Date
import java.text.{SimpleDateFormat, ParseException}
import scala.Predef._

object DateUtil {
  def getCurrentYear = DateFormatUtils.format(new Date(), "yyyy").toInt
  def isFutureDate(date:EventDate) = {
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
          if(date != null && date.after(new Date())){
              true
          }else{
              false
          }         
      }
      else{
          false
      }
//    val date = DateUtils.parseDate(str, Array("yyyy-MM-dd"))
//    if (date!=null && date.after(new Date())){
//      true
//    } else {
//      false
//    }
  }
}

/**
 * Date parser that uses scala extractors to handle the different formats.
 */
object DateParser {

  final val logger: Logger = LoggerFactory.getLogger("DateParser")

  def parseDate(dateStr: String, maxYear: Option[Int] = None, minYear: Option[Int] = None): Option[EventDate] = {

    val dateStrNormalised = dateStr.trim
    //assume ISO
    val eventDateWithOption = parseISODate(dateStrNormalised)

    //if max year set, validate
    eventDateWithOption match {
      case Some(eventDate) => {
        if (!isValid(eventDate)) {
          val secondAttempt = parseNonISODate(dateStrNormalised)
          if(!secondAttempt.isEmpty && isValid(secondAttempt.get)){
            secondAttempt
          } else {
            None
          }
        } else {
          eventDateWithOption
        }
      }
      case None => None
    }
  }

  /**
   * Parses a ISO Date time string to a Date
   */
  def parseStringToDate(date: String): Option[Date] = {
    try {
      if(date == "")
        None
      else
        Some(DateUtils.parseDate(date,
          Array("yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss")))
    }
    catch {
      case _ => None
    }
  }

  /**
   * Handles these formats (taken from Darwin Core specification):
   *
   * 1963-03-08T14:07-0600" is 8 Mar 1963 2:07pm in the time zone six hours earlier than UTC,
   * "2009-02-20T08:40Z" is 20 Feb 2009 8:40am UTC, "1809-02-12" is 12 Feb 1809,
   * "1906-06" is Jun 1906, "1971" is just that year,
   * "2007-03-01T13:00:00Z/2008-05-11T15:30:00Z" is the interval between 1 Mar 2007 1pm UTC and
   * 11 May 2008 3:30pm UTC, "2007-11-13/15" is the interval between 13 Nov 2007 and 15 Nov 2007
   *
   * 2005-06-12 00:00:00.0/2005-06-12 00:00:00.0 
   */
  def parseISODate(date: String): Option[EventDate] = {

    date match {
      case ISOSingleDate(date) => Some(date)
      case ISOSingleYear(date) => Some(date)
      case ISOWithMonthNameDate(date) => Some(date)
      case ISODateRange(date) => Some(date)
      case ISODayDateRange(date) => Some(date)
      case ISODayMonthRange(date)=>Some(date)
      case ISODateTimeRange(date) => Some(date)
      case ISOMonthDate(date) => Some(date)
      case ISOMonthDateRange(date) => Some(date)
      case ISOMonthYearDateRange(date) => Some(date)
      case ISOYearRange(date) => Some(date)
      case ISOVerboseDateTime(date) => Some(date)
      case ISOVerboseDateTimeRange(date) => Some(date)
      case _ => None
    }
  }

  def parseNonISODate(date: String): Option[EventDate] = {

    date match {
      case NonISOSingleDate(date) => Some(date)
      case _ => None
    }
  }

  def isValid(eventDate: EventDate): Boolean = {

    val currentYear = DateUtil.getCurrentYear

    if (eventDate.startYear != null) {
      val year = eventDate.startYear.toInt
      if (year > currentYear) return false
    }

    if (eventDate.endYear != null) {
      val year = eventDate.endYear.toInt
      if (year < 1600) return false
    }

    if (eventDate.startYear != null && eventDate.endYear != null) {
      val startYear = eventDate.startYear.toInt
      val endYear = eventDate.endYear.toInt
      if (startYear > endYear) return false
    }
    true
  }
}

case class EventDate(startDate: String, startDay: String, startMonth: String, startYear: String,
                     endDate: String, endDay: String, endMonth: String, endYear: String, singleDate: Boolean)

/**yyyy-MM-dd */
object ISOWithMonthNameDate /*extends (String=>Option[EventDate]) */ {

  /**
   * Extraction method
   */
  def unapply(str: String): Option[EventDate] = {
    try {
      val eventDateParsed = DateUtils.parseDate(str,
        Array("yyyy-MMMMM-dd", "yyyy-MMMMM-dd'T'hh:mm-ss", "yyyy-MMMMM-dd'T'hh:mm'Z'"))

      val startDate, endDate = DateFormatUtils.format(eventDateParsed, "yyyy-MM-dd")
      val startDay, endDay = DateFormatUtils.format(eventDateParsed, "dd")
      val startMonth, endMonth = DateFormatUtils.format(eventDateParsed, "MM")
      val startYear, endYear = DateFormatUtils.format(eventDateParsed, "yyyy")

      Some(EventDate(startDate, startDay, startMonth, startYear, endDate, endDay,
        endMonth: String, endYear, true))
    } catch {
      case e: ParseException => None
    }
  }
}

/**yyyy-MM-dd */
object ISOSingleYear {

  def formats = Array("yyyy")

  /**
   * Extraction method
   */
  def unapply(str: String): Option[EventDate] = {
    try {
      val eventDateParsed = DateUtils.parseDate(str,formats)
      val startYear, endYear = DateFormatUtils.format(eventDateParsed, "yyyy")
      val startDate, endDate = ""
      val startDay, endDay = ""
      val startMonth, endMonth = ""
      Some(EventDate(startDate, startDay, startMonth, startYear, endDate, endDay,
        endMonth: String, endYear, false))
    } catch {
      case e: ParseException => None
    }
  }
}

/**yyyy-MM-dd */
class SingleDate {

  def baseFormats = Array("yyyy-MM-dd","yyyy/MM/dd")

//  2001-03-14T00:00:00+11:00
  def formats = baseFormats.map(f => Array(f, f + "'Z'", f + "'T'hh:mm'Z'", f + "'T'hh:mm:ss", f + "'T'hh:mm:ssZ",f + " hh:mm:ss")).flatten

  /**
   * Extraction method
   */
  def unapply(str: String): Option[EventDate] = {
    try {
      val eventDateParsed = DateUtils.parseDate(str,formats)
      val startYear, endYear = DateFormatUtils.format(eventDateParsed, "yyyy")
      val startDate, endDate = DateFormatUtils.format(eventDateParsed, "yyyy-MM-dd")
      val startDay, endDay = DateFormatUtils.format(eventDateParsed, "dd")
      val startMonth, endMonth = DateFormatUtils.format(eventDateParsed, "MM")
      Some(EventDate(startDate, startDay, startMonth, startYear, endDate, endDay,
        endMonth: String, endYear, true))
    } catch {
      case e: ParseException => None
    }
  }
}

trait NonISO extends SingleDate {
  override def baseFormats = Array("dd-MM-yyyy","dd/MM/yyyy","dd-MMM-yyyy","dd/MMM/yyyy","dd MMM yyyy")
}

object ISOSingleDate extends SingleDate

object NonISOSingleDate extends SingleDate with NonISO

/**yyyy-MM-dd */
object ISOMonthDate {

  /**
   * Extraction method
   */
  def unapply(str: String): Option[EventDate] = {
    try {
      val eventDateParsed = DateUtils.parseDate(str,
        Array("yyyy-MM", "yyyy-MM-", "MM yyyy", "MMM-yyyy"))

      val startDate, endDate = ""
      val startDay, endDay = ""
      val startMonth, endMonth = DateFormatUtils.format(eventDateParsed, "MM")
      val startYear, endYear = DateFormatUtils.format(eventDateParsed, "yyyy")

      Some(EventDate(startDate, startDay, startMonth, startYear, endDate, endDay,
        endMonth: String, endYear, true))
    } catch {
      case e: ParseException => None
    }
  }
}

/**yyyy-MM-dd/yyyy-MM-dd */
object ISODateRange {

  val formats = Array("yyyy-MM-dd", "yyyy-MM-dd'T'hh:mm-ss", "yyyy-MM-dd'T'hh:mm'Z'")

  def unapply(str: String): Option[EventDate] = {
    try {

      val parts = str.split("/")
      if (parts.length != 2) return None
      val startDateParsed = DateUtils.parseDate(parts(0), formats)
      val endDateParsed = DateUtils.parseDate(parts(1), formats)

      val startDate = DateFormatUtils.format(startDateParsed, "yyyy-MM-dd")
      val endDate = DateFormatUtils.format(endDateParsed, "yyyy-MM-dd")
      val startDay = DateFormatUtils.format(startDateParsed, "dd")
      val endDay = DateFormatUtils.format(endDateParsed, "dd")
      val startMonth = DateFormatUtils.format(startDateParsed, "MM")
      val endMonth = DateFormatUtils.format(endDateParsed, "MM")
      val startYear = DateFormatUtils.format(startDateParsed, "yyyy")
      val endYear = DateFormatUtils.format(endDateParsed, "yyyy")

      Some(EventDate(startDate, startDay, startMonth, startYear, endDate, endDay,
        endMonth: String, endYear, startDate.equals(endDate)))
    } catch {
      case e: ParseException => None
    }
  }
}

/**yyyy-MM/yyyy-MM */
object ISOMonthYearDateRange {

  def unapply(str: String): Option[EventDate] = {
    try {
      val parts = str.split("/")
      if (parts.length != 2) return None
      val startDateParsed = DateUtils.parseDate(parts(0),
        Array("yyyy-MM", "yyyy-MM-"))
      val endDateParsed = DateUtils.parseDate(parts(1),
        Array("yyyy-MM", "yyyy-MM-"))

      val startDate, endDate = ""
      val startDay, endDay = ""
      val startMonth = DateFormatUtils.format(startDateParsed, "MM")
      val endMonth = DateFormatUtils.format(endDateParsed, "MM")
      val startYear = DateFormatUtils.format(startDateParsed, "yyyy")
      val endYear = DateFormatUtils.format(endDateParsed, "yyyy")

      val singleDate = (startMonth equals endMonth) && (startYear equals endYear)

      Some(EventDate(startDate, startDay, startMonth, startYear,
        endDate, endDay, endMonth: String, endYear, singleDate))
    } catch {
      case e: ParseException => None
    }
  }
}

/**yyyy-MM/MM */
object ISOMonthDateRange {

  def unapply(str: String): Option[EventDate] = {
    try {
      val parts = str.split("/")
      if (parts.length != 2) return None
      val startDateParsed = DateUtils.parseDate(parts(0),
        Array("yyyy-MM", "yyyy-MM-"))
      val endDateParsed = DateUtils.parseDate(parts(1),
        Array("MM", "MM-"))

      val startDate, endDate = ""
      val startDay, endDay = ""
      val startMonth = DateFormatUtils.format(startDateParsed, "MM")
      val endMonth = DateFormatUtils.format(endDateParsed, "MM")
      val startYear, endYear = DateFormatUtils.format(startDateParsed, "yyyy")

      Some(EventDate(startDate, startDay, startMonth, startYear,
        endDate, endDay, endMonth: String, endYear, startMonth equals endMonth))
    } catch {
      case e: ParseException => None
    }
  }
}

/**yyyy-MM-dd/dd */
object ISODateTimeRange {

  def unapply(str: String): Option[EventDate] = {
    try {
      val parts = str.split("/")
      if (parts.length != 2) return None
      val startDateParsed = DateUtils.parseDate(parts(0),
        Array("yyyy-MM-dd hh:mm:ss.sss"))
      val endDateParsed = DateUtils.parseDate(parts(1),
        Array("yyyy-MM-dd hh:mm:ss.sss"))

      val startDate = DateFormatUtils.format(startDateParsed, "yyyy-MM-dd")
      val endDate = DateFormatUtils.format(endDateParsed, "yyyy-MM-dd")
      val startDay = DateFormatUtils.format(startDateParsed, "dd")
      val endDay = DateFormatUtils.format(endDateParsed, "dd")
      val startMonth = DateFormatUtils.format(startDateParsed, "MM")
      val endMonth = DateFormatUtils.format(endDateParsed, "MM")
      val startYear = DateFormatUtils.format(startDateParsed, "yyyy")
      val endYear = DateFormatUtils.format(endDateParsed, "yyyy")

      Some(EventDate(startDate, startDay, startMonth, startYear,
        endDate, endDay, endMonth: String, endYear, startDate.equals(endDate)))
    } catch {
      case e: ParseException => None
    }
  }
}

//Fri Aug 12 15:19:20 EST 2011
object ISOVerboseDateTime {

  def unapply(str: String): Option[EventDate] = {
    try {

      val eventDateParsed = DateUtils.parseDate(str,
        Array("EEE MMM dd hh:mm:ss zzz yyyy"))

      val startYear, endYear = DateFormatUtils.format(eventDateParsed, "yyyy")
      val startDate, endDate = DateFormatUtils.format(eventDateParsed, "yyyy-MM-dd")
      val startDay, endDay = DateFormatUtils.format(eventDateParsed, "dd")
      val startMonth, endMonth = DateFormatUtils.format(eventDateParsed, "MM")

      Some(EventDate(startDate, startDay, startMonth, startYear,
        endDate, endDay, endMonth: String, endYear, true))

    } catch {
      case e: ParseException => None
    }
  }
}

//Mon Apr 23 00:00:00 EST 1984/Sun Apr 29 00:00:00 EST 1984
object ISOVerboseDateTimeRange {

  def unapply(str: String): Option[EventDate] = {
    try {
      val parts = str.split("/")
      if (parts.length != 2) return None
      val startDateParsed = DateUtils.parseDate(parts(0),
        Array("EEE MMM dd hh:mm:ss zzz yyyy"))
      val endDateParsed = DateUtils.parseDate(parts(1),
        Array("EEE MMM dd hh:mm:ss zzz yyyy"))

      val startDate = DateFormatUtils.format(startDateParsed, "yyyy-MM-dd")
      val endDate = DateFormatUtils.format(endDateParsed, "yyyy-MM-dd")
      val startDay = DateFormatUtils.format(startDateParsed, "dd")
      val endDay = DateFormatUtils.format(endDateParsed, "dd")
      val startMonth = DateFormatUtils.format(startDateParsed, "MM")
      val endMonth = DateFormatUtils.format(endDateParsed, "MM")
      val startYear = DateFormatUtils.format(startDateParsed, "yyyy")
      val endYear = DateFormatUtils.format(endDateParsed, "yyyy")

      Some(EventDate(startDate, startDay, startMonth, startYear,
        endDate, endDay, endMonth: String, endYear, startDate.equals(endDate)))
    } catch {
      case e: ParseException => None
    }
  }
}


/**yyyy-MM-dd/MM-dd */
object ISODayMonthRange {

  def unapply(str: String): Option[EventDate] = {
    try {
      val parts = str.split("/")
      if (parts.length != 2) return None
      val startDateParsed = DateUtils.parseDate(parts(0),
        Array("yyyy-MM-dd"))
      val endDateParsed = DateUtils.parseDate(parts(1),
        Array("MM-dd"))

      val startDate = DateFormatUtils.format(startDateParsed, "yyyy-MM-dd")
      val startDay = DateFormatUtils.format(startDateParsed, "dd")
      val endDay = DateFormatUtils.format(endDateParsed, "dd")
      val startMonth = DateFormatUtils.format(startDateParsed, "MM")
      val endMonth = DateFormatUtils.format(endDateParsed, "MM")
      val startYear, endYear = DateFormatUtils.format(startDateParsed, "yyyy")
      val endDate = endYear + '-' + endMonth + '-' + endDay

      Some(EventDate(startDate, startDay, startMonth, startYear,
        endDate, endDay, endMonth: String, endYear, startDate.equals(endDate)))
    } catch {
      case e: ParseException => None
    }
  }
}

/**yyyy-MM-dd/dd */
object ISODayDateRange {

  def unapply(str: String): Option[EventDate] = {
    try {
      val parts = str.split("/")
      if (parts.length != 2) return None
      val startDateParsed = DateUtils.parseDate(parts(0),
        Array("yyyy-MM-dd"))
      val endDateParsed = DateUtils.parseDate(parts(1),
        Array("dd"))

      val startDate = DateFormatUtils.format(startDateParsed, "yyyy-MM-dd")
      val startDay = DateFormatUtils.format(startDateParsed, "dd")
      val endDay = DateFormatUtils.format(endDateParsed, "dd")
      val startMonth, endMonth = DateFormatUtils.format(startDateParsed, "MM")
      val startYear, endYear = DateFormatUtils.format(startDateParsed, "yyyy")
      val endDate = endYear + '-' + endMonth + '-' + endDay

      Some(EventDate(startDate, startDay, startMonth, startYear,
        endDate, endDay, endMonth: String, endYear, startDate.equals(endDate)))
    } catch {
      case e: ParseException => None
    }
  }
}

/**yyyy/yyyy and yyyy/yy and yyyy/y*/
object ISOYearRange {

  def unapply(str: String): Option[EventDate] = {
    try {
      val parts = str.split("/")
      if (parts.length != 2) return None
      val startDateParsed = DateUtils.parseDate(parts(0),
        Array("yyyy"))
      val startDate, endDate = ""
      val startDay, endDay = ""
      val startMonth, endMonth = ""
      val startYear = DateFormatUtils.format(startDateParsed, "yyyy")
      val endYear = {
        if (parts.length == 2) {
          val endDateParsed = DateUtils.parseDate(parts(1),
            Array("yyyy", "yy", "y"))

          if (parts(1).length == 1) {
            val decade = (startYear.toInt / 10).toString
            decade + parts(1)
          } else if (parts(1).length == 2) {
            val century = (startYear.toInt / 100).toString
            century + parts(1)
          } else if (parts(1).length == 3) {
            val millen = (startYear.toInt / 1000).toString
            millen + parts(1)
          } else {
            parts(1)
          }
        } else {
          parts(0)
        }
      }
      Some(EventDate(startDate, startDay, startMonth, startYear,
        endDate, endDay, endMonth: String, endYear, false))
    } catch {
      case e: ParseException => None
    }
  }
}