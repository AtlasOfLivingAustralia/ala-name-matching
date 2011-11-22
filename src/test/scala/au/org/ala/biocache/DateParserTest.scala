package au.org.ala.biocache
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Tests for event date parsing. To run these tests create a new scala application
 * run configuration in your IDE.
 * 
 * See http://www.scalatest.org/getting_started_with_fun_suite
 * 
 * scala -cp scalatest-1.0.jar org.scalatest.tools.Runner -p . -o -s au.org.ala.biocache.ProcessEventTests
 * 
 * @author Dave Martin (David.Martin@csiro.au)
 */
@RunWith(classOf[JUnitRunner])
class DateParserTest extends FunSuite {

  test("Single Year"){
    val result = DateParser.parseDate("1978")
    expect(false){ result.isEmpty }
    expect("1978"){ result.get.startYear }
    expect("1978"){ result.get.endYear }
    expect(false){ result.get.singleDate }
  }

  test("Single Year with full month"){
    val result = DateParser.parseDate("1978-April-01")
    expect(false){ result.isEmpty }
    expect("1978"){ result.get.startYear }
    expect("1978"){ result.get.endYear }
    expect("04"){ result.get.startMonth }
    expect("01"){ result.get.startDay }
    expect(true){ result.get.singleDate }
  }

  test("Single Year with full month - long name"){
    val result = DateParser.parseDate("1978-December-01")
    expect(false){ result.isEmpty }
    expect("1978"){ result.get.startYear }
    expect("1978"){ result.get.endYear }
    expect("12"){ result.get.startMonth }
    expect("01"){ result.get.startDay }
    expect(true){ result.get.singleDate }
  }

  test("Single date") {

    val result = DateParser.parseDate("1978-01-01")

    expect(false){ result.isEmpty }
    expect("1978"){ result.get.startYear }
    expect("1978"){ result.get.endYear }
    expect(true){ result.get.singleDate }
  }

  test("Single date with time") {

    val result = DateParser.parseDate("2009-02-20T08:40Z")

    expect(false){ result.isEmpty }
    expect("2009"){ result.get.startYear }
    expect("02"){ result.get.startMonth }
    expect("20"){ result.get.startDay }
    expect(true){ result.get.singleDate }
  }

  test("Single month date") {

    val result = DateParser.parseDate("1978-01")

    expect(false){ result.isEmpty }
    expect("1978"){ result.get.startYear }
    expect("1978"){ result.get.endYear }
    expect("01"){ result.get.startMonth }
    expect("01"){ result.get.endMonth }
    expect(true){ result.get.singleDate }
  }

  test("Single month date with trailing hyphen") {

    val result = DateParser.parseDate("1978-01-")

    expect(false){ result.isEmpty }
    expect("1978"){ result.get.startYear }
    expect("1978"){ result.get.endYear }
    expect("01"){ result.get.startMonth }
    expect("01"){ result.get.endMonth }
    expect(true){ result.get.singleDate }
  }

  test("Date range") {

    val result = DateParser.parseDate("1978-01-01/1979-02-13")

    expect(false){ result.isEmpty }
    expect("1978"){ result.get.startYear }
    expect("1979"){ result.get.endYear }
    expect("01"){ result.get.startMonth }
    expect("02"){ result.get.endMonth }
    expect(false){ result.get.singleDate }
  }

  test("Month range") {

    val result = DateParser.parseDate("1978-01/1979-05")

    expect(false){ result.isEmpty }
    expect("1978"){ result.get.startYear }
    expect("1979"){ result.get.endYear }
    expect("01"){ result.get.startMonth }
    expect("05"){ result.get.endMonth }
    expect(false){ result.get.singleDate }
  }

  test("Month range trailing hyphens") {

    val result = DateParser.parseDate("1978-01-/1979-05-")

    expect(false){ result.isEmpty }
    expect("1978"){ result.get.startYear }
    expect("1979"){ result.get.endYear }
    expect("01"){ result.get.startMonth }
    expect("05"){ result.get.endMonth }
    expect(false){ result.get.singleDate }
  }

  test("Year range") {

    val result = DateParser.parseDate("1978/1991")

    expect(false){ result.isEmpty }
    expect("1978"){ result.get.startYear }
    expect("1991"){ result.get.endYear }
    expect(false){ result.get.singleDate }
  }

  test("Same century range") {

    val result = DateParser.parseDate("1978/91")

    expect(false){ result.isEmpty }
    expect("1978"){ result.get.startYear }
    expect("1991"){ result.get.endYear }
    expect(false){ result.get.singleDate }
  }

  test("2002-03-10 00:00:00.0/2002-03-10 00:00:00.0") {

    val result = DateParser.parseDate("2002-03-10 00:00:00.0/2003-03-10 00:00:00.0")

    expect(false){ result.isEmpty }
    expect("2002"){ result.get.startYear }
    expect("2003"){ result.get.endYear }
    expect(false){ result.get.singleDate }
  }

  test("2005-06-12 00:00:00.0/2005-06-12 00:00:00.0") {
    val result = DateParser.parseDate("2005-06-12 00:00:00.0/2005-06-12 00:00:00.0")
    expect(false){ result.isEmpty }
    expect("2005"){ result.get.startYear }
    expect("2005"){ result.get.endYear }
    expect(true){ result.get.singleDate }
  }
 
  test("12-06-2005") {
    val result = DateParser.parseDate("12-06-2005")
    expect(false){ result.isEmpty }
    expect("2005"){ result.get.startYear }
    expect("2005"){ result.get.endYear }
    expect(true){ result.get.singleDate }
  }

  test("Mon Apr 23 00:00:00 EST 1984/Sun Apr 29 00:00:00 EST 1984") {
    val result = DateParser.parseDate("Mon Apr 23 00:00:00 EST 1984/Sun Apr 29 00:00:00 EST 1984")
    expect(false){ result.isEmpty }
    expect("1984"){ result.get.startYear }
    expect("1984"){ result.get.endYear }
    expect(false){ result.get.singleDate }
  }
  
  test("Fri Aug 12 15:19:20 EST 2011"){
      val result = DateParser.parseDate("Fri Aug 12 15:19:20 EST 2011")
      expect(false){ result.isEmpty }
      expect("2011"){ result.get.startYear }
      expect("2011"){ result.get.endYear }
      expect(true){ result.get.singleDate }
  }

  test("1982-03-12"){
      val result = DateParser.parseDate("1982-03-12 ")
      expect(false){ result.isEmpty }
      expect("1982"){ result.get.startYear }
      expect("03"){ result.get.startMonth }
      expect(true){ result.get.singleDate }
  }

  test("2011-09-13 09:29:08"){
      val result = DateParser.parseDate("2011-09-13 09:29:08")
      expect(false){ result.isEmpty }
      expect("2011"){ result.get.startYear }
      expect("09"){ result.get.startMonth }
      expect("13"){ result.get.startDay }
      expect(true){ result.get.singleDate }
  }

  test("21-Aug-2005"){
      val result = DateParser.parseDate("21-Aug-2005")
      expect(false){ result.isEmpty }
      expect("2005"){ result.get.startYear }
      expect("08"){ result.get.startMonth }
      expect("21"){ result.get.startDay }
      expect(true){ result.get.singleDate }

  }
}