package au.org.ala.biocache.qa

import au.org.ala.biocache.{AssertionCodes, QualityAssertion, Config}
import java.util.concurrent.ArrayBlockingQueue
import au.org.ala.util.{FileHelper, OptionParser, GenericConsumer, StringConsumer}
import java.io.File
import collection.mutable.ArrayBuffer
import org.slf4j.LoggerFactory

/*
 * Copyright (C) 2012 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */

/**
 *  A class that is responsible for marking a set of record passed for a specific test.
 *
 *  Mostly used for the offline processing
 * @author Natasha Carter (natasha.carter@csiro.au)
 */
object QaPasser{
  import FileHelper._
  def main(args:Array[String]){
    var qa:Option[QualityAssertion] = None
    var deleteColumns:Option[List[String]] =None
    var threads=16
    var file="/data/offline/rowkeys"
    val parser = new OptionParser(""){
      arg("<file>","The file to load the rowkeys from",{v:String => file = v})
      arg("<qa>", "The QA to pass", { v: String => {
        //QualityAssertion(AssertionCodes.SPECIES_OUTSIDE_EXPERT_RANGE
          val errorCode=AssertionCodes.getByName(v)
          if(errorCode.isDefined){
            qa = Some(QualityAssertion(errorCode.get,1))
          }
      } })
      opt("dc","delete","CSV list of columns to delete",{v:String => deleteColumns = Some(v.split(",").toList)})
      intOpt("t","threads","The number of threads to perform the operation on" , {v:Int=>threads =v})
    }
    if (parser.parse(args)){
      if(qa.isDefined){
        val qaPasser = new QaPasser(qa.get, threads, deleteColumns=deleteColumns)
        new File(file).foreachLine{ line =>
          qaPasser.markRecord(line)
        }
        qaPasser.stop()
      }
    }
  }
}
class QaPasser(qa:QualityAssertion, numThreads:Int, isUuid:Boolean=false, deleteColumns:Option[List[String]]=None) {
  val logger = LoggerFactory.getLogger("QaPasser")
  val queue = new ArrayBlockingQueue[String](500000)
  var ids =0
  val pool:Array[GenericConsumer[String]] = Array.fill(numThreads){
      var counter=0
      var startTime = System.currentTimeMillis
      var finishTime = System.currentTimeMillis

      val thread = new GenericConsumer[String](queue, ids, (value,id)=>{
        counter+=1
        //the value needs to have the QA applied to it
        val rowKey = if (isUuid) Config.occurrenceDAO.getRowKeyFromUuid(value) else Some(value)
        //now assign the QA to the record
        if (rowKey.isDefined){
          Config.occurrenceDAO.addSystemAssertion(rowKey.get,qa,replaceExistCode = true)
          if (deleteColumns.isDefined){
            Config.persistenceManager.deleteColumns(value, "occ",deleteColumns.get:_*)
          }
         // println(rowKey.toString + " has passed " + qa)
        }

        if (counter % 1000 == 0) {
          finishTime = System.currentTimeMillis
          logger.info(counter +">>"+id+ " >> Last key : " + value + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f) +" " + queue.size())
          startTime = System.currentTimeMillis
        }

      })
      thread.start()
      ids+=1
      thread
  }
  def markRecord(key:String){
    queue.put(key)
  }
  def markRecords(rowKeys:List[String]){
    rowKeys.foreach(key=>{
      queue.put(key)
    })
  }
  def stop(){
    logger.info("Stopping the QAPasser " + queue.size())
    pool.foreach(t =>t.shouldStop = true)
    pool.foreach(_.join)
  }
}
