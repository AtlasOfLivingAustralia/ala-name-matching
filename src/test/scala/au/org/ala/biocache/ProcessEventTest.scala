package au.org.ala.biocache
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Date
import org.apache.commons.lang.time.DateUtils

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
    expect(1){ assertions.size }
    expect(30009){ assertions(0).code }
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

    expect(0){ assertions.size }
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

    expect(0){ assertions.size }
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

    expect(0){ assertions.size }
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

    expect(0){ assertions.size }
  }

  test("today"){
    var raw = new FullRecord("1234", "1234")
    var processed = new FullRecord("1234", "1234")
    val sf = new SimpleDateFormat("yyyy-MM-dd")
    raw.event.eventDate = sf.format(new Date())
    val assertions = (new EventProcessor).process("1234", raw, processed)
    expect(DateUtil.getCurrentYear.toString){ processed.event.year }
    expect(0){ assertions.size }
  }

  test("tomorrow"){
    var raw = new FullRecord("1234", "1234")
    var processed = new FullRecord("1234", "1234")
    val sf = new SimpleDateFormat("yyyy-MM-dd")
    raw.event.eventDate = sf.format(DateUtils.addDays(new Date(),1))
    val assertions = (new EventProcessor).process("1234", raw, processed)
    expect(DateUtil.getCurrentYear.toString){ processed.event.year }
    expect(true){ assertions.size > 0 }
  }
}