package au.org.ala.util

import org.scale7.cassandra.pelops.Pelops
import collection.JavaConversions
import org.apache.cassandra.thrift._
import org.apache.cassandra.thrift.CfDef
import au.org.ala.biocache._
import collection.mutable.ListBuffer
import actors.Actor

//
//object CreateTestIndex {
//
//  def main(args:Array[String]){
//      val keyspace = "occ"
//      val indexName = "taxonConceptLookup"
//      val keyOn = Array("scientificName")
//      val createIndex = new CreateInvertedIndexOnProperty
//      createIndex.create(keyspace, indexName, keyOn, "basisOfRecord", 1000000, true)
//  }
//}

//object CreateTaxonUUIDIndex {
//
//  def main(args:Array[String]){
//    val keyspace = "occ"
//    val indexName = "taxonIndex"
//    val keyOn = Array("scientificName")
//    val propertiesToStore = Array("el593.p","decimalLatitude","decimalLongitude")
//    val createIndex = new CreateCompositeKeyIndex
//    createIndex.create(keyspace, indexName, keyOn, propertiesToStore, 1000000,false)
//  }
//}

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

class CreateCompositeKeyIndex {

    import JavaConversions._

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
        //println(map.get(keyOn) +" " + guid)
        //retrieve all the values required to construct the key

        val key = {
            val keyParts = new ListBuffer[String]
            for(keyPart <- keyOn){
                val keyPartValue = map.getOrElse(keyPart, "")
                if(keyPartValue != ""){
                   keyParts + keyPartValue
                }
                keyParts + guid
            }
            keyParts.toList.mkString("||")
        }

        //println("Generated key : " + key)
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
      },100, fieldsToRequest:_* )

      //close cassandra connections
      Pelops.shutdown
    }
}

class CreateInvertedIndexOnProperty(keyspace:String) {

    import JavaConversions._

    val pm:CassandraPersistenceManager = Config.persistenceManager.asInstanceOf[CassandraPersistenceManager]
    val columnFamilyManager = Pelops.createColumnFamilyManager(pm.cluster, keyspace)

    def create(keyspace:String, indexName:String, keyOn:Array[String], keyOnVariables:Array[String],
               maxRecords:Int = Integer.MAX_VALUE,
               log:Boolean = false){

      val fieldsToRequest = keyOn ++ keyOnVariables

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
      val threads = 8
      var ids = 0
      val pool = Array.fill(threads){ val p = new IndexHelper(Actor.self,ids, indexName, keyOn, keyOnVariables); ids+=1; p.start }

      //page through and create the index
      pm.pageOverSelect(keyspace, (guid, map) => {

        val actor = pool(counter % threads).asInstanceOf[IndexHelper]
        //find a ready actor...
        while(!actor.ready){ Thread.sleep(50) }
        actor ! (guid, map)
        counter = counter + 1
        if (counter % 100 == 0)   println("Record:"+counter)
        counter < maxRecords
      //},"",10000)
      },1000, fieldsToRequest:_* )

      //close cassandra connections
      Pelops.shutdown
    }

    class IndexHelper (master:Actor, id:Int, indexName:String, keyOn:Array[String], keyOnVariables:Array[String])  extends Actor  {

        println("Initialising thread: "+id)
        val processor = new RecordProcessor
        var received, processedRecords = 0
        def ready = processedRecords == received

        def act {
            println("In (Actor.act) thread: "+id)
            loop{
              react {
                case guidAndMap : (String,Map[String,String]) => {
                    process(guidAndMap._1, guidAndMap._2)
                }
              }
            }
        }

        def process(guid:String, map:Map[String,String]){

            val mutator = Pelops.createMutator(pm.poolName)
            keyOnVariables.foreach( variableName => {
                val key = {
                    val keyParts = new ListBuffer[String]
                    keyParts + variableName
                    for(keyPart <- keyOn){
                        val keyPartValue = map.getOrElse(keyPart, "")
                        if(keyPartValue != ""){
                           keyParts + keyPartValue
                        }
                    }
                    keyParts.toList.mkString("||")
                }

                val propertyValue = map.getOrElse(variableName, "")
                if(propertyValue != ""){
                    mutator.writeColumn(indexName, key, mutator.newColumn(guid, propertyValue))
                }
            })
            mutator.execute(ConsistencyLevel.ONE)
        }
    }
}