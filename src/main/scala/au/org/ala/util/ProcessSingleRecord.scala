package au.org.ala.util
import au.org.ala.biocache.OccurrenceDAO
import au.org.ala.biocache.Config
import java.io.{InputStreamReader, BufferedReader}
import org.codehaus.jackson.map.ObjectMapper

object ProcessSingleRecord {

    def main(args:Array[String]){

        val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]

        print("Supply a UUID for a record: ")
        var uuid = readStdIn
        while(uuid != "q" || uuid !="exit"){
            val rawRecord = occurrenceDAO.getByUuid(uuid, au.org.ala.biocache.Raw)
            if(rawRecord.isEmpty){
                println("UUID not recognised : " + uuid)
            } else {
               println("Processing record.....")
               ProcessRecords.processRecord(rawRecord.get)
               val processedRecord = occurrenceDAO.getByUuid(uuid, au.org.ala.biocache.Processed)
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