package au.org.ala.biocache.util

import au.org.ala.biocache.tool.CreateInvertedIndexOnProperty

/**
 * Created by mar759 on 17/02/2014.
 */
object CreateLayerByTaxon {

  def main(args:Array[String]){
    val keyspace = "occ"
    val indexName = "layerByTaxon"
    val keyOn = Array("scientificName")
    //val keyOnVariable = "el593.p"
    val keyOnVariables = (593 to 895).toList.map(x => "el" + x + ".p").toArray[String]
    val createIndex = new CreateInvertedIndexOnProperty(keyspace)
    createIndex.create(keyspace, indexName, keyOn, keyOnVariables, Integer.MAX_VALUE, false)
  }
}
