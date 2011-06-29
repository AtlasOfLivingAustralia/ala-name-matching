package au.org.ala.biocache

import collection.JavaConversions
import org.apache.cassandra.thrift.{SlicePredicate, Column, ConsistencyLevel}
//import org.wyki.cassandra.pelops.{Policy, Selector, Pelops}
import org.scale7.cassandra.pelops.{Cluster,Pelops,Selector, Bytes}
import collection.mutable.ListBuffer
import org.slf4j.LoggerFactory
import com.google.inject.name.Named
import com.google.inject.Inject
import java.lang.Class
import com.mongodb.casbah.Imports._
import java.util.UUID

/**
 * This trait should be implemented for Cassandra,
 * but could also be implemented for Google App Engine
 * or another backend supporting basic key value pair storage
 *
 * @author Dave Martin (David.Martin@csiro.au)
 */
trait PersistenceManager {

    /**
     * Get a single property.
     */
    def get(uuid:String, entityName:String, propertyName:String) : Option[String]

    /**
     * Get a key value pair map for this record.
     */
    def get(uuid:String, entityName:String): Option[Map[String, String]]

    /**
     * Retrieve an array of objects from a single column.
     */
    def getList[A](uuid: String, entityName: String, propertyName: String, theClass:java.lang.Class[_]) : List[A]

    /**
     * Put a single property.
     */
    def put(uuid:String, entityName:String, propertyName:String, propertyValue:String) : String

    /**
     * Put a set of key value pairs.
     */
    def put(uuid:String, entityName:String, keyValuePairs:Map[String, String]) : String

    /**
     * Add a batch of properties.
     */
    def putBatch(entityName:String, batch:Map[String, Map[String,String]])

    /**
     * @overwrite if true, current stored value will be replaced without a read.
     */
    def putList[A](uuid: String, entityName: String, propertyName: String, objectList:List[A], theClass:java.lang.Class[_], overwrite: Boolean) : String

    /**
     * Page over all entities, passing the retrieved UUID and property map to the supplied function.
     * Function should return false to exit paging.
     */
    def pageOverAll(entityName:String, proc:((String, Map[String,String])=>Boolean),startUuid:String="", pageSize:Int = 1000)

    /**
     * Page over the records, retrieving the supplied columns only.
     */
    def pageOverSelect(entityName:String, proc:((String, Map[String,String])=>Boolean), startUuid:String, pageSize:Int, columnName:String*)

    /**
     * Select the properties for the supplied record UUIDs
     */
    def selectRows(uuids:Array[String],entityName:String,propertyNames:Array[String],proc:((Map[String,String])=>Unit))

    /**
     * The column to delete.
     */
    def deleteColumn(uuid:String, entityName:String, columnName:String)

    /**
     * Close db connections etc
     */
    def shutdown

    /**
     * The field delimiter to use
     */
    def fieldDelimiter = '.'
}

/**
 * Cassandra based implementation of a persistence manager.
 * This should maintain most of the cassandra logic
 *
 * This has been modified to support cassandra 0.7.x.  For
 * cassandra 0.6.x support see:
 * http://code.google.com/p/ala-portal/source/browse/tags/biocache-store-cass0.6.x
 *
 * Major change:  The thrift API now works with ByteBuffer instead of byte[]
 *
 */
class CassandraPersistenceManager @Inject() (
    @Named("cassandraHosts") val host:String = "localhost",
    @Named("cassandraPort") val port:Int = 9160,
    @Named("cassandraPoolName") val poolName:String = "biocache-store-pool",
    @Named("cassandraKeyspace") val keyspace:String = "occ") extends PersistenceManager {

    protected val logger = LoggerFactory.getLogger("CassandraPersistenceManager")

    val maxColumnLimit = 10000
    import JavaConversions._
    logger.info("Initialising cassandra connection pool with pool name: " + poolName)
    logger.info("Initialising cassandra connection pool with hosts: " + host)
    logger.info("Initialising cassandra connection pool with port: " + port)
    val cluster = new Cluster(host,port)
    Pelops.addPool(poolName, cluster, keyspace)

    /**
     * Retrieve an array of objects, parsing the JSON stored.
     */
    def get(uuid:String, entityName:String) = {
        val selector = Pelops.createSelector(poolName)
        val slicePredicate = Selector.newColumnsPredicateAll(true, maxColumnLimit)
        try {
            val columnList = selector.getColumnsFromRow(entityName,uuid, slicePredicate, ConsistencyLevel.ONE)
            if(columnList.isEmpty){
                None
            } else {
                Some(columnList2Map(columnList))
            }
        } catch {
            case e:Exception => logger.debug(e.getMessage, e); None
        }
    }

    /**
     * Retreive the column value, handling NotFoundExceptions from cassandra thrift.
     */
    def get(uuid:String, entityName:String, propertyName:String) = {
      try {
          val selector = Pelops.createSelector(poolName)
          val column = selector.getColumnFromRow(entityName,uuid, propertyName, ConsistencyLevel.ONE)
          Some(new String(column.value.array,"UTF-8"))
      } catch {
          case e:Exception => logger.debug(e.getMessage, e); None
      }
    }

    /**
     * Store the supplied batch of maps of properties as separate columns in cassandra.
     */
    def putBatch(entityName: String, batch: Map[String, Map[String, String]]) = {
        val mutator = Pelops.createMutator(poolName)
        batch.foreach(uuidMap => {
            val uuid = uuidMap._1
            val keyValuePairs = uuidMap._2
            keyValuePairs.foreach( keyValue => {
              mutator.writeColumn(entityName, uuid, mutator.newColumn(keyValue._1, keyValue._2))
            })
        })
        mutator.execute(ConsistencyLevel.ONE)
    }

    /**
     * Store the supplied map of properties as separate columns in cassandra.
     */
    def put(uuid:String, entityName:String, keyValuePairs:Map[String, String]) = {

        val recordId = { if(uuid != null) uuid else UUID.randomUUID.toString }

        val mutator = Pelops.createMutator(poolName)
        keyValuePairs.foreach( keyValue => {
          //NC: only add the column if the value is not null
          if(keyValue._2!=null)
            mutator.writeColumn(entityName, recordId, mutator.newColumn(keyValue._1, keyValue._2))
        })
        //add the recordId to the columns if it has been generated.  This makes uuid value reads faster that ByteBuffer key conversions
        if(uuid == null){
          mutator.writeColumn(entityName, recordId, mutator.newColumn("uuid", recordId))
        }
        mutator.execute(ConsistencyLevel.ONE)
        recordId
    }

    /**
     * Store the supplied property value in the column
     */
    def put(uuid:String, entityName:String, propertyName:String, propertyValue:String) = {
        val recordId = { if(uuid != null) uuid else UUID.randomUUID.toString }
        val mutator = Pelops.createMutator(poolName)
        mutator.writeColumn(entityName, recordId, mutator.newColumn(propertyName, propertyValue))
        //add the recordId to the columns if it has been generated.  This makes uuid value reads faster that ByteBuffer key conversions
        if(uuid == null){
          mutator.writeColumn(entityName, recordId, mutator.newColumn("uuid", recordId))
        }
        mutator.execute(ConsistencyLevel.ONE)
        recordId
    }

    /**
     * Retrieve the column value, and parse from JSON to Array
     */
    def getList[A](uuid:String, entityName:String, propertyName:String, theClass:java.lang.Class[_]): List[A] = {
        val column = getColumn(uuid, entityName, propertyName)
        if (column.isEmpty) {
            List()
        } else {
            val json = new String(column.get.getValue)
            //Json.toList(json,theClass)
            Json.toListWithGeneric(json, theClass)
        }
    }

    /**
     * Store arrays in a single column as JSON.
     */
    //def putList(uuid:String, entityName:String, propertyName:String, newList:List[AnyRef], overwrite:Boolean) = {
    def putList[A](uuid: String, entityName: String, propertyName: String, newList: List[A], theClass:java.lang.Class[_], overwrite: Boolean) = {

        val recordId = { if(uuid != null) uuid else UUID.randomUUID.toString }
        //initialise the serialiser
//        val gson = new Gson
        val mutator = Pelops.createMutator(poolName)

        if (overwrite) {
            //val json = Json.toJSON(newList)
            val json:String = Json.toJSONWithGeneric(newList)
            mutator.writeColumn(entityName, recordId, mutator.newColumn(propertyName, json))
        } else {

            //retrieve existing values
            val column = getColumn(uuid, entityName, propertyName)
            //if empty, write, if populated resolve
            if (column.isEmpty) {
                //write new values
                val json:String = Json.toJSONWithGeneric(newList)
                mutator.writeColumn(entityName, recordId, mutator.newColumn(propertyName, json))
            } else {
                //retrieve the existing objects
                val currentJson = new String(column.get.getValue)
                var currentList = Json.toListWithGeneric(currentJson, theClass)

                var written = false
                var buffer = new ListBuffer[A]

                for (theObject <- currentList) {
                    if (!newList.contains(theObject)) {
                        //add to buffer
                        buffer + theObject
                    }
                }

                //PRESERVE UNIQUENESS
                buffer ++= newList

                // check equals
                //val newJson = Json.toJSON(buffer.toList)
                val newJson:String = Json.toJSONWithGeneric(buffer.toList)
                mutator.writeColumn(entityName, recordId, mutator.newColumn(propertyName, newJson))
            }
        }
        mutator.execute(ConsistencyLevel.ONE)
        recordId
    }

    /**
     * Generic page over method. Individual pageOver methods should provide a slicePredicate that
     * is used to determine the columns that are returned...
     *
     * @param startUuid, The uuid of the occurrence at which to start the paging
     */
    def pageOver(entityName:String,proc:((String, Map[String,String])=>Boolean), pageSize:Int, slicePredicate:SlicePredicate,startUuid:String="")={
      val selector = Pelops.createSelector(poolName)
      var startKey = new Bytes(startUuid.getBytes)
      var endKey = new Bytes("".getBytes)
      var keyRange = Selector.newKeyRange(startKey, endKey, pageSize+1)
      var hasMore = true
      var counter = 0
      //Please note we are not paging by UTF8 because it is much slower
      var columnMap = selector.getColumnsFromRows(entityName, keyRange, slicePredicate, ConsistencyLevel.ONE)
      var continue = true
      while (!columnMap.isEmpty && continue) {
        val columnsObj = List(columnMap.keySet.toArray : _*)
        //convert to scala List
        val keys = columnsObj.asInstanceOf[List[Bytes]]
        startKey = keys.last
        for(buuid<-keys){
          val columnList = columnMap.get(buuid)
          //procedure a map of key value pairs
          val map = columnList2Map(columnList)
          val uuid = map.getOrElse("uuid", buuid.toUTF8)
          //pass the record ID and the key value pair map to the proc
          continue = proc(uuid, map)
        }
        counter += keys.size
        keyRange = Selector.newKeyRange(startKey, endKey, pageSize+1)
        columnMap = selector.getColumnsFromRows(entityName, keyRange, slicePredicate, ConsistencyLevel.ONE)
        columnMap.remove(startKey)
      }
      println("Finished paging. Total count: "+counter)
    }

    /**
     * Pages over all the records with the selected columns.
     * @param columnName The names of the columns that need to be provided for processing by the proc
     */
    def pageOverSelect(entityName:String, proc:((String, Map[String,String])=>Boolean), startUuid:String, pageSize:Int, columnName:String*){
      val slicePredicate = Selector.newColumnsPredicate(columnName:_*)
      pageOver(entityName, proc, pageSize, slicePredicate, startUuid)
    }
//
//    /**
//     * Pages over all the records with the selected columns.
//     * @param columnName The names of the columns that need to be provided for processing by the proc
//     */
//    def pageOverSelect(entityName:String, proc:((String, Map[String,String])=>Boolean), pageSize:Int, colNames:Array[String]){
//      val slicePredicate = Selector.newColumnsPredicate(colNames:_*)
//      pageOver(entityName, proc, pageSize, slicePredicate)
//    }

    /**
     * Iterate over all occurrences, passing the objects to a function.
     * Function returns a boolean indicating if the paging should continue.
     *
     * @param occurrenceType
     * @param proc
     * @param startUuid, The uuid of the occurrence at which to start the paging
     */
    def pageOverAll(entityName:String, proc:((String, Map[String,String])=>Boolean),startUuid:String="", pageSize:Int = 1000) {
      val slicePredicate = Selector.newColumnsPredicateAll(true, maxColumnLimit)
      pageOver(entityName, proc, pageSize, slicePredicate,startUuid)
    }

    /**
     * Select fields from rows and pass to the supplied function.
     */
    def selectRows(uuids:Array[String], entityName:String, fields:Array[String], proc:((Map[String,String])=>Unit)) {
       val selector = Pelops.createSelector(poolName)
       val slicePredicate = Selector.newColumnsPredicate(fields:_*)
       //var slicePredicate = new SlicePredicate
       //slicePredicate.setColumn_names(fields.toList.map(_.getBytes))

       //retrieve the columns
       var columnMap = selector.getColumnsFromRowsUtf8Keys(entityName, uuids.toList, slicePredicate, ConsistencyLevel.ONE)

       //write them out to the output stream
       val keys = List(columnMap.keySet.toArray : _*)

       for(key<-keys){
         val columnsList = columnMap.get(key)
         val fieldValues = columnsList.map(column => (new String(column.getName, "UTF-8"),new String(column.getValue, "UTF-8"))).toArray
         val map = scala.collection.mutable.Map.empty[String,String]
         for(fieldValue <-fieldValues){
           map(fieldValue._1) = fieldValue._2
         }
         proc(map.toMap)
       }
     }

    /**
     * Convert a set of cassandra columns into a key-value pair map.
     */
    protected def columnList2Map(columnList:java.util.List[Column]) : Map[String,String] = {
        val tuples = {
            for(column <- columnList)
              yield (new String(column.getName, "UTF-8"), new String(column.getValue, "UTF-8"))
        }
        //convert the list
        Map(tuples map {s => (s._1, s._2)} : _*)
    }

    /**
     * Convienience method for accessing values.
     */
    protected def getColumn(uuid:String, columnFamily:String, columnName:String): Option[Column] = {
        try {
            val selector = Pelops.createSelector(poolName)
            Some(selector.getColumnFromRow(columnFamily, uuid, columnName, ConsistencyLevel.ONE))
        } catch {
            case e:Exception => {
                logger.debug(e.getMessage + " for " + uuid + " - " + columnFamily + " - " +columnName)
                None //expected behaviour when row doesnt exist
            }
        }
    }

    def shutdown = Pelops.shutdown
    
    /**
     * Delete the value for the supplied column 
     */
    def deleteColumn(uuid:String, entityName:String, columnName:String)={
      if(uuid != null && entityName != null && columnName != null){
        val mutator = Pelops.createMutator(poolName)
        mutator.deleteColumn(entityName, uuid, columnName)
        mutator.execute(ConsistencyLevel.ONE)
      }
    }
}


/**
 * To be added.....
 */
class MongoDBPersistenceManager @Inject()(
    @Named("mongoHost") host:String = "localhost", @Named("mongoDatabase") db:String = "occ", @Named("padOcc") pad:Boolean=false) extends PersistenceManager {

    import JavaConversions._
    val uuidColumn = "_id"
    val mongoConn = MongoConnection(host)
    val largeMap = Map("DUMMY"-> "X" *10000)

    override def fieldDelimiter = '_'

    def get(uuid: String, entityName: String) = {
        val mongoColl = mongoConn(db)(entityName)
        val q = MongoDBObject(uuidColumn -> uuid)
        val result = mongoColl.findOne(q)
        if(!result.isEmpty){
            Some(result.get.toMap.map({ case(key,value) => (key.toString, value.toString) }).toMap)
        } else {
            None
        }
    }

    def get(uuid: String, entityName: String, propertyName: String) = {
        val mongoColl = mongoConn(db)(entityName)
        val query = MongoDBObject(uuidColumn -> uuid)
        val fields = MongoDBObject(propertyName -> 1)
        val map = mongoColl.findOne(query,fields)
        if(!map.isEmpty){
            Some(map.get.toMap.get(propertyName).asInstanceOf[String])
        } else {
            None
        }
    }

    def getList[A](uuid: String, entityName: String, propertyName: String, theClass:java.lang.Class[_]) = {

        val mongoColl = mongoConn(db)(entityName)
        val query = MongoDBObject(uuidColumn -> uuid)
        val fields = MongoDBObject(propertyName -> 1)
        val map = mongoColl.findOne(query,fields)
        val propertyInJSON = map.get(propertyName).asInstanceOf[String]
        if(propertyInJSON.isEmpty){
            List()
        } else {
            Json.toListWithGeneric(propertyInJSON,theClass)
        }
    }

    def put(uuid: String, entityName: String, propertyName: String, propertyValue: String) = {

        val mongoColl = mongoConn(db)(entityName)
        if(uuid!=null){
            val set = $set( (propertyName,propertyValue) )
            //Allow "upserts" so that missing records are inserted...
            mongoColl.update(Map(uuidColumn -> uuid), set, true, false)
            uuid
        } else {
            val recordId = { if(uuid != null) uuid else UUID.randomUUID.toString }
            val mongoColl = mongoConn(db)(entityName)
            mongoColl.save(Map(uuidColumn -> recordId,propertyName -> propertyValue ))
            recordId
        }


//        val mongoColl = mongoConn(db)(entityName)
//        if(uuid != null){
//            val set = $set( (propertyName,propertyValue) )
//            mongoColl.update(Map(uuidColumn -> recordId), set, false, false)
//            uuid
//        } else {
//            val newInsert = Map(propertyName -> propertyValue).asDBObject
//            val writeResult = mongoColl.insert(newInsert)
//            if(newInsert._id.isEmpty){
//                throw new RuntimeException("Insert failed.")
//            } else {
//                newInsert._id.get.toString
//            }
//        }
    }
    
    def put(uuid: String, entityName: String, keyValuePairs: Map[String, String]) = {

        val mongoColl = mongoConn(db)(entityName)
        if(uuid!=null){
            val mapToSave = keyValuePairs.filter( { case (key, value) => { value!=null && !value.trim.isEmpty } })
            if(!mapToSave.isEmpty){
              //We need to wrap it in a $set to allow the existing values for the document to remain unchanged
              val setToSave = $set(mapToSave.toList:_*)

              //Allow "upserts" so that missing records are inserted...
              mongoColl.update(Map(uuidColumn -> uuid),setToSave, true, false)
              
            }
            uuid
        } else {
                        
            val recordId = { if(uuid != null) uuid else UUID.randomUUID.toString }
            val mongoColl = mongoConn(db)(entityName)
            if(entityName == "occ" && pad){
              mongoColl.save(Map(uuidColumn -> recordId) ++ keyValuePairs ++ largeMap)
              //now remove the padding from the record
              mongoColl.update(Map(uuidColumn->recordId), $unset("DUMMY"))
            }
            else{
              mongoColl.save(Map(uuidColumn -> recordId) ++ keyValuePairs)
            }
            
            
            recordId
        }
//        val mongoColl = mongoConn(db)(entityName)
//        if(uuid != null){
//            mongoColl.update(Map(uuidColumn -> uuid), keyValuePairs.asDBObject, false, false)
//            uuid
//        } else {
//            val newInsert = keyValuePairs.asDBObject
//            val writeResult = mongoColl.insert(newInsert)
//            if(newInsert._id.isEmpty){
//                throw new RuntimeException("Insert failed.")
//            } else {
//                newInsert._id.get.toString
//            }
//        }
    }

    def putList[A](uuid: String, entityName: String, propertyName: String, objectList: List[A], theClass:java.lang.Class[_], overwrite: Boolean) = {

        if(!overwrite){
            throw new RuntimeException("Overwrite currently not supported.")
        }

        //TODO support append (overwrite = false)
        val mongoColl = mongoConn(db)(entityName)
        val json = Json.toJSONWithGeneric(objectList)
        if(uuid !=null){
            val set = $set( (propertyName,json) )
            mongoColl.update(Map(uuidColumn -> uuid), set, false, false)
            uuid
        } else {
            val recordId = { if(uuid != null) uuid else UUID.randomUUID.toString }
            mongoColl.save(Map(uuidColumn -> recordId, propertyName -> json) )
            recordId
        }
//        if(uuid != null){
//            val set = $set( (propertyName,json) )
//            mongoColl.update(Map(uuidColumn -> uuid), set, false, false)
//            uuid
//        } else {
//
//            val newInsert = Map(propertyName -> json).asDBObject
//            val writeResult = mongoColl.insert(newInsert)
//            if(newInsert._id.isEmpty){
//                throw new RuntimeException("Insert failed.")
//            } else {
//                newInsert._id.get.toString
//            }
//        }  .
    }

    def putBatch(entityName: String, batch: Map[String, Map[String, String]]) = {
        throw new RuntimeException("currently not implemented")
    }

    def deleteColumn(uuid:String, entityName:String, columnName:String)={
      throw new RuntimeException("currently not implemented")
    }
    def pageOverSelect(entityName: String, proc: (String, Map[String, String]) => Boolean, startUuid:String, pageSize: Int, columnName: String*) = {

        //page through all records
        val mongoColl = mongoConn(db)(entityName)

        var counter = 0
        val fields = MongoDBObject( List() )
        //val fields = MongoDBObject( columnName.map(x => { counter += 1; (columnName -> counter) } ).toSeq )
        val cursor = mongoColl.find(MongoDBObject(),fields,0,pageSize)

        //val cursor = mongoColl.find
        for(dbObject:DBObject <- cursor){
            val map = dbObject.toMap.map({ case(key,value) => (key.toString, value.toString) }).toMap
            proc(map.getOrElse(uuidColumn, ""), map)
        }
    }

    def pageOverAll(entityName: String, proc: (String, Map[String, String]) => Boolean,startUuid:String="", pageSize: Int) = {
        //page through all records
        val mongoColl = mongoConn(db)(entityName)
        //val cursor = mongoColl.find(0,pageSize)
        //Take a snapshot so that each document is only returned once
        val cursor = mongoColl.find.snapshot//sort(MongoDBObject("_id"->1))//.snapshot
        for(dbObject:DBObject <- cursor){
            val map = dbObject.toMap.map({ case(key,value) => (key.toString, value.toString) }).toMap
            //println("ID: " + map.get("_id").get)
            proc(map.getOrElse(uuidColumn, ""), map)
        }
    }

    def selectRows(uuids: Array[String], entityName: String, propertyNames: Array[String], proc: (Map[String, String]) => Unit) = {
       throw new RuntimeException("currently not implemented")
    }

    def shutdown = mongoConn.close
}