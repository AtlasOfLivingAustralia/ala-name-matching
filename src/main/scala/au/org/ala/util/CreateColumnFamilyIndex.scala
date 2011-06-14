package au.org.ala.util

import org.scale7.cassandra.pelops.{Bytes, Pelops}
import collection.JavaConversions
import org.apache.cassandra.thrift._
import org.apache.cassandra.thrift.CfDef
import au.org.ala.biocache._
import collection.mutable.ListBuffer

object CreateTestIndex {

  def main(args:Array[String]){
      val keyspace = "occ"
      val indexName = "taxonConceptLookup"
      val keyOn = Array("scientificName")
      val createIndex = new CreateColumnFamilyIndex
      createIndex.create(keyspace, indexName, keyOn, "basisOfRecord", 1000000, true)
  }
}

object CreateTaxonIndex {

  def main(args:Array[String]){
    val keyspace = "occ"
    val indexName = "el"
    val keyOn = Array("scientificName")
    //val environLayerIDs = ( to 895).toList.map(x => "el" + x + ".p")        //create IDs of the form "e1234.p"
    val field = "el593.p"
    val createIndex = new CreateColumnFamilyIndex
    createIndex.create(keyspace, indexName, keyOn, field, 1000000,false)
  }
}

class CreateColumnFamilyIndex {

    import JavaConversions._

    def create(keyspace:String, indexName:String, keyOn:Array[String], field:String,
               maxRecords:Int = Integer.MAX_VALUE,
               log:Boolean = false){

      val fieldsToRequest = keyOn ++ Array(field)

      val pm:CassandraPersistenceManager = Config.persistenceManager.asInstanceOf[CassandraPersistenceManager]
      val columnFamilyManager = Pelops.createColumnFamilyManager(pm.cluster, keyspace)

      try {
        var cdef = new CfDef(keyspace, indexName)
        cdef.column_type = "Standard"
        cdef.comparator_type = "UTF8Type"
        cdef.default_validation_class = "UTF8Type"
        columnFamilyManager.addColumnFamily(cdef)
      } catch {
        case e:InvalidRequestException => columnFamilyManager.truncateColumnFamily(indexName)
      }

      var counter = 0
      //page through and create the index
      pm.pageOverSelect(keyspace, (guid, map) => {
        //println(map.get(keyOn) +" " + guid)
        //retrieve all the values required to construct the key

        val key = {
            val keyParts = new ListBuffer[String]
            keyParts + field
            for(keyPart <- keyOn){
                val keyPartValue = map.getOrElse(keyPart, "")
                if(keyPartValue != ""){
                   keyParts + keyPartValue
                }
            }
            keyParts.toList.mkString("||")
        }

        //println("Generated key : " + key)
        val mutator = Pelops.createMutator(pm.poolName)
        val propertyValue = map.getOrElse(field, "")
        if(propertyValue != ""){
            if(log) {
                printf("[%s] Index name (CF): %s, Keyed On: %s, Guid (Column): %s,  Value: %s \n", counter.toString, indexName, key, guid,  propertyValue)
            }
            mutator.writeColumn(indexName, key, mutator.newColumn(guid, propertyValue))
        }
        mutator.execute(ConsistencyLevel.ONE)
        counter = counter +1
        counter<maxRecords
      },"",100, fieldsToRequest:_* )

      //close cassandra connections
      Pelops.shutdown
    }
}
