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
package au.org.ala.perfomance

import au.org.ala.biocache.util.{FileHelper, StringConsumer, OptionParser}
import java.util.concurrent.ArrayBlockingQueue
import io.Source
import au.org.ala.biocache.util.{StringConsumer, OptionParser, FileHelper}
import org.junit.Ignore

/**
 * Will perform GET operations on the supplied number of threads.  The URLs for the get operations will be
 * extracted from the supplied file. Each URL shoudl be in a separate thread.
 *
 * This is will test any URLs. It may be better to move this to a utilities project.
 */
@Ignore
object ThreadedServiceTest {
  import FileHelper._
  def main(args:Array[String]){
    var numThreads = 8
    var fileName = ""
    val parser = new OptionParser("threaded service test"){
      arg("<url file>", "The absolute path to the file that contains the urls", {v:String => fileName = v})
      intOpt("t","threads", "The number of threads to URL gets on",{v: Int => numThreads =v})
    }
    if(parser.parse(args)){

      val queue = new ArrayBlockingQueue[String](100)
      var ids =0
      val pool:Array[StringConsumer] = Array.fill(numThreads){
        var counter=0
        var error=0
        var startTime = System.currentTimeMillis
        var finishTime = System.currentTimeMillis
        val id = ids
        val p = new StringConsumer(queue,ids,{url =>
          counter +=1
          try{
          Source.fromURL(url)
          }
          catch{
            case e:Exception => error+=1;println(e.getMessage)
          }
          //debug counter
          if (counter % 10 == 0) {
            finishTime = System.currentTimeMillis
            println(id +" >> " + counter +" >> errors >> " + error +" >> average speed for url request " + ((finishTime -startTime).toFloat)/10000f +" seconds " )
            //println(id + " >> " +counter + " >> Last url : " + url + ", records per sec: " + 10f / (((finishTime - startTime).toFloat) / 1000f))
            startTime = System.currentTimeMillis
          }
        });ids +=1;p.start;p }
      new java.io.File(fileName).foreachLine(line =>{
        //add to the queue
        queue.put(line.trim)
      })
      pool.foreach(t =>t.shouldStop = true)
      pool.foreach(_.join)

    }
  }
}
