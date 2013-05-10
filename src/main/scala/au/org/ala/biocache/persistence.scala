package au.org.ala.biocache

import collection.JavaConversions
import scala.collection.mutable.HashMap
import org.apache.cassandra.thrift._
import org.scale7.cassandra.pelops.{Cluster,Pelops,Selector, Bytes}
import collection.mutable.ListBuffer
import org.slf4j.LoggerFactory
import com.google.inject.name.Named
import com.google.inject.Inject
import java.lang.Class
import java.util.UUID
import org.scale7.cassandra.pelops.pool.CommonsBackedPool
import org.scale7.cassandra.pelops.OperandPolicy

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
     * Gets the supplied properties for this record
     */
    def getSelected(uuid:String, entityName:String, propertyNames:Array[String]):Option[Map[String,String]]

    /**
     * Get a key value pair map for this record.
     */
    def get(uuid:String, entityName:String): Option[Map[String, String]]

    /**
     * Get a key value pair map for this column timestamps of this record.
     */
    def getColumnsWithTimestamps(uuid:String, entityName:String): Option[Map[String, Long]]
    
    /**
     * Gets KVP map for a record based on a value in an index
     */
    def getByIndex(uuid:String, entityName:String, idxColumn:String) : Option[Map[String,String]]

    /**
     * Gets a single property based on an indexed value.  Returns the value of the "first" matched record.
     */
    def getByIndex(uuid:String, entityName:String, idxColumn:String, propertyName:String) :Option[String]

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
    def pageOverAll(entityName:String, proc:((String, Map[String,String])=>Boolean),startUuid:String="",endUuid:String="", pageSize:Int = 1000)

    /**
     * Page over the records, retrieving the supplied columns only.
     */
    def pageOverSelect(entityName:String, proc:((String, Map[String,String])=>Boolean), startUuid:String, endUuid:String, pageSize:Int, columnName:String*)

    /**
     * Page over the records, retrieving the supplied columns range.
     */
    def pageOverColumnRange(entityName:String, proc:((String, Map[String,String])=>Boolean), startUuid:String="", endUuid:String="", pageSize:Int=1000, startColumn:String="", endColumn:String="")
    
    /**
     * Select the properties for the supplied record UUIDs
     */
    def selectRows(uuids:Array[String],entityName:String,propertyNames:Array[String],proc:((Map[String,String])=>Unit))

    /**
     * The column to delete.
     */
    def deleteColumns(uuid:String, entityName:String, columnName:String*)
    
    /**
     * Delete row 
     */
    def delete(uuid:String, entityName:String)

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
    @Named("cassandraKeyspace") val keyspace:String = "occ",
    @Named("cassandraMaxConnections")val maxConnections:Int= -1,
    @Named("cassandraMaxRetries") val maxRetries:Int= 3,
    @Named("thriftOperationTimeout") val operationTimeout:Int= 4000) extends PersistenceManager {

    protected val logger = LoggerFactory.getLogger("CassandraPersistenceManager")

    val maxColumnLimit = 10000
    import JavaConversions._

    val cluster = new Cluster(host,port,operationTimeout, false)
    val policy = new CommonsBackedPool.Policy()
    policy.setMaxTotal(maxConnections)
    //According to Pelops : As a general rule the pools maxWaitForConnection should be three times larger than the thrift timeout value.
    policy.setMaxWaitForConnection(3 * operationTimeout);
    //operations policy, first arg indicates how many time a failed operation will be retried, the second indicates that null value insert should be treated as a delete
    val operandPolicy = new OperandPolicy(maxRetries,false)

    initialise     //setup DB connections

    def initialise {
      logger.debug("Initialising cassandra connection pool with pool name: " + poolName)
      logger.debug("Initialising cassandra connection pool with hosts: " + host)
      logger.debug("Initialising cassandra connection pool with port: " + port)
      logger.debug("Initialising cassandra connection pool with max connections: " + maxConnections)
      logger.debug("Initialising cassandra connection pool with max retries: " + maxRetries)
      logger.debug("Initialising cassandra connection pool with operation timeout: " + operationTimeout)

      //Cluster wide settings including the thrift operation timeout
      Pelops.addPool(poolName, cluster, keyspace, policy, operandPolicy)
    }
    


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
            case e:Exception => logger.trace(e.getMessage, e); None   //this is expected behaviour where no value exists
        }
    }
    /**
     * Retrieves a range of columns for the supplied uuid from the specified entity
     */
    def get(uuid:String, entityName:String, startProperty:String, endProperty:String):Option[java.util.List[Column]]={
      val selector = Pelops.createSelector(poolName)
      val slicePredicate = Selector.newColumnsPredicate(startProperty,endProperty,false,maxColumnLimit)
      try {
        Some(selector.getColumnsFromRow(entityName, uuid, slicePredicate, ConsistencyLevel.ONE))
      }
      catch {
        case e:Exception => None
      }
    }
    /**
     * Retrieves a list of columns and the last time in ms that they were modified.
     * 
     * This will support removing columns that were not updated during a reload
     */
    def getColumnsWithTimestamps(uuid:String, entityName:String): Option[Map[String, Long]]={
        val selector = Pelops.createSelector(poolName)
        val slicePredicate = Selector.newColumnsPredicateAll(true, maxColumnLimit)
        try {
            val columnList = selector.getColumnsFromRow(entityName,uuid, slicePredicate, ConsistencyLevel.ONE)
            if(columnList.isEmpty){
                None
            } else {
                Some(columnList2TimeMap(columnList))
            }
        } catch {
            case e:Exception => logger.debug(e.getMessage, e); None
        }
    }

    /**
     * Retrieve an array of objects, parsing the JSON stored.
     *
     * We are storing rows keyed against unique id's that we don't wish to expose
     * to users. We wish to expose a static UUID to the uses. This UUID will be
     * indexed against thus is queryable.
     *
     * The performance of the index is slightly worse that lookup by key. This should
     * be alright because the index should only be hit for reads via webapp.
     *
     */
    def getByIndex(uuid:String, entityName:String, idxColumn:String) : Option[Map[String,String]]={
        getFirstValuesFromIndex(entityName, idxColumn, uuid, Selector.newColumnsPredicateAll(true, maxColumnLimit))
        
    }
    /**
     * Retrieves a specific property value using the index as the retrieval method.
     */
  def getByIndex(uuid:String, entityName:String, idxColumn:String, propertyName:String) = {
        val map = getFirstValuesFromIndex(entityName, idxColumn, uuid, Selector.newColumnsPredicate(propertyName))
        if(map.isEmpty)
          None
        else
          Some(map.get.getOrElse(propertyName, ""))
  }

  /**
   * Retrieves the first record from the index that matches the value.
   *
   * In the use case for the biocache index there will only erve be one record for each value.
   */
  def getFirstValuesFromIndex(entityName:String, idxColumn:String, value:String, slicePredicate:SlicePredicate) : Option[Map[String,String]] ={
    val selector = Pelops.createSelector(poolName)
    //set up the index clause information
    val indexClause = Selector.newIndexClause(value, 1, Selector.newIndexExpression(idxColumn, IndexOperator.EQ, Bytes.fromUTF8(value) ))
    try{

      val columnMap = selector.getIndexedColumns(entityName, indexClause,slicePredicate, ConsistencyLevel.ONE)
      if(columnMap != null && !columnMap.isEmpty){
        val columnList = columnMap.entrySet.iterator.next.getValue.asInstanceOf[java.util.List[Column]]
        val map = columnList2Map(columnList)
        Some(map)
      }
      else
        None
    } catch{
      case e:Exception => logger.warn(e.getMessage, e); None
    }
  }
  /**
   def lookupByIndex(uuid:String, entityName:String){
    val selector = Pelops.createSelector(poolName)
    try{
      val results = selector.getIndexedColumns(entityName, Selector.newIndexClause(uuid, 1, Selector.newIndexExpression("uuid", IndexOperator.EQ, Bytes.fromUTF8(uuid) )), false, ConsistencyLevel.ONE)
       val columnsObj = List(results.keySet.toArray : _*)
        //convert to scala List
        val keys = columnsObj.asInstanceOf[List[Bytes]]
        val colList = results.get(keys.last)
        columnList2Map(colList)
    }
    catch{
      case e:Exception => e.printStackTrace
    }
  }
   */

    /**
     * Retrieve the column value, handling NotFoundExceptions from cassandra thrift.
     * 
     * NC:2013-05-10: we want to throw the exception when any other exception is received.  This is so that errors are
     * not swallowed in the guise of being a "NotFoundException". Thus allowing us to terminate a load midcycle.
     * 
     */
    def get(uuid:String, entityName:String, propertyName:String) = {
      //logger.info("Getting " + propertyName)
      try {
          val selector = Pelops.createSelector(poolName)
          val column = selector.getColumnFromRow(entityName, uuid, propertyName, ConsistencyLevel.ONE)
          Some(new String(column.getValue, "UTF-8"))
      } catch {
          case e:org.scale7.cassandra.pelops.exceptions.NotFoundException => None   //this is epected behaviour with cassandra
          case e:Exception =>e.printStackTrace();throw e
      }
    }
    /**
     * Only retrieves the supplied fields for the record.
     */
    def getSelected(uuid:String, entityName:String, propertyNames:Array[String]):Option[Map[String,String]] ={
      val selector = Pelops.createSelector(poolName)
        val slicePredicate = Selector.newColumnsPredicate(propertyNames:_*)
        try {
            val columnList = selector.getColumnsFromRow(entityName,uuid, slicePredicate, ConsistencyLevel.ONE)
            if(columnList.isEmpty){
                None
            } else {
                Some(columnList2Map(columnList))
            }
        } catch {
            case e:Exception => logger.trace(e.getMessage, e); None   //this is expected behaviour where no value exists
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
     * Stores the supplied map of values
     */
    def put(uuid:(String,String),entityName:String, keyValuePairs:Map[String,String]){
      val mutator = Pelops.createMutator(poolName)
      throw new RuntimeException("Method NOT implemented..")
    }

    /**
     * Store the supplied map of properties as separate columns in cassandra.
     */
    def put(uuid:String, entityName:String, keyValuePairs:Map[String, String]) = {

        val recordId = { if(uuid != null) uuid else UUID.randomUUID.toString }

        val mutator = Pelops.createMutator(poolName)
        keyValuePairs.foreach( keyValue => {
          //NC: only add the column if the value is not null
          if(keyValue._2!=null){
            mutator.writeColumn(entityName, recordId, mutator.newColumn(keyValue._1, keyValue._2))
          }
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
     *  We need to ignore empty rows if the SlicePredicate id for ALL columns.  This is configuration in Cassandra 0.8.8:
     *  https://issues.apache.org/jira/browse/CASSANDRA-2855
     *  
     *
     * @param startUuid, The uuid of the occurrence at which to start the paging
     */
    def pageOver(entityName:String,proc:((String, Map[String,String])=>Boolean), pageSize:Int,
                 slicePredicate:SlicePredicate, checkEmpty:Boolean=false,startUuid:String="",endUuid:String="") = {
     
      var startKey = new Bytes(startUuid.getBytes)
      var endKey = new Bytes(endUuid.getBytes)
      var keyRange = Selector.newKeyRange(startKey, endKey, pageSize+1)
      var hasMore = true
      var counter = 0
      //Please note we are not paging by UTF8 because it is much slower
      var continue = true
      var columnMap = getColumnsFromRowsWithRetries(entityName, keyRange, slicePredicate, ConsistencyLevel.ONE, 10)

      while (!columnMap.isEmpty && continue) {
        val columnsObj = List(columnMap.keySet.toArray : _*)
        //convert to scala List
        val keys = columnsObj.asInstanceOf[List[Bytes]]
        startKey = keys.last
        keys.foreach(buuid =>{
          val columnList = columnMap.get(buuid)
          if(!checkEmpty || !columnList.isEmpty){
              //procedure a map of key value pairs
              val map = columnList2Map(columnList)
              //more efficient to use a stored version of the rowKey then attempt to convert the buuid
              val uuid = map.getOrElse("rowKey", buuid.toUTF8)              
              //pass the record ID and the key value pair map to the proc
              continue = proc(uuid, map)
          }
        })
        counter += keys.size
        keyRange = Selector.newKeyRange(startKey, endKey, pageSize+1)
        columnMap = getColumnsFromRowsWithRetries(entityName, keyRange, slicePredicate, ConsistencyLevel.ONE, 10)
        columnMap.remove(startKey)
      }

      if(counter > 0) logger.debug("Finished paging. Records paged over : "+counter)
    }

   /**
    * Retrieve a list of records as maps of columns.
    */
    private def getColumnsFromRowsWithRetries(entityName:String, keyRange:KeyRange, slicePredicate:SlicePredicate,
               cl:ConsistencyLevel, permittedRetries:Int) : java.util.Map[Bytes, java.util.List[Column]] = {
      var success = false
      var noOfRetries = 0
      var columnMap:java.util.Map[Bytes, java.util.List[Column]] = null
      while(!success){
        try {
          val selector = Pelops.createSelector(poolName)
          columnMap = selector.getColumnsFromRows(entityName, keyRange, slicePredicate, cl)
          success = true
        } catch {
          case ir:org.scale7.cassandra.pelops.exceptions.InvalidRequestException => {
            logger.debug("Invalid key range supplied to cassandra: " + ir.getMessage, ir)
            success = true
            columnMap = new HashMap[Bytes, java.util.List[Column]]
          }
          case e:Exception => {
            logger.debug("Problem retrieving data. Number of retries left:" + (permittedRetries - noOfRetries) +
              ", Error: " + e.getMessage)
            Thread.sleep(20000)
            //Don't remove the pool because all requests while the reinit happens will result in NPE.
            //Pelops.removePool(poolName)
            initialise //re-initialise
            if (noOfRetries == permittedRetries){
              logger.error("Problem retrieving data. Number of DB connection retries exceeeded. Error: " + e.getMessage)
              e.printStackTrace()
              throw new RuntimeException(e)
            }
            noOfRetries += 1
          }
        }
      }
      columnMap
    }

    /**
     * Pages over all the records with the selected columns.
     * @param columnName The names of the columns that need to be provided for processing by the proc
     */
    def pageOverSelect(entityName:String, proc:((String, Map[String,String])=>Boolean), startUuid:String, endUuid:String, pageSize:Int, columnName:String*){
      val slicePredicate = Selector.newColumnsPredicate(columnName:_*)
      pageOver(entityName, proc, pageSize, slicePredicate, startUuid=startUuid, endUuid=endUuid)
    }
    /**
     * Pages over the records returns the columns that fit within the startColumn and endColumn range
     */
    def pageOverColumnRange(entityName:String, proc:((String, Map[String,String])=>Boolean), startUuid:String="", endUuid:String="", pageSize:Int=1000, startColumn:String="", endColumn:String=""){
      val slicePredicate = Selector.newColumnsPredicate(startColumn, endColumn, false, maxColumnLimit)
      //can't use this because we want to page of the column range too just in case the number of columns > than maxColumnLimit 
      //pageOver(entityName, proc, pageSize, slicePredicate,true,startUuid=startUuid, endUuid=endUuid)
      var startKey = new Bytes(startUuid.getBytes)
      var endKey = new Bytes(endUuid.getBytes)
      var keyRange = Selector.newKeyRange(startKey, endKey, pageSize+1)
      var hasMore = true
      var counter = 0
      //Please note we are not paging by UTF8 because it is much slower
      var continue = true
      var columnMap = getColumnsFromRowsWithRetries(entityName, keyRange, slicePredicate, ConsistencyLevel.ONE, 10)

      while (!columnMap.isEmpty && continue) {
        val columnsObj = List(columnMap.keySet.toArray : _*)
        //convert to scala List
        val keys = columnsObj.asInstanceOf[List[Bytes]]
        startKey = keys.last
        keys.foreach(buuid =>{
          val columnList = columnMap.get(buuid)
          
          //now get the remaining columns for the record
              var moreCols = true
              val uuid = buuid.toUTF8()
              var startCol = new String(columnList.get(columnList.size-1).getName(),"UTF-8")
              while(moreCols){
                var nextCols = get(uuid, entityName,startCol,endColumn)
                if(nextCols.isDefined){
                    nextCols.get.remove(0) //remove the repeated item from the last set of columns
                    if(nextCols.get.size >0){
                      columnList.addAll(nextCols.get)
                      startCol = new String(columnList.get(columnList.size-1).getName(),"UTF-8")
                    }
                    else
                      moreCols = false;
                }
              }
          
          if(!columnList.isEmpty){
              //procedure a map of key value pairs
              var map = columnList2Map(columnList)              
                           
              //pass the record ID and the key value pair map to the proc
              continue = proc(uuid, map)
          }
        })
        counter += keys.size
        keyRange = Selector.newKeyRange(startKey, endKey, pageSize+1)
        columnMap = getColumnsFromRowsWithRetries(entityName, keyRange, slicePredicate, ConsistencyLevel.ONE, 10)
        columnMap.remove(startKey)
      }

      if(counter > 0) logger.debug("Finished paging. Records paged over : "+counter)
    }

    /**
     * Iterate over all occurrences, passing the objects to a function.
     * Function returns a boolean indicating if the paging should continue.
     *
     * @param proc
     * @param startUuid, The uuid of the occurrence at which to start the paging
     */
    def pageOverAll(entityName:String, proc:((String, Map[String,String])=>Boolean),startUuid:String="", endUuid:String="", pageSize:Int = 1000) {
      val slicePredicate = Selector.newColumnsPredicateAll(true, maxColumnLimit)
      pageOver(entityName, proc, pageSize, slicePredicate,true,startUuid=startUuid, endUuid=endUuid)
    }

    /**
     * Select fields from rows and pass to the supplied function.
     */
    def selectRows(rowkeys:Array[String], entityName:String, fields:Array[String], proc:((Map[String,String])=>Unit)) {
       val selector = Pelops.createSelector(poolName)
       val slicePredicate = Selector.newColumnsPredicate(fields:_*)

       //retrieve the columns
       var columnMap = selector.getColumnsFromRowsUtf8Keys(entityName, rowkeys.toList, slicePredicate, ConsistencyLevel.ONE)

       //write them out to the output stream
       val keys = List(columnMap.keySet.toArray : _*)

       //identify el* cl* fields
       //val locFields = fields.filter( a => a.startsWith("el") || a.startsWith("cl") )

       keys.foreach(key =>{
         val columnsList = columnMap.get(key)
         val fieldValues = columnsList.map(column => (new String(column.getName, "UTF-8"),new String(column.getValue, "UTF-8"))).toArray
         val map = scala.collection.mutable.Map.empty[String,String]
         fieldValues.foreach(fieldValue =>  map(fieldValue._1) = fieldValue._2)

         //add el* cl* fields - this is handled at the higher level.  NB environment and contexual layers are included in the occ columnFamily
         //so there is no need to look them up in the loc columnFamily.
//         if(!locFields.isEmpty) {
//           val locSome = get(map.getOrElse("decimalLatitude.p","") + "|" + map.getOrElse("decimalLongitude.p",""),"loc")
//           if (locSome != None) {
//             val locMap = locSome.get
//             locFields.foreach(lf => if(locMap.contains(lf)) map += (lf -> locMap(lf)))
//           }
//         }
         proc(map.toMap) //pass the map to the function for processing
       })
     }

    /**
     * Convert a set of cassandra columns into a key-value pair map.
     */
    protected def columnList2Map(columnList:java.util.List[Column]) : Map[String,String] = {
      val map = new HashMap[String, String]
      val iter = columnList.iterator()
      while(iter.hasNext){
        val c = iter.next()
        map.put(new String(c.getName, "UTF-8"), new String(c.getValue, "UTF-8"))
      }
      map.toMap
    }
    /**
     * Converts a set of cassandra columns to a column name to last modified map.
     * 
     *  This will support the removal of columns that were not updated during a new load
     *  
     *  NB Pelops provides timestamps in microseconds thus values will be /1000 and returned in milliseconds
     *  
     */
    protected def columnList2TimeMap(columnList:java.util.List[Column]) :Map[String,Long]={
      val map = new HashMap[String, Long]
      val iter = columnList.iterator()
      while(iter.hasNext){
        val c = iter.next()
        map.put(new String(c.getName, "UTF-8"), c.getTimestamp()/1000)
      }
      map.toMap
    }

  /**
   * Convienience method for accessing values.
   */
  protected def getColumn(uuid: String, columnFamily: String, columnName: String): Option[Column] = {
    try {
      val selector = Pelops.createSelector(poolName)
      Some(selector.getColumnFromRow(columnFamily, uuid, columnName, ConsistencyLevel.ONE))
    } catch {
      case e: Exception => {
        logger.debug(e.getMessage + " for " + uuid + " - " + columnFamily + " - " + columnName)
        None //expected behaviour when row doesnt exist
      }
    }
  }

    def shutdown = {
        try {
        	Pelops.shutdown
        } catch {
            case e:Exception => e.printStackTrace()
        }
    }
    
    /**
     * Delete the value for the supplied column 
     */
    def deleteColumns(uuid:String, entityName:String, columnName:String*)={
      if(uuid != null && entityName != null && columnName != null){
        val mutator = Pelops.createMutator(poolName)
        mutator.deleteColumns(entityName, uuid, columnName:_*)
        mutator.execute(ConsistencyLevel.ONE)
      }
    }
    /**
     * Removes the record for the supplied uuid from entityName.
     */
    def delete(uuid:String, entityName:String)={
        if(uuid != null && entityName != null){
            val deletor =Pelops.createRowDeletor(poolName)
            deletor.deleteRow(entityName, uuid, ConsistencyLevel.ONE)
        }
    }
    
}


///**
// * To be added.....
// */
//class MongoDBPersistenceManager @Inject()(
//    @Named("mongoHost") host:String = "localhost", @Named("mongoDatabase") db:String = "occ", @Named("padOcc") pad:Boolean=false) extends PersistenceManager {
//
//    import JavaConversions._
//    val uuidColumn = "_id"
//    val mongoConn = MongoConnection(host)
//    val largeMap = Map("DUMMY"-> "X" *10000)
//
//    override def fieldDelimiter = '_'
//
//    def get(uuid: String, entityName: String) = {
//        val mongoColl = mongoConn(db)(entityName)
//        val q = MongoDBObject(uuidColumn -> uuid)
//        val result = mongoColl.findOne(q)
//        if(!result.isEmpty){
//            Some(result.get.toMap.map({ case(key,value) => (key.toString, value.toString) }).toMap)
//        } else {
//            None
//        }
//    }
//
//    def get(uuid: String, entityName: String, propertyName: String) = {
//        val mongoColl = mongoConn(db)(entityName)
//        val query = MongoDBObject(uuidColumn -> uuid)
//        val fields = MongoDBObject(propertyName -> 1)
//        val map = mongoColl.findOne(query,fields)
//        if(!map.isEmpty){
//            Some(map.get.toMap.get(propertyName).asInstanceOf[String])
//        } else {
//            None
//        }
//    }
//
//    def getList[A](uuid: String, entityName: String, propertyName: String, theClass:java.lang.Class[_]) = {
//
//        val mongoColl = mongoConn(db)(entityName)
//        val query = MongoDBObject(uuidColumn -> uuid)
//        val fields = MongoDBObject(propertyName -> 1)
//        val map = mongoColl.findOne(query,fields)
//        val propertyInJSON = map.get(propertyName).asInstanceOf[String]
//        if(propertyInJSON.isEmpty){
//            List()
//        } else {
//            Json.toListWithGeneric(propertyInJSON,theClass)
//        }
//    }
//
//    def put(uuid: String, entityName: String, propertyName: String, propertyValue: String) = {
//
//        val mongoColl = mongoConn(db)(entityName)
//        if(uuid!=null){
//            val set = $set( (propertyName,propertyValue) )
//            //Allow "upserts" so that missing records are inserted...
//            mongoColl.update(Map(uuidColumn -> uuid), set, true, false)
//            uuid
//        } else {
//            val recordId = { if(uuid != null) uuid else UUID.randomUUID.toString }
//            val mongoColl = mongoConn(db)(entityName)
//            mongoColl.save(Map(uuidColumn -> recordId,propertyName -> propertyValue ))
//            recordId
//        }
//
//
////        val mongoColl = mongoConn(db)(entityName)
////        if(uuid != null){
////            val set = $set( (propertyName,propertyValue) )
////            mongoColl.update(Map(uuidColumn -> recordId), set, false, false)
////            uuid
////        } else {
////            val newInsert = Map(propertyName -> propertyValue).asDBObject
////            val writeResult = mongoColl.insert(newInsert)
////            if(newInsert._id.isEmpty){
////                throw new RuntimeException("Insert failed.")
////            } else {
////                newInsert._id.get.toString
////            }
////        }
//    }
//
//    def put(uuid: String, entityName: String, keyValuePairs: Map[String, String]) = {
//
//        val mongoColl = mongoConn(db)(entityName)
//        if(uuid!=null){
//            val mapToSave = keyValuePairs.filter( { case (key, value) => { value!=null && !value.trim.isEmpty } })
//            if(!mapToSave.isEmpty){
//              //We need to wrap it in a $set to allow the existing values for the document to remain unchanged
//              val setToSave = $set(mapToSave.toList:_*)
//
//              //Allow "upserts" so that missing records are inserted...
//              mongoColl.update(Map(uuidColumn -> uuid),setToSave, true, false)
//
//            }
//            uuid
//        } else {
//
//            val recordId = { if(uuid != null) uuid else UUID.randomUUID.toString }
//            val mongoColl = mongoConn(db)(entityName)
//            if(entityName == "occ" && pad){
//              mongoColl.save(Map(uuidColumn -> recordId) ++ keyValuePairs ++ largeMap)
//              //now remove the padding from the record
//              mongoColl.update(Map(uuidColumn->recordId), $unset("DUMMY"))
//            }
//            else{
//              mongoColl.save(Map(uuidColumn -> recordId) ++ keyValuePairs)
//            }
//
//
//            recordId
//        }
////        val mongoColl = mongoConn(db)(entityName)
////        if(uuid != null){
////            mongoColl.update(Map(uuidColumn -> uuid), keyValuePairs.asDBObject, false, false)
////            uuid
////        } else {
////            val newInsert = keyValuePairs.asDBObject
////            val writeResult = mongoColl.insert(newInsert)
////            if(newInsert._id.isEmpty){
////                throw new RuntimeException("Insert failed.")
////            } else {
////                newInsert._id.get.toString
////            }
////        }
//    }
//
//    def putList[A](uuid: String, entityName: String, propertyName: String, objectList: List[A], theClass:java.lang.Class[_], overwrite: Boolean) = {
//
//        if(!overwrite){
//            throw new RuntimeException("Overwrite currently not supported.")
//        }
//
//        //TODO support append (overwrite = false)
//        val mongoColl = mongoConn(db)(entityName)
//        val json = Json.toJSONWithGeneric(objectList)
//        if(uuid !=null){
//            val set = $set( (propertyName,json) )
//            mongoColl.update(Map(uuidColumn -> uuid), set, false, false)
//            uuid
//        } else {
//            val recordId = { if(uuid != null) uuid else UUID.randomUUID.toString }
//            mongoColl.save(Map(uuidColumn -> recordId, propertyName -> json) )
//            recordId
//        }
////        if(uuid != null){
////            val set = $set( (propertyName,json) )
////            mongoColl.update(Map(uuidColumn -> uuid), set, false, false)
////            uuid
////        } else {
////
////            val newInsert = Map(propertyName -> json).asDBObject
////            val writeResult = mongoColl.insert(newInsert)
////            if(newInsert._id.isEmpty){
////                throw new RuntimeException("Insert failed.")
////            } else {
////                newInsert._id.get.toString
////            }
////        }  .
//    }
//
//    def putBatch(entityName: String, batch: Map[String, Map[String, String]]) = {
//        throw new RuntimeException("currently not implemented")
//    }
//
//    def deleteColumns(uuid:String, entityName:String, variableName:String*)={
//      throw new RuntimeException("currently not implemented")
//    }
//    def delete(uuid:String, entityName:String)={
//        throw new RuntimeException("currently not implemented")
//    }
//    def pageOverSelect(entityName: String, proc: (String, Map[String, String]) => Boolean, startUuid:String, endUuid:String, pageSize: Int, variableName: String*) = {
//
//        //page through all records
//        val mongoColl = mongoConn(db)(entityName)
//
//        var counter = 0
//        val fields = MongoDBObject( List() )
//        //val fields = MongoDBObject( variableName.map(x => { counter += 1; (variableName -> counter) } ).toSeq )
//        val cursor = mongoColl.find(MongoDBObject(),fields,0,pageSize)
//
//        //val cursor = mongoColl.find
//        for(dbObject:DBObject <- cursor){
//            val map = dbObject.toMap.map({ case(key,value) => (key.toString, value.toString) }).toMap
//            proc(map.getOrElse(uuidColumn, ""), map)
//        }
//    }
//
//    def pageOverAll(entityName: String, proc: (String, Map[String, String]) => Boolean,startUuid:String="", endUuid:String="", pageSize: Int) = {
//        //page through all records
//        val mongoColl = mongoConn(db)(entityName)
//        //val cursor = mongoColl.find(0,pageSize)
//        //Take a snapshot so that each document is only returned once
//        val cursor = mongoColl.find.snapshot//sort(MongoDBObject("_id"->1))//.snapshot
//        for(dbObject:DBObject <- cursor){
//            val map = dbObject.toMap.map({ case(key,value) => (key.toString, value.toString) }).toMap
//            //println("ID: " + map.get("_id").get)
//            proc(map.getOrElse(uuidColumn, ""), map)
//        }
//    }
//
//    def selectRows(uuids: Array[String], entityName: String, propertyNames: Array[String], proc: (Map[String, String]) => Unit) = {
//       throw new RuntimeException("currently not implemented")
//    }
//    def getByIndex(uuid:String, entityName:String, idxColumn:String) : Option[Map[String,String]]={
//       throw new RuntimeException("currently not implemented")
//    }
//
//    def getByIndex(uuid:String, entityName:String, idxColumn:String, propertyName:String) ={
//       throw new RuntimeException("currently not implemented")
//    }
//
//    def shutdown = mongoConn.close
//}