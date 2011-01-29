package au.org.ala.biocache
import org.scalatest.FunSuite
import au.org.ala.util.ProcessRecords

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
class ProcessEventTests extends FunSuite {

  test("yyyy-dd-mm correctly sets year, month, day values in process object") {

    var raw = new FullRecord
    raw.o.uuid = "1234"
    raw.e.eventDate = "1978-12-31"
    var processed = raw.clone
    ProcessRecords.processEvent("1234", raw, processed)

    expect("1978-12-31"){ processed.e.eventDate }
    expect("31"){ processed.e.day }
    expect("12"){ processed.e.month }
    expect("1978"){ processed.e.year }
  }

  test("yyyy-dd-mm verbatim date correctly sets year, month, day values in process object") {

    var raw = new FullRecord
    raw.o.uuid = "1234"
    raw.e.verbatimEventDate = "1978-12-31/1978-12-31"
    var processed = raw.clone
    ProcessRecords.processEvent("1234", raw, processed)

    expect("1978-12-31"){ processed.e.eventDate }
    expect("31"){ processed.e.day }
    expect("12"){ processed.e.month }
    expect("1978"){ processed.e.year }
  }

  test("if year, day, month supplied, eventDate is correctly set") {

    var raw = new FullRecord
    raw.o.uuid = "1234"
    raw.e.year = "1978"
    raw.e.month = "12"
    raw.e.day = "31"
    var processed = raw.clone
    ProcessRecords.processEvent("1234", raw, processed)

    expect("1978-12-31"){ processed.e.eventDate }
    expect("31"){ processed.e.day }
    expect("12"){ processed.e.month }
    expect("1978"){ processed.e.year }
  }

  test("if year supplied in 'yy' format, eventDate is correctly set") {

    var raw = new FullRecord
    raw.o.uuid = "1234"
    raw.e.year = "78"
    raw.e.month = "12"
    raw.e.day = "31"
    var processed = raw.clone
    ProcessRecords.processEvent("1234", raw, processed)

    expect("1978-12-31"){ processed.e.eventDate }
    expect("31"){ processed.e.day }
    expect("12"){ processed.e.month }
    expect("1978"){ processed.e.year }
  }
}