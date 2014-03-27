package au.org.ala.biocache.tool

import au.org.ala.biocache.persistence.CassandraPersistenceManager
import org.scale7.cassandra.pelops.Pelops
import org.apache.cassandra.thrift.{ConsistencyLevel, InvalidRequestException, CfDef}
import au.org.ala.biocache.Config
import scala.collection.mutable.ListBuffer

/**
 * Cassandra specific tool for creating a composite key.
 * @Deprecated Cassandra specific
 */
class CreateCompositeKeyIndex {

  def create(keyspace:String, indexName:String, keyOn:Array[String], propertiesToStore:Array[String],
               maxRecords:Int = Integer.MAX_VALUE,
               log:Boolean = false){

      val fieldsToRequest = keyOn ++ propertiesToStore
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
        //retrieve all the values required to construct the key
        val key = {
            val keyParts = new ListBuffer[String]
            for(keyPart <- keyOn){
                val keyPartValue = map.getOrElse(keyPart, "")
                if(keyPartValue != ""){
                   keyParts += keyPartValue
                }
                keyParts += guid
            }
            keyParts.toList.mkString("||")
        }

        val mutator = Pelops.createMutator(pm.poolName)
        for(propertyName <- propertiesToStore){
            val propertyValue = map.getOrElse(propertyName, "")
            if(propertyValue != ""){
                    printf("[%s] Index name (CF): %s, Keyed On: %s, Guid (Column): %s,  Value: %s \n", counter.toString, indexName, key, propertyName,  propertyValue)
                mutator.writeColumn(indexName, key, mutator.newColumn(propertyName, propertyValue))
            }
        }
        mutator.execute(ConsistencyLevel.ONE)
        counter = counter +1
        if (counter % 100 == 0) {println("record: "+counter)}

        counter<maxRecords
      }, "", "", 100, fieldsToRequest:_* )

      //close cassandra connections
      Pelops.shutdown
    }
}
