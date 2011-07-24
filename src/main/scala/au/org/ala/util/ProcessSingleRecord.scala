package au.org.ala.util
import au.org.ala.biocache.Config
import java.io.{InputStreamReader, BufferedReader}
import org.codehaus.jackson.map.ObjectMapper

object ProcessSingleRecord {

    def main(args:Array[String]){

        val processor = new RecordProcessor

        print("Supply a UUID or a Row Key for a record: ")
        var uuid = readStdIn
        while(uuid != "q" || uuid !="exit"){
            var rawRecord = Config.occurrenceDAO.getByRowKey(uuid, au.org.ala.biocache.Raw)
            if(rawRecord.isEmpty){
            	rawRecord = Config.occurrenceDAO.getByUuid(uuid, au.org.ala.biocache.Raw)
            }
            if(!rawRecord.isEmpty) {
               println("Processing record.....")
               processor.processRecord(rawRecord.get)
               val processedRecord = Config.occurrenceDAO.getByUuid(rawRecord.get.uuid, au.org.ala.biocache.Processed)
               val objectMapper = new ObjectMapper
               println(objectMapper.writeValueAsString(processedRecord.get))
            } else {
            	println("UUID or row key not stored....")
            }
            print("\n\nSupply a Row Key for a record: ")
            uuid = readStdIn
        }
        println("Exiting...")
        exit(1)
    }

    def readStdIn = (new BufferedReader(new InputStreamReader(System.in))).readLine.trim
}