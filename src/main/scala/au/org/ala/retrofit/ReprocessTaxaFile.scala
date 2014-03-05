package au.org.ala.retrofit
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


import au.com.bytecode.opencsv.CSVReader
import java.io.{FileInputStream, InputStreamReader, File}
import au.org.ala.biocache.Config
import io.Source
import java.net.URL
import au.org.ala.util.{RecordProcessor, IndexRecords}

/**
 * Reprocesses and indexes a file a taxa.
 */
object ReprocessTaxaFile {

  def main(args:Array[String]){
    val reader =  new CSVReader(new InputStreamReader(new org.apache.commons.io.input.BOMInputStream(new FileInputStream(new File(args(0))))), ',', '"', '~')
    var line = reader.readNext()
    var count =0
    var numberRecords=0
    val processor = new RecordProcessor
    val buffer = new scala.collection.mutable.ArrayBuffer[String]
    while(line != null){
      val url = Config.biocacheServiceURL + "/occurrences/facets/download?q=lsid:"+line(0)+"&facets=row_key"
      val values=Source.fromURL(new URL(url)).mkString.split("\n").map(v=>if(v.length >2)v.substring(1,v.length-1) else v)

      println(line(0) + " " + line(1) + " :: " + values.length)
      if(values.length>1){
        //processor.processRecords(values.toList)
        //IndexRecords.indexList(values.toList)
        buffer ++= values
        numberRecords += values.length
      }
      count += 1
      line = reader.readNext()

    }
    println("Starting to process " + new java.util.Date())
    val list = buffer.toList
    processor.processRecords(list)
    println("Starting to index " + new java.util.Date())
    IndexRecords.indexList(list)

    println("Processed " + count +" taxa. With  " + numberRecords + " reprocessed and indexed" )
  }

}
