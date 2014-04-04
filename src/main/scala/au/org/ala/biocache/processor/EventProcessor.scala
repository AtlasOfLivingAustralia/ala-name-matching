package au.org.ala.biocache.processor

import scala.collection.mutable.ArrayBuffer
import org.apache.commons.lang.time.{DateFormatUtils, DateUtils}
import java.util.{GregorianCalendar, Date}
import org.apache.commons.lang.StringUtils
import scala.Some
import au.org.ala.biocache.parser.DateParser
import au.org.ala.biocache.model.{QualityAssertion, FullRecord}
import au.org.ala.biocache.vocab.AssertionCodes
import au.org.ala.biocache.util.{DateUtil, StringHelper}

/**
 * Processor for event (date) information.
 */
class EventProcessor extends Processor {

  import StringHelper._

  /**
   * Validate the supplied number using the supplied function.
   */
  def validateNumber(number: String, f: (Int => Boolean)): (Int, Boolean) = {
    try {
      if (number != null) {
        val parsedNumber = number.toInt
        (parsedNumber, f(parsedNumber))
      } else {
        (-1, false)
      }
    } catch {
      case e: NumberFormatException => (-1, false)
    }
  }

  /**
   * Date parsing
   *
   * TODO needs splitting into several methods
   */
  def process(guid: String, raw: FullRecord, processed: FullRecord, lastProcessed: Option[FullRecord]=None): Array[QualityAssertion] = {
    var assertions = new ArrayBuffer[QualityAssertion]
    if ((raw.event.day == null || raw.event.day.isEmpty)
      && (raw.event.month == null || raw.event.month.isEmpty)
      && (raw.event.year == null || raw.event.year.isEmpty)
      && (raw.event.eventDate == null || raw.event.eventDate.isEmpty)
      && (raw.event.verbatimEventDate == null || raw.event.verbatimEventDate.isEmpty)
    ){
      assertions += QualityAssertion(AssertionCodes.MISSING_COLLECTION_DATE, "No date information supplied")
    } else {

      var date: Option[java.util.Date] = None
      val currentYear = DateUtil.getCurrentYear
      var comment = ""
      var addPassedInvalidCollectionDate=true
      var dateComplete=false

      var (year, validYear) = validateNumber(raw.event.year, {
        year => year > 0 && year <= currentYear
      })
      var (month, validMonth) = validateNumber(raw.event.month, {
        month => month >= 1 && month <= 12
      })
      var (day, validDay) = validateNumber(raw.event.day, {
        day => day >= 1 && day <= 31
      })
      //check month and day not transposed
      if (!validMonth && raw.event.month.isInt && raw.event.day.isInt) {
        //are day and month transposed?
        val monthValue = raw.event.month.toInt
        val dayValue = raw.event.day.toInt
        if (monthValue > 12 && dayValue < 12) {
          month = dayValue
          day = monthValue
          assertions += QualityAssertion(AssertionCodes.DAY_MONTH_TRANSPOSED, "Assume day and month transposed")
          //assertions += QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE,1) //this one is not applied
          validMonth = true
        } else {
          assertions += QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE, "Invalid month supplied")
          addPassedInvalidCollectionDate = false
          assertions += QualityAssertion(AssertionCodes.DAY_MONTH_TRANSPOSED, 1) //this one has been tested and does not apply
        }
      }

      //TODO need to check for other months
      if (day == 0 || day > 31) {
        assertions += QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE, "Invalid day supplied")
        addPassedInvalidCollectionDate = false
      }

      //check for sensible year value
      if (year > 0) {
        val (newComment,newValidYear,newYear) = runYearValidation(year,currentYear,day,month)
        comment = newComment
        validYear = newValidYear
        year = newYear

        if (StringUtils.isNotEmpty(comment)){
          assertions += QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE, comment)
          addPassedInvalidCollectionDate=false
        }
      }

      var validDayMonthYear = validYear && validDay && validMonth

      //construct
      if (validDayMonthYear) {
        try {

          val calendar = new GregorianCalendar(
            year.toInt,
            month.toInt - 1,
            day.toInt
          );
          //don't allow the calendar to be lenient we want exceptions with incorrect dates
          calendar.setLenient(false);
          date = Some(calendar.getTime)
          dateComplete = true
        } catch {
          case e: Exception => {
            validDayMonthYear = false
            comment = "Invalid year, day, month"
            assertions += QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE, comment)
            addPassedInvalidCollectionDate = false
          }
        }
      }

      //set the processed values
      if (validYear) processed.event.year = year.toString
      if (validMonth) processed.event.month = String.format("%02d", int2Integer(month)) //NC ensure that a month is 2 characters long
      if (validDay) processed.event.day = day.toString
      if (!date.isEmpty) processed.event.eventDate = DateFormatUtils.format(date.get, "yyyy-MM-dd")

      //deal with event date if we dont have separate day, month, year fields
      if (date.isEmpty && raw.event.eventDate != null && !raw.event.eventDate.isEmpty) {
        val parsedDate = DateParser.parseDate(raw.event.eventDate)
        if (!parsedDate.isEmpty) {
          //set processed values
          processed.event.eventDate = parsedDate.get.startDate
          processed.event.day = parsedDate.get.startDay
          processed.event.month = parsedDate.get.startMonth
          processed.event.year = parsedDate.get.startYear
          //set the valid year if one was supplied in the eventDate
          if(parsedDate.get.startYear != "") {
            val(newComment,newValidYear,newYear) = runYearValidation(parsedDate.get.startYear.toInt, currentYear,if(parsedDate.get.startDay =="") 0 else parsedDate.get.startDay.toInt, if(parsedDate.get.startMonth =="") 0 else parsedDate.get.startMonth.toInt)
            comment = newComment
            validYear = newValidYear
            year = newYear
          }

          if(StringUtils.isNotBlank(parsedDate.get.startDate)){
            //we have a complete date
            dateComplete=true
          }

          if (DateUtil.isFutureDate(parsedDate.get)) {
            assertions += QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE, "Future date supplied")
            addPassedInvalidCollectionDate = false
          }
        }
      }

      //deal with verbatim date
      if (date.isEmpty && raw.event.verbatimEventDate != null && !raw.event.verbatimEventDate.isEmpty) {
        val parsedDate = DateParser.parseDate(raw.event.verbatimEventDate)
        if (!parsedDate.isEmpty) {
          //set processed values
          processed.event.eventDate = parsedDate.get.startDate
          processed.event.day = parsedDate.get.startDay
          processed.event.month = parsedDate.get.startMonth
          processed.event.year = parsedDate.get.startYear

          //set the valid year if one was supplied in the verbatimEventDate
          if(parsedDate.get.startYear != "") {
            val(newComment,newValidYear,newYear) = runYearValidation(parsedDate.get.startYear.toInt, currentYear,if(parsedDate.get.startDay =="") 0 else parsedDate.get.startDay.toInt, if(parsedDate.get.startMonth =="") 0 else parsedDate.get.startMonth.toInt)
            comment = newComment
            validYear = newValidYear
            year = newYear
          }

          if(StringUtils.isNotBlank(parsedDate.get.startDate)){
            //we have a complete date
            dateComplete=true
          }

          if (DateUtil.isFutureDate(parsedDate.get)) {
            assertions += QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE, "Future date supplied")
            addPassedInvalidCollectionDate = false
          }
        }
      }

      //if invalid date, add assertion
      if (!validYear && (processed.event.eventDate == null || processed.event.eventDate == "" || comment != "")) {
        assertions += QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE, comment)
        addPassedInvalidCollectionDate = false
      }

      //chec for future date
      if (!date.isEmpty && date.get.after(new Date())) {
        assertions += QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE, "Future date supplied")
        addPassedInvalidCollectionDate = false
      }

      //check to see if we need add a passed test for the invalid collection dates
      if(addPassedInvalidCollectionDate)
        assertions += QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE, 1)

      if(dateComplete){
        //add a pass condition for this test
        assertions += QualityAssertion(AssertionCodes.INCOMPLETE_COLLECTION_DATE, 1)
      } else{
        //incomplete date
        assertions += QualityAssertion(AssertionCodes.INCOMPLETE_COLLECTION_DATE, "The supplied collection date is not complete")
      }
    }

    //now process the other dates
    processOtherDates(raw, processed, assertions)
    //check for the "first" of month,year,century
    processFirstDates(raw, processed, assertions)
    assertions.toArray
  }

  def runYearValidation(rawyear:Int, currentYear:Int, day:Int=0, month:Int=0):(String,Boolean,Int)={
    var validYear =true
    var comment=""
    var year = rawyear
    if (year > 0) {
      if (year < 100) {
        //parse 89 for 1989
        if (year > currentYear % 100) {
          // Must be in last century
          year += ((currentYear / 100) - 1) * 100
        } else {
          // Must be in this century
          year += (currentYear / 100) * 100

          //although check that combined year-month-day isnt in the future
          if (day != 0 && month != 0) {
            val date = DateUtils.parseDate(year.toString + String.format("%02d", int2Integer(month)) + day.toString, Array("yyyyMMdd"))
            if (date.after(new Date())) {
              year -= 100
            }
          }
        }
      } else if (year >= 100 && year < 1600) {
        year = -1
        validYear = false
        comment = "Year out of range"
      } else if (year > DateUtil.getCurrentYear) {
        year = -1
        validYear = false
        comment = "Future year supplied"
        //assertions + QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE,comment)
      } else if (year == 1788 && month == 1 && day == 26) {
        //First fleet arrival date indicative of a null date.
        validYear = false
        comment = "First Fleet arrival implies a null date"
      }

    }
    (comment,validYear,year)
  }

  def processFirstDates(raw:FullRecord, processed:FullRecord, assertions:ArrayBuffer[QualityAssertion]){
    //check to see if the date is the first of a month
    if(processed.event.day == "1" || processed.event.day == "01"){
      assertions += QualityAssertion(AssertionCodes.FIRST_OF_MONTH)
      //check to see if the date is the first of the year
      if(processed.event.month == "01" || processed.event.month == "1") {
        assertions += QualityAssertion(AssertionCodes.FIRST_OF_YEAR)
        //check to see if the date is the first of the century
        if(processed.event.year != null){
          var (year, validYear) = validateNumber(processed.event.year, {
            year => year > 0
          })
          if(validYear && year % 100 == 0){
            assertions += QualityAssertion(AssertionCodes.FIRST_OF_CENTURY)
          } else {
            //the date is NOT the first of the century
            assertions += QualityAssertion(AssertionCodes.FIRST_OF_CENTURY, 1)
          }
        }
      } else if(processed.event.month != null) {
        //the date is not the first of the year
        assertions += QualityAssertion(AssertionCodes.FIRST_OF_YEAR, 1)
      }
    } else if(processed.event.day != null) {
      //the date is not the first of the month
      assertions += QualityAssertion(AssertionCodes.FIRST_OF_MONTH, 1)
    }
  }

  /**
   * processed the other dates for the occurrence including performing data checks
   * @param raw
   * @param processed
   * @param assertions
   */
  def processOtherDates(raw:FullRecord, processed:FullRecord, assertions:ArrayBuffer[QualityAssertion]){
    //process the "modified" date for the occurrence - we want all modified dates in the same format so that we can index on them...
    if (raw.occurrence.modified != null) {
      val parsedDate = DateParser.parseDate(raw.occurrence.modified)
      if (parsedDate.isDefined) {
        processed.occurrence.modified = parsedDate.get.startDate
      }
    }

    if (raw.identification.dateIdentified != null) {
      val parsedDate = DateParser.parseDate(raw.identification.dateIdentified)
      if (parsedDate.isDefined)
        processed.identification.dateIdentified = parsedDate.get.startDate
    }

    if (raw.location.georeferencedDate != null || raw.miscProperties.containsKey("georeferencedDate")){
      def rawdate = if (raw.location.georeferencedDate != null) raw.location.georeferencedDate else raw.miscProperties.get("georeferencedDate")
      val parsedDate = DateParser.parseDate(rawdate)
      if (parsedDate.isDefined){
        processed.location.georeferencedDate = parsedDate.get.startDate
      }
    }

    if (StringUtils.isNotBlank(processed.event.eventDate)){
      val eventDate = DateParser.parseStringToDate(processed.event.eventDate).get
      //now test if the record was identified before it was collected
      if(StringUtils.isNotBlank(processed.identification.dateIdentified)){
        if (DateParser.parseStringToDate(processed.identification.dateIdentified).get.before(eventDate)){
          //the record was identified before it was collected !!
          assertions += QualityAssertion(AssertionCodes.ID_PRE_OCCURRENCE, "The records was identified before it was collected")
        } else {
          assertions += QualityAssertion(AssertionCodes.ID_PRE_OCCURRENCE, 1)
        }
      }

      //now check if the record was georeferenced after the collection date
      if(StringUtils.isNotBlank(processed.location.georeferencedDate)) {
        if(DateParser.parseStringToDate(processed.location.georeferencedDate).get.after(eventDate)){
          //the record was not georeference when it was collected!!
          assertions += QualityAssertion(AssertionCodes.GEOREFERENCE_POST_OCCURRENCE, "The record was not georeferenced when it was collected")
        } else {
          assertions += QualityAssertion(AssertionCodes.GEOREFERENCE_POST_OCCURRENCE, 1)
        }
      }
    }
  }

  def getName = "event"
}
