package au.org.ala.biocache
import org.apache.commons.lang.time.DateUtils
import org.apache.commons.lang.time.DateFormatUtils
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import java.text.ParseException
import java.util.Date

object DateUtil {
    
    def getCurrentYear =  DateFormatUtils.format(new Date(), "yyyy").toInt
}

/**
 * Date parser that uses scala extractors to handle the different formats.
 */
object DateParser {

 final val logger:Logger = LoggerFactory.getLogger("DateParser")
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
   * 
   * 
   */
  def parseDate(date:String) : Option[EventDate] = {
    date match {
      case ISODate(date) =>  Some(date)
      case ISOWithMonthNameDate(date) => Some(date)
      case ISODateRange(date) =>  Some(date)
      case ISODayDateRange(date) =>  Some(date)
      case ISODateTimeRange(date) =>  Some(date)
      case ISOMonthDate(date) =>  Some(date)
      case ISOMonthDateRange(date) =>  Some(date)
      case ISOMonthYearDateRange(date) =>  Some(date)
      case ISOYearRange(date) =>  Some(date)
      case _ => None
    }
  }
 
  def isValid(eventDate:EventDate) : Boolean = {
      
      val today = new Date();
      val currentYear = DateUtil.getCurrentYear
      
      if(eventDate.startYear != null){
          val year = eventDate.startYear.toInt
          if(year > currentYear) return false
      }

      if(eventDate.endYear != null){
          val year = eventDate.endYear.toInt
          if(year < 1600) return false
      }

      if(eventDate.startYear != null && eventDate.endYear != null){
          val startYear = eventDate.startYear.toInt
          val endYear = eventDate.endYear.toInt
          if(startYear> endYear) return false
      }
      true
  }
}

case class EventDate(startDate:String,startDay:String,startMonth:String,startYear:String,
    endDate:String,endDay:String,endMonth:String,endYear:String,singleDate:Boolean)

/** yyyy-MM-dd */
object ISOWithMonthNameDate /*extends (String=>Option[EventDate]) */{

  /**
   * Extraction method
   */
  def unapply(str:String) : Option[EventDate] = {
   try{
       val eventDateParsed = DateUtils.parseDate(str,
          Array("yyyy-MMMMM-dd", "yyyy-MMMMM-dd'T'hh:mm-ss", "yyyy-MMMMM-dd'T'hh:mm'Z'"))

       val startDate, endDate = DateFormatUtils.format(eventDateParsed, "yyyy-MM-dd")
       val startDay, endDay = DateFormatUtils.format(eventDateParsed, "dd")
       val startMonth, endMonth = DateFormatUtils.format(eventDateParsed, "MM")
       val startYear, endYear = DateFormatUtils.format(eventDateParsed, "yyyy")

       Some(EventDate(startDate,startDay,startMonth,startYear,endDate,endDay,
           endMonth:String,endYear,true))
     } catch {
      case e:ParseException => None
    }
  }
}


/** yyyy-MM-dd */
object ISODate /*extends (String=>Option[EventDate]) */{ 

  /**
   * Extraction method
   */
  def unapply(str:String) : Option[EventDate] = {
   try{
       val eventDateParsed = DateUtils.parseDate(str,
          Array("yyyy-MM-dd", "yyyy-MM-dd'T'hh:mm-ss", "yyyy-MM-dd'T'hh:mm'Z'",
            "yyyy/MM/dd", "dd-MM-yyyy", "dd/MM/yyyy"))

       val startDate, endDate = DateFormatUtils.format(eventDateParsed, "yyyy-MM-dd")
       val startDay, endDay = DateFormatUtils.format(eventDateParsed, "dd")
       val startMonth, endMonth = DateFormatUtils.format(eventDateParsed, "MM")
       val startYear, endYear = DateFormatUtils.format(eventDateParsed, "yyyy")

       Some(EventDate(startDate,startDay,startMonth,startYear,endDate,endDay,
           endMonth:String,endYear,true))
     } catch {
      case e:ParseException => None
    }
  }
}

/** yyyy-MM-dd */
object ISOMonthDate { 

  /**
   * Extraction method
   */
  def unapply(str:String) : Option[EventDate] = {
   try{
       val eventDateParsed = DateUtils.parseDate(str,
          Array("yyyy-MM","yyyy-MM-"))

       val startDate, endDate = ""
       val startDay, endDay = ""
       val startMonth, endMonth = DateFormatUtils.format(eventDateParsed, "MM")
       val startYear, endYear = DateFormatUtils.format(eventDateParsed, "yyyy")

       Some(EventDate(startDate,startDay,startMonth,startYear,endDate,endDay,
           endMonth:String,endYear,true))
     } catch {
      case e:ParseException => None
    }
  }
}

/** yyyy-MM-dd/yyyy-MM-dd */
object ISODateRange {

  val formats = Array("yyyy-MM-dd", "yyyy-MM-dd'T'hh:mm-ss", "yyyy-MM-dd'T'hh:mm'Z'")

  def unapply(str:String) : Option[EventDate] = {
   try{

       val parts = str.split("/")
       val startDateParsed = DateUtils.parseDate(parts(0),formats)
       val endDateParsed = DateUtils.parseDate(parts(1),formats)

       val startDate = DateFormatUtils.format(startDateParsed, "yyyy-MM-dd")
       val endDate = DateFormatUtils.format(endDateParsed, "yyyy-MM-dd")
       val startDay = DateFormatUtils.format(startDateParsed, "dd")
       val endDay = DateFormatUtils.format(endDateParsed, "dd")
       val startMonth = DateFormatUtils.format(startDateParsed, "MM")
       val endMonth = DateFormatUtils.format(endDateParsed, "MM")
       val startYear = DateFormatUtils.format(startDateParsed, "yyyy")
       val endYear = DateFormatUtils.format(endDateParsed, "yyyy")

       Some(EventDate(startDate,startDay,startMonth,startYear,endDate,endDay,
           endMonth:String,endYear,startDate.equals(endDate)))
     } catch {
      case e:ParseException => None
    }
  }
}

/** yyyy-MM/yyyy-MM */
object ISOMonthYearDateRange {

  def unapply(str:String) : Option[EventDate] = {
   try{
       val parts = str.split("/")
       val startDateParsed = DateUtils.parseDate(parts(0),
          Array("yyyy-MM", "yyyy-MM-"))
       val endDateParsed = DateUtils.parseDate(parts(1),
          Array("yyyy-MM", "yyyy-MM-"))

       val startDate, endDate = ""
       val startDay,endDay = ""
       val startMonth = DateFormatUtils.format(startDateParsed, "MM")
       val endMonth = DateFormatUtils.format(endDateParsed, "MM")
       val startYear = DateFormatUtils.format(startDateParsed, "yyyy")
       val endYear = DateFormatUtils.format(endDateParsed, "yyyy")

       val singleDate = (startMonth equals endMonth) && (startYear equals endYear)

       Some(EventDate(startDate,startDay,startMonth,startYear, 
           endDate,endDay,endMonth:String,endYear,singleDate))
     } catch {
      case e:ParseException => None
    }
  }
}

/** yyyy-MM/MM */
object ISOMonthDateRange {

  def unapply(str:String) : Option[EventDate] = {
   try{
       val parts = str.split("/")
       val startDateParsed = DateUtils.parseDate(parts(0),
          Array("yyyy-MM", "yyyy-MM-"))
       val endDateParsed = DateUtils.parseDate(parts(1),
          Array("MM", "MM-"))

       val startDate, endDate = ""
       val startDay, endDay = ""
       val startMonth = DateFormatUtils.format(startDateParsed, "MM")
       val endMonth = DateFormatUtils.format(endDateParsed, "MM")
       val startYear, endYear = DateFormatUtils.format(startDateParsed, "yyyy")

       Some(EventDate(startDate,startDay,startMonth,startYear,
           endDate,endDay,endMonth:String,endYear,startMonth equals endMonth))
     } catch {
      case e:ParseException => None
    }
  }
}

/** yyyy-MM-dd/dd */
object ISODateTimeRange {

  def unapply(str:String) : Option[EventDate] = {
   try{
       val parts = str.split("/")
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

       Some(EventDate(startDate,startDay,startMonth,startYear,
           endDate,endDay,endMonth:String,endYear,startDate.equals(endDate)))
     } catch {
      case e:ParseException => None
    }
  }
}


/** yyyy-MM-dd/dd */
object ISODayDateRange {

  def unapply(str:String) : Option[EventDate] = {
   try{
       val parts = str.split("/")
       val startDateParsed = DateUtils.parseDate(parts(0),
          Array("yyyy-MM-dd"))
       val endDateParsed = DateUtils.parseDate(parts(1),
          Array("dd"))

       val startDate = DateFormatUtils.format(startDateParsed, "yyyy-MM-dd")
       val startDay = DateFormatUtils.format(startDateParsed, "dd")
       val endDay = DateFormatUtils.format(endDateParsed, "dd")
       val startMonth, endMonth = DateFormatUtils.format(startDateParsed, "MM")
       val startYear,endYear = DateFormatUtils.format(startDateParsed, "yyyy")
       val endDate = endYear+'-'+endMonth+'-'+endDay

       Some(EventDate(startDate,startDay,startMonth,startYear,
           endDate,endDay,endMonth:String,endYear,startDate.equals(endDate)))
     } catch {
      case e:ParseException => None
    }
  }
}

/** yyyy/yyyy and yyyy/yy and yyyy/y*/
object ISOYearRange {

  def unapply(str:String) : Option[EventDate] = {
   try{
       val parts = str.split("/")
       val startDateParsed = DateUtils.parseDate(parts(0),
          Array("yyyy"))
       val startDate, endDate = ""
       val startDay,endDay = ""
       val startMonth, endMonth = ""
       val startYear = DateFormatUtils.format(startDateParsed, "yyyy")
       val endYear = {
         if(parts.length==2){
             val endDateParsed = DateUtils.parseDate(parts(1),
                Array("yyyy", "yy", "y"))

             if(parts(1).length==1){
               val decade = (startYear.toInt / 10).toString
               decade + parts(1)
             } else if(parts(1).length==2) {
               val century = (startYear.toInt / 100).toString
               century + parts(1)
             } else if(parts(1).length==3) {
               val millen = (startYear.toInt / 1000).toString
               millen + parts(1)
             } else {
               parts(1)
             }
         } else {
             parts(0)
         }
       }
       Some(EventDate(startDate,startDay,startMonth,startYear,
           endDate,endDay,endMonth:String,endYear,false))
     } catch {
      case e:ParseException => None
    }
  }
}