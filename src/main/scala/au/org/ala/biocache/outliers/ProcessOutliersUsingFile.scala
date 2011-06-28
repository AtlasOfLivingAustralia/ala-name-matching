package au.org.ala.biocache.outliers

import collection.mutable.{HashMap, ListBuffer}
import math._
import org.scale7.cassandra.pelops.Pelops
import au.org.ala.biocache.{Config, CassandraPersistenceManager}
import org.apache.cassandra.thrift.{InvalidRequestException, CfDef, ConsistencyLevel}
import org.apache.commons.io.FileUtils
import java.io.{File, FileReader, BufferedReader}

object ProcessOutliersUsingFile {

    //retrieve all the records
    val pm:CassandraPersistenceManager = Config.persistenceManager.asInstanceOf[CassandraPersistenceManager]
    val columnFamilyManager = Pelops.createColumnFamilyManager(pm.cluster, "occ")

    //expects a file pre-sorted by taxon
    def processBufferForTaxon(listToProcess: List[Map[String, String]], keyOnVariables: Array[String], taxonConceptID: String) {

        //iterate through each variable
        for (keyVariable <- keyOnVariables) {
            //retrieve a map of GUID -> value
            val mapBuffer = new HashMap[String, Float]
            listToProcess.foreach(map => {
                val value = map.get(keyVariable)
                if (!value.isEmpty && value.get != "")
                    mapBuffer.put(map.getOrElse("guid", ""), value.get.toFloat)
            })

            val map = mapBuffer.toMap
            val values = map.values.map(x => x.toFloat)
            val mean = values.foldLeft(0.0)((total, value) => total + value) / values.size.toFloat
            val stdDev = sqrt(values.foldLeft(0.0)((total, value) => total + pow((value - mean), 2.0)) / values.size.toFloat)
            //which points are 2 standard deviations away from the mean
            val (min, max) = (mean - (stdDev * 2), mean + (stdDev * 2))
            val erroneousGuids = map.filter({
                case (key, value) => value < min || value > max
            })

            if (!erroneousGuids.isEmpty) {
                //print(", erroneous guids: "+ erroneousGuids.size)
                //write that back to cassandra
                val mutator = Pelops.createMutator(pm.poolName)
                for (erroneousGuid <- erroneousGuids) {
                    // i need the scientific name
                    val tiGuid = taxonConceptID + "||" + erroneousGuid._1
                    //println("GUID: " + tiGuid)
                    mutator.writeColumn("outliers", tiGuid, mutator.newColumn(keyVariable,
                        "value:" + erroneousGuid._2 + ",mean:" + mean + ",stddev:" + stdDev
                         + ",count: " + values.size))
                }
                mutator.execute(ConsistencyLevel.ONE)
            }
        }
    }

    def processFile(filePath: String, columnHeaders: List[String]) {
        val fr = new BufferedReader(new FileReader(filePath))

        val keyOnVariables = (593 to 895).toList.map(x => "el" + x + ".p").toArray[String]

        var currentTaxonID = ""
        var currentLine = fr.readLine
        val listBuffer = new ListBuffer[Map[String, String]]
        var currentListBufferSize = 0
        var taxaProcessed = 0

        while (currentLine != null) {
            val columns = currentLine.split("\t")
            if (columns.size > 5) {
                val taxonConceptID = columns(2)

                if (currentTaxonID != taxonConceptID) {
                    
                    if(currentTaxonID!=""){
	                    taxaProcessed += 1
	                    println(taxaProcessed + ", processing: " + currentTaxonID + ", elements buffered: " + currentListBufferSize)
	                    //process this batch
	                    processBufferForTaxon(listBuffer.toList, keyOnVariables, taxonConceptID)
	                    print("\n")
	                    listBuffer.clear
	                    currentListBufferSize = 0
                    }
                    currentTaxonID = taxonConceptID
                }

                val map = (columnHeaders zip columns).toMap[String, String]
                currentListBufferSize += map.size
                listBuffer + map
            }

            currentLine = fr.readLine
        }
        println(taxaProcessed + ", processing: " + currentTaxonID + ", elements buffered: " + currentListBufferSize)
        processBufferForTaxon(listBuffer.toList, keyOnVariables, currentTaxonID)

        fr.close
    }

    def main(args:Array[String]){
        
              try {
            var cdef = new CfDef("occ", "outliers")
            cdef.column_type = "Standard"
            cdef.comparator_type = "UTF8Type"
            cdef.default_validation_class = "UTF8Type"
            columnFamilyManager.addColumnFamily(cdef)
        } catch {
            case e: InvalidRequestException => //columnFamilyManager.truncateColumnFamily("outliers")
        }  
        
        
      val columnHeaders = FileUtils.readFileToString( new File(args(0)) ).split("\t").toList
      val filePaths = args.tail
      for(filePath <- filePaths){
        processFile(filePath, columnHeaders)
      }
    }
}