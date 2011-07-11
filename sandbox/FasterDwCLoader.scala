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
import scala.util.parsing.json._
import java.util.HashMap
import scala.io.Source
import org.apache.commons.lang.time.DateFormatUtils
import java.util.Date
/**
 * Reads a DwC-A in csv format and writes the data to the BioCache
 *
 * The data resource uid is provided as an argument or is a value in ghte file.
 *
 * The collectory is looked upto find the unique record identfier for the data resource
 *
 * The following data source have been loaded individually:
 *
 * dr350 Eremaea
 * dr355 Kangawalla
 * dr354 Barrow Island
 * dr340 AM
 * dr348 WAM
 * dr346 SAMA
 * dr343 NT
 * dr344 QM
 * dr347 TMAG
 * dr342 NMV
 * dr130 ANIC
 * dr341 ANWC
 * dr345 QVMAG
 * dr349 CSIRO
 *
 * The remaining data resources are loaded from a complete dump of values.
 *
 * @author Natasha Carter (Natasha.Carter@csiro.au)
 */
object FasterDwCLoader {

  val hosts = Array { "localhost" }
  val keyspace = "occ"
  val columnFamily = "occ"
  val poolName = "occ-pool"
  val drToSwapRecordToCatalog = Array("dr356", "dr358", "dr359", "dr361", "dr365", "dr366","dr368", "dr380")
  val drToIgnore = Array("dr130", "dr132", "dr133", "dr134", "dr360", "dr350", "dr355", "dr354")
  //Caches the dr uid to the unique fields
  val drCache = new HashMap[String, List[String]]
  //var resourceUid = "dp20"
  val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]
  val loadTime = DateFormatUtils.format(new Date, "yyyy-MM-dd hh:mm:ss")
  def main(args: Array[String]): Unit = {

    import ReflectBean._
    println(">>> Starting DwC loader ....")
    val fileName = if(args.length >= 1) args(0) else "/data/biocache/ozcam/ozcam.csv"
    val resourceUid = if(args.length >=2) args(1) else null;
    val file = new File(fileName)
    if(file.isDirectory){
      for(name <- file.list){
        try{
          processFile(fileName + "/"+name, resourceUid)
        }
        catch{
          case e:Exception=> println("WARNING: Unable to load " + name)
        }
      }
    }
    else
      processFile(fileName, resourceUid)


    persistenceManager.shutdown

  }
  def processFile(fileName:String, dataResourceUid:String)={
    println("Starting to load " + fileName)
    val csvReader = new CSVReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));

    var count = 0

    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis

    val columnsHeaders = csvReader.readNext
    var columns:Array[String] = csvReader.readNext
    //val occMap = new scala.collection.mutable.HashMap[String, Map[String, String]]()
    //val drMap = new scala.collection.mutable.HashMap[String, Map[String, String]]()
    while (columns != null) {

      count += 1
      //the newly assigned record UUID

      val fieldTuples = {
          for {
             i <- 0 until columns.length-1
             if(columns(i).trim.length > 0)
          } yield (columnsHeaders(i), columns(i))
      }

       //lookup the column
     // val recordUuid = UUID.randomUUID.toString
      var map = Map(fieldTuples map {s => (s._1, s._2)} : _*)
      if(!map.isEmpty){
          val uuid = UUID.randomUUID.toString
          var resourceUid = if(dataResourceUid != null) dataResourceUid else map.getOrElse("dataResourceUid", null)
          if(resourceUid != null){
//              val cn = map.get("catalogNumber").getOrElse("")
//              val cc = map.get("collectionCode").getOrElse("")
//              val ic = map.get("institutionCode").getOrElse("")
//              val id = map.get("portalId").getOrElse("")
//              //Use the supplied dataResourceUid for the resourceId
              if(map.contains("dataResourceUid")){

                map =performDRPreprocessing(map, resourceUid)
              }
              if(map != null){
                    val uniqueTerms = retrieveConnectionDetails(resourceUid)
                  val uniqueTermValues = if(uniqueTerms != null && uniqueTerms.size>0) uniqueTerms.map(t => map.getOrElse(t, "")) else List(uuid)
                  val uniqueID =(List(resourceUid) ::: uniqueTermValues).mkString("|")


                  //val recordUuid = persistenceManager.put(recordUuid, "occ", map)
                  persistenceManager.put(uniqueID, "occ", map + ("rowKey"->uniqueID,"uuid"->uuid, "lastLoadTime"->loadTime))
                  //persistenceManager.put(uniqueID, "dr", "uuid", recordUuid)


              if (count % 1000 == 0 && count > 0) {

                finishTime = System.currentTimeMillis

                println(count +", >> Row Key: " + uniqueID+ ", >>  UUID: " + uuid + ", records per sec: " + 1000 / (((finishTime - startTime).toFloat) / 1000f))
                startTime = System.currentTimeMillis
              }
            }
          }
      }
      columns = csvReader.readNext
    }
     println("Finished "+fileName+" loading. Records processed: " + count)
  }

    def performDRPreprocessing(map:Map[String, String], resourceUid:String):Map[String,String]={
      //
      if(drToSwapRecordToCatalog.contains(resourceUid)){
        var catNum = map.getOrElse("occurrenceID", "")
        if(resourceUid == "dr359" && catNum.contains("."))
          catNum = catNum.substring(0, catNum.indexOf("."))

        val retMap = map - "occurrenceID" + ("catalogNumber"->catNum)
        return retMap

      }
      else if(drToIgnore.contains(resourceUid)){
        return null
      }
      map
    }


    def retrieveConnectionDetails(resourceUid: String): List[String] = {
      //check to see if it is in the cache first
      if(drCache.containsKey(resourceUid))
        drCache.get(resourceUid)
      else{
        try{
          println("Attempting to get information from http://collections.ala.org.au/ws/dataResource/"  + resourceUid + ".json" )
          val json = Source.fromURL("http://collections.ala.org.au/ws/dataResource/" + resourceUid + ".json").getLines.mkString
          val map = JSON.parseFull(json).get.asInstanceOf[Map[String, AnyRef]]
          val connectionParameters = JSON.parseFull(map("connectionParameters").asInstanceOf[String]).get.asInstanceOf[Map[String, AnyRef]]

          val uniqueTerms: List[String] = connectionParameters.getOrElse("termsForUniqueKey", List[String]()).asInstanceOf[List[String]]
          drCache.put(resourceUid, uniqueTerms)
          uniqueTerms
        }
        catch{
          case e:Exception => {
              drCache.put(resourceUid, null);
              null
          }
        }
      }
    }
}
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
import scala.util.parsing.json._
import java.util.HashMap
import scala.io.Source
import org.apache.commons.lang.time.DateFormatUtils
import java.util.Date
/**
 * Reads a DwC-A in csv format and writes the data to the BioCache
 *
 * The data resource uid is provided as an argument or is a value in ghte file.
 *
 * The collectory is looked upto find the unique record identfier for the data resource
 *
 * The following data source have been loaded individually:
 *
 * dr350 Eremaea
 * dr355 Kangawalla
 * dr354 Barrow Island
 * dr340 AM
 * dr348 WAM
 * dr346 SAMA
 * dr343 NT
 * dr344 QM
 * dr347 TMAG
 * dr342 NMV
 * dr130 ANIC
 * dr341 ANWC
 * dr345 QVMAG
 * dr349 CSIRO
 *
 * The remaining data resources are loaded from a complete dump of values.
 *
 * @author Natasha Carter (Natasha.Carter@csiro.au)
 */
object FasterDwCLoader {

  val hosts = Array { "localhost" }
  val keyspace = "occ"
  val columnFamily = "occ"
  val poolName = "occ-pool"
  val drToSwapRecordToCatalog = Array("dr356", "dr358", "dr359", "dr361", "dr365", "dr366","dr368", "dr380")
  val drToIgnore = Array("dr130", "dr132", "dr133", "dr134", "dr360", "dr350", "dr355", "dr354")
  //Caches the dr uid to the unique fields
  val drCache = new HashMap[String, List[String]]
  //var resourceUid = "dp20"
  val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]
  val loadTime = DateFormatUtils.format(new Date, "yyyy-MM-dd hh:mm:ss")
  def main(args: Array[String]): Unit = {

    import ReflectBean._
    println(">>> Starting DwC loader ....")
    val fileName = if(args.length >= 1) args(0) else "/data/biocache/ozcam/ozcam.csv"
    val resourceUid = if(args.length >=2) args(1) else null;
    val file = new File(fileName)
    if(file.isDirectory){
      for(name <- file.list){
        try{
          processFile(fileName + "/"+name, resourceUid)
        }
        catch{
          case e:Exception=> println("WARNING: Unable to load " + name)
        }
      }
    }
    else
      processFile(fileName, resourceUid)


    persistenceManager.shutdown

  }
  def processFile(fileName:String, dataResourceUid:String)={
    println("Starting to load " + fileName)
    val csvReader = new CSVReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));

    var count = 0

    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis

    val columnsHeaders = csvReader.readNext
    var columns:Array[String] = csvReader.readNext
    //val occMap = new scala.collection.mutable.HashMap[String, Map[String, String]]()
    //val drMap = new scala.collection.mutable.HashMap[String, Map[String, String]]()
    while (columns != null) {

      count += 1
      //the newly assigned record UUID

      val fieldTuples = {
          for {
             i <- 0 until columns.length-1
             if(columns(i).trim.length > 0)
          } yield (columnsHeaders(i), columns(i))
      }

       //lookup the column
     // val recordUuid = UUID.randomUUID.toString
      var map = Map(fieldTuples map {s => (s._1, s._2)} : _*)
      if(!map.isEmpty){
          val uuid = UUID.randomUUID.toString
          var resourceUid = if(dataResourceUid != null) dataResourceUid else map.getOrElse("dataResourceUid", null)
          if(resourceUid != null){
//              val cn = map.get("catalogNumber").getOrElse("")
//              val cc = map.get("collectionCode").getOrElse("")
//              val ic = map.get("institutionCode").getOrElse("")
//              val id = map.get("portalId").getOrElse("")
//              //Use the supplied dataResourceUid for the resourceId
              if(map.contains("dataResourceUid")){

                map =performDRPreprocessing(map, resourceUid)
              }
              if(map != null){
                    val uniqueTerms = retrieveConnectionDetails(resourceUid)
                  val uniqueTermValues = if(uniqueTerms != null && uniqueTerms.size>0) uniqueTerms.map(t => map.getOrElse(t, "")) else List(uuid)
                  val uniqueID =(List(resourceUid) ::: uniqueTermValues).mkString("|")


                  //val recordUuid = persistenceManager.put(recordUuid, "occ", map)
                  persistenceManager.put(uniqueID, "occ", map + ("rowKey"->uniqueID,"uuid"->uuid, "lastLoadTime"->loadTime))
                  //persistenceManager.put(uniqueID, "dr", "uuid", recordUuid)


              if (count % 1000 == 0 && count > 0) {

                finishTime = System.currentTimeMillis

                println(count +", >> Row Key: " + uniqueID+ ", >>  UUID: " + uuid + ", records per sec: " + 1000 / (((finishTime - startTime).toFloat) / 1000f))
                startTime = System.currentTimeMillis
              }
            }
          }
      }
      columns = csvReader.readNext
    }
     println("Finished "+fileName+" loading. Records processed: " + count)
  }

    def performDRPreprocessing(map:Map[String, String], resourceUid:String):Map[String,String]={
      //
      if(drToSwapRecordToCatalog.contains(resourceUid)){
        var catNum = map.getOrElse("occurrenceID", "")
        if(resourceUid == "dr359" && catNum.contains("."))
          catNum = catNum.substring(0, catNum.indexOf("."))

        val retMap = map - "occurrenceID" + ("catalogNumber"->catNum)
        return retMap

      }
      else if(drToIgnore.contains(resourceUid)){
        return null
      }
      map
    }


    def retrieveConnectionDetails(resourceUid: String): List[String] = {
      //check to see if it is in the cache first
      if(drCache.containsKey(resourceUid))
        drCache.get(resourceUid)
      else{
        try{
          println("Attempting to get information from http://collections.ala.org.au/ws/dataResource/"  + resourceUid + ".json" )
          val json = Source.fromURL("http://collections.ala.org.au/ws/dataResource/" + resourceUid + ".json").getLines.mkString
          val map = JSON.parseFull(json).get.asInstanceOf[Map[String, AnyRef]]
          val connectionParameters = JSON.parseFull(map("connectionParameters").asInstanceOf[String]).get.asInstanceOf[Map[String, AnyRef]]

          val uniqueTerms: List[String] = connectionParameters.getOrElse("termsForUniqueKey", List[String]()).asInstanceOf[List[String]]
          drCache.put(resourceUid, uniqueTerms)
          uniqueTerms
        }
        catch{
          case e:Exception => {
              drCache.put(resourceUid, null);
              null
          }
        }
      }
    }
}
