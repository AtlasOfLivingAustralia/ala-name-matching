package au.org.ala.biocache.tool

import au.org.ala.biocache.persistence.CassandraPersistenceManager
import au.org.ala.biocache.Config
import org.scale7.cassandra.pelops.Pelops
import org.apache.cassandra.thrift.{ConsistencyLevel, InvalidRequestException, CfDef}
import scala.actors.Actor
import scala.collection.mutable.ListBuffer

/**
 * @Deprecated Cassandra specific
 */
class CreateInvertedIndexOnProperty(keyspace: String) {

  val pm: CassandraPersistenceManager = Config.persistenceManager.asInstanceOf[CassandraPersistenceManager]
  val columnFamilyManager = Pelops.createColumnFamilyManager(pm.cluster, keyspace)

  def create(keyspace: String, indexName: String, keyOn: Array[String], keyOnVariables: Array[String],
             maxRecords: Int = Integer.MAX_VALUE,
             log: Boolean = false) {

    val fieldsToRequest = keyOn ++ keyOnVariables

    try {
      var cdef = new CfDef(keyspace, indexName)
      cdef.column_type = "Standard"
      cdef.comparator_type = "UTF8Type"
      cdef.default_validation_class = "UTF8Type"
      columnFamilyManager.addColumnFamily(cdef)
    } catch {
      case e: InvalidRequestException => columnFamilyManager.truncateColumnFamily(indexName)
    }

    var counter = 0
    val threads = 8
    var ids = 0
    val pool = Array.fill(threads) {
      val p = new IndexHelper(Actor.self, ids, indexName, keyOn, keyOnVariables); ids += 1; p.start
    }

    //page through and create the index
    pm.pageOverSelect(keyspace, (guid, map) => {

      val actor = pool(counter % threads).asInstanceOf[IndexHelper]
      //find a ready actor...
      while (!actor.ready) {
        Thread.sleep(50)
      }
      actor !(guid, map)
      counter = counter + 1
      if (counter % 100 == 0) println("Record:" + counter)
      counter < maxRecords
    }, "", "", 1000, fieldsToRequest: _*)

    //close cassandra connections
    Pelops.shutdown
  }

  class IndexHelper(master: Actor, id: Int, indexName: String, keyOn: Array[String], keyOnVariables: Array[String]) extends Actor {

    println("Initialising thread: " + id)
    val processor = new RecordProcessor
    var received, processedRecords = 0

    def ready = processedRecords == received

    def act {
      println("In (Actor.act) thread: " + id)
      loop {
        react {
          case guidAndMap: (String, Map[String, String]) => {
            process(guidAndMap._1, guidAndMap._2)
          }
        }
      }
    }

    def process(guid: String, map: Map[String, String]) {

      val mutator = Pelops.createMutator(pm.poolName)
      keyOnVariables.foreach(variableName => {
        val key = {
          val keyParts = new ListBuffer[String]
          keyParts += variableName
          for (keyPart <- keyOn) {
            val keyPartValue = map.getOrElse(keyPart, "")
            if (keyPartValue != "") {
              keyParts += keyPartValue
            }
          }
          keyParts.toList.mkString("||")
        }

        val propertyValue = map.getOrElse(variableName, "")
        if (propertyValue != "") {
          mutator.writeColumn(indexName, key, mutator.newColumn(guid, propertyValue))
        }
      })
      mutator.execute(ConsistencyLevel.ONE)
    }
  }

}
