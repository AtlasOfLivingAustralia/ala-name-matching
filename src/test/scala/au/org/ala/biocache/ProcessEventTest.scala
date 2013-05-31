package au.org.ala.biocache
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.apache.commons.lang.time.DateUtils
import java.util.Date
import java.text.SimpleDateFormat

/**
 * Tests for event date parsing. To run these tests create a new scala application
 * run configuration in your IDE.
 * 
 * See http://www.scalatest.org/getting_started_with_fun_suite
 * 
 * scala -cp scalatest-1.0.jar org.scalatest.tools.Runner -p . -o -s ay.org.ala.biocache.ProcessEventTests 
 * 
 * @author Dave Martin (David.Martin@csiro.au)
 */
@RunWith(classOf[JUnitRunner])
class ProcessEventTest extends ConfigFunSuite {

  test("00 month test"){
    var raw = new FullRecord("1234","1234")
    raw.event.day ="0"
    raw.event.month = "0"
    raw.event.year = "0"
    var processed = raw.clone
    (new EventProcessor).process("1234", raw, processed)
    println(processed.event)
  }
  
  test("yyyy-dd-mm correctly sets year, month, day values in process object") {

    var raw = new FullRecord("1234", "1234")
    raw.event.eventDate = "1978-12-31"
    var processed = raw.clone
    (new EventProcessor).process("1234", raw, processed)

    expect("1978-12-31"){ processed.event.eventDate }
    expect("31"){ processed.event.day }
    expect("12"){ processed.event.month }
    expect("1978"){ processed.event.year }
  }

  test("yyyy-dd-mm verbatim date correctly sets year, month, day values in process object") {

    var raw = new FullRecord("1234", "1234")
    raw.event.verbatimEventDate = "1978-12-31/1978-12-31"
    var processed = raw.clone
    (new EventProcessor).process("1234", raw, processed)

    expect("1978-12-31"){ processed.event.eventDate }
    expect("31"){ processed.event.day }
    expect("12"){ processed.event.month }
    expect("1978"){ processed.event.year }
  }

  test("if year, day, month supplied, eventDate is correctly set") {

    var raw = new FullRecord("1234", "1234")
    raw.event.year = "1978"
    raw.event.month = "12"
    raw.event.day = "31"
    var processed = raw.clone
    (new EventProcessor).process("1234", raw, processed)

    expect("1978-12-31"){ processed.event.eventDate }
    expect("31"){ processed.event.day }
    expect("12"){ processed.event.month }
    expect("1978"){ processed.event.year }
  }

  test("if year supplied in 'yy' format, eventDate is correctly set") {

    var raw = new FullRecord("1234", "1234")
    raw.event.year = "78"
    raw.event.month = "12"
    raw.event.day = "31"
    var processed = raw.clone
    (new EventProcessor).process("1234", raw, processed)

    expect("1978-12-31"){ processed.event.eventDate }
    expect("31"){ processed.event.day }
    expect("12"){ processed.event.month }
    expect("1978"){ processed.event.year }
  }

  test("day month transposed") {

    var raw = new FullRecord("1234", "1234")
    raw.event.year = "78"
    raw.event.month = "16"
    raw.event.day = "6"
    var processed = raw.clone
    val assertions = (new EventProcessor).process("1234", raw, processed)

    expect("1978-06-16"){ processed.event.eventDate }
    expect("16"){ processed.event.day }
    expect("06"){ processed.event.month }
    expect("1978"){ processed.event.year }
    //expect(1){ assertions.size }
    expect(0){ assertions.find(_.code == 30009).get.qaStatus }
  }

  test("invalid month test") {

    var raw = new FullRecord("1234", "1234")
    var processed = new FullRecord("1234", "1234")
    raw.event.year = "78"
    raw.event.month = "16"
    raw.event.day = "16"

    val assertions = (new EventProcessor).process("1234", raw, processed)

    expect(null){ processed.event.eventDate }
    expect("16"){ processed.event.day }
    expect(null){ processed.event.month }
    expect("1978"){ processed.event.year }

    //expect(1){ assertions.size }
    expect(0){ assertions.find(_.code == 30007).get.qaStatus }
  }

  test("invalid month test > 12") {

    var raw = new FullRecord("1234", "1234")
    var processed = new FullRecord("1234", "1234")
    raw.event.year = "1978"
    raw.event.month = "40"
    raw.event.day = "16"

    val assertions = (new EventProcessor).process("1234", raw, processed)

    expect(null){ processed.event.eventDate }
    expect("16"){ processed.event.day }
    expect(null){ processed.event.month }
    expect("1978"){ processed.event.year }

    //expect(1){ assertions.size }
    expect(0){ assertions.find(_.code == 30007).get.qaStatus }
  }

  test("year = 11, month = 02, day = 01") {

    var raw = new FullRecord("1234", "1234")
    var processed = new FullRecord("1234", "1234")
    raw.event.year = "11"
    raw.event.month = "02"
    raw.event.day = "01"

    val assertions = (new EventProcessor).process("1234", raw, processed)

    expect("2011-02-01"){ processed.event.eventDate }
    expect("1"){ processed.event.day }
    expect("02"){ processed.event.month }
    expect("2011"){ processed.event.year }

    //expect(0){ assertions.size }
    expect(1){ assertions.find(_.code == 30007).get.qaStatus }
  }

  test("1973-10-14") {

    var raw = new FullRecord("1234", "1234")
    var processed = new FullRecord("1234", "1234")
    raw.event.eventDate = "1973-10-14"

    val assertions = (new EventProcessor).process("1234", raw, processed)

    expect("1973-10-14"){ processed.event.eventDate }
    expect("14"){ processed.event.day }
    expect("10"){ processed.event.month }
    expect("1973"){ processed.event.year }

    //expect(0){ assertions.size }
    expect(1){ assertions.find(_.code == 30007).get.qaStatus }
  }

  test("today"){
    var raw = new FullRecord("1234", "1234")
    var processed = new FullRecord("1234", "1234")
    val sf = new SimpleDateFormat("yyyy-MM-dd")
    raw.event.eventDate = sf.format(new Date())
    val assertions = (new EventProcessor).process("1234", raw, processed)
    expect(DateUtil.getCurrentYear.toString){ processed.event.year }
    //expect(0){ assertions.size }
    expect(1){ assertions.find(_.code == 30007).get.qaStatus }
  }

  test("tomorrow"){
    var raw = new FullRecord("1234", "1234")
    var processed = new FullRecord("1234", "1234")
    val sf = new SimpleDateFormat("yyyy-MM-dd")
    raw.event.eventDate = sf.format(DateUtils.addDays(new Date(),1))
    val assertions = (new EventProcessor).process("1234", raw, processed)
    expect(DateUtil.getCurrentYear.toString){ processed.event.year }
    expect(true){ assertions.size > 0 }
    expect(0){ assertions.find(_.code == 30007).get.qaStatus }
  }

  test("a digit year which gives a future date") {
    var raw = new FullRecord("1234", "1234")
    var processed = new FullRecord("1234", "1234")
    val futureDate = DateUtils.addDays(new Date(),2)

    val twoDigitYear =(new SimpleDateFormat("yy")).format(futureDate)

    raw.event.year = (new SimpleDateFormat("yy")).format(futureDate)
    raw.event.month = (new SimpleDateFormat("MM")).format(futureDate)
    raw.event.day = (new SimpleDateFormat("dd")).format(futureDate)

    val assertions = (new EventProcessor).process("1234", raw, processed)

    expect("19"+twoDigitYear){ processed.event.year }

    //expect(0){ assertions.size }
    expect(1){ assertions.find(_.code == 30007).get.qaStatus }
  }

  test ("Identification predates the occurrence") {
    val raw = new FullRecord
    val processed = new FullRecord
    raw.identification.dateIdentified = "2012-01-01"
    raw.event.eventDate = " 2013-01-01"

    var qas = (new EventProcessor).process("test", raw, processed)
    expect(0) {
      //the identification happened before the collection !!
      qas.find {_.getName == "idPreOccurrence"}.get.qaStatus
    }

    raw.identification.dateIdentified = "2013-01-01"
    qas = (new EventProcessor).process("test", raw, processed)
    expect(1) {
      //the identification happened at the same time of the collection
      qas.find {_.getName == "idPreOccurrence"}.get.qaStatus
    }
  }

  test ("Georeferencing postdates the occurrence") {
    val raw = new FullRecord
    val processed = new FullRecord
    raw.location.georeferencedDate = "2013-04-01"
    raw.event.eventDate = " 2013-01-01"

    var qas = (new EventProcessor).process("test", raw, processed)
    expect(0) {
      //the georeferencing happened after the collection !!
      qas.find {_.getName == "georefPostDate"}.get.qaStatus
    }

    raw.location.georeferencedDate = "2013-01-01"
    qas = (new EventProcessor).process("test", raw, processed)
    expect(1) {
      //the georeferecing happened at the same time as the collection
      qas.find {_.getName == "georefPostDate"}.get.qaStatus
    }
  }

  test("First of dates") {
    val raw = new FullRecord
    var processed = new FullRecord
    raw.event.day ="1"
    raw.event.month="1"
    raw.event.year="2000"

    var qas = (new EventProcessor).process("test", raw, processed)
    expect(0) {
      //date is first of month
      qas.find {_.getName == "firstOfMonth"}.get.qaStatus
    }
    expect(0) {
      //date is also the first of the year
      qas.find {_.getName == "firstOfYear"}.get.qaStatus
    }
    expect(0) {
      //date is also the first of the century
      qas.find {_.getName == "firstOfCentury"}.get.qaStatus
    }

    raw.event.year="2001"
    processed = new FullRecord
    qas = (new EventProcessor).process("test", raw, processed)
    expect(0) {
      //date is first of month
      qas.find {_.getName == "firstOfMonth"}.get.qaStatus
    }
    expect(0) {
      //date is also the first of the year
      qas.find {_.getName == "firstOfYear"}.get.qaStatus
    }
    expect(1) {
      //date is NOT the first of the century
      qas.find {_.getName == "firstOfCentury"}.get.qaStatus
    }

    raw.event.month="2"
    processed = new FullRecord
    qas = (new EventProcessor).process("test", raw, processed)
    expect(0) {
      //date is first of month
      qas.find {_.getName == "firstOfMonth"}.get.qaStatus
    }
    expect(1) {
      //date is NOT the first of the year
      qas.find {_.getName == "firstOfYear"}.get.qaStatus
    }
    expect(None) {
      //date is NOT the first of the century  - not tested since the month is not January
      qas.find {_.getName == "firstOfCentury"}
    }

    raw.event.day = "2"
    processed = new FullRecord
    qas = (new EventProcessor).process("test", raw, processed)
    expect(1) {
      //date is NOT first of month
      qas.find {_.getName == "firstOfMonth"}.get.qaStatus
    }
    expect(None) {
      //date is NOT the first of the year - gtested since the day is not 1
      qas.find {_.getName == "firstOfYear"}
    }
    expect(None) {
      //date is NOT the first of the century - not tested since the month is not January
      qas.find {_.getName == "firstOfCentury"}
    }
  }
}