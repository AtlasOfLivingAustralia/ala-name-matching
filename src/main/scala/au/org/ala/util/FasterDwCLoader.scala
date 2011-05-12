/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 * 
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 * 
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/
package au.org.ala.util


import java.util.UUID
import au.com.bytecode.opencsv.CSVReader
import java.io.{FileReader, File, InputStreamReader, FileInputStream}
import au.org.ala.biocache._

/**
 * Reads a DwC-A and writes the data to the BioCache
 * 
 * @author Dave Martin (David.Martin@csiro.au)
 */
object FasterDwCLoader {

  val hosts = Array { "localhost" }
  val keyspace = "occ"
  val columnFamily = "occ"
  val poolName = "occ-pool"
  val resourceUid = "dp20"
  val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]
  
  def main(args: Array[String]): Unit = {

    import ReflectBean._
    println(">>> Starting DwC loader ....")
    val fileName = if(args.length == 1) args(0) else "/data/biocache/ozcam/ozcam.csv"
    val csvReader = new CSVReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));

    var count = 0

    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis

    val columnsHeaders = csvReader.readNext
    var columns:Array[String] = csvReader.readNext

    while (columns != null) {

      count += 1
      //the newly assigned record UUID

      val fieldTuples = {
          for {
             i <- 0 until columns.length-1
             if(columns(i).length > 0)
          } yield (columnsHeaders(i), columns(i))
      }

       //lookup the column
      //val recordUuid = UUID.randomUUID.toString
      val map = Map(fieldTuples map {s => (s._1, s._2)} : _*)
      if(!map.isEmpty){
          val cn = map.get("catalogNumber").getOrElse("")
          val cc = map.get("collectionCode").getOrElse("")
          val ic = map.get("institutionCode").getOrElse("")
          val uniqueID = resourceUid + "|" + ic + "|" + cc + "|" + cn

          //val recordUuid = persistenceManager.put(recordUuid, "occ", map)
          val recordUuid = persistenceManager.put(null, "occ", map)
          persistenceManager.put(uniqueID, "dr", "uuid", recordUuid)
          //debug
          if (count % 1000 == 0 && count > 0) {
            finishTime = System.currentTimeMillis
            println(count + ", >>  UUID: " + recordUuid + ", records per sec: " + 1000 / (((finishTime - startTime).toFloat) / 1000f))
            startTime = System.currentTimeMillis
          }
      }
      columns = csvReader.readNext
    }

    persistenceManager.shutdown
    println("Finished DwC loader. Records processed: " + count)
  }
}
