package au.org.ala.biocache.outliers

import math._
import au.org.ala.biocache.{Config, CassandraPersistenceManager}
import org.scale7.cassandra.pelops.Pelops
import org.apache.cassandra.thrift.{InvalidRequestException, CfDef, ConsistencyLevel}

object CalcStandardDeviation {

    def main(args:Array[String]){

      //retrieve all the records
      val pm:CassandraPersistenceManager = Config.persistenceManager.asInstanceOf[CassandraPersistenceManager]
      val columnFamilyManager = Pelops.createColumnFamilyManager(pm.cluster, "occ")

      try {
        var cdef = new CfDef("occ", "outliers")
        cdef.column_type = "Standard"
        cdef.comparator_type = "UTF8Type"
        cdef.default_validation_class = "UTF8Type"
        columnFamilyManager.addColumnFamily(cdef)
      } catch {
        case e:InvalidRequestException => columnFamilyManager.truncateColumnFamily("outliers")
      }

      pm.pageOverAll("layerByTaxon", (guid, map) => {

            val guidsAndValues = map.map( {case(k,v) => k -> v.toFloat })
            val values =  map.values.map(x => x.toFloat)
            val mean = values.foldLeft(0.0)( (total, value) => total + value) / values.size.toFloat
            val stdDev = sqrt( values.foldLeft(0.0)( (total,value) => total + pow((value - mean),2.0) ) / values.size.toFloat)

            //which points are 2 standard deviations away from the mean
            val (min, max) = (mean - (stdDev * 2) , mean + (stdDev * 2)  )

            val erroneousGuids = guidsAndValues.filter({ case(key,value) => value<min || value>max })
            println(guid + ": outliers ("+erroneousGuids.size+"/"+values.size+") : " + erroneousGuids.keys.mkString(","))

            val (layerName, scientificName) = {
                val layerAndGuid = guid.split("\\|\\|")
                (layerAndGuid(0),layerAndGuid(1))
            }

            //write that back to cassandra???
            val mutator = Pelops.createMutator(pm.poolName)
            for (erroneousGuid <- erroneousGuids){
                // i need the scientific name
                val tiGuid = scientificName + "||" + erroneousGuid._1
                println("GUID: " + tiGuid)
                mutator.writeColumn("outliers", tiGuid, mutator.newColumn("detectedOutlier-"+layerName,
                    "Value: " + erroneousGuid._2 + ", Mean: "+mean + ", Standard Deviation: "+stdDev+", Number of values: " + values.size))
            }
            mutator.execute(ConsistencyLevel.ONE)
            true
        })
    }




}