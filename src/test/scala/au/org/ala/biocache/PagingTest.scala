package au.org.ala.biocache

object PagingTest {
	
	def main(args : Array[String]) : Unit = {
		OccurrenceDAO.pageOverAll(Raw, fullRecord => { 
				val occurrence = fullRecord.get.o
				val classification = fullRecord.get.c
				val location = fullRecord.get.l
				val event = fullRecord.get.e
				println(occurrence.uuid+"\t"+classification.genus+"\t"+classification.specificEpithet+"\t"+classification.scientificName)
			}
		)
	}
}