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

import au.org.ala.biocache._
import org.wyki.cassandra.pelops.Pelops
import java.util.UUID
import au.com.bytecode.opencsv.CSVReader
import au.org.ala.biocache.OccurrenceDAO
import java.io.{FileReader, File, InputStreamReader, FileInputStream}

/**
 * Reads a CSV file with DwC header fields, updates existing but inserts new
 * 
 * @author Dave Martin (David.Martin@csiro.au)
 */
object FasterDwCLoaderUpdates {

  val hosts = Array { "localhost" }
  val keyspace = "occ"
  val columnFamily = "occ"
  val poolName = "occ-pool"
  val resourceUid = "dp20"
  val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
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
    var columns:Array[String] = null
    columns = csvReader.readNext

    while (columns != null) {

      count += 1
      //the newly assigned record UUID
      if(columns.length == columnsHeaders.length){
      val fieldTuples = {
          for {
             i <- 0 until columns.length-1
             if(columns(i).length > 0)
          } yield (columnsHeaders(i), columns(i).trim) //need to trim fields in case a space character has been inserted
      }

       //lookup the column
      //val recordUuid = UUID.randomUUID.toString
      val map = Map(fieldTuples map {s => (s._1, s._2)} : _*)

      val cn = map.get("catalogNumber").getOrElse(null)
      val cc = map.get("collectionCode").getOrElse(null)
      val ic = map.get("institutionCode").getOrElse(null)
      val uniqueID = resourceUid + "|" + ic + "|" + cc + "|" + cn

      val recordUuid = occurrenceDAO.createOrRetrieveUuid(uniqueID)

      persistenceManager.put(recordUuid, "occ", map)
      //DAO.persistentManager.put(uniqueID, "dr", "uuid", recordUuid)
      if (count % 100 == 0 && count > 0) {
        finishTime = System.currentTimeMillis
        println(count + ", >>  UUID: " + recordUuid + ", records per sec: " + 100 / (((finishTime - startTime).toFloat) / 1000f))
        startTime = System.currentTimeMillis
      }
      }
      else{
        println("Error processing line : " + count + " : " + columns(0) + " - " + columns(2))
      }
      //debug
      

      columns = csvReader.readNext
    }

    persistenceManager.shutdown
    println("Finished DwC loader. Records processed: " + count)
  }
}
