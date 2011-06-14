package au.org.ala.util

import math._
import au.org.ala.biocache.{Config, CassandraPersistenceManager}

object CalcStandardDeviation {

    def main(args:Array[String]){

        //retrieve all the records
        val pm:CassandraPersistenceManager = Config.persistenceManager.asInstanceOf[CassandraPersistenceManager]

        pm.pageOverAll("el", (guid, map) => {

            val guidsAndValues = map.map( {case(k,v) => k -> v.toFloat })
            val values =  map.values.map(x => x.toFloat)
            val mean = values.foldLeft(0.0)( (total, value) => total + value) / values.size.toFloat
            val stdDev = sqrt( values.foldLeft(0.0)( (total,value) => total + pow((value - mean),2.0) ) / values.size.toFloat)

            //which points are 2 standard deviations away from the mean
            val (min, max) = (mean - (stdDev * 2) , mean + (stdDev * 2)  )

            val erroneousGuids = guidsAndValues.filter({ case(key,value) => value<min || value>max})
            println(guid + ": Dodgy records ("+erroneousGuids.size+"/"+values.size+")")
            true
        })
    }
}