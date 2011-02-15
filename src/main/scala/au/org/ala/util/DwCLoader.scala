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

import java.io.File
import org.gbif.dwc.terms._
import org.gbif.dwc.text._
import org.wyki.cassandra.pelops.Pelops
import au.org.ala.biocache.{Raw, OccurrenceDAO}
import collection.mutable.ArrayBuffer
import scala.actors._
import scala.actors.Actor
import collection.JavaConversions

/**
 * Reads a DwC-A and writes the data to the BioCache
 * 
 * @author Dave Martin (David.Martin@csiro.au)
 */
object DwCLoader {

  val hosts = Array { "localhost" }
  val keyspace = "occ"
  val columnFamily = "occ"
  val poolName = "occ-pool"
  val resourceUid = "dp20"

  def main(args: Array[String]): Unit = {

    import scala.actors.Actor._
    import JavaConversions._
  import scalaj.collection.Imports._
    import ReflectBean._
    println(">>> Starting DwC loader ....")
    val file = new File("/data/biocache/ozcam/")
    val archive = ArchiveFactory.openArchive(file)
    val iter = archive.iteratorDwc
    val terms = DwcTerm.values
    var count = 0

    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis
    var currentBatch = new ArrayBuffer[au.org.ala.biocache.FullRecord]

    while (iter.hasNext) {

      count += 1
      //the newly assigned record UUID
      val dwc = iter.next

      //the details of how to construct the UniqueID belong in the Collectory
      val cc: String = dwc.getProperty(DwcTerm.collectionCode)
      val ic: String = dwc.getProperty(DwcTerm.institutionCode)
      val cn: String = dwc.getProperty(DwcTerm.catalogNumber)
      val uniqueID = resourceUid + "|" + ic + "|" + cc + "|" + cn

      //create a map of properties
      val fieldTuples:Array[(String,String)] = {
        (for {
          term <- terms
          val property = dwc.getterWithOption(term.simpleName)
          if (!property.isEmpty && property.get.toString.trim.length>0)
        } yield (term.simpleName -> property.get.toString))
      }

       //lookup the column
      val recordUuid = OccurrenceDAO.createOrRetrieveUuid(uniqueID)
      val fullRecord = OccurrenceDAO.createFullRecord(recordUuid, fieldTuples, Raw)

      currentBatch += fullRecord

      //debug
      if (count % 1000 == 0 && count > 0) {

        OccurrenceDAO.addRawOccurrenceBatch(currentBatch.toArray)
        finishTime = System.currentTimeMillis
        println(count + ", >> last key : " + uniqueID + ", UUID: " + recordUuid + ", records per sec: " + 1000 / (((finishTime - startTime).toFloat) / 1000f))
        startTime = System.currentTimeMillis
        //clear the buffer
        currentBatch.clear
      }
    }
    //commit the batch
    OccurrenceDAO.addRawOccurrenceBatch(currentBatch.toArray)

    Pelops.shutdown
    println("Finished DwC loader. Records processed: " + count)
  }
}
