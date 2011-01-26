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
class DateParserTests extends FunSuite {
	
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
}