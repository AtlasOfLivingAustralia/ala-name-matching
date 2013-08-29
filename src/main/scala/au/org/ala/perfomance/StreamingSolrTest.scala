package au.org.ala.perfomance

import org.apache.solr.client.solrj.impl.HttpSolrServer
import scala.collection.JavaConversions._
import collection.immutable._
import org.apache.solr.client.solrj.StreamingResponseCallback
import java.lang.Float
import org.apache.solr.common.SolrDocument
import org.apache.solr.common._
import org.apache.solr.common.params._

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
 * Test the performance of the stream solr download
 */
object StreamingSolrTest {
  def main(args:Array[String]){
    val server=new HttpSolrServer("http://ala-rufus.it.csiro.au:8080/solr/biocache")
    val params = HashMap(
      "collectionName" -> "biocache",
      "q" -> "*:*",
      //"q" -> """species_guid:urn\:lsid\:biodiversity.org.au\:afd.taxon\:b76f8dcf-fabd-4e48-939c-fd3cafc1887a""",
      //"q" -> """species_guid:"urn:lsid:biodiversity.org.au:afd.taxon:b76f8dcf-fabd-4e48-939c-fd3cafc1887a"""",
      "start" -> "0",
      "rows" -> Int.MaxValue.toString,
      "fl" -> "id,row_key,data_resource_uid,data_provider_uid,collection_uid,institution_uid,species_guid,subspecies_guid")
      //"sort" ->"species_guid asc,subspecies_guid asc")

    val solrParams = new MapSolrParams(params)
    val solrCallback = new StreamingResponseCallback() {
      var maxResults=0l
      var counter =0l
      val start = System.currentTimeMillis
      var startTime = System.currentTimeMillis
      var finishTime = System.currentTimeMillis
      def streamSolrDocument(doc: SolrDocument) {
        //println(doc.getFieldValueMap)
        //println(new java.util.Date())
        //exit(1)
        counter+=1
        if (counter%100000==0){
          finishTime = System.currentTimeMillis
          println(counter + " >> Last record : " + doc.getFieldValueMap + ", records per sec: " +
            100000.toFloat / (((finishTime - startTime).toFloat) / 1000f))
          startTime = System.currentTimeMillis
        }
      }

      def streamDocListInfo(numFound: Long, start: Long, maxScore: Float) {
        println("NumbFound: " + numFound +" start: " +start + " maxScore: " +maxScore)
        println(new java.util.Date())
        startTime = System.currentTimeMillis
        //exit(-2)
        maxResults = numFound
      }
    }
    println(new java.util.Date())
    server.queryAndStreamResponse(solrParams, solrCallback)
  }
}
