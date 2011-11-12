package au.org.ala.biocache.outliers

import au.org.ala.biocache._
import org.apache.cassandra.thrift.{InvalidRequestException, CfDef}
import org.apache.solr.core.CoreContainer
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer
import org.apache.solr.common.{SolrInputDocument, SolrDocument}
import collection.mutable.ListBuffer

object GenerateOutlierIndex {

    import scalaj.collection.Imports._


    def main(args:Array[String]){

          //set the solr home
        System.setProperty("solr.solr.home", "/data/solr/outliers")

        val cc = new CoreContainer.Initializer().initialize
        val solrServer = new EmbeddedSolrServer(cc, "")

        val docbuff = new ListBuffer[SolrInputDocument]

        //page through and create the index
        Config.persistenceManager.pageOverAll("outlier_lookup", (guid, map) => {

            val solrDocument = new SolrInputDocument
            solrDocument.addField("lat_long_taxon_id",guid.replaceAll(":", "_"))

            val parts = guid.split("\\|\\|")

            solrDocument.addField("taxon_concept",parts(0).replaceAll(":", "_"))
            solrDocument.addField("latitude",parts(1))
            solrDocument.addField("longitude",parts(2))

            map.head._2.split(",").foreach(layer => {
                val (layerId, sdOver) = { val keyValue = layer.split(":"); (keyValue(0), keyValue(1).toFloat) }
                for(i <- List(2.0,2.5,3.0,3.5,4.0,4.5,5.0,5.5,6.0,6.5,7.0,7.5,8.0,8.5,9.0,9.5,10.0)){
                    if(sdOver >= i){
                        solrDocument.addField("outlier_for_layer_" + i, layerId)
                    }
                }
            })

            docbuff += solrDocument

            if(docbuff.size>=1000){
                solrServer.add(docbuff.toList.asJava)
                solrServer.commit
                docbuff.clear
            }
            true
        })

        solrServer.add(docbuff.toList.asJava)
        solrServer.commit
        solrServer.optimize
        docbuff.clear
        Config.persistenceManager.shutdown
        cc.shutdown
    }
}


object GenerateOutlierDump {

    def main(args:Array[String]){

      //page through and create the index
      Config.persistenceManager.pageOverAll("outliers", (guid, map) => {

          val (taxonConceptID, recordID) = {
            val values = guid.split("\\|\\|")
            (values.head, values.last)
          }
          val elvalues = map.filter({ case(k,v) => k.startsWith("outlier-el")}).keys.toSet
          for (elvalue <- elvalues){
            printf("%s\t%s\t%s\n", taxonConceptID,recordID, elvalue )
          }
          true
      })
    }
}