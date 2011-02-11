package au.org.ala.util

import au.org.ala.biocache.OccurrenceDAO
import java.io.{InputStreamReader, BufferedReader}

object ProcessSingleRecord {

    def main(args:Array[String]){

        print("Supply a UUID for a record: ")
        var uuid = readStdIn
        while(uuid != "q" || uuid !="exit"){
            val rawRecord = OccurrenceDAO.getByUuid(uuid.trim)
            if(rawRecord.isEmpty){
                println("UUID not recognised : " + uuid)
            } else {
               ProcessRecords.processRecord(rawRecord.get)
               println("Processing record")
            }
            print("Supply a UUID for a record: ")
            uuid = readStdIn
        }
        println("Exiting...")
        exit(1)
    }


    def readStdIn = (new BufferedReader(new InputStreamReader(System.in))).readLine
}