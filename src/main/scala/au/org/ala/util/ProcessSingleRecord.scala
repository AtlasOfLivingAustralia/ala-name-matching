package au.org.ala.util
import au.org.ala.biocache.Config
import java.io.{InputStreamReader, BufferedReader}
import org.codehaus.jackson.map.ObjectMapper

object ProcessSingleRecord {

    def main(args:Array[String]){

        val processor = new RecordProcessor

        print("Supply a UUID for a record: ")
        var uuid = readStdIn
        while(uuid != "q" || uuid !="exit"){
            val rawRecord = Config.occurrenceDAO.getByUuid(uuid, au.org.ala.biocache.Raw)
            if(rawRecord.isEmpty){
                println("UUID not recognised : " + uuid)
            } else {
               println("Processing record.....")
               processor.processRecord(rawRecord.get)
               val processedRecord = Config.occurrenceDAO.getByUuid(uuid, au.org.ala.biocache.Processed)
               val objectMapper = new ObjectMapper
               println(objectMapper.writeValueAsString(processedRecord.get))
            }
            
            print("\n\nSupply a UUID for a record: ")
            uuid = readStdIn
        }
        println("Exiting...")
        exit(1)
    }

    def readStdIn = (new BufferedReader(new InputStreamReader(System.in))).readLine.trim
}