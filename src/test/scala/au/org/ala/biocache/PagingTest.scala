package au.org.ala.biocache
/**
 * Demonstrator paging code.
 * 
 * @author Dave Martin (David.Martin@csiro.au)
 */
object PagingTest {

	
	def main(args : Array[String]) : Unit = {
		var count = 0
		OccurrenceDAO.pageOverAll(Raw, fullRecord => { 
				val occurrence = fullRecord.get.o
				val classification = fullRecord.get.c
				val location = fullRecord.get.l
				val event = fullRecord.get.e
				println(occurrence.uuid+"\t"+classification.genus+"\t"+classification.specificEpithet+"\t"+classification.scientificName)
				count +=1
				if(count>10) System.exit(1)
			}
		)
	}
}