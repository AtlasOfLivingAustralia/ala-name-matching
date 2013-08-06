package au.org.ala.biocache.qa

import au.org.ala.util.OptionParser
import java.io.{FileInputStream, InputStreamReader, File}
import au.com.bytecode.opencsv.CSVReader
import au.org.ala.biocache.{Config, Json, AssertionCodes}
import collection.mutable.ArrayBuffer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.annotation.JsonInclude.Include
import scalaj.http.Http
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.PostMethod

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
 * A class provided as an interim method to load a CSV file that contains a set of assertions to be applied
 * @author Natasha Carter (natasha.carter@csiro.au)
 */
object LoadBulkQas {
   def main(args:Array[String]){
     var filename = ""
     var userId =""
     var userDisplayName=""
     val parser = new OptionParser("CSV assertions bulk load"){
       arg("<filename>", "The absolute path to the CSV file that should be loaded as a bulk annotation. Relies on file structure: recordUuid, assertionType, comment", {v:String => filename = v})
       arg("<userId>", "The id for the user to apply the assertions for",{v:String => userId = v})
       arg("<display name>", "The display name for the user.",{v:String =>userDisplayName = v})

     }
     if (parser.parse(args)){

       val mapper = new ObjectMapper
       mapper.registerModule(DefaultScalaModule)
       mapper.setSerializationInclusion(Include.NON_NULL)
       //perform the CSV bulk load
       val reader =  new CSVReader(new InputStreamReader(new org.apache.commons.io.input.BOMInputStream(new FileInputStream(filename))), ',', '"', '~')
       val headers = reader.readNext()
       var nextLine = headers
       if (headers.length>=3){
         val(uuid,atype,comment) = findRequiredHeaders(headers).get
         val buf = new ArrayBuffer[Map[String,String]]
         while(nextLine != null){
           if (nextLine != headers){
             val recordUuid = if (nextLine(uuid).startsWith("http:")) {
               val path =new java.net.URL(nextLine(uuid)).getPath()
               path.substring(path.lastIndexOf("/") +1)}
             else nextLine(uuid)

             val code = AssertionCodes.getByName(nextLine(atype))
             if (code.isDefined){
                //check to see if the uuid is a URL, if so get the last component
                 val map = Map("recordUuid" -> recordUuid,
                            "code" -> code.get.code.toString(),
                            "comment" ->nextLine(comment))

                buf += map

             } else{
               println("Unable to apply " + nextLine.toList)
             }
           }
           nextLine = reader.readNext()
         }
         val http = new HttpClient
         val post = new PostMethod(Config.biocacheServiceUrl+"/bulk/assertions/add")
         println(Config.biocacheServiceUrl+"/bulk/assertions/add")
         post.addParameter("assertions",mapper.writeValueAsString(buf.toList))
         post.addParameter("userId",userId)
         post.addParameter("userDisplayName",userDisplayName)
         post.addParameter("apiKey",Config.getProperty("apiKey"))
         println(post.toString)
         val responseCode = http.executeMethod(post)
         println("Response: " + responseCode)

       } else{
         println("Need at least 3 columns to load a CSV annotations: uuid, annotation type, comment")
       }

     }
   }
  def findRequiredHeaders(header:Array[String]):Option[(Int,Int,Int)]={
    //TODO potentially we will need to support more heading types

    Some((0,1,2))
  }
}
